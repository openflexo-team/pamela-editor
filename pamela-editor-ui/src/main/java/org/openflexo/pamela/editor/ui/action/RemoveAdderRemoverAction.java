package org.openflexo.pamela.editor.ui.action;

import java.util.function.Supplier;

import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Removes the {@code @Adder}/{@code @Remover} accessors of a LIST property (C8).
 * Toggle action; delegates to {@link SourceModelProperty#removeAdderRemover}.
 */
public class RemoveAdderRemoverAction extends SourceEditingAction {

    @Override
    public String getLabel() {
        return "Remove Adder + Remover";
    }

    @Override
    public boolean isApplicable(Object target) {
        if (!(target instanceof SourceModelProperty)) {
            return false;
        }
        SourceModelProperty p = (SourceModelProperty) target;
        return p.getCardinality() == Cardinality.LIST
                && (p.getAdderMethodName() != null || p.getRemoverMethodName() != null);
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelProperty p = (SourceModelProperty) target;
        SourceModelEntity entity = p.getModelEntity();
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();
        p.removeAdderRemover();
        return () -> model.getEntity(entityQN);
    }
}
