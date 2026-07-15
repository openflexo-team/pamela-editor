package org.openflexo.pamela.editor.ui.action;
import org.openflexo.icon.IconMarker;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.diagram.PamelaClassDiagramDrawing;
import org.openflexo.pamela.editor.ui.diagram.PamelaClassDiagramEditor;

/**
 * Hides a member (property, initializer or custom method) on the active diagram. For a
 * property this also removes its connector (a hidden member is fully hidden — see
 * {@code ui-design.md §9.2}).
 *
 * <p>Applies to a {@code SourceModelProperty} / {@code SourceModelInitializer} /
 * {@code SourceCustomMethod} facet, which is the target both in the browsers (the member
 * node) and on the diagram (a compartment row, or a connector → its property). It is
 * applicable only when there is an active diagram on which the member's entity is present
 * and the member is currently shown.</p>
 *
 * <p>The member stays available in the browser; re-show it with {@link ShowMemberAction}
 * from the browser's contextual menu.</p>
 */
public class HideMemberAction extends ContextualAction {


    @Override
    public ActionGroup getGroup() {
        return ActionGroup.DIAGRAM;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.PROPERTY_ICON;
    }

    @Override
    protected IconMarker[] getMarkers() {
        return new IconMarker[] { PamelaEditorIconLibrary.MINUS };
    }

    private final PamelaEditorApplication app;

    public HideMemberAction(PamelaEditorApplication app) {
        this.app = app;
    }

    @Override
    public String getLabel() {
        return loc("hide_on_diagram_action");
    }

    @Override
    public boolean isApplicable(Object target) {
        if (!PamelaClassDiagramDrawing.isHideableMember(target)) {
            return false;
        }
        PamelaClassDiagramEditor editor = app.getActiveDiagramEditor();
        return editor != null
                && editor.isMemberOnDiagram(target)
                && !editor.isMemberHidden(target);
    }

    @Override
    protected void doPerform(Object target, PamelaEditorApplication app) {
        PamelaClassDiagramEditor editor = app.getActiveDiagramEditor();
        if (editor != null) {
            editor.setMemberHidden(target, true);
        }
    }
}
