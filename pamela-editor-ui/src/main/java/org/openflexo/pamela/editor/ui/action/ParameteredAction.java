package org.openflexo.pamela.editor.ui.action;

import java.beans.PropertyChangeSupport;

import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.dialog.ModelEditingDialogs;
import org.openflexo.rm.Resource;
import org.openflexo.toolbox.HasPropertyChangeSupport;

/**
 * A {@link SourceEditingAction} that collects parameters in a modal dialog
 * before mutating the source.
 *
 * <p><b>The action <i>is</i> the dialog's data object.</b> It carries the bound
 * parameters (JavaBean getters/setters) and exposes {@link #isInputValid()},
 * which drives the generic Validate button ({@code enable="data.inputValid"}).
 * The generic shell ({@code Fib/dialogs/ModelEditingDialog.fib}) owns the
 * Validate/Cancel buttons once and embeds this action's specific form fragment
 * ({@link #getFormFib()}) via a {@code FIBReferencedComponent} bound to the same
 * data. See {@code model-editing-design.md §5}.</p>
 *
 * <p>The dialog is the lifecycle's {@link #confirmPerform} gate; the mutation
 * ({@link #applyMutation}) and the rebuild/reselection are inherited from
 * {@link SourceEditingAction}. Because parameters are per-invocation state,
 * {@link #instanceForPerform()} returns a fresh instance so the registered
 * singleton stays stateless.</p>
 */
public abstract class ParameteredAction extends SourceEditingAction
        implements HasPropertyChangeSupport {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /** The form-fragment FIB (fields only, no buttons), bound to this action. */
    public abstract Resource getFormFib();

    /** Window title of the dialog. */
    protected abstract String getDialogTitle();

    /**
     * Seeds the bound parameters from {@code target}. Return {@code false} to
     * cancel before showing the dialog (e.g. nothing to choose from).
     */
    protected abstract boolean prepareDialog(Object target, PamelaEditorApplication app);

    /** Drives the generic Validate button's {@code enable="data.inputValid"}. */
    public abstract boolean isInputValid();

    // --- lifecycle hooks ----------------------------------------------------

    @Override
    protected ContextualAction instanceForPerform() {
        return makeFreshInstance();
    }

    @Override
    protected final boolean confirmPerform(Object target, PamelaEditorApplication app) {
        return prepareDialog(target, app)
                && ModelEditingDialogs.showDialog(app.getFrame(), getDialogTitle(), this);
    }

    /** A fresh instance per invocation so the registered action stays stateless. */
    protected ParameteredAction makeFreshInstance() {
        try {
            return getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(getClass().getSimpleName()
                    + " needs a public no-arg constructor", e);
        }
    }

    /** Subclasses call this from a parameter setter that affects validity. */
    protected void fireInputValidChanged() {
        pcs.firePropertyChange("inputValid", null, isInputValid());
    }

    // --- HasPropertyChangeSupport -------------------------------------------

    @Override
    public PropertyChangeSupport getPropertyChangeSupport() {
        return pcs;
    }

    @Override
    public String getDeletedProperty() {
        return null;
    }
}
