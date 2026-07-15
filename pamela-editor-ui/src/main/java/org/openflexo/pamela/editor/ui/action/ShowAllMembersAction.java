package org.openflexo.pamela.editor.ui.action;
import org.openflexo.icon.IconMarker;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

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


    @Override
    public ActionGroup getGroup() {
        return ActionGroup.DIAGRAM;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        switch (kind) {
            case INITIALIZERS:
                return PamelaEditorIconLibrary.INITIALIZER_ICON;
            case METHODS:
                return PamelaEditorIconLibrary.METHOD_ICON;
            case PROPERTIES:
            default:
                return PamelaEditorIconLibrary.PROPERTY_ICON;
        }
    }

    @Override
    protected IconMarker[] getMarkers() {
        return new IconMarker[] { PamelaEditorIconLibrary.PLUS };
    }

    private final PamelaEditorApplication app;
    private final Compartment kind;

    public ShowAllMembersAction(PamelaEditorApplication app, Compartment kind) {
        this.app = app;
        this.kind = kind;
    }

    @Override
    public String getLabel() {
        switch (kind) {
            case INITIALIZERS: return loc("show_all_initializers_action");
            case METHODS:      return loc("show_all_methods_action");
            case PROPERTIES:
            default:           return loc("show_all_properties_action");
        }
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
