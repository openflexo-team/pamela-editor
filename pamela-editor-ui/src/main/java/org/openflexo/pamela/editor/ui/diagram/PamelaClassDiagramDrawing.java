package org.openflexo.pamela.editor.ui.diagram;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.openflexo.connie.DataBinding;
import org.openflexo.diana.BackgroundImageBackgroundStyle;
import org.openflexo.diana.BackgroundImageBackgroundStyle.ImageBackgroundType;
import org.openflexo.diana.ConnectorGraphicalRepresentation;
import org.openflexo.diana.ContainerGraphicalRepresentation;
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
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;

/**
 * Diana drawing for a {@link PamelaClassDiagram}.
 *
 * <p>Uses {@link PersistenceMode#UniqueGraphicalRepresentations}: each node owns
 * its own {@code GraphicalRepresentation}, so node geometry is stored in (and read
 * from) the GR. This is required for the nested header/icon child shapes whose
 * geometry is expressed with constraint bindings against the live parent GR
 * (e.g. {@code width = parent.width}) — in shared mode the parent GR width is not
 * updated on resize, so {@code parent.width} would stay stale and the header would
 * not follow the container. Entity geometry ({@code x/y} and {@code width/height})
 * is synced back to the {@link EntityView} model via settable dynamic property
 * values, so user drag and resize are persisted to the {@code .diagram} sidecar;
 * {@link ComputedConnector} remains entirely transient.</p>
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

    /** Fixed height (logical units) of the title band at the top of each box. */
    private static final double HEADER_HEIGHT = 26.0;
    /** Side of the square entity icon shown at the left of the header. */
    private static final double ICON_SIZE = 16.0;
    /** Left inset of the icon inside the header. */
    private static final double ICON_INSET = 6.0;

    private final SourceMetaModel metaModel;
    private final DianaModelFactory factory;

    private ShapeGRBinding<EntityView> entityViewBinding;
    private ShapeGRBinding<EntityView> entityHeaderBinding;
    private ShapeGRBinding<EntityView> entityIconBinding;
    private ConnectorGRBinding<ComputedConnector> connectorBinding;

    public PamelaClassDiagramDrawing(PamelaClassDiagram diagram,
                                     SourceMetaModel metaModel,
                                     DianaModelFactory factory) {
        super(diagram, factory, PersistenceMode.UniqueGraphicalRepresentations);
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

        // 2. Shape binding for EntityView — the UML class box CONTAINER.
        //    Variable size, movable/resizable. It carries no label of its own:
        //    the title is shown in a fixed-height header child shape (see 2b),
        //    and future compartments (properties, initializers, methods) will be
        //    added as further child shapes stacked below the header.
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
                    applyContainerStyle(gr, ev, factory);
                    gr.setIsSelectable(true);
                    gr.setIsFocusable(true);
                    gr.setIsReadOnly(false);
                    return gr;
                }
            });

        // 2b. Shape binding for the entity HEADER — a fixed-height band pinned to
        //     the top of the container, spanning its full width, holding the entity
        //     name. It reuses the SAME EntityView drawable, bound a second time, and
        //     is drawn as a child of the container (see the header walker below).
        //     Geometry is expressed with Diana constraint bindings relative to the
        //     parent container GR:
        //         x = 0, y = 0, width = parent.width, height = HEADER_HEIGHT
        //     The constraint mechanism also makes the header unmovable/unresizable
        //     on its own, so all direct interaction targets the container.
        entityHeaderBinding = bindShape(EntityView.class, "entityHeader",
            new ShapeGRProvider<EntityView>() {
                @Override
                public ShapeGraphicalRepresentation provideGR(
                        EntityView ev, DianaModelFactory factory) {
                    ShapeGraphicalRepresentation gr =
                        factory.makeShapeGraphicalRepresentation(ShapeType.RECTANGLE);
                    gr.setX(0);
                    gr.setY(0);
                    gr.setWidth(ev.getWidth());
                    gr.setHeight(HEADER_HEIGHT);
                    gr.setXConstraints(new DataBinding<Double>("0"));
                    gr.setYConstraints(new DataBinding<Double>("0"));
                    gr.setWidthConstraints(new DataBinding<Double>("parent.width"));
                    gr.setHeightConstraints(new DataBinding<Double>(String.valueOf(HEADER_HEIGHT)));
                    applyHeaderStyle(gr, ev, factory);
                    // Bold, centered title; allow the «abstract»/«unresolved»
                    // stereotype line to wrap onto a second line.
                    gr.setTextStyle(factory.makeTextStyle(
                            Color.BLACK, new Font("SansSerif", Font.BOLD, 11)));
                    gr.setIsMultilineAllowed(true);
                    gr.setIsFloatingLabel(false);
                    // Non-interactive: clicks and drags pass through to the container.
                    gr.setIsSelectable(false);
                    gr.setIsFocusable(false);
                    gr.setIsReadOnly(true);
                    return gr;
                }
            });

        // 2c. Shape binding for the entity ICON — a small fixed-size square pinned
        //     to the top-left of the header, carrying the entity icon as an image
        //     background (concrete vs abstract; nothing when unresolved). Reuses the
        //     SAME EntityView drawable a third time; drawn as a child of the header.
        //     Constraints keep it at a fixed position/size (and non-resizable):
        //         x = ICON_INSET, y = (HEADER_HEIGHT - ICON_SIZE) / 2,
        //         width = height = ICON_SIZE
        entityIconBinding = bindShape(EntityView.class, "entityIcon",
            new ShapeGRProvider<EntityView>() {
                @Override
                public ShapeGraphicalRepresentation provideGR(
                        EntityView ev, DianaModelFactory factory) {
                    double iconY = (HEADER_HEIGHT - ICON_SIZE) / 2;
                    ShapeGraphicalRepresentation gr =
                        factory.makeShapeGraphicalRepresentation(ShapeType.RECTANGLE);
                    gr.setX(ICON_INSET);
                    gr.setY(iconY);
                    gr.setWidth(ICON_SIZE);
                    gr.setHeight(ICON_SIZE);
                    gr.setXConstraints(new DataBinding<Double>(String.valueOf(ICON_INSET)));
                    gr.setYConstraints(new DataBinding<Double>(String.valueOf(iconY)));
                    gr.setWidthConstraints(new DataBinding<Double>(String.valueOf(ICON_SIZE)));
                    gr.setHeightConstraints(new DataBinding<Double>(String.valueOf(ICON_SIZE)));
                    applyIconStyle(gr, ev, factory);
                    gr.setShadowStyle(factory.makeNoneShadowStyle());
                    // Non-interactive: a label-less icon badge.
                    gr.setIsSelectable(false);
                    gr.setIsFocusable(false);
                    gr.setIsReadOnly(true);
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

        // 4. Walker: draw entity-view containers as children of the drawing.
        drawingBinding.addToWalkers(new GRStructureVisitor<PamelaClassDiagram>() {
            @Override
            public void visit(PamelaClassDiagram diagram) {
                for (EntityView ev : diagram.getEntityViews()) {
                    drawShape(entityViewBinding, ev, diagram);
                }
            }
        });

        // 4b. Walker: draw the header as a child of each entity-view container.
        //     Future compartments will be drawn here too, below the header.
        entityViewBinding.addToWalkers(new GRStructureVisitor<EntityView>() {
            @Override
            public void visit(EntityView ev) {
                drawShape(entityHeaderBinding, ev);
            }
        });

        // 4c. Walker: draw the entity icon as a child of the header.
        entityHeaderBinding.addToWalkers(new GRStructureVisitor<EntityView>() {
            @Override
            public void visit(EntityView ev) {
                drawShape(entityIconBinding, ev);
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

        // 6. Dynamic text: the entity label is shown on the HEADER child shape
        //    (bold name, «abstract» stereotype when abstract, «unresolved» when the
        //    entity is missing from the meta-model).
        entityHeaderBinding.setDynamicPropertyValue(
            GraphicalRepresentation.TEXT,
            new DataBinding<String>("drawable.displayLabel"),
            false);

        // 7. Sync geometry back to the EntityView model (settable bindings), so that
        //    user drag (x/y) and resize (width/height) are persisted to the .diagram
        //    sidecar. In Unique mode setPropertyValue first updates the container GR
        //    (keeping the header's `parent.width` constraint live) and then writes the
        //    value back to the model through these settable bindings.
        entityViewBinding.setDynamicPropertyValue(
            ShapeGraphicalRepresentation.X,
            new DataBinding<Double>("drawable.x"), true);
        entityViewBinding.setDynamicPropertyValue(
            ShapeGraphicalRepresentation.Y,
            new DataBinding<Double>("drawable.y"), true);
        entityViewBinding.setDynamicPropertyValue(
            ContainerGraphicalRepresentation.WIDTH,
            new DataBinding<Double>("drawable.width"), true);
        entityViewBinding.setDynamicPropertyValue(
            ContainerGraphicalRepresentation.HEIGHT,
            new DataBinding<Double>("drawable.height"), true);
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

    /** Border color of a box: dark gray when resolved, red when unresolved. */
    private static Color borderColor(EntityView ev) {
        return ev.getEntity() == null ? new Color(190, 70, 70) : Color.DARK_GRAY;
    }

    /**
     * Styles the container box: white body with a colored border (dark gray when
     * the entity is resolved, red for an "unresolved" placeholder). The body is
     * intentionally plain — the colored title band is the header (see below).
     */
    private void applyContainerStyle(ShapeGraphicalRepresentation gr,
                                     EntityView ev, DianaModelFactory f) {
        gr.setForeground(f.makeForegroundStyle(borderColor(ev), 1.0f));
        gr.setBackground(f.makeColoredBackground(Color.WHITE));
    }

    /**
     * Styles the header band: a colored background (blue when resolved, pink for an
     * "unresolved" placeholder) with the same border color as the container. Its
     * bottom edge visually separates the title from the (future) compartments.
     */
    private void applyHeaderStyle(ShapeGraphicalRepresentation gr,
                                  EntityView ev, DianaModelFactory f) {
        boolean unresolved = ev.getEntity() == null;
        Color bg = unresolved ? new Color(245, 228, 228) : new Color(230, 240, 255);
        gr.setForeground(f.makeForegroundStyle(borderColor(ev), 1.0f));
        gr.setBackground(f.makeColoredBackground(bg));
    }

    /** The icon for an entity view: abstract vs concrete; {@code null} when unresolved. */
    private static ImageIcon iconForEntityView(EntityView ev) {
        SourceModelEntity e = ev.getEntity();
        if (e == null) {
            return null;
        }
        return e.isAbstract()
                ? PamelaEditorIconLibrary.ABSTRACT_ENTITY_ICON
                : PamelaEditorIconLibrary.ENTITY_ICON;
    }

    /**
     * Styles the icon badge: the entity icon as a transparent, fit-to-shape image
     * background and no border. When the entity is unresolved (or the icon resource
     * is missing) the badge is left empty (invisible), so the header band shows
     * through.
     */
    private void applyIconStyle(ShapeGraphicalRepresentation gr,
                                EntityView ev, DianaModelFactory f) {
        ImageIcon icon = iconForEntityView(ev);
        if (icon != null && icon.getImage() != null) {
            BackgroundImageBackgroundStyle bg = f.makeImageBackground(icon.getImage());
            bg.setFitToShape(true);
            bg.setImageBackgroundType(ImageBackgroundType.TRANSPARENT);
            gr.setBackground(bg);
        } else {
            gr.setBackground(f.makeEmptyBackground());
        }
        gr.setForeground(f.makeNoneForegroundStyle());
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
            // Restyle both the container and its header (two shape nodes share the
            // same EntityView drawable, so disambiguate by binding).
            ShapeNode<EntityView> containerNode = getShapeNode(ev, entityViewBinding);
            if (containerNode != null
                    && containerNode.getGraphicalRepresentation() instanceof ShapeGraphicalRepresentation) {
                applyContainerStyle(
                        (ShapeGraphicalRepresentation) containerNode.getGraphicalRepresentation(),
                        ev, factory);
            }
            ShapeNode<EntityView> headerNode = getShapeNode(ev, entityHeaderBinding);
            if (headerNode != null
                    && headerNode.getGraphicalRepresentation() instanceof ShapeGraphicalRepresentation) {
                applyHeaderStyle(
                        (ShapeGraphicalRepresentation) headerNode.getGraphicalRepresentation(),
                        ev, factory);
            }
            ShapeNode<EntityView> iconNode = getShapeNode(ev, entityIconBinding);
            if (iconNode != null
                    && iconNode.getGraphicalRepresentation() instanceof ShapeGraphicalRepresentation) {
                applyIconStyle(
                        (ShapeGraphicalRepresentation) iconNode.getGraphicalRepresentation(),
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
