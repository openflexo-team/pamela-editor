package org.openflexo.pamela.editor.ui.action;

import java.io.IOException;
import java.util.List;

import org.openflexo.pamela.editor.model.SourceModelProperty;

/** Attaches an existing {@code method(T)} as the {@code @Remover} of a LIST property of element type {@code T}. */
public class DeclareAsRemoverAction extends DeclareAccessorAction {

    @Override
    public String getLabel() {
        return "Declare as Remover of…";
    }

    @Override
    protected String getDialogTitle() {
        return "Declare Method as Remover";
    }

    @Override
    protected String roleNoun() {
        return "remover";
    }

    @Override
    protected List<SourceModelProperty> candidateProperties(PromotableMethod pm) {
        return listPropertiesOfElementType(pm, p -> p.getRemoverMethodName() == null);
    }

    @Override
    protected void attach(SourceModelProperty target, String methodName) throws IOException {
        target.attachRemover(methodName);
    }
}
