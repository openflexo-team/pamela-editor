package org.openflexo.pamela.editor.diagram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openflexo.pamela.editor.ui.preferences.ConnectorStylePreference;
import org.openflexo.pamela.editor.ui.preferences.ConnectorStylePreferences;
import org.openflexo.pamela.editor.ui.preferences.PreferencesManager;

/**
 * Base implementation for {@link ConnectorView}. Provides the non-PAMELA defaults
 * ({@link #getLabel()} → none, {@link #isPersistable()} → has a moved label or a
 * non-default style) and the {@link ConnectorStylePreference} resolution against the
 * connector-style preferences; the subtype implementations override {@link #getLabel()}
 * and {@link #getDefaultStyleId()}.
 */
public abstract class ConnectorViewImpl implements ConnectorView {

    /** Transient — set during the drawing walk; not serialized. */
    private PamelaClassDiagram owningDiagram;

    @Override
    public PamelaClassDiagram getOwningDiagram() {
        return owningDiagram;
    }

    @Override
    public void setOwningDiagram(PamelaClassDiagram diagram) {
        this.owningDiagram = diagram;
    }

    @Override
    public String getLabel() {
        return null;
    }

    private static ConnectorStylePreferences preferences() {
        return PreferencesManager.getInstance().connectors();
    }

    @Override
    public ConnectorStylePreference getStyle() {
        ConnectorStylePreferences prefs = preferences();
        if (prefs == null) {
            return null;
        }
        ConnectorStylePreference s = prefs.getStyleById(getStyleId());
        if (s != null) {
            return s;
        }
        s = prefs.getStyleById(getDefaultStyleId());
        if (s != null) {
            return s;
        }
        List<ConnectorStylePreference> all = prefs.getStyles();
        return (all != null && !all.isEmpty()) ? all.get(0) : null;
    }

    @Override
    public void setStyle(ConnectorStylePreference style) {
        setStyleId(style != null ? style.getId() : "");
    }

    @Override
    public List<ConnectorStylePreference> getAvailableStyles() {
        ConnectorStylePreferences prefs = preferences();
        if (prefs == null || prefs.getStyles() == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(prefs.getStyles());
    }

    @Override
    public boolean isPersistable() {
        return getLabelX() != 0.0 || getLabelY() != 0.0
                || (getStyleId() != null && !getStyleId().isEmpty());
    }
}
