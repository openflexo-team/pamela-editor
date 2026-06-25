package org.openflexo.pamela.editor.ui.action;

import java.io.IOException;
import java.util.List;

import org.openflexo.pamela.editor.model.SourceModelProperty;

/** Attaches an existing {@code method(T)} as the {@code @Updater} of a SINGLE property of type {@code T}. */
public class DeclareAsUpdaterAction extends DeclareAccessorAction {

    @Override
    public String getLabel() {
        return "Declare as Updater of…";
    }

    @Override
    protected String getDialogTitle() {
        return "Declare Method as Updater";
    }

    @Override
    protected String roleNoun() {
        return "updater";
    }

    @Override
    protected List<SourceModelProperty> candidateProperties(PromotableMethod pm) {
        return singlePropertiesOfParamType(pm, p -> p.getUpdaterMethodName() == null);
    }

    @Override
    protected void attach(SourceModelProperty target, String methodName) throws IOException {
        target.attachUpdater(methodName);
    }
}
