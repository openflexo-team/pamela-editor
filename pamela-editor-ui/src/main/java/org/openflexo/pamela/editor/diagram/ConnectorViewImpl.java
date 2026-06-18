package org.openflexo.pamela.editor.diagram;

import java.util.List;

/**
 * Base implementation for {@link ConnectorView}. Provides the non-PAMELA defaults
 * ({@link #getLabel()} → none, {@link #isPersistable()} → has a moved label or a
 * non-default style) and the {@link ConnectorStyle} resolution; the subtype
 * implementations override {@link #getLabel()} and {@link #getStyleCategory()}.
 */
public abstract class ConnectorViewImpl implements ConnectorView {

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public ConnectorStyle getStyle() {
        ConnectorStyle s = ConnectorStyle.forId(getStyleId());
        if (s != null && s.getCategory() == getStyleCategory()) {
            return s;
        }
        return ConnectorStyle.defaultFor(getStyleCategory());
    }

    @Override
    public void setStyle(ConnectorStyle style) {
        setStyleId(style != null ? style.getId() : "");
    }

    @Override
    public List<ConnectorStyle> getAvailableStyles() {
        return ConnectorStyle.stylesFor(getStyleCategory());
    }

    @Override
    public boolean isPersistable() {
        return getLabelX() != 0.0 || getLabelY() != 0.0
                || (getStyleId() != null && !getStyleId().isEmpty());
    }
}
