package org.openflexo.pamela.editor.ui.action;

import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;

/**
 * Contextual action: renames a {@link PamelaClassDiagram} (display name only).
 * The diagram's stable id and sidecar file are unchanged.
 */
public class RenameClassDiagramAction extends ContextualAction {

    @Override
    public String getLabel() {
        return "Rename Diagram";
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof PamelaClassDiagram;
    }

    @Override
    protected void doPerform(Object target, PamelaEditorApplication app) {
        app.renameDiagram((PamelaClassDiagram) target);
    }
}
