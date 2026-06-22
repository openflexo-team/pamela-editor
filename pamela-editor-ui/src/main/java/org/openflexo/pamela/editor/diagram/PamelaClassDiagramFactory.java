package org.openflexo.pamela.editor.diagram;

import org.openflexo.diana.DianaModelFactoryImpl;
import org.openflexo.pamela.editor.ui.preferences.ConnectorStylePreference;
import org.openflexo.pamela.exceptions.ModelDefinitionException;
import org.openflexo.pamela.factory.EditingContext;

/**
 * PAMELA / Diana factory for the class-diagram model.
 * Configured with {@link PamelaClassDiagram} and {@link EntityView} as managed types, plus the
 * embedded style entities ({@link DiagramEntityStyle}, {@link ConnectorStylePreference}).
 */
public class PamelaClassDiagramFactory extends DianaModelFactoryImpl {

    public PamelaClassDiagramFactory(EditingContext editingContext) throws ModelDefinitionException {
        super(PamelaClassDiagram.class, EntityView.class,
                ConnectorView.class, PropertyView.class, InheritanceView.class,
                DiagramEntityStyle.class, ConnectorStylePreference.class);
        setEditingContext(editingContext);
    }

    /**
     * Creates a new empty diagram with the given name, snapshotting the current preference
     * defaults (entity look + default connector styles) into it so it is self-contained.
     *
     * @param name the display name for the diagram
     * @return a new {@link PamelaClassDiagram} instance
     */
    public PamelaClassDiagram newDiagram(String name) {
        PamelaClassDiagram diagram = newInstance(PamelaClassDiagram.class);
        diagram.setName(name);
        DiagramStyleSnapshot.initializeFromDefaults(diagram, this);
        return diagram;
    }

    /**
     * Creates a new {@link EntityView} with the given geometry.
     *
     * @param qualifiedName the fully qualified name of the entity being represented
     * @param x             X position on the canvas
     * @param y             Y position on the canvas
     * @param width         shape width
     * @param height        shape height
     * @return a new {@link EntityView} instance (not yet added to any diagram)
     */
    public EntityView newEntityView(String qualifiedName,
                                    double x, double y,
                                    double width, double height) {
        EntityView ev = newInstance(EntityView.class);
        ev.setQualifiedName(qualifiedName);
        ev.setX(x);
        ev.setY(y);
        ev.setWidth(width);
        ev.setHeight(height);
        return ev;
    }

    /**
     * Creates a new {@link PropertyView} for the given connector identity (not yet added
     * to any diagram). Label offsets default to {@code 0}.
     *
     * @param sourceQualifiedName qualified name of the source (declaring) entity
     * @param targetQualifiedName qualified name of the target (property type) entity
     * @param propertyIdentifier  identifier of the originating property
     * @return a new {@link PropertyView} instance
     */
    public PropertyView newPropertyView(String sourceQualifiedName,
                                        String targetQualifiedName,
                                        String propertyIdentifier) {
        PropertyView pv = newInstance(PropertyView.class);
        pv.setSourceQualifiedName(sourceQualifiedName);
        pv.setTargetQualifiedName(targetQualifiedName);
        pv.setPropertyIdentifier(propertyIdentifier);
        return pv;
    }

    /**
     * Creates a new {@link InheritanceView} for the given inheritance link (not yet added
     * to any diagram). Inheritance views are never persisted.
     *
     * @param sourceQualifiedName qualified name of the sub-entity (extends the super)
     * @param targetQualifiedName qualified name of the super-entity
     * @return a new {@link InheritanceView} instance
     */
    public InheritanceView newInheritanceView(String sourceQualifiedName,
                                              String targetQualifiedName) {
        InheritanceView iv = newInstance(InheritanceView.class);
        iv.setSourceQualifiedName(sourceQualifiedName);
        iv.setTargetQualifiedName(targetQualifiedName);
        return iv;
    }
}
