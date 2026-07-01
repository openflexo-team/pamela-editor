package org.openflexo.pamela.editor.model;

/**
 * Describes how PAMELA property identifiers are written in the source of an entity.
 *
 * <p>A property identifier is a {@code String}. Two source styles coexist:
 * <ul>
 *   <li><b>{@link #LITERAL}</b>: {@code @Getter(value = "outgoingEdges")} — the key is a string
 *       literal.</li>
 *   <li><b>{@link #CONSTANT}</b>: a {@code public static final String OUTGOING_EDGES =
 *       "outgoingEdges";} field referenced as {@code @Getter(value = OUTGOING_EDGES)} — the
 *       recommended idiom (single source of truth, refactor-safe, reusable in {@code inverse},
 *       {@code @Parameter}, {@code @Embedded}).</li>
 * </ul>
 *
 * <p>Per <em>property</em> the style is always {@link #CONSTANT} or {@link #LITERAL} (decided by
 * the raw {@code @Getter.value} AST: a {@code CtFieldRead} → {@code CONSTANT}, a {@code CtLiteral}
 * → {@code LITERAL}). Per <em>entity</em> the style is the aggregate over the declared properties:
 * all-constant → {@code CONSTANT}, all-literal → {@code LITERAL}, both → {@link #MIXED}, no
 * declared property → {@link #UNDETERMINED}.</p>
 *
 * <p>This is derived data (recomputed on each build, never serialized). See
 * {@code property-identifier-constant-design.md}.</p>
 */
public enum PropertyIdentifierStyle {

    /** All declared properties reference a {@code static final String} constant. */
    CONSTANT,

    /** All declared properties use string literals. */
    LITERAL,

    /** Both styles are present, with no clean dominant. */
    MIXED,

    /** The entity declares no property → nothing can be inferred. */
    UNDETERMINED;

    /** The default style to use when generating on an {@link #UNDETERMINED} entity. */
    public static final PropertyIdentifierStyle DEFAULT = CONSTANT;
}
