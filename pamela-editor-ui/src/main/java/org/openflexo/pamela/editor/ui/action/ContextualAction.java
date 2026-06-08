package org.openflexo.pamela.editor.ui.action;

import javax.swing.ImageIcon;

import org.openflexo.pamela.editor.ui.PamelaEditorApplication;

/**
 * A single action that can appear in a contextual (right-click) menu.
 *
 * <p>Actions are registered on {@link PamelaEditorApplication} at startup.
 * When the user right-clicks a node in any browser panel, the application
 * filters the registered actions via {@link #isApplicable(Object)} and builds
 * a {@link javax.swing.JPopupMenu} from the matching ones.</p>
 *
 * <p>Actions are stateless — they receive the target object and the application
 * as parameters in {@link #perform}.</p>
 */
public interface ContextualAction {

    /**
     * The label shown in the contextual menu item.
     * Should be short (≤ 40 chars) and use title case.
     */
    String getLabel();

    /**
     * Optional icon shown next to the label.
     * Return {@code null} for no icon.
     */
    default ImageIcon getIcon() {
        return null;
    }

    /**
     * Returns {@code true} if this action makes sense for the given target object.
     * Called for every registered action on each right-click — keep it fast.
     *
     * @param target the object on which the user right-clicked (never null)
     */
    boolean isApplicable(Object target);

    /**
     * Executes the action on the given target.
     * Always called on the Event Dispatch Thread.
     *
     * @param target the object the user right-clicked on
     * @param app    the running application instance (for navigation, model access, etc.)
     */
    void perform(Object target, PamelaEditorApplication app);
}
