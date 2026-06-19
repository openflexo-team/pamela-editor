package org.openflexo.pamela.editor.ui.preferences;

/**
 * Implementation class for {@link PreferencesNode}. Provides the non-PAMELA
 * navigation/display helpers; the {@code name}/{@code children}/{@code parent}
 * properties are managed by PAMELA.
 *
 * <p>{@code displayLabel} and {@code iconKey} are <em>transient</em> (plain fields,
 * not PAMELA-managed, never serialized): they are set by the tree builder from the
 * registry contribution (see {@link PreferencesRegistry}).</p>
 */
public abstract class PreferencesNodeImpl implements PreferencesNode {

    private String displayLabel;
    private String iconKey;

    @Override
    public boolean isRoot() {
        return getParent() == null;
    }

    @Override
    public String getPath() {
        PreferencesNode parent = getParent();
        if (parent == null) {
            return "/";
        }
        String name = getName() != null ? getName() : "";
        return parent.isRoot() ? "/" + name : parent.getPath() + "/" + name;
    }

    @Override
    public String getDisplayLabel() {
        return displayLabel != null ? displayLabel : getName();
    }

    @Override
    public void setDisplayLabel(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    @Override
    public String getIconKey() {
        return iconKey;
    }

    @Override
    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    @Override
    public PreferencesNode getChild(String name) {
        if (getChildren() == null || name == null) {
            return null;
        }
        for (PreferencesNode child : getChildren()) {
            if (name.equals(child.getName())) {
                return child;
            }
        }
        return null;
    }
}
