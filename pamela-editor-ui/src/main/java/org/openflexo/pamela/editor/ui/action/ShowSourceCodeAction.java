package org.openflexo.pamela.editor.ui.action;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;

/**
 * Contextual action: navigates the central view to the source code of an entity (or
 * the parent entity of a property, with the property's methods highlighted).
 *
 * <p>Pure navigation — no model mutation. Applicable both in the browsers and on the
 * class diagram (right-click a box or a property row), giving those menus content.</p>
 */
public class ShowSourceCodeAction extends ContextualAction {

    private final PamelaEditorApplication application;

    /**
     * @param application back-reference used by {@link #isApplicable} to hide the action when the
     *        target entity's source code is already the active central view.
     */
    public ShowSourceCodeAction(PamelaEditorApplication application) {
        this.application = application;
    }

    @Override
    public ActionGroup getGroup() {
        return ActionGroup.OPEN;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.JAVA_FILE_ICON;
    }

    @Override
    public String getLabel() {
        return "Show source code";
    }

    @Override
    public boolean isApplicable(Object target) {
        SourceModelEntity entity;
        if (target instanceof SourceModelEntity) {
            entity = (SourceModelEntity) target;
        } else if (target instanceof SourceModelProperty) {
            entity = ((SourceModelProperty) target).getModelEntity();
        } else {
            return false;
        }
        // Hide when that entity's source code is already the active central view (redundant).
        return !application.isSourceCodeViewActive(entity);
    }

    @Override
    protected void doPerform(Object target, PamelaEditorApplication app) {
        // The navigating (one-arg) selection: switches the central view to the source,
        // and highlights the property's methods when target is a SourceModelProperty.
        app.setCurrentSelectedElement(target);
    }
}
