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
        return target instanceof SourceModelEntity
                || target instanceof SourceModelProperty;
    }

    @Override
    protected void doPerform(Object target, PamelaEditorApplication app) {
        // The navigating (one-arg) selection: switches the central view to the source,
        // and highlights the property's methods when target is a SourceModelProperty.
        app.setCurrentSelectedElement(target);
    }
}
