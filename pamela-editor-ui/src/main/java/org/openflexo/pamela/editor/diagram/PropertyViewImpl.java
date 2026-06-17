package org.openflexo.pamela.editor.diagram;

import org.openflexo.pamela.editor.model.SourceModelProperty;

/**
 * Implementation of {@link PropertyView}. Holds the transient resolved property and
 * exposes the property identifier as the connector label (UML association role).
 */
public abstract class PropertyViewImpl extends ConnectorViewImpl implements PropertyView {

    private SourceModelProperty property;

    @Override
    public SourceModelProperty getProperty() {
        return property;
    }

    @Override
    public void setProperty(SourceModelProperty property) {
        this.property = property;
    }

    @Override
    public String getLabel() {
        if (property != null) {
            return property.getPropertyIdentifier();
        }
        return getPropertyIdentifier();
    }
}
