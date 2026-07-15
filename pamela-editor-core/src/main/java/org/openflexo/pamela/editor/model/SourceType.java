package org.openflexo.pamela.editor.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

/**
 * Value object representing the type of a {@link SourceModelProperty}.
 * Wraps a Spoon {@code CtTypeReference<?>} and pre-computes the information
 * needed by the editor.
 *
 * <p>This class is <em>not</em> a {@link SourceElement}: it has no place in
 * the containment hierarchy. It is a thin, immutable wrapper used as a field
 * on {@link SourceModelProperty}.</p>
 *
 * <p>The {@link #modelEntity} field is initially {@code null} after construction
 * and is set during Phase 3 of {@link SourceMetaModel} construction once all
 * entities have been discovered.</p>
 */
public final class SourceType {

    /**
     * Qualified names of the non-primitive, non-enum types for which PAMELA's own
     * {@code org.openflexo.pamela.model.StringConverterLibrary} registers a converter
     * (see {@code preferences-design.md §7bis} for the same list applied to preference
     * fields). Kept in sync by hand with that registry — it is a fixed, small set.
     */
    private static final Set<String> CONVERTER_SUPPORTED_TYPES = new HashSet<>(Arrays.asList(
            "java.lang.String",
            "java.lang.Boolean",
            "java.lang.Byte",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.Number",
            "java.lang.Class",
            "java.util.Date",
            "java.net.URL",
            "java.io.File",
            "java.awt.Point",
            "java.awt.Color",
            "java.awt.Font"));

    // Internal Spoon reference — never exposed in the public API
    private final CtTypeReference<?> ctTypeReference;

    // Resolved during Phase 3 — null if the type is not a discovered @ModelEntity
    private SourceModelEntity modelEntity;

    // Pre-computed from ctTypeReference
    private final String qualifiedName;
    private final String simpleName;
    private final boolean primitive;
    private final boolean array;
    private final boolean enumType;
    private final List<SourceType> typeArguments;

    /**
     * Builds a {@code SourceType} from a Spoon type reference.
     * Type arguments are recursively wrapped as {@code SourceType}s.
     *
     * @param ctTypeReference the Spoon reference (must not be {@code null})
     */
    public SourceType(CtTypeReference<?> ctTypeReference) {
        this.ctTypeReference = ctTypeReference;
        this.qualifiedName = ctTypeReference.getQualifiedName();
        this.simpleName = ctTypeReference.getSimpleName();
        this.primitive = ctTypeReference.isPrimitive();
        this.array = ctTypeReference.isArray();
        this.enumType = computeIsEnum(ctTypeReference);

        List<CtTypeReference<?>> ctArgs = ctTypeReference.getActualTypeArguments();
        if (ctArgs == null || ctArgs.isEmpty()) {
            this.typeArguments = Collections.emptyList();
        } else {
            List<SourceType> args = new ArrayList<>(ctArgs.size());
            for (CtTypeReference<?> arg : ctArgs) {
                args.add(new SourceType(arg));
            }
            this.typeArguments = Collections.unmodifiableList(args);
        }
    }

    private static boolean computeIsEnum(CtTypeReference<?> ctTypeReference) {
        if (ctTypeReference.isPrimitive() || ctTypeReference.isArray()) {
            return false;
        }
        try {
            CtType<?> declaration = ctTypeReference.getTypeDeclaration();
            return declaration instanceof CtEnum;
        }
        catch (Exception e) {
            // Unresolvable type (e.g. no classpath configured) — not an enum as far as we can tell.
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Fully qualified type name, e.g. {@code "java.lang.String"} or {@code "org.example.Person"}. */
    public String getQualifiedName() {
        return qualifiedName;
    }

    /** Simple type name, e.g. {@code "String"} or {@code "Person"}. */
    public String getSimpleName() {
        return simpleName;
    }

    /** {@code true} if this is a Java primitive type ({@code int}, {@code boolean}, etc.). */
    public boolean isPrimitive() {
        return primitive;
    }

    /** {@code true} if this is an array type ({@code int[]}, {@code String[][]}, etc.). */
    public boolean isArray() {
        return array;
    }

    /** {@code true} if this type resolves to a Java {@code enum} declaration. */
    public boolean isEnum() {
        return enumType;
    }

    /**
     * {@code true} if a value of this type can be converted to/from a {@code String} —
     * i.e. it is legal as a PAMELA {@code @Getter(defaultValue = ...)}. Mirrors what
     * PAMELA's own {@code StringConverterLibrary} (plus its built-in enum handling)
     * accepts at runtime: a primitive, an enum, or one of the small set of JDK types for
     * which a converter is registered. A {@code @ModelEntity} reference, a collection, or
     * any other arbitrary type is not convertible.
     */
    public boolean isStringConvertible() {
        return primitive || enumType || CONVERTER_SUPPORTED_TYPES.contains(qualifiedName);
    }

    /**
     * The generic type arguments of this type, e.g. for {@code List<Person>} returns
     * a single-element list containing the {@code SourceType} for {@code Person}.
     * Returns an empty list when there are no type arguments.
     */
    public List<SourceType> getTypeArguments() {
        return typeArguments;
    }

    /**
     * The model entity corresponding to this type, or {@code null} if the type is
     * not a discovered {@code @ModelEntity}.  Set during Phase 3 of
     * {@link SourceMetaModel} construction.
     */
    public SourceModelEntity getModelEntity() {
        return modelEntity;
    }

    /**
     * Called during Phase 3 of {@link SourceMetaModel} construction to link
     * this type to its discovered entity.
     *
     * @param entity the discovered entity (must not be {@code null})
     */
    void setModelEntity(SourceModelEntity entity) {
        this.modelEntity = entity;
    }

    /**
     * Checks whether this type is a subtype of {@code other},
     * delegating to Spoon's static analysis.
     *
     * @param other the candidate supertype
     * @return {@code true} if this type is a subtype of {@code other}
     */
    public boolean isSubtypeOf(SourceType other) {
        return ctTypeReference.isSubtypeOf(other.ctTypeReference);
    }

    @Override
    public String toString() {
        return qualifiedName;
    }
}
