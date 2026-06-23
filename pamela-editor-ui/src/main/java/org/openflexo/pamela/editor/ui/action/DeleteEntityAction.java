package org.openflexo.pamela.editor.ui.action;

import java.io.IOException;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.pamela.editor.ui.dialog.ConfirmParameters;
import org.openflexo.pamela.editor.ui.dialog.ModelEditingDialogs;

/**
 * Contextual action: deletes a {@link SourceModelEntity}, removing its backing
 * {@code .java} file from disk after confirmation.
 *
 * <p>Delegates to {@link SourceModelEntity#delete}. If the entity was a
 * registered root type, it is unregistered here so the rebuild does not log a
 * warning about an unresolvable root.</p>
 */
public class DeleteEntityAction implements ContextualAction {

    @Override
    public String getLabel() {
        return "Delete Entity";
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceModelEntity;
    }

    @Override
    public void perform(Object target, PamelaEditorApplication app) {
        SourceModelEntity entity = (SourceModelEntity) target;
        SourceMetaModel model = entity.getMetaModel();
        PamelaProject project = app.getProjectForElement(entity);
        if (model == null || project == null) {
            return;
        }

        final String qualifiedName = entity.getQualifiedName();
        SourcePackage pkg = entity.getSourcePackage();
        final String packageName = pkg != null ? pkg.getQualifiedName() : null;

        ConfirmParameters confirm = new ConfirmParameters(
                "Delete entity '" + qualifiedName + "' and its source file from disk?\n"
                        + "This cannot be undone.",
                "Delete");
        if (!ModelEditingDialogs.confirm(app.getFrame(), "Delete Entity", confirm)) {
            return;
        }

        try {
            entity.delete();
            model.removeRootTypeName(qualifiedName);
        } catch (IOException | RuntimeException e) {
            ModelEditingSupport.error(app, "Delete Entity",
                    "Could not delete '" + qualifiedName + "':\n" + e.getMessage());
            return;
        }

        app.rebuildProject(project, () -> {
            // Selection had pointed at the now-deleted entity; fall back to its package.
            Object fallback = packageName != null ? model.getPackage(packageName) : model;
            if (fallback != null) {
                app.selectInBrowser(fallback);
            }
        });
    }
}
