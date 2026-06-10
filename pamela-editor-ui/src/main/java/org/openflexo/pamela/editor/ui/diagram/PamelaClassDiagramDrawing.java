package org.openflexo.pamela.editor.ui.diagram;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
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
import org.openflexo.diana.GraphicalRepresentation.HorizontalTextAlignment;
import org.openflexo.diana.GraphicalRepresentation.VerticalTextAlignment;
import org.openflexo.diana.ShapeGraphicalRepresentation;
import org.openflexo.diana.connectors.ConnectorSpecification;
import org.openflexo.diana.connectors.ConnectorSpecification.ConnectorType;
import org.openflexo.diana.connectors.ConnectorSymbol.EndSymbolType;
import org.openflexo.diana.connectors.ConnectorSymbol.StartSymbolType;
import org.openflexo.diana.impl.DrawingImpl;
import org.openflexo.diana.shapes.ShapeSpecification.ShapeType;
import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.editor.diagram.ComputedConnector;
import org.openflexo.pamela.editor.diagram.ComputedConnector.RelationshipType;
import org.openflexo.pamela.editor.diagram.EntityView;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.model.SourceCustomMethod;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelInitializer;
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
    /** Height of one item row inside a compartment. */
    private static final double ROW_HEIGHT = 16.0;
    /** Height of an empty compartment (nothing to show). */
    private static final double EMPTY_COMPARTMENT_HEIGHT = 5.0;
    /** Top inset of an item-row label. */
    private static final double TEXT_INSET_Y = 2.0;
    /** Left inset of an item-row icon inside its row. */
    private static final double ROW_ICON_INSET = 2.0;
    /** X where an item-row label starts (after its icon). */
    private static final double ROW_TEXT_INSET_X = ROW_ICON_INSET + ICON_SIZE + 2.0;
    /** Right padding added to a compartment line when computing the optimal width. */
    private static final double COMPARTMENT_RIGHT_PAD = 8.0;
    /** Minimum box width (floor of the content-based width heuristic). */
    private static final double MIN_BOX_WIDTH = 80.0;

    /** Fonts used for the title and compartment text (shared with the width heuristic). */
    private static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 11);
    private static final Font COMPARTMENT_FONT = new Font("SansSerif", Font.PLAIN, 10);

    /** Background tints highlighting a selected / focused item row (light enough to
     *  keep the black label readable). */
    private static final Color ROW_SELECTED_BG = new Color(184, 207, 244);
    private static final Color ROW_FOCUSED_BG = new Color(223, 233, 249);

    /** Off-screen graphics used only to measure string widths (FontMetrics). */
    private static final java.awt.Graphics MEASURE_GRAPHICS =
            new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).getGraphics();

    /** The three UML compartments stacked below the header, in display order. */
    private enum Compartment { INITIALIZERS, PROPERTIES, METHODS }

    private final SourceMetaModel metaModel;
    private final DianaModelFactory factory;

    private ShapeGRBinding<EntityView> entityViewBinding;
    private ShapeGRBinding<EntityView> entityHeaderBinding;
    private ShapeGRBinding<EntityView> entityIconBinding;
    private ShapeGRBinding<EntityView> entityInitializersBinding;
    private ShapeGRBinding<EntityView> entityPropertiesBinding;
    private ShapeGRBinding<EntityView> entityMethodsBinding;
    private ShapeGRBinding<CompartmentItem> itemRowBinding;
    private ShapeGRBinding<CompartmentItem> itemIconBinding;
    private ConnectorGRBinding<ComputedConnector> connectorBinding;

    /**
     * Pool of canonical {@link CompartmentItem} instances, keyed by value. Diana's
     * structure reconciliation reuses a node only for the <em>same drawable
     * instance</em>, so we must hand back the same instance for an unchanged row across
     * successive walks (otherwise every walk creates duplicate/orphan nodes and Diana
     * logs "something strange … see isValid()"). A row whose text changes yields a new
     * value → a new instance → the node is correctly recreated with up-to-date content.
     */
    private final Map<CompartmentItem, CompartmentItem> itemPool = new HashMap<>();

    /** Same interning rationale as {@link #itemPool}, for the computed connectors. */
    private final Map<ComputedConnector, ComputedConnector> connectorPool = new HashMap<>();

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
        //    It carries no label of its own: the title is shown in a fixed-height
        //    header child shape (2b), and the three compartments (initializers,
        //    properties, methods) are further child shapes stacked below it (2d).
        //    Width AND height are user-resizable (and persisted); the compartments
        //    distribute the available vertical space below the header proportionally
        //    to their content (see applyCompartmentConstraints).
        entityViewBinding = bindShape(EntityView.class, "entityView",
            new ShapeGRProvider<EntityView>() {
                @Override
                public ShapeGraphicalRepresentation provideGR(
                        EntityView ev, DianaModelFactory factory) {
                    ShapeGraphicalRepresentation gr =
                        factory.makeShapeGraphicalRepresentation(ShapeType.RECTANGLE);
                    // Resolve the entity now so the box is styled correctly on first draw.
                    resolveEntity(ev);
                    gr.setX(ev.getX());
                    gr.setY(ev.getY());
                    gr.setWidth(ev.getWidth());
                    gr.setHeight(ev.getHeight());
                    applyContainerStyle(gr, ev, factory);
                    gr.setIsFloatingLabel(false);
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
                    gr.setShadowStyle(factory.makeNoneShadowStyle());
                    // Bold, centered title; allow the «abstract»/«unresolved»
                    // stereotype line to wrap onto a second line.
                    gr.setTextStyle(factory.makeTextStyle(Color.BLACK, HEADER_FONT));
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
                    gr.setIsFloatingLabel(false);
                    return gr;
                }
            });

        // 2d. Shape bindings for the three UML COMPARTMENTS — child shapes stacked
        //     below the header, each spanning the container width (parent.width).
        //     Their vertical geometry (y/height) is content-driven: a compartment is
        //     ROW_HEIGHT per item, or EMPTY_COMPARTMENT_HEIGHT (5 px) when empty. Each
        //     reuses the same EntityView drawable, bound once per compartment kind.
        entityInitializersBinding = bindShape(EntityView.class, "entityInitializers",
            new ShapeGRProvider<EntityView>() {
                @Override
                public ShapeGraphicalRepresentation provideGR(
                        EntityView ev, DianaModelFactory factory) {
                    return makeCompartmentGR(ev, Compartment.INITIALIZERS, factory);
                }
            });
        entityPropertiesBinding = bindShape(EntityView.class, "entityProperties",
            new ShapeGRProvider<EntityView>() {
                @Override
                public ShapeGraphicalRepresentation provideGR(
                        EntityView ev, DianaModelFactory factory) {
                    return makeCompartmentGR(ev, Compartment.PROPERTIES, factory);
                }
            });
        entityMethodsBinding = bindShape(EntityView.class, "entityMethods",
            new ShapeGRProvider<EntityView>() {
                @Override
                public ShapeGraphicalRepresentation provideGR(
                        EntityView ev, DianaModelFactory factory) {
                    return makeCompartmentGR(ev, Compartment.METHODS, factory);
                }
            });

        // 2e. Shape binding for one ITEM ROW inside a compartment — a full-width row
        //     carrying the element's icon (image background, natural size at the left)
        //     and its left-aligned label. One node per item; reconciled by value
        //     (CompartmentItem.equals on entity-view + compartment + index + text).
        itemRowBinding = bindShape(CompartmentItem.class, "compartmentItem",
            new ShapeGRProvider<CompartmentItem>() {
                @Override
                public ShapeGraphicalRepresentation provideGR(
                        CompartmentItem item, DianaModelFactory factory) {
                    return makeItemRowGR(item, factory);
                }
            });

        // 2f. Shape binding for the ITEM ICON — a fixed 16×16 square at the left of a
        //     row, with the element icon as an image background. Reuses the same
        //     CompartmentItem drawable, drawn as a child of the row, so the row's own
        //     background stays free to be tinted on selection/focus.
        itemIconBinding = bindShape(CompartmentItem.class, "compartmentItemIcon",
            new ShapeGRProvider<CompartmentItem>() {
                @Override
                public ShapeGraphicalRepresentation provideGR(
                        CompartmentItem item, DianaModelFactory factory) {
                    return makeItemIconGR(item, factory);
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

        // 4b. Walker: draw the header and the three compartments as children of each
        //     entity-view container (header on top, then initializers / properties /
        //     methods, stacked top-to-bottom).
        entityViewBinding.addToWalkers(new GRStructureVisitor<EntityView>() {
            @Override
            public void visit(EntityView ev) {
                drawShape(entityHeaderBinding, ev);
                drawShape(entityInitializersBinding, ev);
                drawShape(entityPropertiesBinding, ev);
                drawShape(entityMethodsBinding, ev);
            }
        });

        // 4c. Walker: draw the entity icon as a child of the header.
        entityHeaderBinding.addToWalkers(new GRStructureVisitor<EntityView>() {
            @Override
            public void visit(EntityView ev) {
                drawShape(entityIconBinding, ev);
            }
        });

        // 4d. Walkers: draw one item row per element, as children of each compartment.
        entityInitializersBinding.addToWalkers(new GRStructureVisitor<EntityView>() {
            @Override
            public void visit(EntityView ev) {
                for (CompartmentItem item : itemsFor(ev, Compartment.INITIALIZERS)) {
                    drawShape(itemRowBinding, item);
                }
            }
        });
        entityPropertiesBinding.addToWalkers(new GRStructureVisitor<EntityView>() {
            @Override
            public void visit(EntityView ev) {
                for (CompartmentItem item : itemsFor(ev, Compartment.PROPERTIES)) {
                    drawShape(itemRowBinding, item);
                }
            }
        });
        entityMethodsBinding.addToWalkers(new GRStructureVisitor<EntityView>() {
            @Override
            public void visit(EntityView ev) {
                for (CompartmentItem item : itemsFor(ev, Compartment.METHODS)) {
                    drawShape(itemRowBinding, item);
                }
            }
        });

        // 4e. Walker: draw the element icon as a child of its row.
        itemRowBinding.addToWalkers(new GRStructureVisitor<CompartmentItem>() {
            @Override
            public void visit(CompartmentItem item) {
                drawShape(itemIconBinding, item);
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
        //    user drag (x/y) and width resize are persisted to the .diagram sidecar.
        //    In Unique mode setPropertyValue first updates the container GR (keeping
        //    the children's `parent.width` constraints live) and then writes the value
        //    back to the model through these settable bindings.
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
                    result.add(internConnector(new ComputedConnector(
                            RelationshipType.INHERITANCE, sourceView, targetView, null)));
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
                result.add(internConnector(new ComputedConnector(type, sourceView, targetView, prop)));
            }
        }

        return result;
    }

    /** Returns the canonical (interned) instance for a connector value. */
    private ComputedConnector internConnector(ComputedConnector cc) {
        ComputedConnector canonical = connectorPool.get(cc);
        if (canonical == null) {
            connectorPool.put(cc, cc);
            canonical = cc;
        }
        return canonical;
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
    // Compartments (initializers / properties / methods)
    // =========================================================================

    /** The text lines shown in a compartment; empty when unresolved or nothing to show. */
    private List<String> compartmentLines(EntityView ev, Compartment c) {
        SourceModelEntity e = ev.getEntity();
        if (e == null) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>();
        switch (c) {
            case INITIALIZERS:
                for (SourceModelInitializer init : e.getInitializers()) {
                    lines.add(init.getMethodName()
                            + "(" + String.join(", ", init.getParameters()) + ")");
                }
                break;
            case PROPERTIES:
                for (SourceModelProperty p : e.getDeclaredProperties().values()) {
                    String type = (p.getType() != null) ? p.getType().getSimpleName() : "?";
                    String suffix = (p.getCardinality() == Cardinality.LIST) ? " [*]" : "";
                    lines.add(p.getPropertyIdentifier() + " : " + type + suffix);
                }
                break;
            case METHODS:
                if (e.getImplementationClass() != null) {
                    for (SourceCustomMethod m : e.getImplementationClass().getCustomMethods()) {
                        lines.add(m.getMethodName() + "()");
                    }
                }
                break;
        }
        return lines;
    }

    /** Height of a compartment given its item count: ROW_HEIGHT per item, 5 px if empty. */
    private static double compartmentHeight(int itemCount) {
        return itemCount == 0 ? EMPTY_COMPARTMENT_HEIGHT : itemCount * ROW_HEIGHT;
    }

    /** Sum of the three compartments' natural (content-driven) heights. Always ≥ 15. */
    private double naturalCompartmentsSum(EntityView ev) {
        double s = 0;
        for (Compartment k : Compartment.values()) {
            s += compartmentHeight(compartmentLines(ev, k).size());
        }
        return s;
    }

    /**
     * The optimal (natural) box height for an entity view: the header plus the three
     * compartments at their content-driven heights. Used as the default height when an
     * entity is dropped on the diagram so the box fits its content exactly. The entity
     * must already be resolved ({@link EntityView#getEntity()} non-null) for the
     * compartments to be counted.
     */
    public double optimalHeight(EntityView ev) {
        return HEADER_HEIGHT + naturalCompartmentsSum(ev);
    }

    /** Width (px) of {@code s} rendered in {@code font}, measured off-screen. */
    private static double stringWidth(Font font, String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        FontMetrics fm = MEASURE_GRAPHICS.getFontMetrics(font);
        return fm.stringWidth(s);
    }

    /**
     * The optimal box width for an entity view: wide enough for the title (bold,
     * with room on both sides so the centered label clears the left icon) and for the
     * longest line of every compartment, floored at {@link #MIN_BOX_WIDTH}. Used as the
     * default width when an entity is dropped on the diagram. The entity must already
     * be resolved for the compartment lines to be counted.
     */
    public double optimalWidth(EntityView ev) {
        double w = MIN_BOX_WIDTH;
        // Header: each title line plus a left+right margin that reserves the icon area.
        double headerMargin = 2 * (ICON_INSET + ICON_SIZE);
        for (String line : ev.getDisplayLabel().split("\n")) {
            w = Math.max(w, stringWidth(HEADER_FONT, line) + headerMargin);
        }
        // Compartments: each item line is preceded by its icon, so reserve the icon
        // offset (ROW_TEXT_INSET_X) plus the text width and a right padding.
        for (Compartment c : Compartment.values()) {
            for (String line : compartmentLines(ev, c)) {
                w = Math.max(w, stringWidth(COMPARTMENT_FONT, line)
                        + ROW_TEXT_INSET_X + COMPARTMENT_RIGHT_PAD);
            }
        }
        return Math.ceil(w);
    }

    /** Sum of the natural heights of the compartments before {@code c}. */
    private double naturalBefore(EntityView ev, Compartment c) {
        double s = 0;
        for (Compartment k : Compartment.values()) {
            if (k == c) {
                break;
            }
            s += compartmentHeight(compartmentLines(ev, k).size());
        }
        return s;
    }

    /**
     * Sets the geometry constraints of a compartment so that it follows the container
     * live on resize. Width tracks the container ({@code parent.width}); the vertical
     * space below the header ({@code parent.height - HEADER_HEIGHT}) is distributed
     * among the three compartments <b>proportionally to their natural (content-driven)
     * heights</b>:
     * <pre>
     *   height = (parent.height - HEADER) * natural_c    / Σnatural
     *   y      =  HEADER + (parent.height - HEADER) * Σnatural(before c) / Σnatural
     * </pre>
     * So at the natural total height each compartment shows exactly its content (5 px
     * when empty); when the box is made taller the extra room is shared out
     * proportionally; when shorter, compartments shrink and content is simply clipped
     * (the proportional form never yields a negative height since {@code parent.height
     * ≥ 0}).
     */
    private void applyCompartmentConstraints(ShapeGraphicalRepresentation gr,
                                             EntityView ev, Compartment c) {
        double natural = compartmentHeight(compartmentLines(ev, c).size());
        double before = naturalBefore(ev, c);
        double sum = naturalCompartmentsSum(ev);
        String avail = "(parent.height - " + HEADER_HEIGHT + ")";
        gr.setXConstraints(new DataBinding<Double>("0"));
        gr.setWidthConstraints(new DataBinding<Double>("parent.width"));
        gr.setYConstraints(new DataBinding<Double>(
                HEADER_HEIGHT + " + " + avail + " * " + before + " / " + sum));
        gr.setHeightConstraints(new DataBinding<Double>(
                avail + " * " + natural + " / " + sum));
    }

    /** Builds a compartment GR: a white bordered box spanning the container width. It
     *  carries no text itself — its items are drawn as child rows (see makeItemRowGR).
     *  Vertical geometry is constraint-driven (see {@link #applyCompartmentConstraints}). */
    private ShapeGraphicalRepresentation makeCompartmentGR(
            EntityView ev, Compartment c, DianaModelFactory f) {
        List<String> lines = compartmentLines(ev, c);
        ShapeGraphicalRepresentation gr =
            f.makeShapeGraphicalRepresentation(ShapeType.RECTANGLE);
        gr.setX(0);
        gr.setY(HEADER_HEIGHT);
        gr.setWidth(ev.getWidth());
        gr.setHeight(compartmentHeight(lines.size()));
        applyCompartmentConstraints(gr, ev, c);
        gr.setBackground(f.makeColoredBackground(Color.WHITE));
        gr.setForeground(f.makeForegroundStyle(Color.DARK_GRAY, 1.0f));
        gr.setShadowStyle(f.makeNoneShadowStyle());
        // Non-interactive: clicks/drag target the container.
        gr.setIsSelectable(false);
        gr.setIsFocusable(false);
        gr.setIsReadOnly(true);
        return gr;
    }

    /** Icon shown at the left of each item row, by compartment kind. */
    private static ImageIcon iconForCompartment(Compartment c) {
        switch (c) {
            case PROPERTIES: return PamelaEditorIconLibrary.PROPERTY_ICON;
            case METHODS:    return PamelaEditorIconLibrary.INITIALIZER_ICON;
            case INITIALIZERS:
            default:         return PamelaEditorIconLibrary.INITIALIZER_ICON;
        }
    }

    /** The item rows of a compartment, in order, each carrying its text and kind icon.
     *  Instances are interned (see {@link #itemPool}) so an unchanged row keeps a stable
     *  identity across walks. */
    private List<CompartmentItem> itemsFor(EntityView ev, Compartment c) {
        List<String> lines = compartmentLines(ev, c);
        List<CompartmentItem> items = new ArrayList<>(lines.size());
        ImageIcon icon = iconForCompartment(c);
        for (int i = 0; i < lines.size(); i++) {
            CompartmentItem item = new CompartmentItem(ev, c, i, lines.get(i), icon);
            CompartmentItem canonical = itemPool.get(item);
            if (canonical == null) {
                itemPool.put(item, item);
                canonical = item;
            }
            items.add(canonical);
        }
        return items;
    }

    /** Builds an item-row GR: a full-width row carrying the element label (its icon is
     *  a separate child shape, see {@link #makeItemIconGR}). The row background is
     *  transparent normally and tinted on focus/selection. */
    private ShapeGraphicalRepresentation makeItemRowGR(CompartmentItem item, DianaModelFactory f) {
        double y = item.index * ROW_HEIGHT;
        ShapeGraphicalRepresentation gr =
            f.makeShapeGraphicalRepresentation(ShapeType.RECTANGLE);
        gr.setX(0);
        gr.setY(y);
        gr.setWidth(100);
        gr.setHeight(ROW_HEIGHT);
        gr.setXConstraints(new DataBinding<Double>("0"));
        gr.setWidthConstraints(new DataBinding<Double>("parent.width"));
        gr.setYConstraints(new DataBinding<Double>(String.valueOf(y)));
        gr.setHeightConstraints(new DataBinding<Double>(String.valueOf(ROW_HEIGHT)));
        // Transparent normally; the icon is drawn on top by a separate child shape.
        gr.setBackground(f.makeEmptyBackground());
        gr.setForeground(f.makeNoneForegroundStyle());
        gr.setShadowStyle(f.makeNoneShadowStyle());
        gr.setTextStyle(f.makeTextStyle(Color.BLACK, COMPARTMENT_FONT));
        gr.setText(item.text);
        gr.setIsMultilineAllowed(false);
        gr.setIsFloatingLabel(true);
        gr.setAbsoluteTextX(ROW_TEXT_INSET_X);
        gr.setAbsoluteTextY(TEXT_INSET_Y);
        gr.setHorizontalTextAlignment(HorizontalTextAlignment.LEFT);
        gr.setVerticalTextAlignment(VerticalTextAlignment.TOP);
        // Item rows ARE selectable/focusable (the container, header, icon and
        // compartments are not — so a click on a member selects the member, while a
        // drag on the box chrome still moves the whole container).
        gr.setIsSelectable(true);
        gr.setIsFocusable(true);
        gr.setIsReadOnly(true);
        // Highlight via a background tint (the icon sits on a separate child shape, so
        // it stays visible over the tint).
        gr.setHasFocusedBackground(true);
        gr.setFocusedBackground(f.makeColoredBackground(ROW_FOCUSED_BG));
        gr.setHasSelectedBackground(true);
        gr.setSelectedBackground(f.makeColoredBackground(ROW_SELECTED_BG));
        // A row is not resizable; show only the highlight, not the resize control
        // points, on focus/selection.
        gr.setDrawControlPointsWhenSelected(false);
        gr.setDrawControlPointsWhenFocused(false);
        return gr;
    }

    /** Builds the icon GR for a row: a fixed 16×16 square at the row's left with the
     *  element icon as a transparent, fit-to-shape image background; non-interactive,
     *  so clicks fall through to the row. */
    private ShapeGraphicalRepresentation makeItemIconGR(CompartmentItem item, DianaModelFactory f) {
        double iconY = (ROW_HEIGHT - ICON_SIZE) / 2;
        ShapeGraphicalRepresentation gr =
            f.makeShapeGraphicalRepresentation(ShapeType.RECTANGLE);
        gr.setX(ROW_ICON_INSET);
        gr.setY(iconY);
        gr.setWidth(ICON_SIZE);
        gr.setHeight(ICON_SIZE);
        gr.setXConstraints(new DataBinding<Double>(String.valueOf(ROW_ICON_INSET)));
        gr.setYConstraints(new DataBinding<Double>(String.valueOf(iconY)));
        gr.setWidthConstraints(new DataBinding<Double>(String.valueOf(ICON_SIZE)));
        gr.setHeightConstraints(new DataBinding<Double>(String.valueOf(ICON_SIZE)));
        ImageIcon icon = item.icon;
        if (icon != null && icon.getImage() != null) {
            BackgroundImageBackgroundStyle bg = f.makeImageBackground(icon.getImage());
            bg.setFitToShape(true);
            bg.setImageBackgroundType(ImageBackgroundType.TRANSPARENT);
            gr.setBackground(bg);
        } else {
            gr.setBackground(f.makeEmptyBackground());
        }
        gr.setForeground(f.makeNoneForegroundStyle());
        gr.setShadowStyle(f.makeNoneShadowStyle());
        gr.setIsSelectable(false);
        gr.setIsFocusable(false);
        gr.setIsReadOnly(true);
        return gr;
    }

    /** Returns the binding for a compartment kind. */
    private ShapeGRBinding<EntityView> compartmentBinding(Compartment c) {
        switch (c) {
            case INITIALIZERS: return entityInitializersBinding;
            case PROPERTIES:   return entityPropertiesBinding;
            default:           return entityMethodsBinding;
        }
    }

    /**
     * Recomputes and re-applies the proportional geometry constraints of the three
     * compartments for one entity view. Called after a metamodel rebuild, where the
     * entity content — and thus the natural heights driving the proportions — may have
     * changed but the existing compartment nodes are not re-provided. The item rows
     * themselves are reconciled by the walkers (by {@link CompartmentItem} value), so
     * only the compartment proportions need an explicit refresh here. Re-setting a
     * constraint binding re-registers its value-change listener, which re-evaluates
     * immediately, so the new layout takes effect at once.
     */
    private void applyCompartmentGeometry(EntityView ev) {
        for (Compartment c : Compartment.values()) {
            ShapeNode<EntityView> node = getShapeNode(ev, compartmentBinding(c));
            if (node != null
                    && node.getGraphicalRepresentation() instanceof ShapeGraphicalRepresentation) {
                applyCompartmentConstraints(
                        (ShapeGraphicalRepresentation) node.getGraphicalRepresentation(), ev, c);
            }
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
            // Recompute compartment content + the content-driven box height.
            applyCompartmentGeometry(ev);
        }
        updateGraphicalObjectsHierarchy();
    }

    /** Returns the {@link SourceMetaModel} used to compute connectors. */
    public SourceMetaModel getMetaModel() {
        return metaModel;
    }

    /**
     * Transient drawable for one element row inside a compartment (an initializer, a
     * model property or a method). Diana reconciles drawables by {@code equals}, so the
     * identity is the owning entity view (by reference, stable across rebuilds), the
     * compartment kind, the row index and the displayed text — a content change thus
     * recreates the row with up-to-date text/icon, an unchanged row is reused. Not a
     * {@code @ModelEntity}.
     */
    private static final class CompartmentItem {
        final EntityView ev;
        final Compartment kind;
        final int index;
        final String text;
        final ImageIcon icon;

        CompartmentItem(EntityView ev, Compartment kind, int index, String text, ImageIcon icon) {
            this.ev = ev;
            this.kind = kind;
            this.index = index;
            this.text = text;
            this.icon = icon;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CompartmentItem)) {
                return false;
            }
            CompartmentItem that = (CompartmentItem) o;
            return ev == that.ev && kind == that.kind && index == that.index
                    && java.util.Objects.equals(text, that.text);
        }

        @Override
        public int hashCode() {
            int h = System.identityHashCode(ev);
            h = 31 * h + kind.hashCode();
            h = 31 * h + index;
            h = 31 * h + (text == null ? 0 : text.hashCode());
            return h;
        }
    }
}
