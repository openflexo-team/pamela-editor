package org.openflexo.pamela.editor.ui.action;

import java.io.IOException;
import java.util.List;

import org.openflexo.pamela.editor.model.SourceModelProperty;

/** Attaches an existing {@code method(T)} as the {@code @Setter} of a SINGLE property of type {@code T}. */
public class DeclareAsSetterAction extends DeclareAccessorAction {

    @Override
    public String getLabel() {
        return "Declare as Setter of…";
    }

    @Override
    protected String getDialogTitle() {
        return "Declare Method as Setter";
    }

    @Override
    protected String roleNoun() {
        return "setter";
    }

    @Override
    protected List<SourceModelProperty> candidateProperties(PromotableMethod pm) {
        return singlePropertiesOfParamType(pm, p -> p.getSetterMethodName() == null);
    }

    @Override
    protected void attach(SourceModelProperty target, String methodName) throws IOException {
        target.attachSetter(methodName);
    }
}
