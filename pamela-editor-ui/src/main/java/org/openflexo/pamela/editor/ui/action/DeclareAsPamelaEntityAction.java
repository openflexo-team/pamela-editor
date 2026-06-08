package org.openflexo.pamela.editor.ui.action;

import java.io.IOException;

import javax.swing.JOptionPane;

import org.openflexo.pamela.editor.model.SourceJavaFile;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Contextual action: promotes a plain {@link SourceJavaFile} into a PAMELA
 * entity by inserting an {@code @ModelEntity} annotation into its source.
 *
 * <p>The heavy lifting (resolving the Spoon type, mutating the AST, writing the
 * file, registering the root type) is delegated to
 * {@link SourceMetaModel#declareAsEntity(SourceJavaFile)} — a fast, EDT-safe
 * operation. The subsequent full re-analysis is run off the EDT via
 * {@link PamelaEditorApplication#rebuildProject(PamelaProject)}.</p>
 *
 * <p>Shown for any Java file that does not yet carry an {@code @ModelEntity}
 * annotation. Applicable to both classes and interfaces; a class will surface a
 * validation error after the rebuild (handled by the normal validation flow).</p>
 */
public class DeclareAsPamelaEntityAction implements ContextualAction {

    @Override
    public String getLabel() {
        return "Declare as PAMELA Entity";
    }

    @Override
    public boolean isApplicable(Object target) {
        if (!(target instanceof SourceJavaFile)) {
            return false;
        }
        SourceJavaFile file = (SourceJavaFile) target;
        // Not already a (potential) entity, and the file belongs to a metamodel.
        return !file.isPotentialModelEntity() && file.getMetaModel() != null;
    }

    @Override
    public void perform(Object target, PamelaEditorApplication app) {
        SourceJavaFile file = (SourceJavaFile) target;
        SourceMetaModel model = file.getMetaModel();
        if (model == null) {
            return;
        }

        try {
            model.declareAsEntity(file);
        } catch (IOException | IllegalStateException e) {
            JOptionPane.showMessageDialog(app.getFrame(),
                    "Could not declare '" + file.getQualifiedName()
                            + "' as a PAMELA entity:\n" + e.getMessage(),
                    "Declare as PAMELA Entity", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Re-run Spoon analysis in the background so the new entity materialises.
        PamelaProject project = findProject(model, app);
        if (project != null) {
            app.rebuildProject(project);
        }
    }

    // -------------------------------------------------------------------------

    private static PamelaProject findProject(SourceMetaModel model,
                                             PamelaEditorApplication app) {
        for (PamelaProject p : app.getProjects()) {
            if (p.getMetaModel() == model) {
                return p;
            }
        }
        return null;
    }
}
