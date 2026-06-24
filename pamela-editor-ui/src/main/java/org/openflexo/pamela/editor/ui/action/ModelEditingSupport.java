package org.openflexo.pamela.editor.ui.action;

import javax.swing.JOptionPane;

import org.openflexo.pamela.editor.ui.PamelaEditorApplication;

/**
 * Small shared helpers for the model-editing contextual actions.
 *
 * <p>Stateless utility methods only — validation of user input and error
 * reporting. Parameter collection itself is now done with FIB dialogs (see
 * {@code org.openflexo.pamela.editor.ui.dialog}); this class keeps the
 * identifier check (shared with the dialog beans) and the error popup used by
 * the actions when an orchestration call fails.</p>
 *
 * <p>See {@code model-editing-design.md §5} for the standard action skeleton.</p>
 */
public final class ModelEditingSupport {

    private ModelEditingSupport() {
    }

    /**
     * Returns {@code true} if {@code name} is a legal, simple Java identifier
     * (no dots). PAMELA type names are conventionally capitalised but that is
     * not enforced here.
     */
    public static boolean isValidJavaIdentifier(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validity rule shared by the name/identifier dialogs: a legal Java
     * identifier that is not already taken. Shared as a helper (behaviour),
     * never via a form-shaped superclass.
     */
    public static boolean isAvailableIdentifier(String name, java.util.Set<String> taken) {
        String n = name == null ? "" : name.trim();
        return isValidJavaIdentifier(n) && !taken.contains(n);
    }

    /** Shows a blocking error dialog with the given title and message. */
    public static void error(PamelaEditorApplication app, String title, String message) {
        JOptionPane.showMessageDialog(app.getFrame(), message, title,
                JOptionPane.ERROR_MESSAGE);
    }
}
