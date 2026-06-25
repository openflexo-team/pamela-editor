package org.openflexo.pamela.editor.model;

import java.util.Collections;
import java.util.List;

/**
 * A minimal, Spoon-free description of a method declared on an entity interface:
 * its name and the qualified names of its parameter types. Exposed by
 * {@link SourceModelEntity#getCandidateAccessorMethods()} so the UI can decide, by
 * <strong>signature</strong>, which plain methods could become a property's accessors
 * when a getter is promoted (model-editing-design.md §3.3a) — without touching Spoon.
 */
public final class MethodSignature {

    private final String methodName;
    private final List<String> parameterTypeQualifiedNames;

    public MethodSignature(String methodName, List<String> parameterTypeQualifiedNames) {
        this.methodName = methodName;
        this.parameterTypeQualifiedNames = parameterTypeQualifiedNames == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(parameterTypeQualifiedNames);
    }

    public String getMethodName() {
        return methodName;
    }

    public int getParameterCount() {
        return parameterTypeQualifiedNames.size();
    }

    /** Qualified name of parameter {@code i}, or {@code null} if out of range. */
    public String getParameterTypeQualifiedName(int i) {
        return i >= 0 && i < parameterTypeQualifiedNames.size()
                ? parameterTypeQualifiedNames.get(i) : null;
    }
}
