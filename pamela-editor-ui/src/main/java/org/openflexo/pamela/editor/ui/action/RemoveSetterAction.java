package org.openflexo.pamela.editor.ui.action;
import org.openflexo.icon.IconMarker;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import java.util.function.Supplier;

import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Removes the {@code @Setter} accessor of a SINGLE property, making it
 * read-only (C8). Toggle action; delegates to {@link SourceModelProperty#removeSetter}.
 */
public class RemoveSetterAction extends SourceEditingAction {


    @Override
    public ActionGroup getGroup() {
        return ActionGroup.GENERATE;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.METHOD_ICON;
    }

    @Override
    protected IconMarker[] getMarkers() {
        return new IconMarker[] { PamelaEditorIconLibrary.MINUS };
    }

    @Override
    public String getLabel() {
        return loc("remove_setter_action");
    }

    @Override
    public boolean isApplicable(Object target) {
        if (!(target instanceof SourceModelProperty)) {
            return false;
        }
        SourceModelProperty p = (SourceModelProperty) target;
        return p.getCardinality() == Cardinality.SINGLE && p.getSetterMethodName() != null;
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelProperty p = (SourceModelProperty) target;
        SourceModelEntity entity = p.getModelEntity();
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();
        p.removeSetter();
        return () -> model.getEntity(entityQN);
    }
}
