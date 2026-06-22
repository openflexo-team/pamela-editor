package org.openflexo.pamela.editor.diagram;

import java.util.Objects;

import org.openflexo.pamela.editor.ui.preferences.ConnectorStylePreference;

/**
 * Default implementation class for {@link PamelaClassDiagram}.
 * PAMELA manages all property storage; this class provides the non-PAMELA
 * {@link #findPropertyView(String, String)} lookup.
 */
public abstract class PamelaClassDiagramImpl implements PamelaClassDiagram {

    @Override
    public ConnectorStylePreference getConnectorStyleById(String id) {
        if (id == null || getConnectorStyles() == null) {
            return null;
        }
        for (ConnectorStylePreference s : getConnectorStyles()) {
            if (id.equals(s.getId())) {
                return s;
            }
        }
        return null;
    }

    @Override
    public ConnectorStylePreference getDefaultInheritanceStyle() {
        return getConnectorStyleById(getDefaultInheritanceStyleId());
    }

    @Override
    public void setDefaultInheritanceStyle(ConnectorStylePreference style) {
        setDefaultInheritanceStyleId(style != null ? style.getId() : null);
    }

    @Override
    public ConnectorStylePreference getDefaultAssociationStyle() {
        return getConnectorStyleById(getDefaultAssociationStyleId());
    }

    @Override
    public void setDefaultAssociationStyle(ConnectorStylePreference style) {
        setDefaultAssociationStyleId(style != null ? style.getId() : null);
    }

    @Override
    public PropertyView findPropertyView(String sourceQualifiedName, String propertyIdentifier) {
        if (getConnectorViews() == null) {
            return null;
        }
        for (ConnectorView cv : getConnectorViews()) {
            if (cv instanceof PropertyView
                    && Objects.equals(cv.getSourceQualifiedName(), sourceQualifiedName)
                    && Objects.equals(((PropertyView) cv).getPropertyIdentifier(), propertyIdentifier)) {
                return (PropertyView) cv;
            }
        }
        return null;
    }

    @Override
    public InheritanceView findInheritanceView(String sourceQualifiedName, String targetQualifiedName) {
        if (getConnectorViews() == null) {
            return null;
        }
        for (ConnectorView cv : getConnectorViews()) {
            if (cv instanceof InheritanceView
                    && Objects.equals(cv.getSourceQualifiedName(), sourceQualifiedName)
                    && Objects.equals(cv.getTargetQualifiedName(), targetQualifiedName)) {
                return (InheritanceView) cv;
            }
        }
        return null;
    }
}
