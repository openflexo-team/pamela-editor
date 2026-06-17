package org.openflexo.pamela.editor.diagram;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;
import org.openflexo.pamela.editor.model.SourceModelProperty;

/**
 * Connector view representing a {@link SourceModelProperty} — an association (plain
 * reference) or composition ({@code @Embedded}) between two entities present on the
 * diagram.
 *
 * <p>Identity (within a diagram) is the source entity qualified name + the property
 * identifier; the target is the property's type entity. The transient
 * {@link #getProperty()} reference is resolved at load/rebuild time, like
 * {@link EntityView#getEntity()}, and is never serialized.</p>
 *
 * <p>Persisted only when its label has been moved (see {@link ConnectorView}).</p>
 */
@ModelEntity
@ImplementationClass(PropertyViewImpl.class)
public interface PropertyView extends ConnectorView {

    String PROPERTY_IDENTIFIER = "propertyIdentifier";

    @Getter(PROPERTY_IDENTIFIER)
    String getPropertyIdentifier();

    @Setter(PROPERTY_IDENTIFIER)
    void setPropertyIdentifier(String propertyIdentifier);

    /**
     * Transient reference to the resolved {@link SourceModelProperty}.
     * Not managed by PAMELA — implemented in {@link PropertyViewImpl}.
     */
    SourceModelProperty getProperty();

    void setProperty(SourceModelProperty property);
}
