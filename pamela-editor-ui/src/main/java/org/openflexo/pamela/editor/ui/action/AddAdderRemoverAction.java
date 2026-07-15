package org.openflexo.pamela.editor.ui.action;
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
 * Adds the {@code @Adder}/{@code @Remover} accessors to a LIST property that is
 * missing at least one (C8). Toggle action; delegates to
 * {@link SourceModelProperty#addAdderRemover}.
 */
public class AddAdderRemoverAction extends SourceEditingAction {


    @Override
    public ActionGroup getGroup() {
        return ActionGroup.GENERATE;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.METHOD_ICON;
    }

    @Override
    public String getLabel() {
        return loc("add_adder_remover_action");
    }

    @Override
    public boolean isApplicable(Object target) {
        if (!(target instanceof SourceModelProperty)) {
            return false;
        }
        SourceModelProperty p = (SourceModelProperty) target;
        return p.getCardinality() == Cardinality.LIST
                && (p.getAdderMethodName() == null || p.getRemoverMethodName() == null);
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelProperty p = (SourceModelProperty) target;
        SourceModelEntity entity = p.getModelEntity();
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();
        p.addAdderRemover();
        return () -> model.getEntity(entityQN);
    }
}
