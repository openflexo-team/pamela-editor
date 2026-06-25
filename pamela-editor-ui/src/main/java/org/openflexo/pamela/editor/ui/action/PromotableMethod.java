package org.openflexo.pamela.editor.ui.action;

import java.util.ArrayList;
import java.util.List;

import org.openflexo.pamela.editor.model.SourceModelEntity;

import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtTypeReference;

/**
 * A contextual-menu facet for a <em>plain (not-yet-PAMELA) method</em> of an entity interface,
 * carrying its <strong>signature</strong> so the method-level "promote" actions can decide
 * applicability from parameter/return types — never from the method name, which need not follow
 * the {@code getXxx}/{@code setXxx}/… conventions (model-editing-design.md §3.3).
 *
 * <p>Emitted by the {@link org.openflexo.pamela.editor.ui.widget.SpoonOutlineController Spoon
 * outline} when the user right-clicks a method that backs no existing PAMELA concept. It is a
 * transient UI facet (never part of the {@code Source*} model): it holds only the signature data
 * (strings) plus the owning entity, so it survives one EDT cycle (menu → dialog → mutation) and is
 * re-resolved against the entity by {@code (methodName, paramCount)} when the mutation runs.</p>
 */
public final class PromotableMethod {

    private final SourceModelEntity entity;
    private final String methodName;
    private final List<String> parameterTypeQualifiedNames;
    private final List<String> parameterTypeSimpleNames;
    private final List<String> parameterNames;
    private final String returnTypeQualifiedName;
    private final boolean returnsList;
    private final String listElementTypeQualifiedName; // null unless returnsList

    public PromotableMethod(SourceModelEntity entity, CtMethod<?> m) {
        this.entity = entity;
        this.methodName = m.getSimpleName();
        this.parameterTypeQualifiedNames = new ArrayList<>();
        this.parameterTypeSimpleNames = new ArrayList<>();
        this.parameterNames = new ArrayList<>();
        m.getParameters().forEach(p -> {
            CtTypeReference<?> t = p.getType();
            parameterTypeQualifiedNames.add(t != null ? t.getQualifiedName() : null);
            parameterTypeSimpleNames.add(t != null ? t.getSimpleName() : "?");
            parameterNames.add(p.getSimpleName());
        });
        CtTypeReference<?> ret = m.getType();
        this.returnTypeQualifiedName = ret != null ? ret.getQualifiedName() : "void";
        this.returnsList = ret != null && "java.util.List".equals(ret.getQualifiedName());
        this.listElementTypeQualifiedName = returnsList && !ret.getActualTypeArguments().isEmpty()
                ? ret.getActualTypeArguments().get(0).getQualifiedName()
                : null;
    }

    public SourceModelEntity getEntity() {
        return entity;
    }

    public String getMethodName() {
        return methodName;
    }

    public int getParameterCount() {
        return parameterTypeQualifiedNames.size();
    }

    /** Qualified name of parameter {@code i}, or {@code null}. */
    public String getParameterTypeQualifiedName(int i) {
        return i >= 0 && i < parameterTypeQualifiedNames.size()
                ? parameterTypeQualifiedNames.get(i) : null;
    }

    /** Simple type name of parameter {@code i} (for display), or {@code "?"}. */
    public String getParameterTypeSimpleName(int i) {
        return i >= 0 && i < parameterTypeSimpleNames.size()
                ? parameterTypeSimpleNames.get(i) : "?";
    }

    /** Declared name of parameter {@code i}, or {@code null}. */
    public String getParameterName(int i) {
        return i >= 0 && i < parameterNames.size() ? parameterNames.get(i) : null;
    }

    public String getReturnTypeQualifiedName() {
        return returnTypeQualifiedName;
    }

    /** True when the method returns {@code List<T>} (→ a LIST property candidate). */
    public boolean returnsList() {
        return returnsList;
    }

    /** Element type {@code T} of a {@code List<T>} return, or {@code null}. */
    public String getListElementTypeQualifiedName() {
        return listElementTypeQualifiedName;
    }

    /** True when the method has a non-{@code void} return and no parameters (a getter shape). */
    public boolean isValueGetterShape() {
        return parameterTypeQualifiedNames.isEmpty()
                && returnTypeQualifiedName != null
                && !"void".equals(returnTypeQualifiedName);
    }

    /** Whether the {@code i}-th parameter is an {@code int}/{@link Integer} (for a reindexer index). */
    public boolean isIntParameter(int i) {
        String qn = getParameterTypeQualifiedName(i);
        return "int".equals(qn) || "java.lang.Integer".equals(qn);
    }
}
