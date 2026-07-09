/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor, a component of the software infrastructure
 * developed at Openflexo.
 *
 */

package org.openflexo.pamela.editor.ui.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.GraphicsEnvironment;
import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.model.FIBComponent;
import org.openflexo.gina.swing.utils.FIBJPanel;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.pamela.editor.ui.dialog.ModelEditingDialogs;

/**
 * Coverage for {@link RenameMetaModelAction}'s applicability, the FIB-bound parameter logic
 * (name field, the "also rename the file" checkbox, and the file base-name that auto-syncs from
 * the display name until the user customizes it) and its validity gate — the parts that do not
 * require a live {@code PamelaEditorApplication}. The actual apply (name + optional file rename)
 * lives in {@code PamelaEditorApplication.applyMetaModelRename}, which needs the running app and
 * is verified by hand.
 */
public class TestRenameMetaModelAction {

    private static SourceMetaModel metaModel;

    @BeforeClass
    public static void loadMetaModel() throws Exception {
        File pamelaFile = new File(System.getProperty("user.dir"),
                "../pamela-editor-core/src/test/java/test/model2-full.pamela");
        assertTrue("Test metamodel not found: " + pamelaFile.getAbsolutePath(), pamelaFile.exists());
        metaModel = SourceMetaModelSerializer.load(pamelaFile);
        assertNotNull(metaModel);
    }

    @Test
    public void testApplicableToMetaModelOnly() {
        RenameMetaModelAction action = new RenameMetaModelAction();
        assertTrue(action.isApplicable(metaModel));
        assertFalse(action.isApplicable("not a metamodel"));
        assertFalse(action.isApplicable(null));
    }

    @Test
    public void testFileNameAutoSyncsFromDisplayNameUntilCustomized() {
        RenameMetaModelAction action = new RenameMetaModelAction();
        action.setNewName("Foo");
        assertEquals("Foo", action.getNewFileName());
        action.setNewName("Bar");
        assertEquals("file name tracks the display name until customized",
                "Bar", action.getNewFileName());

        // Once the user edits the file name, changing the display name no longer overrides it.
        action.setNewFileName("MyCustomFile");
        action.setNewName("Baz");
        assertEquals("a user-customized file name is preserved",
                "MyCustomFile", action.getNewFileName());
    }

    @Test
    public void testInputValidity() {
        RenameMetaModelAction action = new RenameMetaModelAction();

        action.setNewName("");
        assertFalse("empty display name is invalid", action.isInputValid());

        action.setNewName("MyModel");
        assertTrue("non-empty name, file rename off → valid", action.isInputValid());

        action.setRenameFile(true);
        assertTrue("file rename on with a non-empty file name → valid", action.isInputValid());

        action.setNewFileName("");
        assertFalse("file rename on but empty file name → invalid", action.isInputValid());
    }

    @Test
    public void testFormFibLoads() {
        RenameMetaModelAction action = new RenameMetaModelAction();
        assertNotNull("FIB resource not located", action.getFormFib());
        FIBComponent component =
                ApplicationFIBLibraryImpl.instance().retrieveFIBComponent(action.getFormFib());
        assertNotNull("RenameMetaModelForm.fib did not load", component);
    }

    /**
     * Regression for the runtime crash: the generic dialog <em>shell</em>
     * ({@code ModelEditingDialog.fib}, {@code dataClassName="ParameteredAction"}) embeds the
     * action's fragment via {@code data.formFib} — so the action <b>must</b> be assignable to
     * {@code ParameteredAction}, or Connie fails to resolve {@code getFormFib()} on it
     * ("Object is not an instance of declaring class"). Building the shell view bound to a
     * {@link RenameMetaModelAction} exercises that exact binding path (which the fragment-only
     * {@link #testFormFibLoads()} did not), and would throw if the type contract were broken
     * again. Needs a display (Swing view construction), so it is skipped when headless.
     */
    @Test
    public void testShellDialogBindsToRenameMetaModelAction() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        RenameMetaModelAction action = new RenameMetaModelAction();
        action.setNewName("MyModel");

        // Build the generic shell view bound to the action (the same path JFIBDialog takes,
        // minus actually showing the modal window). This embeds RenameMetaModelForm.fib via the
        // shell's data.formFib binding — the exact evaluation that used to crash.
        FIBJPanel<ParameteredAction> shell = new FIBJPanel<ParameteredAction>(
                ModelEditingDialogs.MODEL_EDITING_DIALOG_FIB, action,
                ApplicationFIBLibraryImpl.instance(),
                PamelaEditorFIBController.EDITOR_LOCALIZATION) {
            @Override
            public Class<ParameteredAction> getRepresentedType() {
                return ParameteredAction.class;
            }

            @Override
            public void delete() {
            }
        };
        assertNotNull("shell dialog view must build and bind the action", shell.getController());
    }
}
