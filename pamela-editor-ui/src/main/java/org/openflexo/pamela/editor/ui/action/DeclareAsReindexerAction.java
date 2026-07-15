package org.openflexo.pamela.editor.ui.action;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.openflexo.pamela.editor.model.SourceModelProperty;

/**
 * Attaches an existing {@code method(T, int)} as the {@code @Reindexer} of a LIST property of
 * element type {@code T}. Applicable only when the method has two parameters whose second is an
 * {@code int}/{@link Integer} (signature-based, model-editing-design.md §3.3).
 */
public class DeclareAsReindexerAction extends DeclareAccessorAction {

    @Override
    public String getLabel() {
        return loc("declare_as_reindexer_action");
    }

    @Override
    protected String getDialogTitle() {
        return loc("declare_method_as_reindexer_title");
    }

    @Override
    protected String roleNoun() {
        return "reindexer";
    }

    @Override
    protected List<SourceModelProperty> candidateProperties(PromotableMethod pm) {
        if (pm.getParameterCount() != 2 || !pm.isIntParameter(1)) {
            return Collections.emptyList();
        }
        String elementTypeQN = pm.getParameterTypeQualifiedName(0);
        List<SourceModelProperty> result = new java.util.ArrayList<>();
        for (SourceModelProperty p : pm.getEntity().getDeclaredProperties().values()) {
            boolean isList = p.getCardinality()
                    == org.openflexo.pamela.annotations.Getter.Cardinality.LIST;
            if (isList && p.getReindexerMethodName() == null && p.getType() != null
                    && elementTypeQN != null
                    && elementTypeQN.equals(p.getType().getQualifiedName())) {
                result.add(p);
            }
        }
        return result;
    }

    @Override
    protected void attach(SourceModelProperty target, String methodName) throws IOException {
        target.attachReindexer(methodName);
    }
}
