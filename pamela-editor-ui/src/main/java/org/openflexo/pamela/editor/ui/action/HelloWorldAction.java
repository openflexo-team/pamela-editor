package org.openflexo.pamela.editor.ui.action;

import javax.swing.JOptionPane;

import org.openflexo.pamela.editor.ui.PamelaEditorApplication;

/**
 * HelloWorld contextual action — applies to every object.
 *
 * <p>Shows a simple dialog with the {@code toString()} of the target.
 * This action exists only to validate the contextual-menu architecture.</p>
 */
public class HelloWorldAction implements ContextualAction {

    @Override
    public String getLabel() {
        return "Hello World";
    }

    @Override
    public boolean isApplicable(Object target) {
        return true; // applies to every node
    }

    @Override
    public void perform(Object target, PamelaEditorApplication app) {
        JOptionPane.showMessageDialog(
                null,
                "Hello from contextual menu!\n\nSelected object:\n" + target,
                "Hello World",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
