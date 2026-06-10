package org.openflexo.pamela.editor.diagram;

import org.openflexo.pamela.editor.model.SourceModelProperty;

/**
 * Transient, runtime-only value object representing a relationship connector
 * computed from the {@link org.openflexo.pamela.editor.model.SourceMetaModel}.
 *
 * <p>Connectors are NOT stored in the diagram model; they are recomputed every
 * time the drawing structure is walked.  A connector is shown only when both
 * the source and target entities are present as {@link EntityView}s in the
 * diagram.</p>
 *
 * <p>This is not a {@code @ModelEntity}.</p>
 */
public class ComputedConnector {

    /**
     * The visual nature of the relationship.
     */
    public enum RelationshipType {
        /** Interface A extends interface B — hollow triangle arrowhead. */
        INHERITANCE,
        /** A has a property of type B, without {@code @Embedded} — open arrowhead. */
        ASSOCIATION,
        /** A has a property of type B, with {@code @Embedded} — filled diamond at source. */
        COMPOSITION
    }

    private final RelationshipType type;
    private final EntityView sourceView;
    private final EntityView targetView;
    /** The property that causes the relationship; {@code null} for {@link RelationshipType#INHERITANCE}. */
    private final SourceModelProperty property;

    public ComputedConnector(RelationshipType type,
                             EntityView sourceView,
                             EntityView targetView,
                             SourceModelProperty property) {
        this.type = type;
        this.sourceView = sourceView;
        this.targetView = targetView;
        this.property = property;
    }

    public RelationshipType getType() {
        return type;
    }

    public EntityView getSourceView() {
        return sourceView;
    }

    public EntityView getTargetView() {
        return targetView;
    }

    /** {@code null} for {@link RelationshipType#INHERITANCE}. */
    public SourceModelProperty getProperty() {
        return property;
    }

    /**
     * Equality by value — the relationship type and the two endpoint views (by
     * identity, stable across rebuilds) and the originating property (by identity).
     * Diana reconciles drawables by {@code drawable.getDrawable() == ...}, so the
     * drawing interns connectors by value to hand back a stable instance across walks
     * (otherwise each walk would create duplicate/orphan connector nodes and Diana
     * would log "something strange … see isValid()").
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ComputedConnector)) {
            return false;
        }
        ComputedConnector that = (ComputedConnector) o;
        return type == that.type
                && sourceView == that.sourceView
                && targetView == that.targetView
                && property == that.property;
    }

    @Override
    public int hashCode() {
        int h = type.hashCode();
        h = 31 * h + System.identityHashCode(sourceView);
        h = 31 * h + System.identityHashCode(targetView);
        h = 31 * h + System.identityHashCode(property);
        return h;
    }

    @Override
    public String toString() {
        return "ComputedConnector(" + type + ", "
                + (sourceView.getQualifiedName()) + " → "
                + (targetView.getQualifiedName()) + ")";
    }
}
