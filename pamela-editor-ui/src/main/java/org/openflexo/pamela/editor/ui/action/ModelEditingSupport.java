package org.openflexo.pamela.editor.ui.action;

import javax.swing.JOptionPane;

import org.openflexo.pamela.editor.ui.PamelaEditorApplication;

/**
 * Small shared helpers for the model-editing contextual actions (Lot 1+).
 *
 * <p>Stateless utility methods only — validation of user input, error
 * reporting, and the common "prompt for a simple Java name" gesture. The
 * orchestration of the actual source mutation lives in the {@code Source*}
 * core classes; these helpers only support the thin UI layer.</p>
 *
 * <p>See {@code model-editing-design.md §5} for the standard action skeleton.</p>
 */
final class ModelEditingSupport {

    private ModelEditingSupport() {
    }

    /**
     * Returns {@code true} if {@code name} is a legal, simple Java identifier
     * (no dots, no keywords-as-such check beyond the lexical rules — PAMELA
     * type names are conventionally capitalised but that is not enforced here).
     */
    static boolean isValidJavaIdentifier(String name) {
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
     * Shows a modal input dialog asking for a single name, pre-filled with
     * {@code initialValue}. Returns the trimmed input, or {@code null} if the
     * user cancelled or left it empty.
     */
    static String promptForName(PamelaEditorApplication app, String title,
                                String message, String initialValue) {
        Object answer = JOptionPane.showInputDialog(app.getFrame(), message, title,
                JOptionPane.PLAIN_MESSAGE, null, null, initialValue);
        if (answer == null) {
            return null;
        }
        String trimmed = answer.toString().trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Shows a blocking error dialog with the given title and message. */
    static void error(PamelaEditorApplication app, String title, String message) {
        JOptionPane.showMessageDialog(app.getFrame(), message, title,
                JOptionPane.ERROR_MESSAGE);
    }
}
