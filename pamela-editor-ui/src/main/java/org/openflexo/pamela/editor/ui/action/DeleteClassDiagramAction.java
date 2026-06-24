package org.openflexo.pamela.editor.ui.action;

import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;

/**
 * Contextual action: deletes a {@link PamelaClassDiagram} after confirmation.
 * The sidecar {@code .diagram} file is removed when the project is next saved.
 */
public class DeleteClassDiagramAction extends ContextualAction {

    @Override
    public String getLabel() {
        return "Delete Diagram";
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof PamelaClassDiagram;
    }

    @Override
    protected void doPerform(Object target, PamelaEditorApplication app) {
        app.deleteDiagram((PamelaClassDiagram) target);
    }
}
