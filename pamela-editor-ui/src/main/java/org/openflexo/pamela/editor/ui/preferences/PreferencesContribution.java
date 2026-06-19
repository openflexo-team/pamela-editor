package org.openflexo.pamela.editor.ui.preferences;

import org.openflexo.rm.Resource;

/**
 * One theme contributed to the preferences tree (see {@code preferences-design.md §2}).
 * Immutable value object registered with {@link PreferencesRegistry} before the
 * {@link PreferencesFactory} is built.
 */
public final class PreferencesContribution {

    private final Class<? extends PreferencesNode> themeClass;
    private final String parentPath;
    private final String name;
    private final int order;
    private final String labelKey;
    private final String iconKey;
    private final Resource inspectorFib;

    /**
     * @param themeClass   the {@code @ModelEntity} interface of the theme node
     * @param parentPath   slash path where the node attaches ({@code "/"} for a top-level theme,
     *                     e.g. {@code "/general"} to nest under the {@code general} node)
     * @param name         node name, unique among its siblings (also the JSON nesting key)
     * @param order        sort order among siblings (ascending)
     * @param labelKey     localization key for the browser label
     * @param iconKey      icon identifier for the browser (may be {@code null})
     * @param inspectorFib {@code .inspector} resource for the right pane (may be {@code null})
     */
    public PreferencesContribution(Class<? extends PreferencesNode> themeClass, String parentPath,
                                   String name, int order, String labelKey, String iconKey,
                                   Resource inspectorFib) {
        this.themeClass = themeClass;
        this.parentPath = parentPath;
        this.name = name;
        this.order = order;
        this.labelKey = labelKey;
        this.iconKey = iconKey;
        this.inspectorFib = inspectorFib;
    }

    public Class<? extends PreferencesNode> getThemeClass() { return themeClass; }
    public String getParentPath() { return parentPath; }
    public String getName() { return name; }
    public int getOrder() { return order; }
    public String getLabelKey() { return labelKey; }
    public String getIconKey() { return iconKey; }
    public Resource getInspectorFib() { return inspectorFib; }

    /** Absolute path this contribution's node will have, e.g. {@code "/general/window"}. */
    public String getPath() {
        if ("/".equals(parentPath)) {
            return "/" + name;
        }
        return parentPath + "/" + name;
    }
}
