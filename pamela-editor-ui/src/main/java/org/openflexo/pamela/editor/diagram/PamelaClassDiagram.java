package org.openflexo.pamela.editor.diagram;

import java.util.List;

import org.openflexo.pamela.AccessibleProxyObject;
import org.openflexo.pamela.annotations.Adder;
import org.openflexo.pamela.annotations.Embedded;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Remover;
import org.openflexo.pamela.annotations.Setter;

/**
 * Root model object for a PAMELA class diagram.
 * Holds a name and a list of {@link EntityView}s (one per entity placed on the diagram).
 * Connectors are computed at runtime by the drawing layer and are NOT stored here.
 */
@ModelEntity
@ImplementationClass(PamelaClassDiagramImpl.class)
public interface PamelaClassDiagram extends AccessibleProxyObject {

    String ID = "id";
    String NAME = "name";
    String ENTITY_VIEWS = "entityViews";
    String CONNECTOR_VIEWS = "connectorViews";

    /**
     * Stable identifier, assigned at creation and never changed. Used as the
     * sidecar file-name stem ({@code <id>.diagram}). Decoupled from {@link #getName()}
     * so that renaming a diagram never renames or orphans its file.
     */
    @Getter(ID)
    String getId();

    @Setter(ID)
    void setId(String id);

    @Getter(NAME)
    String getName();

    @Setter(NAME)
    void setName(String name);

    @Getter(value = ENTITY_VIEWS, cardinality = Cardinality.LIST)
    @Embedded
    List<EntityView> getEntityViews();

    @Adder(ENTITY_VIEWS)
    void addToEntityViews(EntityView entityView);

    @Remover(ENTITY_VIEWS)
    void removeFromEntityViews(EntityView entityView);

    /**
     * Persisted graphical overrides for connectors (currently association-label
     * positions). Created lazily — only a {@link PropertyView} whose label was moved is
     * stored here; inheritance links and default-positioned labels are recomputed at
     * runtime and never persisted. See {@link ConnectorView}.
     */
    @Getter(value = CONNECTOR_VIEWS, cardinality = Cardinality.LIST)
    @Embedded
    List<ConnectorView> getConnectorViews();

    @Adder(CONNECTOR_VIEWS)
    void addToConnectorViews(ConnectorView connectorView);

    @Remover(CONNECTOR_VIEWS)
    void removeFromConnectorViews(ConnectorView connectorView);

    /**
     * Returns the persisted {@link PropertyView} for the given connector identity (source
     * entity qualified name + property identifier), or {@code null} if none is stored.
     */
    PropertyView findPropertyView(String sourceQualifiedName, String propertyIdentifier);

    /**
     * Returns the persisted {@link InheritanceView} for the given inheritance link identity
     * (sub-entity qualified name + super-entity qualified name), or {@code null} if none is
     * stored. An inheritance view is persisted only when it carries a non-default style.
     */
    InheritanceView findInheritanceView(String sourceQualifiedName, String targetQualifiedName);
}
