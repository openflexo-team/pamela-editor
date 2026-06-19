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

    private final PropertyChangeListener nodeListener = this::onNodePropertyChange;

    private PreferencesManager(Function<String, String> localizer) throws Exception {
        PreferencesRegistry.registerBuiltinThemes();
        this.factory = new PreferencesFactory();
        this.serializer = new PreferencesSerializer(factory);
        this.prefsFile = defaultPreferencesFile();
        this.model = PreferencesRegistry.buildTree(factory, localizer);

        if (prefsFile.exists()) {
            serializer.applyJson(model, prefsFile);
        }
        else {
            LegacyPreferencesImporter.importInto(this);
            saveNow();
        }
        attachListeners(model);
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

    public DiagramPreferences diagram() {
        return (DiagramPreferences) PreferencesRegistry.resolve(model, "/diagram");
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

    /** Attaches the change listener to {@code node} and (recursively) its children. */
    private void attachListeners(PreferencesNode node) {
        node.getPropertyChangeSupport().addPropertyChangeListener(nodeListener);
        for (PreferencesNode child : node.getChildren()) {
            attachListeners(child);
        }
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
        requestSave();
    }

    /** Coalesces save requests: persists ~{@value #SAVE_DEBOUNCE_MS} ms after the last change, off the EDT. */
    public synchronized void requestSave() {
        if (pendingSave != null) {
            pendingSave.cancel(false);
        }
        pendingSave = saveExecutor.schedule(this::saveNow, SAVE_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    /** Persists the model to {@link #prefsFile} immediately (synchronously). */
    public void saveNow() {
        try {
            serializer.save(model, prefsFile);
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
