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

    String NAME = "name";
    String ENTITY_VIEWS = "entityViews";

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
}
