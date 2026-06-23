package org.openflexo.pamela.editor.ui.action;

import java.io.IOException;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.pamela.editor.ui.dialog.ConfirmParameters;
import org.openflexo.pamela.editor.ui.dialog.ModelEditingDialogs;

/**
 * Contextual action: deletes a {@link SourceModelProperty}, removing all its
 * associated accessor methods ({@code @Getter}/{@code @Setter}/{@code @Adder}/…)
 * from the entity's source after confirmation.
 *
 * <p>Delegates to {@link SourceModelProperty#remove}. Reachable from the
 * detailed browser, the diagram property row and its connector (all map to the
 * same {@code SourceModelProperty} facet — {@code ui-design.md §18.4}).</p>
 */
public class DeletePropertyAction implements ContextualAction {

    @Override
    public String getLabel() {
        return "Delete Property";
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceModelProperty;
    }

    @Override
    public void perform(Object target, PamelaEditorApplication app) {
        SourceModelProperty property = (SourceModelProperty) target;
        SourceModelEntity entity = property.getModelEntity();
        if (entity == null) {
            return;
        }
        SourceMetaModel model = entity.getMetaModel();
        PamelaProject project = app.getProjectForElement(entity);
        if (model == null || project == null) {
            return;
        }

        final String entityQN = entity.getQualifiedName();
        ConfirmParameters confirm = new ConfirmParameters(
                "Delete property '" + property.getPropertyIdentifier() + "' from '"
                        + entity.getSimpleName() + "'?\n"
                        + "Its getter/setter/adder/remover methods will be removed from the source.",
                "Delete");
        if (!ModelEditingDialogs.confirm(app.getFrame(), "Delete Property", confirm)) {
            return;
        }

        try {
            property.remove();
        } catch (IOException | RuntimeException e) {
            ModelEditingSupport.error(app, "Delete Property",
                    "Could not delete property '" + property.getPropertyIdentifier()
                            + "':\n" + e.getMessage());
            return;
        }

        app.rebuildProject(project, () -> {
            SourceModelEntity refreshed = model.getEntity(entityQN);
            if (refreshed != null) {
                app.selectInBrowser(refreshed);
            }
        });
    }
}
