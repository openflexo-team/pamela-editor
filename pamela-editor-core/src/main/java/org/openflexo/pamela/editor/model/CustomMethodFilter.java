package org.openflexo.pamela.editor.model;

/**
 * Governs which interface methods of a {@link SourceModelEntity} are surfaced as
 * {@link SourceCustomMethod}s (entity operations) in the editor.
 *
 * <p>The candidate set is always the entity <em>interface</em>'s methods that are neither a
 * property accessor ({@code @Getter/@Setter/@Adder/@Remover/@Reindexer/@Updater}) nor an
 * {@code @Initializer}. This enum decides, among those candidates, which are shown.</p>
 *
 * <p>See {@code custom-method-design.md §5}.</p>
 */
public enum CustomMethodFilter {

    /**
     * Only methods carrying a PAMELA operation annotation
     * ({@code @Finder}, {@code @Deleter}, {@code @Operation}). Default.
     */
    PAMELA_ANNOTATED,

    /** Every candidate interface method (annotated or plain). */
    ALL_INTERFACE_METHODS,

    /**
     * Only candidate interface methods that have a hand-written body in the
     * implementation class (proxy-generated operations such as a bare {@code @Finder}
     * are excluded).
     */
    IMPLEMENTED_IN_IMPL;

    /** The default filter when none is configured. */
    public static final CustomMethodFilter DEFAULT = PAMELA_ANNOTATED;
}
