package org.openflexo.pamela.editor.ui.action;

import org.openflexo.pamela.editor.model.SourceJavaFile;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Contextual action: adds the selected {@link SourceJavaFile} as a root type
 * in its metamodel and triggers a background rebuild.
 *
 * <p>Only shown for Java files that:
 * <ul>
 *   <li>contain an {@code @ModelEntity} annotation (detected by
 *       {@link SourceJavaFile#isPotentialModelEntity()}), and</li>
 *   <li>are not already registered as a root type.</li>
 * </ul>
 * </p>
 */
public class AddAsRootTypeAction implements ContextualAction {

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
    public void perform(Object target, PamelaEditorApplication app) {
        SourceJavaFile file = (SourceJavaFile) target;
        SourceMetaModel model = file.getMetaModel();
        if (model == null) {
            return;
        }

        // Register the root type
        model.addRootTypeName(file.getQualifiedName());

        // Find the owning project and trigger a background rebuild
        PamelaProject project = findSession(model, app);
        if (project != null) {
            app.rebuildProject(project);
        }
    }

    // -------------------------------------------------------------------------

    private static PamelaProject findSession(SourceMetaModel model,
                                                    PamelaEditorApplication app) {
        for (PamelaProject s : app.getProjects()) {
            if (s.getMetaModel() == model) {
                return s;
            }
        }
        return null;
    }
}
