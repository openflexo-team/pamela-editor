package org.openflexo.pamela.editor.ui.action;

import javax.swing.ImageIcon;

import org.openflexo.pamela.editor.ui.PamelaEditorApplication;

/**
 * Base class for an action that can appear in a contextual (right-click) menu.
 *
 * <p>Actions are registered on {@link PamelaEditorApplication} at startup. When
 * the user right-clicks a node in any browser panel (or a diagram element), the
 * application filters the registered actions via {@link #isApplicable(Object)}
 * and builds a {@link javax.swing.JPopupMenu} from the matching ones.</p>
 *
 * <p><b>Action lifecycle.</b> {@link #perform} is a {@code final} template
 * shared by every action, parameterized or not:</p>
 * <ol>
 *   <li>{@link #instanceForPerform()} — stateful actions (those that hold
 *       per-invocation parameters, e.g. {@link ParameteredAction}) return a
 *       fresh instance so the registered singleton stays stateless; stateless
 *       actions run on themselves;</li>
 *   <li>{@link #confirmPerform(Object, PamelaEditorApplication)} — an optional
 *       gate (a parameter dialog, a confirmation…) that may cancel the action;</li>
 *   <li>{@link #doPerform(Object, PamelaEditorApplication)} — the action body.</li>
 * </ol>
 *
 * <p>Concrete actions implement {@link #doPerform}; source-mutating actions
 * usually extend {@link SourceEditingAction} (which factors the project
 * resolution + rebuild + reselection) rather than implementing {@code doPerform}
 * directly.</p>
 */
public abstract class ContextualAction {

    /** The label shown in the contextual menu item (short, title case). */
    public abstract String getLabel();

    /** Optional icon shown next to the label. {@code null} for none. */
    public ImageIcon getIcon() {
        return null;
    }

    /**
     * Returns {@code true} if this action makes sense for the given target.
     * Called for every registered action on each right-click — keep it fast.
     */
    public abstract boolean isApplicable(Object target);

    /**
     * Runs the action lifecycle. Called on the Event Dispatch Thread. Final —
     * subclasses customise the lifecycle through {@link #instanceForPerform()},
     * {@link #confirmPerform} and {@link #doPerform}.
     */
    public final void perform(Object target, PamelaEditorApplication app) {
        ContextualAction action = instanceForPerform();
        if (action.confirmPerform(target, app)) {
            action.doPerform(target, app);
        }
    }

    /**
     * The instance that actually runs this invocation. Default is {@code this}
     * (stateless action); a stateful action returns a fresh instance.
     */
    protected ContextualAction instanceForPerform() {
        return this;
    }

    /**
     * Optional gate before the body runs (a dialog, a confirmation…). Return
     * {@code false} to cancel. Default: proceed.
     */
    protected boolean confirmPerform(Object target, PamelaEditorApplication app) {
        return true;
    }

    /** The action body. */
    protected abstract void doPerform(Object target, PamelaEditorApplication app);
}
