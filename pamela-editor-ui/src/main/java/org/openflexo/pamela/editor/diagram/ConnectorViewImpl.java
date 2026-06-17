package org.openflexo.pamela.editor.diagram;

/**
 * Base implementation for {@link ConnectorView}. Provides the non-PAMELA defaults
 * ({@link #getLabel()} → none, {@link #isPersistable()} → has a moved label); the
 * subtype implementations override {@link #getLabel()}.
 */
public abstract class ConnectorViewImpl implements ConnectorView {

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public boolean isPersistable() {
        return getLabelX() != 0.0 || getLabelY() != 0.0;
    }
}
