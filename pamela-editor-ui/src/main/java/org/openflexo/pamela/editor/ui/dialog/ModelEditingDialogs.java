package org.openflexo.pamela.editor.ui.dialog;

import java.awt.Window;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.controller.FIBController.Status;
import org.openflexo.gina.swing.utils.JFIBDialog;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Shared plumbing to show a model-editing parameter dialog as a modal FIB
 * dialog and read back its outcome.
 *
 * <p>Each dialog is a {@code .fib} form under {@code Fib/dialogs/} bound to a
 * plain parameter bean (a {@link org.openflexo.toolbox.HasPropertyChangeSupport}
 * POJO). The form's OK button is {@code action="controller.validateAndDispose()"}
 * gated by {@code enable="data.inputValid"}; Cancel is
 * {@code controller.cancelAndDispose()}. Confirmation dialogs use
 * {@code chooseYesAndDispose()} / {@code chooseNoAndDispose()}.</p>
 *
 * <p>No custom controller is needed: the base {@code FIBController} already
 * provides {@code validateAndDispose()} / {@code cancelAndDispose()} /
 * {@code chooseYesAndDispose()} / {@code chooseNoAndDispose()}.</p>
 */
public final class ModelEditingDialogs {

    public static final Resource SINGLE_NAME_FIB =
            ResourceLocator.locateResource("Fib/dialogs/SingleNameDialog.fib");
    public static final Resource PICK_FROM_LIST_FIB =
            ResourceLocator.locateResource("Fib/dialogs/PickFromListDialog.fib");
    public static final Resource NEW_PROPERTY_FIB =
            ResourceLocator.locateResource("Fib/dialogs/NewPropertyDialog.fib");
    public static final Resource CONFIRM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/ConfirmDialog.fib");

    private ModelEditingDialogs() {
    }

    /**
     * Shows {@code fib} bound to {@code data} as a modal dialog titled
     * {@code title}. Returns {@code true} when the user validated (status
     * {@link Status#VALIDATED}).
     */
    public static boolean showForm(Window owner, Resource fib, String title, Object data) {
        return show(owner, fib, title, data) == Status.VALIDATED;
    }

    /**
     * Shows a yes/no confirmation. Returns {@code true} when the user chose Yes
     * (status {@link Status#YES}).
     */
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
