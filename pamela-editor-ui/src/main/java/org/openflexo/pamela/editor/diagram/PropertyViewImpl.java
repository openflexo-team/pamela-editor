package org.openflexo.pamela.editor.diagram;

import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.ui.preferences.PreferencesManager;

/**
 * Implementation of {@link PropertyView}. Holds the transient resolved property and
 * exposes the property identifier as the connector label (UML association role).
 */
public abstract class PropertyViewImpl extends ConnectorViewImpl implements PropertyView {

    private SourceModelProperty property;

    private boolean connectorPresent;

    @Override
    public SourceModelProperty getProperty() {
        return property;
    }

    @Override
    public void setProperty(SourceModelProperty property) {
        this.property = property;
    }

    @Override
    public boolean isConnectorPresent() {
        return connectorPresent;
    }

    @Override
    public void setConnectorPresent(boolean connectorPresent) {
        this.connectorPresent = connectorPresent;
    }

    @Override
    public String getDefaultStyleId() {
        PamelaClassDiagram d = getOwningDiagram();
        if (d != null && d.getDefaultAssociationStyleId() != null) {
            return d.getDefaultAssociationStyleId();
        }
        return PreferencesManager.getInstance().connectors().getDefaultAssociationStyleId();
    }

    @Override
    public String getKindLabel() {
        if (property != null && property.isEmbedded()) {
            return "Composition";
        }
        return "Association";
    }

    @Override
    public String getLabel() {
        if (property != null) {
            return property.getPropertyIdentifier();
        }
        return getPropertyIdentifier();
    }
}
