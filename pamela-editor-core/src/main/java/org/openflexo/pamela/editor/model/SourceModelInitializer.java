package org.openflexo.pamela.editor.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openflexo.pamela.annotations.Parameter;

import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;

/**
 * Represents one {@code @Initializer}-annotated method on a {@link SourceModelEntity}.
 *
 * <p>Each initializer declares zero or more parameters, each annotated with
 * {@code @Parameter("propertyId")} to indicate which property the argument
 * maps to.</p>
 */
public class SourceModelInitializer implements SourceElement {

    // Internal Spoon reference — never exposed in the public API
    private final CtMethod<?> ctMethod;

    private final SourceModelEntity entity;
    private final String methodName;
    private final List<String> parameters;
    private final String signature;

    /**
     * Constructs a {@code SourceModelInitializer} from a Spoon method node.
     *
     * @param ctMethod the {@code @Initializer}-annotated method (must not be {@code null})
     * @param entity   the entity that declares this initializer
     */
    public SourceModelInitializer(CtMethod<?> ctMethod, SourceModelEntity entity) {
        this.ctMethod = ctMethod;
        this.entity = entity;
        this.methodName = ctMethod.getSimpleName();
        this.parameters = extractParameters(ctMethod);
        this.signature = buildSignature(ctMethod);
    }

    /**
     * Extracts property identifiers from {@code @Parameter}-annotated method arguments.
     * Arguments without {@code @Parameter} are represented as empty strings.
     */
    private static List<String> extractParameters(CtMethod<?> method) {
        List<String> result = new ArrayList<>();
        for (CtParameter<?> param : method.getParameters()) {
            Parameter annotation = param.getAnnotation(Parameter.class);
            result.add(annotation != null ? annotation.value() : "");
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Builds a human-readable signature in the same format as {@link SourceCustomMethod}:
     * method name and parameter <em>names only</em> (no return type, no parameter types),
     * e.g. {@code "init(start, end)"}.
     */
    private static String buildSignature(CtMethod<?> method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getSimpleName()).append('(');
        boolean first = true;
        for (CtParameter<?> param : method.getParameters()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(param.getSimpleName());
            first = false;
        }
        sb.append(')');
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** The entity that owns this initializer. */
    public SourceModelEntity getEntity() {
        return entity;
    }

    /** The simple name of the initializer method, e.g. {@code "init"}. */
    public String getMethodName() {
        return methodName;
    }

    /**
     * The property identifiers of this initializer's parameters,
     * in declaration order.  Each value is the {@code @Parameter.value()}
     * of the corresponding argument, or an empty string if the argument
     * carries no {@code @Parameter} annotation.
     */
    public List<String> getParameters() {
        return parameters;
    }

    /** Formatted label, e.g. {@code "init(flexoId,name)"}. */
    public String getDisplayLabel() {
        return methodName + "(" + String.join(",", parameters) + ")";
    }

    /**
     * Human-readable signature aligned with {@link SourceCustomMethod#getSignature()}:
     * return type, name, and parameter names only, e.g. {@code "Edge init(start, end)"}.
     */
    public String getSignature() {
        return signature;
    }

    /**
     * The 1-based line of this method's declaration in its source file, or {@code -1} if the
     * position is unavailable. Disambiguates overloaded initializers (same name, distinct lines).
     */
    public int getDeclarationLine() {
        return (ctMethod.getPosition() != null && ctMethod.getPosition().isValidPosition())
                ? ctMethod.getPosition().getLine() : -1;
    }

    @Override
    public String toString() {
        return "SourceModelInitializer(" + methodName + parameters + ")";
    }
}
