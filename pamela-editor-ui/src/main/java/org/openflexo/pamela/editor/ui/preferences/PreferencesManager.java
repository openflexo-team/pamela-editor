package org.openflexo.pamela.editor.ui.preferences;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openflexo.pamela.model.ModelEntity;
import org.openflexo.pamela.model.ModelProperty;

/**
 * Owns the application preferences model and implements the immediate-apply lifecycle
 * (decision D4, {@code preferences-design.md §5}):
 *
 * <ul>
 *   <li>builds the tree from {@link PreferencesRegistry} and overlays {@code preferences.json}
 *       (or, on first run, imports the legacy {@code java.util.prefs} values — §5.1);</li>
 *   <li>listens to every node and, on any value change, (a) saves the JSON file <em>debounced</em>
 *       off the EDT and (b) dispatches a {@link PreferenceChangeListener} event so the app reacts.</li>
 * </ul>
 *
 * <p>Singleton. {@link #initialize(Function)} should be called once at startup with the editor
 * localizer (for browser labels); a lazy identity-localizer init is used otherwise.</p>
 */
public final class PreferencesManager {

    private static final Logger logger = Logger.getLogger(PreferencesManager.class.getPackage().getName());

    private static final long SAVE_DEBOUNCE_MS = 500;

    private static PreferencesManager instance;

    private final PreferencesFactory factory;
    private final PreferencesSerializer serializer;
    private final PamelaEditorPreferencesModel model;
    private final File prefsFile;

