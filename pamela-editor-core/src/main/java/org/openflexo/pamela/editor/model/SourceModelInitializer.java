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

    @Override
    public String toString() {
        return "SourceModelInitializer(" + methodName + parameters + ")";
    }
}
