package org.openflexo.pamela.editor.ui.action;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

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
public class NewClassDiagramAction extends ContextualAction {


    @Override
    public ActionGroup getGroup() {
        return ActionGroup.NEW;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.DIAGRAM_ICON;
    }

    @Override
    public String getLabel() {
        return loc("new_class_diagram_action");
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof PamelaProject
                && ((PamelaProject) target).getDiagramFactory() != null;
    }

    @Override
    protected void doPerform(Object target, PamelaEditorApplication app) {
        app.newDiagram((PamelaProject) target);
    }
}
