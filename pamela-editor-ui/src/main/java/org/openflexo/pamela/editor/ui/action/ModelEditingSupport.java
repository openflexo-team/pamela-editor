package org.openflexo.pamela.editor.ui.action;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import org.openflexo.pamela.editor.model.SourceMetaModel;
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

    /**
     * Returns {@code true} if {@code name} is a legal, non-empty, dot-separated Java package
     * name (e.g. {@code "org.example.sub"}) — every segment a legal Java identifier. The default
     * (empty) package is not accepted here: it always exists implicitly and is never created
     * explicitly via {@code NewPackageAction}.
     */
    public static boolean isValidPackageName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (String segment : name.split("\\.", -1)) {
            if (!isValidJavaIdentifier(segment)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validity rule for the "New Package" dialog: a legal package name that is not already
     * taken (e.g. not already a package of the chosen source folder).
     */
    public static boolean isAvailablePackageName(String name, java.util.Set<String> taken) {
        String n = name == null ? "" : name.trim();
        return isValidPackageName(n) && !taken.contains(n);
    }

    /**
     * The type choices shown in property type pickers: common JDK types first,
     * then the metamodel's entity qualified names (sorted). A dedicated free-text
     * {@code TypeSelector} widget is the agreed follow-up.
     */
    public static List<String> typeChoices(SourceMetaModel model) {
        List<String> choices = new ArrayList<>();
        choices.add("java.lang.String");
        choices.add("boolean");
        choices.add("int");
        choices.add("long");
        choices.add("double");
        choices.add("float");
        choices.add("java.lang.Integer");
        choices.add("java.lang.Boolean");
        choices.add("java.util.Date");
        if (model != null) {
            choices.addAll(new TreeSet<>(model.getEntities().keySet()));
        }
        return choices;
    }

    /** Shows a blocking error dialog with the given title and message. */
    public static void error(PamelaEditorApplication app, String title, String message) {
        JOptionPane.showMessageDialog(app.getFrame(), message, title,
                JOptionPane.ERROR_MESSAGE);
    }

    /** Shows a blocking information dialog with the given title and message. */
    public static void info(PamelaEditorApplication app, String title, String message) {
        JOptionPane.showMessageDialog(app.getFrame(), message, title,
                JOptionPane.INFORMATION_MESSAGE);
    }
}
