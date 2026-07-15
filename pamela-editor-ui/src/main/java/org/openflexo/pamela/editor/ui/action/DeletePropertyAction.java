package org.openflexo.pamela.editor.ui.action;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import java.util.function.Supplier;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.pamela.editor.ui.dialog.ConfirmParameters;
import org.openflexo.pamela.editor.ui.dialog.ModelEditingDialogs;

/**
 * Deletes a {@link SourceModelProperty}, removing all its accessor methods
 * ({@code @Getter}/{@code @Setter}/{@code @Adder}/…) from the entity's source
 * after confirmation. Reachable from the detailed browser, the diagram property
 * row and its connector (same facet — {@code ui-design.md §18.4}).
 */
public class DeletePropertyAction extends SourceEditingAction {


    @Override
    public ActionGroup getGroup() {
        return ActionGroup.DELETE;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.PROPERTY_ICON;
    }

    @Override
    public String getLabel() {
        return loc("delete_property_action");
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceModelProperty;
    }

    @Override
    protected boolean confirmPerform(Object target, PamelaEditorApplication app) {
        SourceModelProperty property = (SourceModelProperty) target;
        SourceModelEntity entity = property.getModelEntity();
        ConfirmParameters confirm = new ConfirmParameters(
                "Delete property '" + property.getPropertyIdentifier() + "' from '"
                        + (entity != null ? entity.getSimpleName() : "?") + "'?\n"
                        + "Its getter/setter/adder/remover methods will be removed from the source.",
                "Delete");
        return ModelEditingDialogs.confirm(app.getFrame(), "Delete Property", confirm);
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelProperty property = (SourceModelProperty) target;
        SourceModelEntity entity = property.getModelEntity();
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();
        property.remove();
        return () -> model.getEntity(entityQN);
    }
}
