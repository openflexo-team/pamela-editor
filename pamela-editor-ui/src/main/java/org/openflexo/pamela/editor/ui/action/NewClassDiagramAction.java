package org.openflexo.pamela.editor.ui.action;

import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Contextual action: creates a new empty class diagram at the root of a
 * {@link PamelaProject}.
 *
 * <p>Shown when the user right-clicks a project node in the browser. Delegates
 * to {@link PamelaEditorApplication#newDiagram(PamelaProject)}, which prompts
 * for a name, registers the diagram on the project and opens it in the central
 * view.</p>
 */
public class NewClassDiagramAction implements ContextualAction {

    @Override
    public String getLabel() {
        return "New Class Diagram";
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof PamelaProject
                && ((PamelaProject) target).getDiagramFactory() != null;
    }

    @Override
    public void perform(Object target, PamelaEditorApplication app) {
        app.newDiagram((PamelaProject) target);
    }
}
