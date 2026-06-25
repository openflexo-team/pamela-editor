package org.openflexo.pamela.editor.ui.action;
import org.openflexo.icon.IconMarker;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import org.openflexo.pamela.editor.diagram.EntityView;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.diagram.PamelaClassDiagramEditor;

/**
 * Diagram-specific contextual action: removes an {@link EntityView} from the active diagram
 * (the box and its computed connectors disappear). The underlying source entity is
 * untouched — only its presence on this diagram is removed.
 *
 * <p>Only ever applies on the diagram (an {@link EntityView} is not a browser node). Hiding
 * an individual member (property / initializer / method) is a different action — see
 * {@link HideMemberAction}.</p>
 */
public class RemoveFromDiagramAction extends ContextualAction {


    @Override
    public ActionGroup getGroup() {
        return ActionGroup.DIAGRAM;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.ENTITY_ICON;
    }

    @Override
    protected IconMarker[] getMarkers() {
        return new IconMarker[] { PamelaEditorIconLibrary.MINUS };
    }

    @Override
    public String getLabel() {
        return "Remove from diagram";
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof EntityView;
    }

    @Override
    protected void doPerform(Object target, PamelaEditorApplication app) {
        PamelaClassDiagramEditor editor = app.getActiveDiagramEditor();
        if (editor == null) {
            return;
        }
        editor.getDiagram().removeFromEntityViews((EntityView) target);
        editor.pruneStaleConnectorData();
        editor.refresh();
    }
}
