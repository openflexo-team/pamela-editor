package org.openflexo.pamela.editor.ui.action;

import org.openflexo.pamela.editor.diagram.EntityView;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.diagram.PamelaClassDiagramEditor;

/**
 * Diagram-specific contextual action: removes an {@link EntityView} from the active
 * diagram (the box and its computed connectors disappear). The underlying source
 * entity is untouched — only its presence on this diagram is removed.
 *
 * <p>Only ever applies on the diagram (an {@link EntityView} is not a browser node).
 * The diagram's dirty tracking marks the project modified automatically when the
 * {@code entityViews} collection changes.</p>
 */
public class RemoveFromDiagramAction implements ContextualAction {

    @Override
    public String getLabel() {
        return "Remove from diagram";
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof EntityView;
    }

    @Override
    public void perform(Object target, PamelaEditorApplication app) {
        EntityView ev = (EntityView) target;
        PamelaClassDiagramEditor editor = app.getActiveDiagramEditor();
        if (editor == null) {
            return;
        }
        editor.getDiagram().removeFromEntityViews(ev);
        editor.refresh();
    }
}
