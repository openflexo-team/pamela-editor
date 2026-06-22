package org.openflexo.pamela.editor.ui.preferences;

import java.awt.Color;

import org.openflexo.diana.connectors.ConnectorSpecification.ConnectorType;
import org.openflexo.diana.connectors.RectPolylinConnectorSpecification.RectPolylinAdjustability;
import org.openflexo.diana.connectors.RectPolylinConnectorSpecification.RectPolylinConstraints;
import org.openflexo.pamela.AccessibleProxyObject;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;

/**
 * A user-editable connector visual style (replaces the former fixed {@code ConnectorStyle}
 * enum). Held in a list on {@link ConnectorStylePreferences} and referenced by its stable
 * {@link #getId() id} from {@code ConnectorView.styleId} and from the inheritance/association
 * default-style assignments.
 *
 * <p>A style governs <b>routing and appearance only</b> (connector type, colour, line width,
 * and the rectilinear-polyline parameters). End symbols (generalisation triangle, composition
 * diamond, association arrow) stay kind-driven and are not part of a style.</p>
 *
 * <p>{@link Color} is string-convertible (global converter) and the Diana routing enums are
 * handled by the generic serializer's enum support.</p>
 */
@ModelEntity
@ImplementationClass(ConnectorStylePreference.ConnectorStylePreferenceImpl.class)
public interface ConnectorStylePreference extends AccessibleProxyObject {

    String ID = "id";
    String NAME = "name";
    String CONNECTOR_TYPE = "connectorType";
    String COLOR = "color";
    String LINE_WIDTH = "lineWidth";
    String STRAIGHT_LINE_WHEN_POSSIBLE = "straightLineWhenPossible";
    String ROUNDED = "rounded";
    String ARC_SIZE = "arcSize";
    String ADJUSTABILITY = "adjustability";
    String CONSTRAINTS = "constraints";

    @Getter(ID)
    String getId();

    @Setter(ID)
    void setId(String id);

    @Getter(NAME)
    String getName();

    @Setter(NAME)
    void setName(String name);

    @Getter(value = CONNECTOR_TYPE, defaultValue = "LINE")
    ConnectorType getConnectorType();

    @Setter(CONNECTOR_TYPE)
    void setConnectorType(ConnectorType type);

    @Getter(value = COLOR, defaultValue = "105,105,105")
    Color getColor();

    @Setter(COLOR)
    void setColor(Color color);

    @Getter(value = LINE_WIDTH, defaultValue = "1.5")
    double getLineWidth();

    @Setter(LINE_WIDTH)
    void setLineWidth(double width);

    @Getter(value = STRAIGHT_LINE_WHEN_POSSIBLE, defaultValue = "true")
    boolean getStraightLineWhenPossible();

    @Setter(STRAIGHT_LINE_WHEN_POSSIBLE)
    void setStraightLineWhenPossible(boolean b);

    @Getter(value = ROUNDED, defaultValue = "false")
    boolean getRounded();

    @Setter(ROUNDED)
    void setRounded(boolean b);

    /** Rounded-corner arc size; {@code < 0} means "leave Diana's default". */
    @Getter(value = ARC_SIZE, defaultValue = "-1")
    int getArcSize();

    @Setter(ARC_SIZE)
    void setArcSize(int arcSize);

    @Getter(value = ADJUSTABILITY, defaultValue = "BASICALLY_ADJUSTABLE")
    RectPolylinAdjustability getAdjustability();

    @Setter(ADJUSTABILITY)
    void setAdjustability(RectPolylinAdjustability adjustability);

    @Getter(value = CONSTRAINTS, defaultValue = "NONE")
    RectPolylinConstraints getConstraints();

    @Setter(CONSTRAINTS)
    void setConstraints(RectPolylinConstraints constraints);

    abstract class ConnectorStylePreferenceImpl implements ConnectorStylePreference {
        @Override
        public String toString() {
            return getName() != null ? getName() : getId();
        }
    }
}
