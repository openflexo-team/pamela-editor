package org.openflexo.pamela.editor.model;

import spoon.reflect.declaration.CtMethod;

/**
 * Represents a method in a {@link SourceImplementationClass} that is
 * <em>not</em> managed by PAMELA (i.e., not annotated with {@code @Getter},
 * {@code @Setter}, {@code @Adder}, etc.).
 *
 * <p>This is where developer logic lives — {@code toString()}, computed helpers,
 * business rules, overrides of PAMELA-generated method signatures, etc.</p>
 */
public class SourceCustomMethod implements SourceElement {

    // Internal Spoon reference — never exposed in the public API
    private final CtMethod<?> ctMethod;

    private final SourceImplementationClass implementationClass;
    private final String methodName;
    private final String signature;
    private final boolean override;

    /**
     * Constructs a {@code SourceCustomMethod}.
     *
     * @param ctMethod            the Spoon method node (must not be {@code null})
     * @param implementationClass the owning implementation class
     */
    public SourceCustomMethod(CtMethod<?> ctMethod, SourceImplementationClass implementationClass) {
        this.ctMethod = ctMethod;
        this.implementationClass = implementationClass;
        this.methodName = ctMethod.getSimpleName();
        this.signature = buildSignature(ctMethod);
        this.override = ctMethod.getAnnotation(Override.class) != null;
    }

    /**
     * Builds a human-readable signature string from the Spoon method node,
     * e.g. {@code "public String getGreeting()"}.
     */
    private static String buildSignature(CtMethod<?> method) {
        StringBuilder sb = new StringBuilder();
        // Visibility modifier
        sb.append(method.getVisibility() != null ? method.getVisibility().toString() : "");
        if (sb.length() > 0) {
            sb.append(' ');
        }
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

    /** The implementation class that owns this method. */
    public SourceImplementationClass getImplementationClass() {
        return implementationClass;
    }

    /** The simple name of the method, e.g. {@code "getGreeting"}. */
    public String getMethodName() {
        return methodName;
    }

    /**
     * A human-readable signature string, e.g.
     * {@code "public String getGreeting()"}.
     */
    public String getSignature() {
        return signature;
    }

    /**
     * {@code true} if the method is annotated with {@code @Override},
     * meaning it overrides a method declared on the {@code @ModelEntity}
     * interface (or one of its supers).
     */
    public boolean isOverride() {
        return override;
    }

    @Override
    public String toString() {
        return "SourceCustomMethod(" + signature + ")";
    }
}
