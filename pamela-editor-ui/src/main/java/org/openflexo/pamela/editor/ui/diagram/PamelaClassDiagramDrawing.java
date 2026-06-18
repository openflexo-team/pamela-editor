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
import org.openflexo.diana.DianaLayoutManager;
import org.openflexo.diana.Drawing.ConnectorNode;
import org.openflexo.diana.Drawing.DrawingTreeNode;
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
import org.openflexo.diana.connectors.ConnectorSymbol.EndSymbolType;
import org.openflexo.diana.connectors.ConnectorSymbol.StartSymbolType;
import org.openflexo.diana.connectors.RectPolylinConnectorSpecification;
import org.openflexo.diana.impl.DrawingImpl;
import org.openflexo.diana.layout.BoxLayoutConstraints;
import org.openflexo.diana.layout.LayoutConstraints;
import org.openflexo.diana.layout.BoxLayoutManagerSpecification;
import org.openflexo.diana.layout.BoxLayoutManagerSpecification.CrossAxisPolicy;
import org.openflexo.diana.layout.BoxLayoutManagerSpecification.MainAxisPolicy;
import org.openflexo.diana.layout.BoxLayoutManagerSpecification.Orientation;
import org.openflexo.diana.shapes.ShapeSpecification.ShapeType;
import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.editor.diagram.ConnectorStyle;
import org.openflexo.pamela.editor.diagram.ConnectorView;
import org.openflexo.pamela.editor.diagram.EntityView;
import org.openflexo.pamela.editor.diagram.InheritanceView;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagramFactory;
import org.openflexo.pamela.editor.diagram.PropertyView;
import org.openflexo.pamela.editor.model.SourceCustomMethod;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelInitializer;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import org.openflexo.pamela.factory.EditingContext;
import org.openflexo.pamela.factory.PamelaModelFactory;

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
 * values, so user drag and resize are persisted to the {@code .diagram} sidecar.</p>
 *
 * <p>Connector <em>existence</em> is computed — a connector is shown when both endpoints
 * are present (and, for a property, not hidden on the source {@link EntityView}). Each is
 * materialised as a {@link ConnectorView}:
 * <ul>
 *   <li><b>{@link InheritanceView}</b> — A extends B (always transient)</li>
 *   <li><b>{@link PropertyView}</b> (association) — A has a property of type B, no {@code @Embedded}</li>
 *   <li><b>{@link PropertyView}</b> (composition) — A has a property of type B, with {@code @Embedded}</li>
 * </ul>
 * A {@link PropertyView} is persisted (in the diagram's {@code connectorViews}) only when
 * its label was moved; otherwise it is a transient interned drawable.</p>
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
    /** Top padding inside a non-empty compartment, before its first item row. */
    private static final double COMPARTMENT_TOP_INSET = 4.0;
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
    /**
     * Left/right margin (logical units) reserved by each compartment's row layout around
     * its item rows. This band belongs to the (non-focusable) compartment, so the cursor
     * there focuses the <em>container</em> rather than a focusable row — which is what
     * makes the left/right borders and the bottom corners grabbable for resizing. The
     * compartments themselves stay flush with the container (their borders coincide), so
     * no "inner box" is drawn. Without it the rows fill the box to its left/right edges
     * and intercept the press meant for the container's resize control point.
     */
    private static final double RESIZE_MARGIN = 6.0;

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

    /** Identifier of the per-compartment {@link BoxLayoutManagerSpecification} that stacks
     *  the item rows (Slice 1 of the BoxLayoutManager integration). */
    private static final String COMPARTMENT_BOX_LM = "compartmentBox";

    /** Identifier of the container {@link BoxLayoutManagerSpecification} that stacks the
     *  three compartments below the header band (Slice 2). The header is reserved via
     *  {@code insetTop} and excluded from the layout because it is self-constrained. */
    private static final String ENTITY_BOX_LM = "entityBox";

    /** The three UML compartments stacked below the header, in display order. */
    public enum Compartment { INITIALIZERS, PROPERTIES, METHODS }

    private final SourceMetaModel metaModel;
    private final DianaModelFactory factory;
    /** Same instance as {@link #factory}, typed for creating {@link ConnectorView}s. */
    private final PamelaClassDiagramFactory pamelaFactory;
    /** Editing context of the diagram factory — needed to register the right-click
     *  {@link PamelaShowContextualMenuControl} on the interactive GRs. */
    private final EditingContext editingContext;

    private ShapeGRBinding<EntityView> entityViewBinding;
    private ShapeGRBinding<EntityView> entityHeaderBinding;
    private ShapeGRBinding<EntityView> entityIconBinding;
    private ShapeGRBinding<EntityView> entityInitializersBinding;
    private ShapeGRBinding<EntityView> entityPropertiesBinding;
    private ShapeGRBinding<EntityView> entityMethodsBinding;
    private ShapeGRBinding<CompartmentItem> itemRowBinding;
    private ShapeGRBinding<CompartmentItem> itemIconBinding;
    private ConnectorGRBinding<ConnectorView> connectorBinding;

    /**
     * Pool of canonical {@link CompartmentItem} instances, keyed by value. Diana's
     * structure reconciliation reuses a node only for the <em>same drawable
     * instance</em>, so we must hand back the same instance for an unchanged row across
     * successive walks (otherwise every walk creates duplicate/orphan nodes and Diana
     * logs "something strange … see isValid()"). A row whose text changes yields a new
     * value → a new instance → the node is correctly recreated with up-to-date content.
     */
    private final Map<CompartmentItem, CompartmentItem> itemPool = new HashMap<>();

    /**
     * Pool of transient (non-persisted) connector views, keyed by connector identity
     * (see {@link #connectorKey}). Same interning rationale as {@link #itemPool}: a
     * connector with no persisted {@link PropertyView} still needs a stable drawable
     * instance across walks. A persisted {@link PropertyView} (label moved) lives in the
     * diagram model instead and is used directly. Inheritance views are always transient.
     */
    private final Map<String, ConnectorView> transientConnectorPool = new HashMap<>();

    /**
     * Connector views on which the promotion + live-restyle listeners are already installed
     * (identity-keyed), so {@link #ensureConnectorListeners} runs only once per instance.
     */
    private final java.util.Set<ConnectorView> styleTrackedViews =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /**
     * Pool of detached (non-drawn) {@link PropertyView}s built solely for the graphical
     * inspector when a property's connector is not currently on the diagram (target absent
     * or property hidden). Kept separate from {@link #transientConnectorPool} — these views
     * are never drawn and never promoted; caching only avoids re-creating one on every
     * soft-selection of the same property.
     */
    private final Map<String, ConnectorView> detachedInspectionPool = new HashMap<>();

    public PamelaClassDiagramDrawing(PamelaClassDiagram diagram,
                                     SourceMetaModel metaModel,
                                     DianaModelFactory factory) {
        super(diagram, factory, PersistenceMode.UniqueGraphicalRepresentations);
        this.metaModel = metaModel;
        this.factory = factory;
        this.pamelaFactory = (factory instanceof PamelaClassDiagramFactory)
                ? (PamelaClassDiagramFactory) factory : null;
        this.editingContext = (factory instanceof PamelaModelFactory)
                ? ((PamelaModelFactory) factory).getEditingContext() : null;
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
                        // Right-click on the empty canvas → diagram-level contextual menu.
                        addContextualMenuControl(gr);
                        return gr;
                    }
                });

        // 2. Shape binding for EntityView — the UML class box CONTAINER.
        //    It carries no label of its own: the title is shown in a fixed-height
        //    header child shape (2b), and the three compartments (initializers,
        //    properties, methods) are further child shapes stacked below it (2d).
        //    Width AND height are user-resizable (and persisted); the compartments
        //    distribute the available vertical space below the header proportionally
        //    to their content via the container's entityBox BoxLayoutManager
        //    (see makeEntityBoxSpec / applyCompartmentGeometry).
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
                    // Slice 2: the three compartments are stacked below the header by this
                    // BoxLayoutManager. The header band is reserved with insetTop and excluded
                    // from the layout (it is self-constrained); compartments share the rest
                    // proportionally to their content via their layoutWeight.
                    gr.addToLayoutManagerSpecifications(makeEntityBoxSpec(factory));
                    // Right-click on the box → entity contextual menu (+ diagram-specific).
                    addContextualMenuControl(gr);
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

        // 3. Connector binding for ConnectorView (PropertyView / InheritanceView)
        connectorBinding = bindConnector(ConnectorView.class, "connector",
            entityViewBinding, entityViewBinding,
            new ConnectorGRProvider<ConnectorView>() {
                @Override
                public ConnectorGraphicalRepresentation provideGR(
                        ConnectorView cv, DianaModelFactory factory) {
                    // Routing + colour come from the connector's referenced ConnectorStyle;
                    // the end symbols are kind-driven (see #applyConnectorStyle). Connectors
                    // are drawn source → target:
                    //  - InheritanceView: sub-type → super-type
                    //  - PropertyView:    owner → property type (association/composition)
                    ConnectorGraphicalRepresentation gr =
                        factory.makeConnectorGraphicalRepresentation();
                    applyConnectorStyle(gr, cv, factory);
                    // Apply the persisted label position now, so a reloaded diagram shows
                    // the label where it was saved (the settable binding below only handles
                    // write-back and live model→view updates, not the initial value).
                    gr.setAbsoluteTextX(cv.getLabelX());
                    gr.setAbsoluteTextY(cv.getLabelY());
                    // Connectors are selectable (→ inspector) and offer a right-click menu.
                    gr.setIsSelectable(true);
                    addContextualMenuControl(gr);
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
                if (isCompartmentDisplayed(ev, Compartment.INITIALIZERS)) {
                    drawShape(entityInitializersBinding, ev);
                }
                if (isCompartmentDisplayed(ev, Compartment.PROPERTIES)) {
                    drawShape(entityPropertiesBinding, ev);
                }
                if (isCompartmentDisplayed(ev, Compartment.METHODS)) {
                    drawShape(entityMethodsBinding, ev);
                }
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
                for (ConnectorDraw cd : computeConnectors(diagram)) {
                    drawConnector(connectorBinding, cd.view,
                                  entityViewBinding, cd.source,
                                  entityViewBinding, cd.target);
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

        // 6b. Label the connectors: PropertyView → property name (UML association role),
        //     InheritanceView → none. Computed by ConnectorView.getLabel().
        connectorBinding.setDynamicPropertyValue(
            GraphicalRepresentation.TEXT,
            new DataBinding<String>("drawable.label"),
            false);

        // 6c. Persist the association-label position: settable bindings on the
        //     connector's absoluteTextX/Y (offset relative to the connector centre).
        //     Dragging the label writes back to drawable.labelX/labelY on the connector
        //     view; a transient view is promoted into the diagram's connectorViews on
        //     first move (see #makeTransientConnector), and the editor's dirty-tracking
        //     persists it.
        connectorBinding.setDynamicPropertyValue(
            GraphicalRepresentation.ABSOLUTE_TEXT_X,
            new DataBinding<Double>("drawable.labelX"), true);
        connectorBinding.setDynamicPropertyValue(
            GraphicalRepresentation.ABSOLUTE_TEXT_Y,
            new DataBinding<Double>("drawable.labelY"), true);

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

    /** A connector view together with the two entity-view endpoints to draw it between. */
    private static final class ConnectorDraw {
        final ConnectorView view;
        final EntityView source;
        final EntityView target;
        ConnectorDraw(ConnectorView view, EntityView source, EntityView target) {
            this.view = view;
            this.source = source;
            this.target = target;
        }
    }

    /**
     * Computes the connectors to display in the diagram. Existence is computed, not
     * persisted: a connector is included only when both endpoint entities are present
     * (and resolved) — and, for a property, only when it is not hidden on the source
     * {@link EntityView}. Each connector is materialised as a {@link PropertyView} /
     * {@link InheritanceView}: a persisted {@link PropertyView} (label moved) when one
     * exists, otherwise a transient interned view. Inheritance views are always transient.
     */
    private List<ConnectorDraw> computeConnectors(PamelaClassDiagram diagram) {
        List<ConnectorDraw> result = new ArrayList<>();

        Map<String, EntityView> viewByName = new HashMap<>();
        for (EntityView ev : diagram.getEntityViews()) {
            viewByName.put(ev.getQualifiedName(), ev);
        }

        for (EntityView sourceView : diagram.getEntityViews()) {
            SourceModelEntity sourceEntity = resolveEntity(sourceView);
            if (sourceEntity == null) {
                continue;
            }

            // Inheritance connectors (always transient — never persisted, never hidden)
            for (SourceModelEntity superEntity : sourceEntity.getDirectSuperEntities()) {
                EntityView targetView = viewByName.get(superEntity.getQualifiedName());
                if (targetView != null) {
                    InheritanceView iv = inheritanceView(diagram, sourceView, targetView,
                            sourceEntity, superEntity);
                    result.add(new ConnectorDraw(iv, sourceView, targetView));
                }
            }

            // Association / Composition connectors from declared (non-hidden) properties
            for (SourceModelProperty prop : sourceEntity.getDeclaredProperties().values()) {
                if (sourceView.isPropertyHidden(prop.getPropertyIdentifier())) {
                    continue;
                }
                SourceModelEntity propTypeEntity = prop.getType().getModelEntity();
                if (propTypeEntity == null) {
                    continue;
                }
                EntityView targetView = viewByName.get(propTypeEntity.getQualifiedName());
                if (targetView == null) {
                    continue;
                }
                PropertyView pv = propertyView(diagram, sourceView, targetView, prop);
                result.add(new ConnectorDraw(pv, sourceView, targetView));
            }
        }

        return result;
    }

    /** Identity key for the transient connector pool. */
    private static String connectorKey(String kind, String sourceQN, String targetQN, String discriminator) {
        return kind + "|" + sourceQN + "|" + targetQN + "|" + discriminator;
    }

    /**
     * Returns the property connector view: the persisted {@link PropertyView} if one
     * exists (label moved), otherwise a transient interned one (promoted to persisted on
     * first label move). The transient/persisted resolved property reference is refreshed
     * each walk so it stays valid across metamodel rebuilds.
     */
    private PropertyView propertyView(PamelaClassDiagram diagram, EntityView sourceView,
                                      EntityView targetView, SourceModelProperty prop) {
        PropertyView pv = diagram.findPropertyView(
                sourceView.getQualifiedName(), prop.getPropertyIdentifier());
        if (pv == null) {
            String key = connectorKey("P", sourceView.getQualifiedName(),
                    targetView.getQualifiedName(), prop.getPropertyIdentifier());
            ConnectorView pooled = transientConnectorPool.get(key);
            if (pooled instanceof PropertyView) {
                pv = (PropertyView) pooled;
            } else {
                pv = pamelaFactory.newPropertyView(sourceView.getQualifiedName(),
                        targetView.getQualifiedName(), prop.getPropertyIdentifier());
                transientConnectorPool.put(key, pv);
            }
        }
        // keep the transient resolved reference + target up to date
        pv.setProperty(prop);
        pv.setTargetQualifiedName(targetView.getQualifiedName());
        // ensure promotion + live-restyle listeners (once per view instance)
        ensureConnectorListeners(diagram, pv);
        return pv;
    }

    /**
     * Resolves the {@link PropertyView} to show in the bottom-right graphical inspector
     * (ui-design.md §19.4) for the given {@code property}.
     *
     * <p>When the property's connector is currently drawn — both endpoint entities are on
     * the diagram and the property is not hidden on its source {@link EntityView} — the
     * live interned view is returned with {@code connectorPresent = true}, so its
     * {@code labelX}/{@code labelY} offset is editable (and write-back promotes/persists it
     * exactly like dragging the label). Otherwise a <em>detached</em>, non-drawn view is
     * returned with {@code connectorPresent = false}: it carries the read-only identity
     * (source/target/label/kind) but is not part of the diagram and its offset is inert.</p>
     *
     * @return the view to inspect, or {@code null} if the property's owning entity does not
     *         resolve.
     */
    public PropertyView getPropertyViewForInspection(SourceModelProperty property) {
        if (property == null) {
            return null;
        }
        PamelaClassDiagram diagram = getModel();
        SourceModelEntity sourceEntity = property.getModelEntity();
        if (sourceEntity == null) {
            return null;
        }

        Map<String, EntityView> viewByName = new HashMap<>();
        for (EntityView ev : diagram.getEntityViews()) {
            viewByName.put(ev.getQualifiedName(), ev);
        }
        EntityView sourceView = viewByName.get(sourceEntity.getQualifiedName());
        SourceModelEntity typeEntity = property.getType().getModelEntity();
        EntityView targetView = (typeEntity != null)
                ? viewByName.get(typeEntity.getQualifiedName()) : null;

        boolean drawn = sourceView != null && targetView != null
                && !sourceView.isPropertyHidden(property.getPropertyIdentifier());

        if (drawn) {
            PropertyView pv = propertyView(diagram, sourceView, targetView, property);
            pv.setConnectorPresent(true);
            return pv;
        }

        // No connector currently drawn (target absent or property hidden): a detached,
        // read-only view. Cached per identity so repeated soft-selections don't churn.
        String key = connectorKey("D", sourceEntity.getQualifiedName(),
                typeEntity != null ? typeEntity.getQualifiedName() : "",
                property.getPropertyIdentifier());
        PropertyView pv;
        ConnectorView cached = detachedInspectionPool.get(key);
        if (cached instanceof PropertyView) {
            pv = (PropertyView) cached;
        } else {
            pv = pamelaFactory.newPropertyView(sourceEntity.getQualifiedName(),
                    typeEntity != null ? typeEntity.getQualifiedName() : "",
                    property.getPropertyIdentifier());
            detachedInspectionPool.put(key, pv);
        }
        pv.setProperty(property);
        pv.setConnectorPresent(false);
        return pv;
    }

    /**
     * Returns the inheritance connector view: the persisted {@link InheritanceView} if one
     * exists (a non-default style was applied), otherwise a transient interned one (promoted
     * to persisted when its style changes). The transient sub/super references are refreshed
     * each walk so they stay valid across metamodel rebuilds.
     */
    private InheritanceView inheritanceView(PamelaClassDiagram diagram, EntityView sourceView,
                                            EntityView targetView, SourceModelEntity subEntity,
                                            SourceModelEntity superEntity) {
        InheritanceView iv = diagram.findInheritanceView(
                sourceView.getQualifiedName(), targetView.getQualifiedName());
        if (iv == null) {
            String key = connectorKey("I", sourceView.getQualifiedName(),
                    targetView.getQualifiedName(), "");
            ConnectorView pooled = transientConnectorPool.get(key);
            if (pooled instanceof InheritanceView) {
                iv = (InheritanceView) pooled;
            } else {
                iv = pamelaFactory.newInheritanceView(
                        sourceView.getQualifiedName(), targetView.getQualifiedName());
                transientConnectorPool.put(key, iv);
            }
        }
        iv.setSubEntity(subEntity);
        iv.setSuperEntity(superEntity);
        ensureConnectorListeners(diagram, iv);
        return iv;
    }

    /**
     * Installs, once per connector-view instance, the listeners that (1) promote a transient
     * view into the diagram's {@code connectorViews} collection when it becomes persistable
     * (a moved label or a non-default style), and (2) re-apply the {@link ConnectorStyle} to
     * the live connector GR when {@code styleId} changes. Idempotent: views already tracked
     * are skipped.
     */
    private void ensureConnectorListeners(PamelaClassDiagram diagram, ConnectorView cv) {
        if (!styleTrackedViews.add(cv)) {
            return;
        }
        java.beans.PropertyChangeListener promote = evt -> {
            if (cv.isPersistable() && !diagram.getConnectorViews().contains(cv)) {
                diagram.addToConnectorViews(cv);
            }
        };
        cv.getPropertyChangeSupport().addPropertyChangeListener(ConnectorView.LABEL_X, promote);
        cv.getPropertyChangeSupport().addPropertyChangeListener(ConnectorView.LABEL_Y, promote);
        cv.getPropertyChangeSupport().addPropertyChangeListener(ConnectorView.STYLE_ID, evt -> {
            promote.propertyChange(evt);
            applyConnectorStyle(cv);
        });
    }

    /**
     * Applies the connector's referenced {@link ConnectorStyle} to a freshly-created GR
     * (routing, colour, line width and rectilinear-polyline parameters). The end symbols are
     * <em>kind-driven</em>, not part of the style: generalisation triangle for inheritance,
     * filled diamond + arrow for composition, open arrow for association.
     */
    private void applyConnectorStyle(ConnectorGraphicalRepresentation gr, ConnectorView cv,
                                     DianaModelFactory factory) {
        ConnectorStyle style = cv.getStyle();
        gr.setForeground(factory.makeForegroundStyle(style.getColor(), style.getLineWidth()));
        ConnectorSpecification spec = factory.makeConnector(style.getConnectorType());
        if (spec instanceof RectPolylinConnectorSpecification) {
            RectPolylinConnectorSpecification rp = (RectPolylinConnectorSpecification) spec;
            rp.setStraightLineWhenPossible(style.isStraightLineWhenPossible());
            rp.setIsRounded(style.isRounded());
            if (style.getArcSize() >= 0) {
                rp.setArcSize(style.getArcSize());
            }
            if (style.getAdjustability() != null) {
                rp.setAdjustability(style.getAdjustability());
            }
            if (style.getConstraints() != null) {
                rp.setRectPolylinConstraints(style.getConstraints());
            }
        }
        if (cv instanceof InheritanceView) {
            spec.setEndSymbol(EndSymbolType.PLAIN_ARROW);
            spec.setEndSymbolSize(12.0);
        } else if (cv instanceof PropertyView && isComposition((PropertyView) cv)) {
            spec.setStartSymbol(StartSymbolType.FILLED_DIAMOND);
            spec.setStartSymbolSize(10.0);
            spec.setEndSymbol(EndSymbolType.ARROW);
            spec.setEndSymbolSize(8.0);
        } else {
            spec.setEndSymbol(EndSymbolType.ARROW);
            spec.setEndSymbolSize(8.0);
        }
        gr.setConnectorSpecification(spec);
    }

    /** Re-applies the connector's style to its live GR (after the {@code styleId} changed). */
    private void applyConnectorStyle(ConnectorView cv) {
        ConnectorNode<ConnectorView> node = getConnectorNode(cv, connectorBinding);
        if (node != null
                && node.getGraphicalRepresentation() instanceof ConnectorGraphicalRepresentation) {
            applyConnectorStyle(
                    (ConnectorGraphicalRepresentation) node.getGraphicalRepresentation(), cv, factory);
        }
    }

    /** True when a property connector should be drawn as a composition ({@code @Embedded}). */
    private static boolean isComposition(PropertyView pv) {
        return pv.getProperty() != null && pv.getProperty().isEmbedded();
    }

    /**
     * Removes persisted {@link PropertyView}s that are no longer drawable: their source or
     * target entity is absent/unresolved, the property no longer exists, or it is now
     * hidden. Also drops stale {@code hiddenProperties} entries for properties that no
     * longer exist. Call after a metamodel rebuild or after removing an entity from the
     * diagram. Returns {@code true} if anything was pruned.
     */
    public boolean pruneStaleConnectorData() {
        PamelaClassDiagram diagram = getModel();
        boolean changed = false;

        Map<String, EntityView> viewByName = new HashMap<>();
        for (EntityView ev : diagram.getEntityViews()) {
            viewByName.put(ev.getQualifiedName(), ev);
        }

        // Stale persisted connector views (PropertyView label/style overrides, or
        // InheritanceView style overrides) that are no longer drawable.
        for (ConnectorView cv : new ArrayList<>(diagram.getConnectorViews())) {
            EntityView sourceView = viewByName.get(cv.getSourceQualifiedName());
            EntityView targetView = viewByName.get(cv.getTargetQualifiedName());
            SourceModelEntity sourceEntity = (sourceView != null) ? resolveEntity(sourceView) : null;
            boolean drawable;
            if (cv instanceof PropertyView) {
                PropertyView pv = (PropertyView) cv;
                drawable = sourceEntity != null && targetView != null
                        && resolveEntity(targetView) != null
                        && sourceEntity.getDeclaredProperties().containsKey(pv.getPropertyIdentifier())
                        && !sourceView.isPropertyHidden(pv.getPropertyIdentifier());
            } else if (cv instanceof InheritanceView) {
                SourceModelEntity targetEntity = (targetView != null) ? resolveEntity(targetView) : null;
                drawable = sourceEntity != null && targetEntity != null
                        && sourceEntity.getDirectSuperEntities().contains(targetEntity);
            } else {
                drawable = false;
            }
            if (!drawable) {
                diagram.removeFromConnectorViews(cv);
                changed = true;
            }
        }

        // Stale hidden-property entries (property no longer declared on the entity)
        for (EntityView ev : diagram.getEntityViews()) {
            SourceModelEntity e = resolveEntity(ev);
            if (e == null) {
                continue;
            }
            for (String id : new ArrayList<>(ev.getHiddenProperties())) {
                if (!e.getDeclaredProperties().containsKey(id)) {
                    ev.removeFromHiddenProperties(id);
                    changed = true;
                }
            }
        }

        return changed;
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

    /**
     * Whether a compartment is shown for an entity view, per its display flags
     * ({@code displayInitializers} / {@code displayProperties} / {@code displayMethods}).
     * A hidden compartment is neither drawn nor counted in the box's natural size.
     */
    private boolean isCompartmentDisplayed(EntityView ev, Compartment c) {
        switch (c) {
            case INITIALIZERS: return ev.getDisplayInitializers();
            case PROPERTIES:   return ev.getDisplayProperties();
            case METHODS:      return ev.getDisplayMethods();
            default:           return true;
        }
    }

    /** A visible compartment row: its index in the FULL model collection (so it resolves
     *  back via {@link #sourceElementFor}) and its display text. Hidden members are omitted
     *  but the surviving rows keep their original full-collection index. */
    private static final class MemberRow {
        final int index;
        final String text;
        MemberRow(int index, String text) {
            this.index = index;
            this.text = text;
        }
    }

    /**
     * The <em>visible</em> rows of a compartment, in order: members listed in
     * {@link EntityView#getHiddenProperties()} / {@code getHiddenInitializers()} /
     * {@code getHiddenMethods()} are skipped. Each surviving row keeps its index in the
     * full model collection so {@link #sourceElementFor} resolves it correctly.
     */
    private List<MemberRow> compartmentRows(EntityView ev, Compartment c) {
        SourceModelEntity e = ev.getEntity();
        List<MemberRow> rows = new ArrayList<>();
        if (e == null) {
            return rows;
        }
        switch (c) {
            case INITIALIZERS: {
                List<SourceModelInitializer> l = e.getInitializers();
                for (int i = 0; i < l.size(); i++) {
                    if (ev.isInitializerHidden(initializerIdentifier(l.get(i)))) {
                        continue;
                    }
                    rows.add(new MemberRow(i, l.get(i).getDisplayLabel()));
                }
                break;
            }
            case PROPERTIES: {
                List<SourceModelProperty> l = new ArrayList<>(e.getDeclaredProperties().values());
                for (int i = 0; i < l.size(); i++) {
                    SourceModelProperty p = l.get(i);
                    if (ev.isPropertyHidden(p.getPropertyIdentifier())) {
                        continue;
                    }
                    String type = (p.getType() != null) ? p.getType().getSimpleName() : "?";
                    String suffix = (p.getCardinality() == Cardinality.LIST) ? " [*]" : "";
                    rows.add(new MemberRow(i, p.getPropertyIdentifier() + " : " + type + suffix));
                }
                break;
            }
            case METHODS: {
                if (e.getImplementationClass() != null) {
                    List<SourceCustomMethod> l = e.getImplementationClass().getCustomMethods();
                    for (int i = 0; i < l.size(); i++) {
                        if (ev.isMethodHidden(methodIdentifier(l.get(i)))) {
                            continue;
                        }
                        rows.add(new MemberRow(i, l.get(i).getMethodName() + "()"));
                    }
                }
                break;
            }
        }
        return rows;
    }

    /** The text lines shown in a compartment (visible members only). */
    private List<String> compartmentLines(EntityView ev, Compartment c) {
        List<MemberRow> rows = compartmentRows(ev, c);
        List<String> lines = new ArrayList<>(rows.size());
        for (MemberRow r : rows) {
            lines.add(r.text);
        }
        return lines;
    }

    /** Height of a compartment given its item count: a top inset plus ROW_HEIGHT per item,
     *  or {@link #EMPTY_COMPARTMENT_HEIGHT} when empty. */
    private static double compartmentHeight(int itemCount) {
        return itemCount == 0 ? EMPTY_COMPARTMENT_HEIGHT : COMPARTMENT_TOP_INSET + itemCount * ROW_HEIGHT;
    }

    /** Sum of the <em>displayed</em> compartments' natural (content-driven) heights. */
    private double naturalCompartmentsSum(EntityView ev) {
        double s = 0;
        for (Compartment k : Compartment.values()) {
            if (isCompartmentDisplayed(ev, k)) {
                s += compartmentHeight(compartmentLines(ev, k).size());
            }
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
        // offset (ROW_TEXT_INSET_X) plus the text width and a right padding. Skip
        // compartments hidden by the entity view's display flags.
        for (Compartment c : Compartment.values()) {
            if (!isCompartmentDisplayed(ev, c)) {
                continue;
            }
            for (String line : compartmentLines(ev, c)) {
                // Rows sit inside the left/right inset band, so reserve 2*RESIZE_MARGIN too.
                w = Math.max(w, stringWidth(COMPARTMENT_FONT, line)
                        + ROW_TEXT_INSET_X + COMPARTMENT_RIGHT_PAD + 2 * RESIZE_MARGIN);
            }
        }
        return Math.ceil(w);
    }

    /** Builds a compartment GR: a white bordered box spanning the container width. It
     *  carries no text itself — its items are drawn as child rows (see makeItemRowGR).
     *  Geometry is driven by the container's entityBox layout (Slice 2): full width, and a
     *  height proportional to its {@code layoutWeight} (= natural content height). */
    private ShapeGraphicalRepresentation makeCompartmentGR(
            EntityView ev, Compartment c, DianaModelFactory f) {
        List<String> lines = compartmentLines(ev, c);
        ShapeGraphicalRepresentation gr =
            f.makeShapeGraphicalRepresentation(ShapeType.RECTANGLE);
        gr.setX(0);
        gr.setY(HEADER_HEIGHT);
        gr.setWidth(ev.getWidth());
        gr.setHeight(compartmentHeight(lines.size()));
        // Slice 2: this compartment is a weighted child of the container's entityBox layout;
        // its share of the vertical space below the header is proportional to its content
        // (weight = natural content height, 5 px floor when empty).
        gr.setLayoutManagerIdentifier(ENTITY_BOX_LM);
        BoxLayoutConstraints compartmentLc = f.newInstance(BoxLayoutConstraints.class);
        compartmentLc.setWeight(compartmentHeight(lines.size()));
        gr.setLayoutConstraints(compartmentLc);
        // Slice 1: a per-compartment vertical box layout stacks the item rows (full-width,
        // fixed ROW_HEIGHT) — replacing the per-row y=index*ROW_HEIGHT constraint bindings.
        gr.addToLayoutManagerSpecifications(makeRowBoxSpec(f));
        gr.setBackground(f.makeColoredBackground(Color.WHITE));
        gr.setForeground(f.makeForegroundStyle(Color.DARK_GRAY, 1.0f));
        gr.setShadowStyle(f.makeNoneShadowStyle());
        // Non-interactive: clicks/drag target the container.
        gr.setIsSelectable(false);
        gr.setIsFocusable(false);
        gr.setIsReadOnly(true);
        return gr;
    }

    /** Builds the container vertical box layout spec (Slice 2): stacks the three
     *  compartments full-width below the header band (reserved via {@code insetTop}),
     *  sharing the remaining height proportionally to each compartment's weight. */
    private BoxLayoutManagerSpecification makeEntityBoxSpec(DianaModelFactory f) {
        BoxLayoutManagerSpecification box =
            f.makeLayoutManagerSpecification(ENTITY_BOX_LM, BoxLayoutManagerSpecification.class);
        box.setOrientation(Orientation.VERTICAL);
        box.setCrossAxisPolicy(CrossAxisPolicy.STRETCH);
        box.setMainAxisPolicy(MainAxisPolicy.DISTRIBUTE_WEIGHTS);
        box.setInsetTop(HEADER_HEIGHT);
        box.setGap(0);
        box.setAnimateLayout(false);
        return box;
    }

    /** Builds the per-compartment vertical box layout spec (stacks item rows full-width,
     *  fixed ROW_HEIGHT, no gap). One instance per compartment GR (each compartment is its
     *  own container). */
    private BoxLayoutManagerSpecification makeRowBoxSpec(DianaModelFactory f) {
        BoxLayoutManagerSpecification box =
            f.makeLayoutManagerSpecification(COMPARTMENT_BOX_LM, BoxLayoutManagerSpecification.class);
        box.setOrientation(Orientation.VERTICAL);
        box.setCrossAxisPolicy(CrossAxisPolicy.STRETCH);
        // Inset the rows left/right inside the (full-bleed) compartment: this margin band
        // belongs to the non-focusable compartment, so the cursor there focuses the
        // container — making the left/right borders and the bottom corners grabbable for
        // resize, without moving the compartment borders (no "inner box" artifact).
        box.setInsetLeft(RESIZE_MARGIN);
        box.setInsetRight(RESIZE_MARGIN);
        // A little breathing room above the first row inside the compartment.
        box.setInsetTop(COMPARTMENT_TOP_INSET);
        box.setGap(0);
        box.setAnimateLayout(false);
        return box;
    }

    /** Icon shown at the left of each item row, by compartment kind. */
    private static ImageIcon iconForCompartment(Compartment c) {
        switch (c) {
            case PROPERTIES: return PamelaEditorIconLibrary.PROPERTY_ICON;
            case METHODS:    return PamelaEditorIconLibrary.METHOD_ICON;
            case INITIALIZERS:
            default:         return PamelaEditorIconLibrary.INITIALIZER_ICON;
        }
    }

    /** The item rows of a compartment, in order, each carrying its text and kind icon.
     *  Instances are interned (see {@link #itemPool}) so an unchanged row keeps a stable
     *  identity across walks. */
    private List<CompartmentItem> itemsFor(EntityView ev, Compartment c) {
        List<MemberRow> rows = compartmentRows(ev, c);
        List<CompartmentItem> items = new ArrayList<>(rows.size());
        ImageIcon icon = iconForCompartment(c);
        for (MemberRow r : rows) {
            // r.index is the full-collection index → sourceElementFor resolves correctly
            // even when earlier members are hidden.
            CompartmentItem item = new CompartmentItem(ev, c, r.index, r.text, icon);
            CompartmentItem canonical = itemPool.get(item);
            if (canonical == null) {
                itemPool.put(item, item);
                canonical = item;
            }
            items.add(canonical);
        }
        return items;
    }

    // =========================================================================
    // Member visibility (hide/show a property / initializer / method on the diagram)
    // =========================================================================

    /** Stable per-entity identifier of an initializer (method name + parameter list). */
    static String initializerIdentifier(SourceModelInitializer init) {
        return init.getMethodName() + "(" + String.join(",", init.getParameters()) + ")";
    }

    /** Stable per-entity identifier of a custom method (its full signature). */
    static String methodIdentifier(SourceCustomMethod method) {
        return method.getSignature();
    }

    /** The entity that declares the given source member, or {@code null}. */
    private static SourceModelEntity entityOfMember(Object member) {
        if (member instanceof SourceModelProperty) {
            return ((SourceModelProperty) member).getModelEntity();
        }
        if (member instanceof SourceModelInitializer) {
            return ((SourceModelInitializer) member).getEntity();
        }
        if (member instanceof SourceCustomMethod) {
            return ((SourceCustomMethod) member).getImplementationClass() != null
                    ? ((SourceCustomMethod) member).getImplementationClass().getEntity() : null;
        }
        return null;
    }

    /** The {@link EntityView} on this diagram that owns the member's entity, or {@code null}. */
    private EntityView entityViewForMember(Object member) {
        SourceModelEntity e = entityOfMember(member);
        return (e != null) ? entityViewFor(e) : null;
    }

    /** True if the member's entity is present on this diagram (so it can be hidden/shown). */
    public boolean isMemberOnDiagram(Object member) {
        return isHideableMember(member) && entityViewForMember(member) != null;
    }

    /** True if the given source member (property / initializer / method) is currently hidden. */
    public boolean isMemberHidden(Object member) {
        EntityView ev = entityViewForMember(member);
        if (ev == null) {
            return false;
        }
        if (member instanceof SourceModelProperty) {
            return ev.isPropertyHidden(((SourceModelProperty) member).getPropertyIdentifier());
        }
        if (member instanceof SourceModelInitializer) {
            return ev.isInitializerHidden(initializerIdentifier((SourceModelInitializer) member));
        }
        if (member instanceof SourceCustomMethod) {
            return ev.isMethodHidden(methodIdentifier((SourceCustomMethod) member));
        }
        return false;
    }

    /** Hides or shows the given source member on this diagram (no-op if not present). */
    public void setMemberHidden(Object member, boolean hidden) {
        EntityView ev = entityViewForMember(member);
        if (ev == null) {
            return;
        }
        if (member instanceof SourceModelProperty) {
            ev.setPropertyHidden(((SourceModelProperty) member).getPropertyIdentifier(), hidden);
        }
        else if (member instanceof SourceModelInitializer) {
            ev.setInitializerHidden(initializerIdentifier((SourceModelInitializer) member), hidden);
        }
        else if (member instanceof SourceCustomMethod) {
            ev.setMethodHidden(methodIdentifier((SourceCustomMethod) member), hidden);
        }
    }

    /** Whether the object is a hide/show-able diagram member. */
    public static boolean isHideableMember(Object member) {
        return member instanceof SourceModelProperty
                || member instanceof SourceModelInitializer
                || member instanceof SourceCustomMethod;
    }

    /**
     * True if the entity's view on this diagram has something to reveal in the given
     * compartment: the compartment is currently hidden, or some of its members are
     * individually hidden.
     */
    public boolean canShowAllMembers(SourceModelEntity entity, Compartment kind) {
        EntityView ev = (entity != null) ? entityViewFor(entity) : null;
        return ev != null
                && (!isCompartmentDisplayed(ev, kind) || !hiddenList(ev, kind).isEmpty());
    }

    /**
     * Reveals every member of the given compartment on this diagram: clears its hidden-member
     * list and makes the compartment visible. The resulting {@code HIDDEN_*}/{@code DISPLAY_*}
     * changes drive the editor's re-walk.
     */
    public void showAllMembers(SourceModelEntity entity, Compartment kind) {
        EntityView ev = (entity != null) ? entityViewFor(entity) : null;
        if (ev == null) {
            return;
        }
        setCompartmentDisplayed(ev, kind, true);
        for (String id : new ArrayList<>(hiddenList(ev, kind))) {
            removeHidden(ev, kind, id);
        }
    }

    /** The entity view's hidden-member list for the given compartment. */
    private static List<String> hiddenList(EntityView ev, Compartment kind) {
        switch (kind) {
            case INITIALIZERS: return ev.getHiddenInitializers();
            case METHODS:      return ev.getHiddenMethods();
            case PROPERTIES:
            default:           return ev.getHiddenProperties();
        }
    }

    private static void removeHidden(EntityView ev, Compartment kind, String id) {
        switch (kind) {
            case INITIALIZERS: ev.removeFromHiddenInitializers(id); break;
            case METHODS:      ev.removeFromHiddenMethods(id); break;
            case PROPERTIES:
            default:           ev.removeFromHiddenProperties(id); break;
        }
    }

    private static void setCompartmentDisplayed(EntityView ev, Compartment kind, boolean displayed) {
        switch (kind) {
            case INITIALIZERS: ev.setDisplayInitializers(displayed); break;
            case METHODS:      ev.setDisplayMethods(displayed); break;
            case PROPERTIES:
            default:           ev.setDisplayProperties(displayed); break;
        }
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
        // Slice 1: the row is laid out by its compartment's BoxLayoutManager (stacked
        // full-width, fixed ROW_HEIGHT via weight 0) instead of per-row constraint bindings.
        gr.setLayoutManagerIdentifier(COMPARTMENT_BOX_LM);
        BoxLayoutConstraints rowLc = f.newInstance(BoxLayoutConstraints.class);
        rowLc.setWeight(0);
        gr.setLayoutConstraints(rowLc);
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
        // Right-click on a row → contextual menu for its property/initializer/method.
        addContextualMenuControl(gr);
        return gr;
    }

    /**
     * Registers the right-click {@link PamelaShowContextualMenuControl} on a GR, so a
     * right mouse button click on it opens the shared contextual menu (no-op when the
     * editing context is unavailable). Attached to the interactive GRs only: the
     * container (entity box), the item rows and the drawing background.
     */
    private void addContextualMenuControl(GraphicalRepresentation gr) {
        if (editingContext != null) {
            gr.addToMouseClickControls(new PamelaShowContextualMenuControl(editingContext));
        }
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
     * Recomputes the compartment weights and relayouts the container's entityBox for one
     * entity view. Called after a metamodel rebuild, where the entity content — and thus the
     * natural heights driving the proportions — may have changed but the existing compartment
     * nodes are not re-provided. The item rows themselves are reconciled by the walkers (by
     * {@link CompartmentItem} value), so only the compartment weights need a refresh here.
     * Since {@code setLayoutWeight} does not by itself trigger a relayout, the entityBox
     * layout manager is invalidated and re-run explicitly.
     */
    private void applyCompartmentGeometry(EntityView ev) {
        for (Compartment c : Compartment.values()) {
            ShapeNode<EntityView> node = getShapeNode(ev, compartmentBinding(c));
            if (node != null
                    && node.getGraphicalRepresentation() instanceof ShapeGraphicalRepresentation) {
                ShapeGraphicalRepresentation cgr =
                    (ShapeGraphicalRepresentation) node.getGraphicalRepresentation();
                LayoutConstraints lc = cgr.getLayoutConstraints();
                if (!(lc instanceof BoxLayoutConstraints)) {
                    lc = getFactory().newInstance(BoxLayoutConstraints.class);
                    cgr.setLayoutConstraints(lc);
                }
                ((BoxLayoutConstraints) lc).setWeight(compartmentHeight(compartmentLines(ev, c).size()));
            }
        }
        // setLayoutWeight does not by itself trigger a relayout (a manager listens only to its
        // own spec), so recompute the container's entityBox layout explicitly.
        ShapeNode<EntityView> container = getShapeNode(ev, entityViewBinding);
        if (container != null) {
            DianaLayoutManager<?, ?> lm = container.getLayoutManager(ENTITY_BOX_LM);
            if (lm != null) {
                lm.invalidate();
                lm.doLayout(true);
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
        // Drop persisted connector data / hidden entries that no longer apply after the
        // rebuild (entity or property removed/renamed in source).
        pruneStaleConnectorData();
        updateGraphicalObjectsHierarchy();
    }

    /**
     * Re-walks the drawing and refreshes the compartment layout for one entity view
     * after a compartment {@code display*} flag or a {@code hidden*} member list changed.
     * The re-walk adds or removes the affected compartment/rows; {@link #applyCompartmentGeometry}
     * then redistributes the remaining compartments' weights below the header.
     *
     * <p>The root node is invalidated first: hiding a <b>property</b> must also drop its
     * connector, but changing the entity view's {@code hidden*} list only invalidates the
     * entity subtree (the row) — the connectors are declared by the <b>root</b> walker
     * (§9.2), which is only re-run when the root is invalidated.</p>
     */
    public void refreshCompartmentVisibility(EntityView ev) {
        // Invalidate the whole tree under the root drawable so the connector walk re-runs.
        invalidateGraphicalObjectsHierarchy(getModel());
        updateGraphicalObjectsHierarchy();
        applyCompartmentGeometry(ev);
    }

    /** Returns the {@link SourceMetaModel} used to compute connectors. */
    public SourceMetaModel getMetaModel() {
        return metaModel;
    }

    // =========================================================================
    // Node → model element mapping (selection and contextual menus)
    // =========================================================================

    /**
     * Maps a selected drawing node to the model element it represents, for the
     * application selection model (inspector / detailed browser):
     * <ul>
     *   <li>an entity box → its resolved {@link SourceModelEntity} (or the
     *       {@link EntityView} itself if unresolved);</li>
     *   <li>a compartment row → the {@code Source*} element it stands for, resolved
     *       <em>live</em> against the current entity (see {@link #sourceElementFor});</li>
     *   <li>the drawing background → the {@link PamelaClassDiagram}.</li>
     * </ul>
     * Returns {@code null} if the node maps to nothing inspectable.
     */
    public Object modelElementFor(DrawingTreeNode<?, ?> node) {
        if (node == null) {
            return null;
        }
        Object d = node.getDrawable();
        if (d instanceof EntityView) {
            SourceModelEntity e = ((EntityView) d).getEntity();
            return (e != null) ? e : d;
        }
        if (d instanceof CompartmentItem) {
            Object src = sourceElementFor((CompartmentItem) d);
            return (src != null) ? src : ((CompartmentItem) d).ev.getEntity();
        }
        if (d instanceof PropertyView) {
            SourceModelProperty p = ((PropertyView) d).getProperty();
            return (p != null) ? p : d;
        }
        if (d instanceof InheritanceView) {
            // Return the view itself so the graphical inspector can edit its connector
            // style. (The contextual-menu facets still expose the super-entity — see
            // #facetsFor — so the browser actions stay reachable.)
            return d;
        }
        if (d instanceof PamelaClassDiagram) {
            return d;
        }
        return null;
    }

    /**
     * The ordered list of contextual-menu target facets for a right-clicked node
     * (see {@code ui-design.md §18.4}): for an entity box, the resolved entity (so
     * the diagram offers the same actions as the browser) <em>then</em> the
     * {@link EntityView} (for diagram-specific actions like "Remove from diagram");
     * for a row, the underlying {@code Source*}; for the background, the diagram.
     */
    public List<Object> facetsFor(DrawingTreeNode<?, ?> node) {
        List<Object> facets = new ArrayList<>();
        if (node == null) {
            return facets;
        }
        Object d = node.getDrawable();
        if (d instanceof EntityView) {
            EntityView ev = (EntityView) d;
            if (ev.getEntity() != null) {
                facets.add(ev.getEntity());
            }
            facets.add(ev);
        }
        else if (d instanceof CompartmentItem) {
            Object src = sourceElementFor((CompartmentItem) d);
            if (src != null) {
                facets.add(src);
            }
        }
        else if (d instanceof PropertyView) {
            // The property (browser actions) then the PropertyView (diagram-specific
            // "Remove from diagram" → hide the connector).
            PropertyView pv = (PropertyView) d;
            if (pv.getProperty() != null) {
                facets.add(pv.getProperty());
            }
            facets.add(pv);
        }
        else if (d instanceof InheritanceView) {
            SourceModelEntity sup = ((InheritanceView) d).getSuperEntity();
            if (sup != null) {
                facets.add(sup);
            }
        }
        else if (d instanceof PamelaClassDiagram) {
            facets.add(d);
        }
        return facets;
    }

    /**
     * Returns the {@link DrawingTreeNode} (shape node) that corresponds to the given
     * application element, or {@code null} if no matching node exists in the current tree.
     *
     * <p>Supported element types:</p>
     * <ul>
     *   <li>{@link EntityView} → container shape node;</li>
     *   <li>{@link SourceModelEntity} → container shape node of the matching EntityView;</li>
     *   <li>{@link SourceModelProperty} / {@link SourceModelInitializer} → item-row shape node.</li>
     * </ul>
     */
    public DrawingTreeNode<?, ?> shapeNodeForSourceElement(Object element) {
        if (element instanceof EntityView) {
            return getShapeNode((EntityView) element, entityViewBinding);
        }
        if (element instanceof SourceModelEntity) {
            for (EntityView ev : getModel().getEntityViews()) {
                if (ev.getEntity() == element) {
                    return getShapeNode(ev, entityViewBinding);
                }
            }
            return null;
        }
        if (element instanceof SourceModelProperty) {
            SourceModelEntity entity = ((SourceModelProperty) element).getModelEntity();
            EntityView ev = entityViewFor(entity);
            if (ev == null) return null;
            List<SourceModelProperty> props = new ArrayList<>(entity.getDeclaredProperties().values());
            int index = props.indexOf(element);
            if (index < 0) return null;
            return findItemRowNode(ev, Compartment.PROPERTIES, index);
        }
        if (element instanceof SourceModelInitializer) {
            SourceModelEntity entity = ((SourceModelInitializer) element).getEntity();
            EntityView ev = entityViewFor(entity);
            if (ev == null) return null;
            int index = entity.getInitializers().indexOf(element);
            if (index < 0) return null;
            return findItemRowNode(ev, Compartment.INITIALIZERS, index);
        }
        return null;
    }

    /** Returns the EntityView whose resolved entity is {@code entity}, or {@code null}. */
    private EntityView entityViewFor(SourceModelEntity entity) {
        if (entity == null) return null;
        for (EntityView ev : getModel().getEntityViews()) {
            if (ev.getEntity() == entity) return ev;
        }
        return null;
    }

    /** Finds the interned {@link CompartmentItem} row node for (ev, kind, index). */
    private DrawingTreeNode<?, ?> findItemRowNode(EntityView ev, Compartment kind, int index) {
        for (CompartmentItem item : itemPool.keySet()) {
            if (item.ev == ev && item.kind == kind && item.index == index) {
                return getShapeNode(item, itemRowBinding);
            }
        }
        return null;
    }

    /**
     * Resolves a compartment row to its {@code Source*} element ({@link SourceModelProperty},
     * {@link SourceModelInitializer} or {@link SourceCustomMethod}) against the
     * <em>current</em> entity, exploiting that {@link #compartmentLines} builds each
     * compartment in the same order as the underlying model collection. Resolved live
     * (never stored on the interned {@link CompartmentItem}) so it stays correct across
     * metamodel rebuilds. Returns {@code null} if the entity is unresolved or the
     * content shrank under the row index since the last walk.
     */
    private Object sourceElementFor(CompartmentItem item) {
        SourceModelEntity e = (item != null) ? item.ev.getEntity() : null;
        if (e == null) {
            return null;
        }
        switch (item.kind) {
            case INITIALIZERS: {
                List<SourceModelInitializer> l = e.getInitializers();
                return (item.index < l.size()) ? l.get(item.index) : null;
            }
            case PROPERTIES: {
                List<SourceModelProperty> l = new ArrayList<>(e.getDeclaredProperties().values());
                return (item.index < l.size()) ? l.get(item.index) : null;
            }
            case METHODS: {
                if (e.getImplementationClass() == null) {
                    return null;
                }
                List<SourceCustomMethod> l = e.getImplementationClass().getCustomMethods();
                return (item.index < l.size()) ? l.get(item.index) : null;
            }
            default:
                return null;
        }
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
