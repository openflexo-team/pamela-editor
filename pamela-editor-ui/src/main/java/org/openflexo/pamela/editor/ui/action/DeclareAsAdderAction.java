package org.openflexo.pamela.editor.ui.action;

import java.io.IOException;
import java.util.List;

import org.openflexo.pamela.editor.model.SourceModelProperty;

/** Attaches an existing {@code method(T)} as the {@code @Adder} of a LIST property of element type {@code T}. */
public class DeclareAsAdderAction extends DeclareAccessorAction {

    @Override
    public String getLabel() {
        return "Declare as Adder of…";
    }

    @Override
    protected String getDialogTitle() {
        return "Declare Method as Adder";
    }

    @Override
    protected String roleNoun() {
        return "adder";
    }

    @Override
    protected List<SourceModelProperty> candidateProperties(PromotableMethod pm) {
        return listPropertiesOfElementType(pm, p -> p.getAdderMethodName() == null);
    }

    @Override
    protected void attach(SourceModelProperty target, String methodName) throws IOException {
        target.attachAdder(methodName);
    }
}
