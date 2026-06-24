package org.openflexo.pamela.editor.ui.dialog;

import java.awt.Window;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.controller.FIBController.Status;
import org.openflexo.gina.swing.utils.JFIBDialog;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.pamela.editor.ui.action.ParameteredAction;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Shared plumbing to show a model-editing dialog as a modal FIB dialog.
 *
 * <p>Parameter dialogs use a single generic <b>shell</b>
 * ({@code Fib/dialogs/ModelEditingDialog.fib}) that owns the Validate/Cancel
 * buttons once and embeds the action's specific form fragment via a
 * {@code FIBReferencedComponent} ({@code dynamicComponentFile="data.formFib"},
 * {@code data="data"}). The dialog's data object is the
 * {@link ParameteredAction} itself, so the shell's
 * {@code enable="data.inputValid"} and the embedded fragment's {@code data.*}
 * bindings all resolve against the same action. No custom controller is needed:
 * the base {@code FIBController} provides {@code validateAndDispose()} /
 * {@code cancelAndDispose()} / {@code chooseYesAndDispose()} /
 * {@code chooseNoAndDispose()}.</p>
 */
public final class ModelEditingDialogs {

    public static final Resource MODEL_EDITING_DIALOG_FIB =
            ResourceLocator.locateResource("Fib/dialogs/ModelEditingDialog.fib");
    public static final Resource CONFIRM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/ConfirmDialog.fib");

    private ModelEditingDialogs() {
    }

    /**
     * Shows the generic shell embedding {@code action}'s form fragment, bound to
     * {@code action}. Returns {@code true} when the user validated.
     */
    public static boolean showDialog(Window owner, String title, ParameteredAction action) {
        return show(owner, MODEL_EDITING_DIALOG_FIB, title, action) == Status.VALIDATED;
    }

    /** Shows a yes/no confirmation. Returns {@code true} when the user chose Yes. */
    public static boolean confirm(Window owner, String title, ConfirmParameters data) {
        return show(owner, CONFIRM_FIB, title, data) == Status.YES;
    }

    private static Status show(Window owner, Resource fib, String title, Object data) {
        JFIBDialog<Object> dialog = JFIBDialog.instanciateAndShowDialog(
                title, ApplicationFIBLibraryImpl.instance().retrieveFIBComponent(fib),
                data, owner, true, PamelaEditorFIBController.EDITOR_LOCALIZATION);
        return dialog == null ? Status.CANCELED : dialog.getStatus();
    }
}
