package org.openflexo.pamela.editor.diagram;

import org.openflexo.diana.DianaModelFactoryImpl;
import org.openflexo.pamela.exceptions.ModelDefinitionException;
import org.openflexo.pamela.factory.EditingContext;

/**
 * PAMELA / Diana factory for the class-diagram model.
 * Configured with {@link PamelaClassDiagram} and {@link EntityView} as managed types.
 */
public class PamelaClassDiagramFactory extends DianaModelFactoryImpl {

    public PamelaClassDiagramFactory(EditingContext editingContext) throws ModelDefinitionException {
        super(PamelaClassDiagram.class, EntityView.class);
        setEditingContext(editingContext);
    }

    /**
     * Creates a new empty diagram with the given name.
     *
     * @param name the display name for the diagram
     * @return a new {@link PamelaClassDiagram} instance
     */
    public PamelaClassDiagram newDiagram(String name) {
        PamelaClassDiagram diagram = newInstance(PamelaClassDiagram.class);
        diagram.setName(name);
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
}
