package org.openflexo.pamela.editor.ui.action;

import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.diagram.PamelaClassDiagramDrawing.Compartment;
import org.openflexo.pamela.editor.ui.diagram.PamelaClassDiagramEditor;

/**
 * Reveals every member of one compartment of an entity on the active diagram: clears that
 * compartment's hidden-member list <em>and</em> makes the compartment visible if it was
 * hidden.
 *
 * <p>Keyed on a {@code SourceModelEntity}, so it appears both on the entity box (diagram)
 * and the entity node (browsers). One registered instance per {@link Compartment}
 * (properties / initializers / methods). Applicable only when the entity is on the active
 * diagram and that compartment actually has something to reveal.</p>
 */
public class ShowAllMembersAction extends ContextualAction {

    private final PamelaEditorApplication app;
    private final Compartment kind;
    private final String label;

    public ShowAllMembersAction(PamelaEditorApplication app, Compartment kind) {
        this.app = app;
        this.kind = kind;
        this.label = labelFor(kind);
    }

    private static String labelFor(Compartment kind) {
        switch (kind) {
            case INITIALIZERS: return "Show all initializers";
            case METHODS:      return "Show all methods";
            case PROPERTIES:
            default:           return "Show all properties";
        }
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public boolean isApplicable(Object target) {
        if (!(target instanceof SourceModelEntity)) {
            return false;
        }
        PamelaClassDiagramEditor editor = app.getActiveDiagramEditor();
        return editor != null && editor.canShowAllMembers((SourceModelEntity) target, kind);
    }

    @Override
    protected void doPerform(Object target, PamelaEditorApplication app) {
        PamelaClassDiagramEditor editor = app.getActiveDiagramEditor();
        if (editor != null && target instanceof SourceModelEntity) {
            editor.showAllMembers((SourceModelEntity) target, kind);
        }
    }
}
