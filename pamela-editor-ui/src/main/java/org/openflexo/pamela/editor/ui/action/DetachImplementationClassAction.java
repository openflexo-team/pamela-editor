package org.openflexo.pamela.editor.ui.action;
import org.openflexo.icon.IconMarker;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import java.util.function.Supplier;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Removes an entity's {@code @ImplementationClass} link (the class file itself is left
 * untouched — only the annotation is dropped). No-dialog toggle action (a single reference,
 * nothing to pick — unlike the list-shaped super-entities/imports), mirrors
 * {@link RemoveSetterAction}. Delegates to {@link SourceModelEntity#detachImplementationClass}.
 */
public class DetachImplementationClassAction extends SourceEditingAction {

    @Override
    public ActionGroup getGroup() {
        return ActionGroup.REFACTOR;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.ENTITY_ICON;
    }

    @Override
    protected IconMarker[] getMarkers() {
        return new IconMarker[] { PamelaEditorIconLibrary.MINUS };
    }

    @Override
    public String getLabel() {
        return loc("detach_implementation_class_action");
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceModelEntity
                && ((SourceModelEntity) target).getImplementationClass() != null;
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelEntity entity = (SourceModelEntity) target;
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();
        entity.detachImplementationClass();
        return () -> model.getEntity(entityQN);
    }
}
