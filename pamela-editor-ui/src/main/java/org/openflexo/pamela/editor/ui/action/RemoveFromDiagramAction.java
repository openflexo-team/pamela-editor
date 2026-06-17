package org.openflexo.pamela.editor.ui.action;

import org.openflexo.pamela.editor.diagram.EntityView;
import org.openflexo.pamela.editor.diagram.PropertyView;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.diagram.PamelaClassDiagramEditor;

/**
 * Diagram-specific contextual action: removes an element from the active diagram without
 * touching the underlying source model.
 *
 * <ul>
 *   <li>{@link EntityView} → removes the box and its computed connectors;</li>
 *   <li>{@link PropertyView} → hides the property's connector by adding it to the source
 *       entity view's {@code hiddenProperties} (it stays available, just not drawn).</li>
 * </ul>
 *
 * <p>Only ever applies on the diagram (neither type is a browser node). The diagram's
 * dirty tracking marks the project modified automatically.</p>
 */
public class RemoveFromDiagramAction implements ContextualAction {

    @Override
    public String getLabel() {
        return "Remove from diagram";
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof EntityView || target instanceof PropertyView;
    }

    @Override
    public void perform(Object target, PamelaEditorApplication app) {
        PamelaClassDiagramEditor editor = app.getActiveDiagramEditor();
        if (editor == null) {
            return;
        }
        if (target instanceof EntityView) {
            editor.getDiagram().removeFromEntityViews((EntityView) target);
            editor.pruneStaleConnectorData();
            editor.refresh();
        }
        else if (target instanceof PropertyView) {
            PropertyView pv = (PropertyView) target;
            EntityView source = entityViewFor(editor, pv.getSourceQualifiedName());
            if (source != null) {
                // Hiding fires HIDDEN_PROPERTIES → dirty + re-walk (see editor tracking).
                source.setPropertyHidden(pv.getPropertyIdentifier(), true);
            }
        }
    }

    /** Finds the entity view with the given qualified name in the active diagram. */
    private static EntityView entityViewFor(PamelaClassDiagramEditor editor, String qualifiedName) {
        if (qualifiedName == null) {
            return null;
        }
        for (EntityView ev : editor.getDiagram().getEntityViews()) {
            if (qualifiedName.equals(ev.getQualifiedName())) {
                return ev;
            }
        }
        return null;
    }
}
