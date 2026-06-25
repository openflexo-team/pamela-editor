package org.openflexo.pamela.editor.ui.action;

import javax.swing.ImageIcon;

import java.util.function.Supplier;

import org.openflexo.icon.IconMarker;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Marks an existing method as an {@code @Operation} — the editor-facing marker that surfaces a
 * method as a significant operation of the entity's API (custom-method-design.md). No dialog.
 *
 * <p>Applicable (by signature) to a {@link PromotableMethod} that is <em>not</em> a plain value
 * getter — i.e. it has parameters or returns {@code void} — so it does not clutter the
 * property-promotion case on a no-arg getter.</p>
 */
public class DeclareAsOperationAction extends SourceEditingAction {

    @Override
    public String getLabel() {
        return "Declare as Operation";
    }

    @Override
    public ActionGroup getGroup() {
        return ActionGroup.PROMOTE;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.METHOD_ICON;
    }

    @Override
    protected IconMarker[] getMarkers() {
        return new IconMarker[] { PamelaEditorIconLibrary.REINJECT };
    }

    @Override
    public boolean isApplicable(Object target) {
        if (!(target instanceof PromotableMethod)) {
            return false;
        }
        PromotableMethod pm = (PromotableMethod) target;
        return pm.getParameterCount() > 0
                || "void".equals(pm.getReturnTypeQualifiedName());
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        PromotableMethod pm = (PromotableMethod) target;
        SourceModelEntity entity = pm.getEntity();
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();
        entity.declareOperation(pm.getMethodName(), pm.getParameterCount());
        return () -> model.getEntity(entityQN);
    }
}
