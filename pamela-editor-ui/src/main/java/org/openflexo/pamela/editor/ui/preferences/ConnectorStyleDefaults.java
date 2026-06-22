package org.openflexo.pamela.editor.ui.preferences;

import java.awt.Color;

import org.openflexo.diana.connectors.ConnectorSpecification.ConnectorType;
import org.openflexo.diana.connectors.RectPolylinConnectorSpecification.RectPolylinAdjustability;
import org.openflexo.diana.connectors.RectPolylinConnectorSpecification.RectPolylinConstraints;

/**
 * Seeds the built-in connector styles (migrated from the former {@code ConnectorStyle} enum)
 * into a {@link ConnectorStylePreferences} node when it has none yet (first run / fresh model).
 */
final class ConnectorStyleDefaults {

    static final String INHERITANCE_LINE = "inheritance-line";
    static final String INHERITANCE_RECT_POLYLIN = "inheritance-rect-polylin";
    static final String RELATIONSHIP_RECT_POLYLIN = "relationship-rect-polylin";
    static final String RELATIONSHIP_LINE = "relationship-line";

    private ConnectorStyleDefaults() {
    }

    /** @return {@code true} if styles were seeded (the node was empty). */
    static boolean seedIfEmpty(ConnectorStylePreferences node, PreferencesFactory factory) {
        if (node == null || (node.getStyles() != null && !node.getStyles().isEmpty())) {
            return false;
        }
        Color darkBlue = new Color(0, 0, 139);
        Color darkGray = Color.DARK_GRAY;

        node.addToStyles(make(factory, INHERITANCE_LINE, "Inheritance — straight line",
                ConnectorType.LINE, darkBlue, 1.5, false, false, -1,
                RectPolylinAdjustability.BASICALLY_ADJUSTABLE, RectPolylinConstraints.NONE));
        node.addToStyles(make(factory, INHERITANCE_RECT_POLYLIN, "Inheritance — orthogonal",
                ConnectorType.RECT_POLYLIN, darkBlue, 1.5, true, false, -1,
                RectPolylinAdjustability.BASICALLY_ADJUSTABLE, RectPolylinConstraints.VERTICAL_LAYOUT));
        node.addToStyles(make(factory, RELATIONSHIP_RECT_POLYLIN, "Relationship — orthogonal",
                ConnectorType.RECT_POLYLIN, darkGray, 1.5, true, true, 10,
                RectPolylinAdjustability.BASICALLY_ADJUSTABLE, RectPolylinConstraints.NONE));
        node.addToStyles(make(factory, RELATIONSHIP_LINE, "Relationship — straight line",
                ConnectorType.LINE, darkGray, 1.5, true, false, -1,
                RectPolylinAdjustability.BASICALLY_ADJUSTABLE, RectPolylinConstraints.NONE));

        if (node.getDefaultInheritanceStyleId() == null || node.getDefaultInheritanceStyleId().isEmpty()) {
            node.setDefaultInheritanceStyleId(INHERITANCE_LINE);
        }
        if (node.getDefaultAssociationStyleId() == null || node.getDefaultAssociationStyleId().isEmpty()) {
            node.setDefaultAssociationStyleId(RELATIONSHIP_RECT_POLYLIN);
        }
        return true;
    }

    private static ConnectorStylePreference make(PreferencesFactory factory, String id, String name,
            ConnectorType type, Color color, double lineWidth, boolean straight, boolean rounded,
            int arcSize, RectPolylinAdjustability adjustability, RectPolylinConstraints constraints) {
        ConnectorStylePreference s = factory.newInstance(ConnectorStylePreference.class);
        s.setId(id);
        s.setName(name);
        s.setConnectorType(type);
        s.setColor(color);
        s.setLineWidth(lineWidth);
        s.setStraightLineWhenPossible(straight);
        s.setRounded(rounded);
        s.setArcSize(arcSize);
        s.setAdjustability(adjustability);
        s.setConstraints(constraints);
        return s;
    }
}
