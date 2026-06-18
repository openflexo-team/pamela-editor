package org.openflexo.pamela.editor.diagram;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.openflexo.diana.connectors.ConnectorSpecification.ConnectorType;
import org.openflexo.diana.connectors.RectPolylinConnectorSpecification.RectPolylinAdjustability;
import org.openflexo.diana.connectors.RectPolylinConnectorSpecification.RectPolylinConstraints;

/**
 * A named, immutable visual style applicable to a connector of a {@link PamelaClassDiagram}.
 *
 * <p>The diagram persists only a <em>reference</em> to a style — its stable {@link #getId() id}
 * stored on the {@link ConnectorView} ({@code styleId}) — never the individual graphical
 * properties. This keeps {@code .diagram} files compact and lets the catalogue of styles
 * evolve without rewriting existing diagrams.</p>
 *
 * <p>A style belongs to a {@link Category}: styles in the {@link Category#INHERITANCE}
 * group apply to inheritance links ({@link InheritanceView}); styles in the
 * {@link Category#RELATIONSHIP} group apply to association / composition links
 * ({@link PropertyView}). A graphical inspector offers a drop-down of the styles of the
 * connector's category (see {@link #stylesFor(Category)}).</p>
 *
 * <p><b>A style governs routing and appearance only</b> — connector type, colour, line
 * width and (for rectilinear routing) the polyline parameters. It does <em>not</em> carry
 * the end symbols: the generalisation triangle, the composition diamond and the association
 * arrow are determined by the connector <em>kind</em>, not by the style, so the two
 * relationship sub-kinds (association / composition) share one style set while keeping
 * their distinct symbols.</p>
 */
public enum ConnectorStyle {

    // --- Inheritance styles (dark blue) ------------------------------------

    /** Default for inheritance: straight line. */
    INHERITANCE_LINE("inheritance-line", Category.INHERITANCE,
            ConnectorType.LINE, new Color(0, 0, 139), 1.5f,
            false, false, -1, null, null),

    /** Orthogonal variant for inheritance: vertical layout, square corners,
     *  straight when possible. */
    INHERITANCE_RECT_POLYLIN("inheritance-rect-polylin", Category.INHERITANCE,
            ConnectorType.RECT_POLYLIN, new Color(0, 0, 139), 1.5f,
            true, false, -1, RectPolylinAdjustability.BASICALLY_ADJUSTABLE,
            RectPolylinConstraints.VERTICAL_LAYOUT),

    // --- Relationship styles (dark gray) -----------------------------------

    /** Default for association / composition: orthogonal routing, rounded corners. */
    RELATIONSHIP_RECT_POLYLIN("relationship-rect-polylin", Category.RELATIONSHIP,
            ConnectorType.RECT_POLYLIN, Color.DARK_GRAY, 1.5f,
            true, true, 10, RectPolylinAdjustability.BASICALLY_ADJUSTABLE,
            RectPolylinConstraints.NONE),

    /** Straight line variant for association / composition. */
    RELATIONSHIP_LINE("relationship-line", Category.RELATIONSHIP,
            ConnectorType.LINE, Color.DARK_GRAY, 1.5f,
            true, false, -1, null, null);

    /** The category a style belongs to — selects which connectors it applies to. */
    public enum Category {
        /** Inheritance links ({@link InheritanceView}). */
        INHERITANCE,
        /** Association / composition links ({@link PropertyView}). */
        RELATIONSHIP
    }

    private final String id;
    private final Category category;
    private final ConnectorType connectorType;
    private final Color color;
    private final float lineWidth;

    // Rectilinear-polyline parameters — meaningful only when connectorType == RECT_POLYLIN.
    private final boolean straightLineWhenPossible;
    private final boolean rounded;
    /** Rounded-corner arc size; {@code < 0} means "leave Diana's default". */
    private final int arcSize;
    private final RectPolylinAdjustability adjustability;
    private final RectPolylinConstraints constraints;

    ConnectorStyle(String id, Category category, ConnectorType connectorType,
                   Color color, float lineWidth,
                   boolean straightLineWhenPossible, boolean rounded, int arcSize,
                   RectPolylinAdjustability adjustability, RectPolylinConstraints constraints) {
        this.id = id;
        this.category = category;
        this.connectorType = connectorType;
        this.color = color;
        this.lineWidth = lineWidth;
        this.straightLineWhenPossible = straightLineWhenPossible;
        this.rounded = rounded;
        this.arcSize = arcSize;
        this.adjustability = adjustability;
        this.constraints = constraints;
    }

    /** Stable identifier — the value persisted on {@link ConnectorView#getStyleId()}. */
    public String getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public ConnectorType getConnectorType() {
        return connectorType;
    }

    public Color getColor() {
        return color;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public boolean isStraightLineWhenPossible() {
        return straightLineWhenPossible;
    }

    public boolean isRounded() {
        return rounded;
    }

    /** Rounded-corner arc size, or {@code < 0} to leave Diana's default. */
    public int getArcSize() {
        return arcSize;
    }

    /** Polyline adjustability — {@code null} for non-{@code RECT_POLYLIN} styles. */
    public RectPolylinAdjustability getAdjustability() {
        return adjustability;
    }

    /** Polyline layout constraints — {@code null} for non-{@code RECT_POLYLIN} styles. */
    public RectPolylinConstraints getConstraints() {
        return constraints;
    }

    /** Human-readable label for the inspector drop-down. */
    public String getDisplayName() {
        return connectorType == ConnectorType.RECT_POLYLIN ? "Orthogonal" : "Straight line";
    }

    // -------------------------------------------------------------------------
    // Catalogue helpers
    // -------------------------------------------------------------------------

    /** All styles applicable to the given category, in declaration order. */
    public static List<ConnectorStyle> stylesFor(Category category) {
        List<ConnectorStyle> result = new ArrayList<>();
        for (ConnectorStyle s : values()) {
            if (s.category == category) {
                result.add(s);
            }
        }
        return result;
    }

    /** The default style for a category (the first declared = the rectilinear one). */
    public static ConnectorStyle defaultFor(Category category) {
        return stylesFor(category).get(0);
    }

    /** Resolves a style id, or {@code null} if unknown / empty. */
    public static ConnectorStyle forId(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (ConnectorStyle s : values()) {
            if (s.id.equals(id)) {
                return s;
            }
        }
        return null;
    }
}
