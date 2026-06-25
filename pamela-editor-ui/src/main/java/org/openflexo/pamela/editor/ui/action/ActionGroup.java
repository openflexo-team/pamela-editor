package org.openflexo.pamela.editor.ui.action;

import org.openflexo.icon.IconMarker;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;

/**
 * Semantic grouping of {@link ContextualAction}s in the right-click menus.
 *
 * <p>The enum order ({@link #ordinal()}) is the order in which the groups appear,
 * top (non-destructive) to bottom (destructive). Consecutive groups are separated by
 * a divider in the menu; an empty group emits no divider. Three groups are rendered as
 * a nested submenu (see {@link #isSubmenu()}); the rest are inline. See
 * {@code context-menu-design.md}.</p>
 */
public enum ActionGroup {

    /** Navigate / reveal (read-only). Inline. */
    OPEN(false, null, null),
    /** Create a brand-new element. Inline. */
    NEW(false, null, PamelaEditorIconLibrary.PLUS),
    /** Lift existing Java into the model (co-construction). Inline. */
    PROMOTE(false, null, PamelaEditorIconLibrary.IMPORT),
    /** Generate companion code on an existing element. Submenu {@code Generate}. */
    GENERATE(true, "generate", PamelaEditorIconLibrary.GENERATE),
    /** Rename / retype / restructure. Submenu {@code Refactor}. */
    REFACTOR(true, "refactor", null),
    /** Visibility on the active diagram. Submenu {@code Diagram}. */
    DIAGRAM(true, "diagram", null),
    /** Destructive. Inline. */
    DELETE(false, null, PamelaEditorIconLibrary.DELETE);

    private final boolean submenu;
    private final String submenuLabelKey;
    private final IconMarker defaultMarker;

    ActionGroup(boolean submenu, String submenuLabelKey, IconMarker defaultMarker) {
        this.submenu = submenu;
        this.submenuLabelKey = submenuLabelKey;
        this.defaultMarker = defaultMarker;
    }

    /** {@code true} when this group is rendered as a nested {@code JMenu}. */
    public boolean isSubmenu() {
        return submenu;
    }

    /** Localization key for the submenu title ({@code null} for inline groups). */
    public String getSubmenuLabelKey() {
        return submenuLabelKey;
    }

    /** Default intent marker overlaid on an action's base icon ({@code null} = none). */
    public IconMarker getDefaultMarker() {
        return defaultMarker;
    }
}
