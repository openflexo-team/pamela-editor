package org.openflexo.pamela.editor.ui.action;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import java.util.function.Supplier;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.pamela.editor.ui.dialog.ConfirmParameters;
import org.openflexo.pamela.editor.ui.dialog.ModelEditingDialogs;

/**
 * Deletes a {@link SourceModelEntity}, removing its backing {@code .java} file
 * from disk after confirmation. Delegates to {@link SourceModelEntity#delete}
 * and unregisters the type as a root if it was one.
 */
public class DeleteEntityAction extends SourceEditingAction {


    @Override
    public ActionGroup getGroup() {
        return ActionGroup.DELETE;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.ENTITY_ICON;
    }

    @Override
    public String getLabel() {
        return "Delete Entity";
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceModelEntity;
    }

    @Override
    protected boolean confirmPerform(Object target, PamelaEditorApplication app) {
        SourceModelEntity entity = (SourceModelEntity) target;
        ConfirmParameters confirm = new ConfirmParameters(
                "Delete entity '" + entity.getQualifiedName() + "' and its source file from disk?\n"
                        + "This cannot be undone.",
                "Delete");
        return ModelEditingDialogs.confirm(app.getFrame(), "Delete Entity", confirm);
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelEntity entity = (SourceModelEntity) target;
        SourceMetaModel model = entity.getMetaModel();
        final String qualifiedName = entity.getQualifiedName();
        SourcePackage pkg = entity.getSourcePackage();
        final String packageName = pkg != null ? pkg.getQualifiedName() : null;

        entity.delete();
        model.removeRootTypeName(qualifiedName);
        // Selection fell on the deleted entity; fall back to its package.
        return () -> packageName != null ? model.getPackage(packageName) : model;
    }
}