    private final List<PreferenceChangeListener> listeners = new ArrayList<>();
    private final ScheduledExecutorService saveExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "preferences-save");
                t.setDaemon(true);
                return t;
            });
    private ScheduledFuture<?> pendingSave;

    /** Thematic JSON last written to / read from disk — the baseline for dirty checks. */
    private com.fasterxml.jackson.databind.JsonNode savedSnapshot;

    private final PropertyChangeListener nodeListener = this::onNodePropertyChange;
    private final Function<String, String> localizer;

    private PreferencesManager(Function<String, String> localizer) throws Exception {
        this.localizer = localizer;
        PreferencesRegistry.registerBuiltinThemes();
        this.factory = new PreferencesFactory();
        this.serializer = new PreferencesSerializer(factory);
        this.prefsFile = defaultPreferencesFile();
        this.model = PreferencesRegistry.buildTree(factory, localizer);

        boolean firstRun = !prefsFile.exists();
        if (firstRun) {
            LegacyPreferencesImporter.importInto(this);
        }
        else {
            serializer.applyJson(model, prefsFile);
        }
        // Seed the built-in connector styles when none are present (first run, or a pre-existing
        // file that predates the connector-styles theme).
        boolean seeded = ConnectorStyleDefaults.seedIfEmpty(connectors(), factory);
        if (firstRun || seeded) {
            saveNow();
        }
        updateSavedSnapshot();
        attachListeners(model);
    }

    private void updateSavedSnapshot() {
        savedSnapshot = serializer.toThematicJsonTree(model);
    }

    // ----------------------------------------------------------------- singleton

    public static synchronized void initialize(Function<String, String> localizer) {
        if (instance == null) {
            try {
                instance = new PreferencesManager(localizer != null ? localizer : Function.identity());
            }
            catch (Exception e) {
                throw new RuntimeException("Could not initialize preferences", e);
            }
        }
    }

    public static synchronized PreferencesManager getInstance() {
        if (instance == null) {
            initialize(Function.identity());
        }
        return instance;
    }

    // ----------------------------------------------------------------- accessors

    public PamelaEditorPreferencesModel getModel() {
        return model;
    }

    public PreferencesFactory getFactory() {
        return factory;
    }

    public File getPreferencesFile() {
        return prefsFile;
    }

    public GeneralPreferences general() {
        return (GeneralPreferences) PreferencesRegistry.resolve(model, "/general");
    }

    public WindowPreferences window() {
        return (WindowPreferences) PreferencesRegistry.resolve(model, "/general/window");
    }

    public RecentFilesPreferences recent() {
        return (RecentFilesPreferences) PreferencesRegistry.resolve(model, "/general/recent");
    }

    public ClassDiagramDesignPreferences classDiagramDesign() {
        return (ClassDiagramDesignPreferences) PreferencesRegistry.resolve(model, "/classDiagramDesign");
    }

    public EntityStylePreferences entityStyle() {
        return (EntityStylePreferences) PreferencesRegistry.resolve(model, "/classDiagramDesign/entities");
    }

    public ConnectorStylePreferences connectors() {
        return (ConnectorStylePreferences) PreferencesRegistry.resolve(model, "/classDiagramDesign/connectors");
    }

    public AnalysisPreferences analysis() {
        return (AnalysisPreferences) PreferencesRegistry.resolve(model, "/analysis");
    }

    // ----------------------------------------------------------------- listeners

    public void addPreferenceChangeListener(PreferenceChangeListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removePreferenceChangeListener(PreferenceChangeListener l) {
        listeners.remove(l);
    }

    // ----------------------------------------------------------------- internals

    /** Objects already carrying our change listener (identity), to avoid double-registration. */
    private final java.util.Set<Object> listenedObjects =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /**
     * Attaches the change listener to every node of the <em>live</em> model (for app-side
     * dispatch and the app-state auto-persist). The dialog edits a separate working copy that
     * is <b>not</b> listened, so editing a preference is not applied until {@code Apply}.
     */
    private void attachListeners(PreferencesNode node) {
        listenTo(node);
        for (PreferencesNode child : node.getChildren()) {
            attachListeners(child);
        }
    }

    private void listenTo(org.openflexo.pamela.AccessibleProxyObject obj) {
        if (obj != null && listenedObjects.add(obj)) {
            obj.getPropertyChangeSupport().addPropertyChangeListener(nodeListener);
        }
    }

    /** Application state (window geometry, recent files) auto-persists; thematic prefs do not. */
    private static boolean isAppState(PreferencesNode node) {
        return node instanceof WindowPreferences || node instanceof RecentFilesPreferences;
    }

    private void onNodePropertyChange(PropertyChangeEvent evt) {
        String key = evt.getPropertyName();
        if (PreferencesNode.CHILDREN.equals(key) || PreferencesNode.PARENT.equals(key)) {
            return; // structural — not a value change
        }
        if (!(evt.getSource() instanceof PreferencesNode)) {
            return;
        }
        PreferencesNode node = (PreferencesNode) evt.getSource();
        for (PreferenceChangeListener l : new ArrayList<>(listeners)) {
            try {
                l.preferenceChanged(node, key, evt.getOldValue(), evt.getNewValue());
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "Preference listener failed for " + node.getPath() + "#" + key, e);
            }
        }
        // Only application state is persisted automatically (outside the dialog's Apply/Save).
        if (isAppState(node)) {
            requestPersistAppState();
        }
    }

    // ----------------------------------------------------------------- dialog lifecycle (Apply/Save/Cancel/Reset)

    /** A fresh, detached working copy of the model for the preferences dialog to edit. */
    public PamelaEditorPreferencesModel createWorkingCopy() {
        PamelaEditorPreferencesModel wc = PreferencesRegistry.buildTree(factory, localizer);
        serializer.copyValues(model, wc);
        return wc;
    }

    /** <b>Apply</b>: push the working copy's values into the live model (no file write). */
    public void applyWorkingCopy(PamelaEditorPreferencesModel workingCopy) {
        if (workingCopy != null) {
            serializer.copyValues(workingCopy, model);
        }
    }

    /** <b>Save</b>: apply the working copy, then persist the whole model to disk. */
    public void save(PamelaEditorPreferencesModel workingCopy) {
        applyWorkingCopy(workingCopy);
        saveNow();
    }

    /** <b>Cancel</b>: revert the live model to the last saved file (un-applies unsaved changes). */
    public void cancelToSaved() {
        try {
            if (prefsFile.exists()) {
                serializer.applyJson(model, prefsFile);
            }
            ConnectorStyleDefaults.seedIfEmpty(connectors(), factory);
            updateSavedSnapshot();
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Could not revert preferences to the saved file", e);
        }
    }

    // ---- Dirty / state queries for the dialog's button enablement -------------------------

    /** <b>Apply</b> is meaningful when the working copy's thematic values differ from the live model. */
    public boolean isModified(PamelaEditorPreferencesModel workingCopy) {
        return workingCopy != null
                && !serializer.toThematicJsonTree(workingCopy).equals(serializer.toThematicJsonTree(model));
    }

    /** <b>Save</b> is meaningful when the working copy's thematic values differ from what is on disk. */
    public boolean isSavable(PamelaEditorPreferencesModel workingCopy) {
        return workingCopy != null
                && (savedSnapshot == null || !serializer.toThematicJsonTree(workingCopy).equals(savedSnapshot));
    }

    /** <b>Cancel</b> is meaningful when the working copy or the live model deviates from the saved file. */
    public boolean isRevertable(PamelaEditorPreferencesModel workingCopy) {
        boolean liveDirty = savedSnapshot != null
                && !serializer.toThematicJsonTree(model).equals(savedSnapshot);
        return isSavable(workingCopy) || liveDirty;
    }

    /** Whether the node's own values already equal its programmatic defaults (Reset would be a no-op). */
    public boolean isNodeAtDefaults(PreferencesNode node) {
        if (node == null) {
            return true;
        }
        try {
            ModelEntity<?> entity = factory.getModelEntityForInstance(node);
            PreferencesNode fresh = (PreferencesNode) factory.newInstance(entity.getImplementedInterface());
            fresh.setName(node.getName());
            if (fresh instanceof ConnectorStylePreferences) {
                ConnectorStyleDefaults.seedIfEmpty((ConnectorStylePreferences) fresh, factory);
            }
            return serializer.scalarsToJson(node).equals(serializer.scalarsToJson(fresh));
        }
        catch (Exception e) {
            return true;
        }
    }

    /**
     * <b>ResetToDefault</b> for a single node (in the working copy): resets its own declared
     * preference properties to their programmatic defaults. Connector styles are re-seeded to
     * the built-in catalogue. No effect / not offered for nodes without resettable properties.
     */
    public void resetNodeToDefaults(PreferencesNode node) {
        if (node == null) {
            return;
        }
        if (node instanceof ConnectorStylePreferences) {
            ConnectorStylePreferences c = (ConnectorStylePreferences) node;
            for (ConnectorStylePreference s : new ArrayList<>(c.getStyles())) {
                c.removeFromStyles(s);
            }
            c.setDefaultInheritanceStyleId(null);
            c.setDefaultAssociationStyleId(null);
            ConnectorStyleDefaults.seedIfEmpty(c, factory);
            return;
        }
        try {
            ModelEntity<?> entity = factory.getModelEntityForInstance(node);
            for (ModelProperty<?> p : entity.getDeclaredProperties()) {
                String key = p.getPropertyIdentifier();
                if (PreferencesNode.NAME.equals(key) || PreferencesNode.CHILDREN.equals(key)
                        || PreferencesNode.PARENT.equals(key)) {
                    continue;
                }
                if (p.getCardinality() == org.openflexo.pamela.annotations.Getter.Cardinality.LIST) {
                    continue; // lists are not reset generically
                }
                node.setObjectForKey(p.getDefaultValue(factory), key);
            }
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Could not reset node to defaults: " + node.getPath(), e);
        }
    }

    /** Whether {@link #resetNodeToDefaults(PreferencesNode)} is meaningful for this node. */
    public boolean canResetNode(PreferencesNode node) {
        if (node == null) {
            return false;
        }
        if (node instanceof ConnectorStylePreferences) {
            return true;
        }
        try {
            ModelEntity<?> entity = factory.getModelEntityForInstance(node);
            for (ModelProperty<?> p : entity.getDeclaredProperties()) {
                String key = p.getPropertyIdentifier();
                if (PreferencesNode.NAME.equals(key) || PreferencesNode.CHILDREN.equals(key)
                        || PreferencesNode.PARENT.equals(key)) {
                    continue;
                }
                if (p.getCardinality() != org.openflexo.pamela.annotations.Getter.Cardinality.LIST) {
                    return true;
                }
            }
        }
        catch (Exception e) {
            return false;
        }
        return false;
    }

    /** Coalesces app-state writes ~{@value #SAVE_DEBOUNCE_MS} ms after the last change, off the EDT. */
    public synchronized void requestPersistAppState() {
        if (pendingSave != null) {
            pendingSave.cancel(false);
        }
        pendingSave = saveExecutor.schedule(this::saveAppStateNow, SAVE_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    private void saveAppStateNow() {
        try {
            serializer.saveAppState(model, prefsFile);
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Could not save application-state preferences to " + prefsFile, e);
        }
    }

    /** Persists the entire model to {@link #prefsFile} immediately (used by Save and first run). */
    public void saveNow() {
        try {
            serializer.save(model, prefsFile);
            updateSavedSnapshot();
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Could not save preferences to " + prefsFile, e);
        }
    }

    /** System property to override the preferences file location (used by tests). */
    public static final String PREFERENCES_FILE_PROPERTY = "pamela.editor.preferences.file";

    private static File defaultPreferencesFile() {
        String override = System.getProperty(PREFERENCES_FILE_PROPERTY);
        if (override != null && !override.isEmpty()) {
            return new File(override);
        }
        File dir = new File(System.getProperty("user.home"), ".pamela-editor");
        return new File(dir, "preferences.json");
    }

    // Test hook
    static synchronized void resetForTests() {
        if (instance != null) {
            instance.saveExecutor.shutdownNow();
        }
        instance = null;
    }
}
