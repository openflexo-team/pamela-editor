package org.openflexo.pamela.editor.ui.diagram;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openflexo.connie.DataBinding;
import org.openflexo.diana.ConnectorGraphicalRepresentation;
import org.openflexo.diana.Drawing.ShapeNode;
import org.openflexo.diana.DianaModelFactory;
import org.openflexo.diana.DrawingGraphicalRepresentation;
import org.openflexo.diana.GRBinding.ConnectorGRBinding;
import org.openflexo.diana.GRBinding.DrawingGRBinding;
import org.openflexo.diana.GRBinding.ShapeGRBinding;
import org.openflexo.diana.GRProvider.ConnectorGRProvider;
import org.openflexo.diana.GRProvider.DrawingGRProvider;
import org.openflexo.diana.GRProvider.ShapeGRProvider;
import org.openflexo.diana.GRStructureVisitor;
import org.openflexo.diana.GraphicalRepresentation;
import org.openflexo.diana.ShapeGraphicalRepresentation;
import org.openflexo.diana.connectors.ConnectorSpecification;
import org.openflexo.diana.connectors.ConnectorSpecification.ConnectorType;
import org.openflexo.diana.connectors.ConnectorSymbol.EndSymbolType;
import org.openflexo.diana.connectors.ConnectorSymbol.StartSymbolType;
import org.openflexo.diana.impl.DrawingImpl;
import org.openflexo.diana.shapes.ShapeSpecification.ShapeType;
import org.openflexo.pamela.editor.diagram.ComputedConnector;
import org.openflexo.pamela.editor.diagram.ComputedConnector.RelationshipType;
import org.openflexo.pamela.editor.diagram.EntityView;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.model.SourceMetaModel;

/**
 * Diana drawing for a {@link PamelaClassDiagram}.
 *
 * <p>Uses {@link PersistenceMode#SharedGraphicalRepresentations} because
 * {@link EntityView} stores raw {@code x/y/width/height} scalars (not a full
 * {@code GraphicalRepresentation} object), and {@link ComputedConnector} is
 * entirely transient.</p>
 *
 * <p>Connectors are <em>computed</em> — they are never stored in the model.
 * Three types are shown when both endpoints are present in the diagram:
 * <ul>
 *   <li><b>INHERITANCE</b> — A extends B</li>
 *   <li><b>ASSOCIATION</b> — A has a property of type B, no {@code @Embedded}</li>
 *   <li><b>COMPOSITION</b> — A has a property of type B, with {@code @Embedded}</li>
 * </ul>
 * </p>
 */
public class PamelaClassDiagramDrawing extends DrawingImpl<PamelaClassDiagram> {

    private final SourceMetaModel metaModel;
    private final DianaModelFactory factory;

    private ShapeGRBinding<EntityView> entityViewBinding;
    private ConnectorGRBinding<ComputedConnector> connectorBinding;

    public PamelaClassDiagramDrawing(PamelaClassDiagram diagram,
                                     SourceMetaModel metaModel,
                                     DianaModelFactory factory) {
        super(diagram, factory, PersistenceMode.SharedGraphicalRepresentations);
        this.metaModel = metaModel;
        this.factory = factory;
    }

