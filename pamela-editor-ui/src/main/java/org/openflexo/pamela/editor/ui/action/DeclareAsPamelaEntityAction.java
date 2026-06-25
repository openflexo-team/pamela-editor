package org.openflexo.pamela.editor.ui.action;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import java.util.function.Supplier;

import org.openflexo.pamela.editor.model.SourceJavaFile;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Promotes a plain {@link SourceJavaFile} into a PAMELA entity by inserting an
 * {@code @ModelEntity} annotation into its source (the "promote" path of
 * {@code model-editing-design.md §1.1} for C1).
 *
 * <p>Delegates to {@link SourceMetaModel#declareAsEntity}, which also registers
 * the type as a root so the new {@link org.openflexo.pamela.editor.model.SourceModelEntity}
 * materialises on the rebuild. Shown for any Java file not yet an entity.</p>
 */
public class DeclareAsPamelaEntityAction extends SourceEditingAction {


    @Override
    public ActionGroup getGroup() {
        return ActionGroup.PROMOTE;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.ENTITY_ICON;
    }

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
        return !file.isPotentialModelEntity() && file.getMetaModel() != null;
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceJavaFile file = (SourceJavaFile) target;
        SourceMetaModel model = file.getMetaModel();
        final String qualifiedName = file.getQualifiedName();
        model.declareAsEntity(file);
        // Keep the selection anchored on the same (now-entity) node.
        return () -> model.getEntity(qualifiedName);
    }
}
