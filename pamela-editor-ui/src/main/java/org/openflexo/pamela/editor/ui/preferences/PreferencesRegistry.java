package org.openflexo.pamela.editor.ui.preferences;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Extensible registry of preference themes (decision D2, {@code preferences-design.md §2}).
 *
 * <p>Modules call {@link #register(PreferencesContribution)} <em>before</em> the
 * {@link PreferencesFactory} is built. The registry then provides the full set of entity
 * classes for the factory ({@link #allEntityClasses()}) and builds the tree structure
 * ({@link #buildTree(PreferencesFactory, Function)}). Values are overlaid from JSON afterwards
 * by {@link PreferencesSerializer}.</p>
 */
public final class PreferencesRegistry {

    private static final Logger logger = Logger.getLogger(PreferencesRegistry.class.getPackage().getName());

    private static final List<PreferencesContribution> CONTRIBUTIONS = new ArrayList<>();
    private static boolean builtinsRegistered = false;

    private PreferencesRegistry() {
    }

    /** Registers a theme contribution. The first registration at a given path wins. */
    public static synchronized void register(PreferencesContribution c) {
        for (PreferencesContribution existing : CONTRIBUTIONS) {
            if (existing.getPath().equals(c.getPath())) {
                logger.warning("Duplicate preferences contribution ignored for path " + c.getPath());
                return;
            }
        }
        CONTRIBUTIONS.add(c);
    }

    /** Registers the built-in themes shipped with {@code pamela-editor-ui} (idempotent). */
    public static synchronized void registerBuiltinThemes() {
        if (builtinsRegistered) {
            return;
        }
        builtinsRegistered = true;
        register(new PreferencesContribution(GeneralPreferences.class, "/", "general", 0,
                "preferences_general", "general", null));
        register(new PreferencesContribution(WindowPreferences.class, "/general", "window", 0,
                "preferences_window", "window", null));
        register(new PreferencesContribution(RecentFilesPreferences.class, "/general", "recent", 1,
                "preferences_recent", "recent", null));
        register(new PreferencesContribution(ClassDiagramDesignPreferences.class, "/", "classDiagramDesign", 1,
                "preferences_class_diagram_design", "diagram", null));
        register(new PreferencesContribution(EntityStylePreferences.class, "/classDiagramDesign", "entities", 0,
                "preferences_entities", "entity", null));
        register(new PreferencesContribution(ConnectorStylePreferences.class, "/classDiagramDesign", "connectors", 1,
                "preferences_connectors", "connector", null));
        register(new PreferencesContribution(AnalysisPreferences.class, "/", "analysis", 2,
                "preferences_analysis", "analysis", null));
    }

    public static synchronized List<PreferencesContribution> contributions() {
        return new ArrayList<>(CONTRIBUTIONS);
    }

    public static synchronized PreferencesContribution contributionForPath(String path) {
        for (PreferencesContribution c : CONTRIBUTIONS) {
            if (c.getPath().equals(path)) {
                return c;
            }
        }
        return null;
    }

    /** The model root class plus every registered theme class — the {@link PreferencesFactory} input. */
    public static synchronized Class<?>[] allEntityClasses() {
        Set<Class<?>> classes = new LinkedHashSet<>();
        classes.add(PamelaEditorPreferencesModel.class);
        for (PreferencesContribution c : CONTRIBUTIONS) {
            classes.add(c.getThemeClass());
        }
        return classes.toArray(new Class<?>[0]);
    }

    /**
     * Builds the preferences tree structure from the registered contributions (parents first),
     * creating each node with its PAMELA defaults and setting its transient display label / icon.
     * Values are <em>not</em> loaded here — see {@link PreferencesSerializer}.
     *
     * @param factory   the factory (must have been built with {@link #allEntityClasses()})
     * @param localizer maps a {@code labelKey} to a localized label (identity is acceptable in tests)
     * @return the freshly built (default-valued) root model
     */
    public static synchronized PamelaEditorPreferencesModel buildTree(PreferencesFactory factory,
                                                                      Function<String, String> localizer) {
        PamelaEditorPreferencesModel root = factory.newInstance(PamelaEditorPreferencesModel.class);
        root.setName("");

        List<PreferencesContribution> sorted = new ArrayList<>(CONTRIBUTIONS);
        // Parents must exist before their children: order by parent-path depth, then by declared order.
        sorted.sort(Comparator
                .comparingInt((PreferencesContribution c) -> depth(c.getParentPath()))
                .thenComparingInt(PreferencesContribution::getOrder));

        for (PreferencesContribution c : sorted) {
            PreferencesNode parent = resolve(root, c.getParentPath());
            if (parent == null) {
                logger.warning("No parent at " + c.getParentPath() + " for preferences theme " + c.getPath());
                continue;
            }
            PreferencesNode node = (PreferencesNode) factory.newInstance(c.getThemeClass());
            node.setName(c.getName());
            node.setDisplayLabel(localizer != null ? localizer.apply(c.getLabelKey()) : c.getLabelKey());
            node.setIconKey(c.getIconKey());
            parent.addToChildren(node);
        }
        return root;
    }

    /** Resolves a node by its absolute slash path within {@code root} ({@code "/"} = root). */
    static PreferencesNode resolve(PreferencesNode root, String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return root;
        }
        PreferencesNode current = root;
        for (String segment : path.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            current = current.getChild(segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static int depth(String path) {
        if (path == null || "/".equals(path) || path.isEmpty()) {
            return 0;
        }
        int d = 0;
        for (String segment : path.split("/")) {
            if (!segment.isEmpty()) {
                d++;
            }
        }
        return d;
    }

    // Test hook: forget all contributions (and the built-ins guard).
    static synchronized void resetForTests() {
        CONTRIBUTIONS.clear();
        builtinsRegistered = false;
    }
}