    @Override
    public void init() {

        // 1. Root drawing
        final DrawingGRBinding<PamelaClassDiagram> drawingBinding =
            bindDrawing(PamelaClassDiagram.class, "drawing",
                new DrawingGRProvider<PamelaClassDiagram>() {
                    @Override
                    public DrawingGraphicalRepresentation provideGR(
                            PamelaClassDiagram diagram, DianaModelFactory factory) {
                        DrawingGraphicalRepresentation gr =
                                factory.makeDrawingGraphicalRepresentation();
                        gr.setDrawWorkingArea(false);
                        return gr;
                    }
                });

        // 2. Shape binding for EntityView — UML class box
        entityViewBinding = bindShape(EntityView.class, "entityView",
            new ShapeGRProvider<EntityView>() {
                @Override
                public ShapeGraphicalRepresentation provideGR(
                        EntityView ev, DianaModelFactory factory) {
                    ShapeGraphicalRepresentation gr =
                        factory.makeShapeGraphicalRepresentation(ShapeType.RECTANGLE);
                    gr.setX(ev.getX());
                    gr.setY(ev.getY());
                    gr.setWidth(ev.getWidth());
                    gr.setHeight(ev.getHeight());
                    // Resolve the entity now so the box is styled correctly on first draw.
                    resolveEntity(ev);
                    applyEntityViewStyle(gr, ev, factory);
                    // Bold, centered entity name; allow the «abstract»/«unresolved»
                    // stereotype line to wrap onto a second line.
                    gr.setTextStyle(factory.makeTextStyle(
                            Color.BLACK, new Font("SansSerif", Font.BOLD, 11)));
                    gr.setIsMultilineAllowed(true);
                    // Keep the label centered inside the box (not floating).
                    gr.setIsFloatingLabel(false);
                    gr.setIsSelectable(true);
                    gr.setIsFocusable(true);
                    gr.setIsReadOnly(false);
                    return gr;
                }
            });

        // 3. Connector binding for ComputedConnector
        connectorBinding = bindConnector(ComputedConnector.class, "connector",
            entityViewBinding, entityViewBinding,
            new ConnectorGRProvider<ComputedConnector>() {
                @Override
                public ConnectorGraphicalRepresentation provideGR(
                        ComputedConnector cc, DianaModelFactory factory) {
                    ConnectorGraphicalRepresentation gr =
                        factory.makeConnectorGraphicalRepresentation(ConnectorType.LINE);
                    // Connectors are drawn source → target:
                    //  - INHERITANCE: sub-type → super-type
                    //  - ASSOCIATION / COMPOSITION: owner → property type
                    ConnectorSpecification spec = gr.getConnectorSpecification();
                    switch (cc.getType()) {
                        case INHERITANCE:
                            // UML generalization: hollow triangle at the super-type end.
                            gr.setForeground(factory.makeForegroundStyle(Color.DARK_GRAY, 1.5f));
                            spec.setEndSymbol(EndSymbolType.PLAIN_ARROW);
                            spec.setEndSymbolSize(12.0);
                            break;
                        case COMPOSITION:
                            // UML composition: filled diamond at the owner end,
                            // open arrow at the part end.
                            gr.setForeground(factory.makeForegroundStyle(new Color(60, 60, 180), 1.5f));
                            spec.setStartSymbol(StartSymbolType.FILLED_DIAMOND);
                            spec.setStartSymbolSize(10.0);
                            spec.setEndSymbol(EndSymbolType.ARROW);
                            spec.setEndSymbolSize(8.0);
                            break;
                        case ASSOCIATION:
                        default:
                            // UML association: open arrow at the type end.
                            gr.setForeground(factory.makeForegroundStyle(Color.GRAY, 1.0f));
                            spec.setEndSymbol(EndSymbolType.ARROW);
                            spec.setEndSymbolSize(8.0);
                            break;
                    }
                    return gr;
                }
            });

        // 4. Walker: draw entity view shapes
        drawingBinding.addToWalkers(new GRStructureVisitor<PamelaClassDiagram>() {
            @Override
            public void visit(PamelaClassDiagram diagram) {
                for (EntityView ev : diagram.getEntityViews()) {
                    drawShape(entityViewBinding, ev, diagram);
                }
            }
        });

        // 5. Walker: draw computed connectors
        drawingBinding.addToWalkers(new GRStructureVisitor<PamelaClassDiagram>() {
            @Override
            public void visit(PamelaClassDiagram diagram) {
                for (ComputedConnector cc : computeConnectors(diagram)) {
                    drawConnector(connectorBinding, cc,
                                  entityViewBinding, cc.getSourceView(),
                                  entityViewBinding, cc.getTargetView());
                }
            }
        });

        // 6. Dynamic text: entity label (bold name, «abstract» stereotype if abstract)
        entityViewBinding.setDynamicPropertyValue(
            GraphicalRepresentation.TEXT,
            new DataBinding<String>("drawable.displayLabel"),
            false);

        // 7. Sync X/Y back to EntityView on user drag
        entityViewBinding.setDynamicPropertyValue(
            ShapeGraphicalRepresentation.X,
            new DataBinding<Double>("drawable.x"), true);
        entityViewBinding.setDynamicPropertyValue(
            ShapeGraphicalRepresentation.Y,
            new DataBinding<Double>("drawable.y"), true);
    }

    // =========================================================================
    // Connector computation
    // =========================================================================

