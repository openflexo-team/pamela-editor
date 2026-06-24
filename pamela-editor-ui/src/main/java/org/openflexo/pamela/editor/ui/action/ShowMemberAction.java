package org.openflexo.pamela.editor.ui.action;

import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.diagram.PamelaClassDiagramDrawing;
import org.openflexo.pamela.editor.ui.diagram.PamelaClassDiagramEditor;

/**
 * Re-shows a member (property, initializer or custom method) that was hidden on the active
 * diagram (see {@link HideMemberAction}).
 *
 * <p>A hidden member has no row/connector on the diagram, so it cannot be right-clicked
 * there — this action is reached from the <b>browser</b> (the member is always listed in the
 * detailed browser regardless of diagram visibility). It is applicable only when there is an
 * active diagram on which the member's entity is present and the member is currently hidden.</p>
 */
public class ShowMemberAction extends ContextualAction {

    private final PamelaEditorApplication app;

    public ShowMemberAction(PamelaEditorApplication app) {
        this.app = app;
    }

    @Override
    public String getLabel() {
        return "Show on diagram";
    }

    @Override
    public boolean isApplicable(Object target) {
        if (!PamelaClassDiagramDrawing.isHideableMember(target)) {
            return false;
        }
        PamelaClassDiagramEditor editor = app.getActiveDiagramEditor();
        return editor != null
                && editor.isMemberOnDiagram(target)
                && editor.isMemberHidden(target);
    }

    @Override
    protected void doPerform(Object target, PamelaEditorApplication app) {
        PamelaClassDiagramEditor editor = app.getActiveDiagramEditor();
        if (editor != null) {
            editor.setMemberHidden(target, false);
        }
    }
}
