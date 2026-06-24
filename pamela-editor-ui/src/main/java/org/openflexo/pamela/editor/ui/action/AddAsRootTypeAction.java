package org.openflexo.pamela.editor.ui.action;

import java.util.function.Supplier;

import org.openflexo.pamela.editor.model.SourceJavaFile;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Adds the selected {@link SourceJavaFile} as a root type in its metamodel and
 * triggers a background rebuild.
 *
 * <p>Only shown for Java files that contain an {@code @ModelEntity} annotation
 * ({@link SourceJavaFile#isPotentialModelEntity()}) and are not already
 * registered as a root type.</p>
 */
public class AddAsRootTypeAction extends SourceEditingAction {

    @Override
    public String getLabel() {
        return "Add as Root Type";
    }

    @Override
    public boolean isApplicable(Object target) {
        if (!(target instanceof SourceJavaFile)) {
            return false;
        }
        SourceJavaFile file = (SourceJavaFile) target;
        if (!file.isPotentialModelEntity()) {
            return false;
        }
        SourceMetaModel model = file.getMetaModel();
        return model != null
                && !model.getRootTypeNames().contains(file.getQualifiedName());
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceJavaFile file = (SourceJavaFile) target;
        file.getMetaModel().addRootTypeName(file.getQualifiedName());
        return null; // no reselection
    }
}