    /**
     * Computes all connectors that should be displayed in the given diagram.
     * A connector is included only when both endpoint entities are present as
     * {@link EntityView}s in the diagram.
     */
    private List<ComputedConnector> computeConnectors(PamelaClassDiagram diagram) {
        List<ComputedConnector> result = new ArrayList<>();

        // Build a map from qualified name → EntityView for fast lookup
        Map<String, EntityView> viewByName = new HashMap<>();
        for (EntityView ev : diagram.getEntityViews()) {
            viewByName.put(ev.getQualifiedName(), ev);
        }

        for (EntityView sourceView : diagram.getEntityViews()) {
            SourceModelEntity sourceEntity = resolveEntity(sourceView);
            if (sourceEntity == null) {
                continue;
            }

            // Inheritance connectors
            for (SourceModelEntity superEntity : sourceEntity.getDirectSuperEntities()) {
                EntityView targetView = viewByName.get(superEntity.getQualifiedName());
                if (targetView != null) {
                    result.add(new ComputedConnector(
                            RelationshipType.INHERITANCE, sourceView, targetView, null));
                }
            }

            // Association / Composition connectors from declared properties
            for (SourceModelProperty prop : sourceEntity.getDeclaredProperties().values()) {
                SourceModelEntity propTypeEntity = prop.getType().getModelEntity();
                if (propTypeEntity == null) {
                    continue;
                }
                EntityView targetView = viewByName.get(propTypeEntity.getQualifiedName());
                if (targetView == null) {
                    continue;
                }
                RelationshipType type = prop.isEmbedded()
                        ? RelationshipType.COMPOSITION
                        : RelationshipType.ASSOCIATION;
                result.add(new ComputedConnector(type, sourceView, targetView, prop));
            }
        }

        return result;
    }

    /**
     * Resolves the {@link SourceModelEntity} for an {@link EntityView} by looking
     * up the entity's qualified name in the meta-model (or using the transient
     * {@code entity} reference if already resolved).
     */
    private SourceModelEntity resolveEntity(EntityView ev) {
        if (ev.getEntity() != null) {
            return ev.getEntity();
        }
        if (metaModel == null || ev.getQualifiedName() == null) {
            return null;
        }
        SourceModelEntity entity = metaModel.getEntity(ev.getQualifiedName());
        if (entity != null) {
            ev.setEntity(entity);
        }
        return entity;
    }

    /**
     * Styles an entity-view box: normal (blue) when its entity is resolved, or a
     * red/pink "unresolved" placeholder when the entity is absent from the meta-model.
     */
    private void applyEntityViewStyle(ShapeGraphicalRepresentation gr,
                                      EntityView ev, DianaModelFactory f) {
        boolean unresolved = ev.getEntity() == null;
        if (unresolved) {
            gr.setForeground(f.makeForegroundStyle(new Color(190, 70, 70), 1.0f));
            gr.setBackground(f.makeColoredBackground(new Color(245, 228, 228)));
        } else {
            gr.setForeground(f.makeForegroundStyle(Color.DARK_GRAY, 1.0f));
            gr.setBackground(f.makeColoredBackground(new Color(230, 240, 255)));
        }
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Re-resolves every {@link EntityView}'s entity reference against the (rebuilt)
     * meta-model, restyles the boxes (resolved vs unresolved placeholder), and
     * re-walks the drawing. Call after a metamodel rebuild: the previous entity
     * references point at stale instances, and entities may have appeared/disappeared.
     */
    public void refreshEntityResolution() {
        for (EntityView ev : getModel().getEntityViews()) {
            SourceModelEntity e = (metaModel != null && ev.getQualifiedName() != null)
                    ? metaModel.getEntity(ev.getQualifiedName()) : null;
            ev.setEntity(e); // overwrite the (possibly stale) reference, may be null
            ShapeNode<EntityView> node = getShapeNode(ev);
            if (node != null
                    && node.getGraphicalRepresentation() instanceof ShapeGraphicalRepresentation) {
                applyEntityViewStyle(
                        (ShapeGraphicalRepresentation) node.getGraphicalRepresentation(),
                        ev, factory);
            }
        }
        updateGraphicalObjectsHierarchy();
    }

    /** Returns the {@link SourceMetaModel} used to compute connectors. */
    public SourceMetaModel getMetaModel() {
        return metaModel;
    }
}
