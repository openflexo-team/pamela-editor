package org.openflexo.pamela.editor.model;

import spoon.reflect.declaration.CtMethod;

/**
 * Represents a significant <em>operation</em> of a {@link SourceModelEntity} — a method
 * declared on the entity <em>interface</em> that is not a PAMELA property accessor
 * ({@code @Getter/@Setter/@Adder/@Remover/@Reindexer/@Updater}) and not an
 * {@code @Initializer}.
 *
 * <p>Conceptually the API of a PAMELA entity is its interface, so custom methods are sourced
 * from the interface, not from the implementation class. The implementation class is, at most,
 * a filter hint and a navigation target (see {@code custom-method-design.md}). Examples:
 * {@code @Finder} queries, {@code @Deleter} destructors, {@code @Operation}-marked or plain
 * hand-written business methods.</p>
 */
public class SourceCustomMethod implements SourceElement {

    /** The kind of operation, derived from the carried PAMELA annotation (if any). */
    public enum Kind {
        /** Carries {@code @Finder}. */
        FINDER,
        /** Carries {@code @Deleter}. */
        DELETER,
        /** Carries {@code @Operation}. */
        OPERATION,
        /** No PAMELA operation annotation. */
        PLAIN
    }

    // Internal Spoon references — never exposed in the public API
    private final CtMethod<?> ctMethod;       // the interface method (identity)
    private final CtMethod<?> ctImplMethod;   // optional impl method (navigation / filter), may be null

    private final SourceModelEntity entity;
    private final Kind kind;
    private final String methodName;
    private final String signature;

    /**
     * Constructs a {@code SourceCustomMethod}.
     *
     * @param ctMethod     the interface method node (must not be {@code null}) — the identity
     * @param entity       the owning {@code @ModelEntity}
     * @param kind         the operation kind (from the carried annotation, or {@link Kind#PLAIN})
     * @param ctImplMethod the matching implementation-class method, or {@code null} if none
     */
    public SourceCustomMethod(CtMethod<?> ctMethod, SourceModelEntity entity, Kind kind,
            CtMethod<?> ctImplMethod) {
        this.ctMethod = ctMethod;
        this.entity = entity;
        this.kind = kind;
        this.ctImplMethod = ctImplMethod;
        this.methodName = ctMethod.getSimpleName();
        this.signature = buildSignature(ctMethod);
    }

    /**
     * Builds a human-readable signature string from the Spoon method node,
     * e.g. {@code "Edge getEdgeNamed(String name)"}.
     */
    private static String buildSignature(CtMethod<?> method) {
        StringBuilder sb = new StringBuilder();
        // Return type
        sb.append(method.getType() != null ? method.getType().getSimpleName() : "void");
        sb.append(' ');
        // Method name
        sb.append(method.getSimpleName());
        // Parameters
        sb.append('(');
        boolean first = true;
        for (spoon.reflect.declaration.CtParameter<?> param : method.getParameters()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(param.getType() != null ? param.getType().getSimpleName() : "?");
            sb.append(' ');
            sb.append(param.getSimpleName());
            first = false;
        }
        sb.append(')');
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** The {@code @ModelEntity} that declares this operation. */
    public SourceModelEntity getEntity() {
        return entity;
    }

    /** The kind of operation (finder / deleter / operation / plain). */
    public Kind getKind() {
        return kind;
    }

    /** The simple name of the method, e.g. {@code "getEdgeNamed"}. */
    public String getMethodName() {
        return methodName;
    }

    /**
     * A human-readable signature string, e.g. {@code "Edge getEdgeNamed(String name)"}.
     */
    public String getSignature() {
        return signature;
    }

    /**
     * {@code true} if this operation has a hand-written body in the implementation class
     * (used by the {@link CustomMethodFilter#IMPLEMENTED_IN_IMPL} filter and as a navigation hint).
     */
    public boolean isImplemented() {
        return ctImplMethod != null && ctImplMethod.getBody() != null;
    }

    @Override
    public String toString() {
        return "SourceCustomMethod(" + signature + ")";
    }
}
