package org.openflexo.pamela.editor.ui.diagram;

import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import javax.swing.JComponent;

import org.openflexo.pamela.editor.diagram.EntityView;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Orchestrates the Diana rendering of a single {@link PamelaClassDiagram}.
 *
 * <p>Layout of responsibilities:
 * <ul>
 *   <li>{@link PamelaClassDiagramDrawing} — model ↔ graphical structure mapping</li>
 *   <li>{@link DianaDrawingEditor} — Swing interactive editor (selection, drag, resize)</li>
 *   <li>{@code PamelaClassDiagramEditor} — lifecycle owner; provides the Swing component
 *       to embed in the central {@code JTabbedPane}</li>
 * </ul>
 * </p>
 *
 * <p>Usage:
 * <pre>
 *   PamelaClassDiagramEditor editor = new PamelaClassDiagramEditor(session, diagram);
 *   tabbedPane.addTab(diagram.getName(), editor.getView());
 * </pre>
 * </p>
 */
public class PamelaClassDiagramEditor {

    private final PamelaProject session;
    private final PamelaClassDiagram diagram;

    private PamelaClassDiagramDrawing drawing;
    private DianaDrawingEditor dianaEditor;

    private boolean dirtyTrackingInstalled;
    private Runnable onDirty;
    private final Set<EntityView> trackedViews =
            Collections.newSetFromMap(new IdentityHashMap<>());

    public PamelaClassDiagramEditor(PamelaProject session, PamelaClassDiagram diagram) {
        this.session = session;
        this.diagram = diagram;
    }

    // -------------------------------------------------------------------------
    // Lazy initialisation
    // -------------------------------------------------------------------------

    /** Returns the Diana drawing (initialised lazily on first call). */
    public PamelaClassDiagramDrawing getDrawing() {
        if (drawing == null) {
            drawing = new PamelaClassDiagramDrawing(
                    diagram,
                    session.getMetaModel(),
                    session.getDiagramFactory());
            drawing.init();
        }
        return drawing;
    }

    /** Returns the interactive Diana editor (initialised lazily on first call). */
    public DianaDrawingEditor getDianaEditor() {
        if (dianaEditor == null) {
            dianaEditor = new DianaDrawingEditor(
                    getDrawing(),
                    session.getDiagramFactory(),
                    session.getToolFactory());
        }
        return dianaEditor;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the Swing component to embed in the central tab pane.
     *
     * <p>The drop target for browser-to-diagram drag-and-drop is wired by Diana
     * itself through {@link DianaDrawingEditor#makeDropListener}, so no Swing
     * {@code DropTarget} is installed here.</p>
     */
    public JComponent getView() {
        return getDianaEditor().getDrawingView();
    }

    /** Returns the diagram model displayed by this editor. */
    public PamelaClassDiagram getDiagram() {
        return diagram;
    }

    /** Returns the session this editor belongs to. */
    public PamelaProject getSession() {
        return session;
    }

    /**
     * Requests a full structural refresh.
     * Call this after any mutation that adds or removes entities from the diagram
     * so that Diana re-walks the model and reconciles shapes and connectors.
     */
    public void refresh() {
        if (drawing != null) {
            drawing.updateGraphicalObjectsHierarchy();
        }
    }

    /**
     * Re-resolves the entity references of all entity views against the (rebuilt)
     * meta-model and restyles unresolved boxes. Call after a project rebuild.
     */
    public void refreshEntityResolution() {
        if (drawing != null) {
            drawing.refreshEntityResolution();
        }
    }

    /**
     * Installs listeners that invoke {@code onDirty} whenever the diagram is mutated:
     * an entity view is added/removed (the {@code entityViews} collection changes) or a
     * shape is moved/resized (an {@link EntityView}'s {@code x/y/width/height} changes).
     * Idempotent — only the first call takes effect.
     *
     * @param onDirty callback run on any diagram mutation (e.g. mark the project dirty)
     */
    public void installDirtyTracking(Runnable onDirty) {
        if (dirtyTrackingInstalled) {
            return;
        }
        dirtyTrackingInstalled = true;
        this.onDirty = onDirty;

        // Collection add/remove → dirty; also start tracking geometry of added views.
        diagram.getPropertyChangeSupport().addPropertyChangeListener(
                PamelaClassDiagram.ENTITY_VIEWS, evt -> {
                    onDirty.run();
                    if (evt.getNewValue() instanceof EntityView) {
                        trackViewGeometry((EntityView) evt.getNewValue());
                    }
                });

        for (EntityView ev : diagram.getEntityViews()) {
            trackViewGeometry(ev);
        }
    }

    /** Registers a geometry listener on an entity view (once) so moves/resizes mark dirty. */
    private void trackViewGeometry(EntityView ev) {
        if (ev == null || onDirty == null || !trackedViews.add(ev)) {
            return;
        }
        PropertyChangeListener geom = evt -> onDirty.run();
        ev.getPropertyChangeSupport().addPropertyChangeListener(EntityView.X, geom);
        ev.getPropertyChangeSupport().addPropertyChangeListener(EntityView.Y, geom);
        ev.getPropertyChangeSupport().addPropertyChangeListener(EntityView.WIDTH, geom);
        ev.getPropertyChangeSupport().addPropertyChangeListener(EntityView.HEIGHT, geom);

        // Toggling a compartment-visibility flag marks the project dirty and re-walks
        // the drawing so the compartment appears/disappears immediately.
        PropertyChangeListener display = evt -> {
            onDirty.run();
            if (drawing != null) {
                drawing.refreshCompartmentVisibility(ev);
            }
        };
        ev.getPropertyChangeSupport().addPropertyChangeListener(EntityView.DISPLAY_INITIALIZERS, display);
        ev.getPropertyChangeSupport().addPropertyChangeListener(EntityView.DISPLAY_PROPERTIES, display);
        ev.getPropertyChangeSupport().addPropertyChangeListener(EntityView.DISPLAY_METHODS, display);
    }

    /**
     * Adds an entity to the diagram at the given logical position, then refreshes
     * the drawing so the new shape and all connectors towards already-present
     * entities appear automatically.
     *
     * <p>If the entity is already present (same qualified name), this is a no-op
     * and returns the existing {@link EntityView} — entities are never duplicated.</p>
     *
     * <p>Delegates to {@link DianaDrawingEditor#addEntity(SourceModelEntity, double, double)},
     * which holds the actual model-mutation logic so the Diana drop delegate can
     * reuse it directly.</p>
     *
     * @param entity the source entity to represent (must not be null)
     * @param x      logical X position on the canvas (drawing coordinates)
     * @param y      logical Y position on the canvas (drawing coordinates)
     * @return the {@link EntityView} representing the entity (new or existing)
     */
    public EntityView addEntity(SourceModelEntity entity, double x, double y) {
        return getDianaEditor().addEntity(entity, x, y);
    }

    /**
     * Wires the diagram selection to a callback (the application's soft-selection
     * path). Invoked with the {@code (lead, selection)} pair of the selected nodes'
     * model elements; {@code lead} is {@code null} when the selection is cleared.
     */
    public void setSelectionListener(DianaDrawingEditor.SelectionListener listener) {
        getDianaEditor().setSelectionListener(listener);
    }

    /** Wires the right-click contextual menu to the application's shared menu builder. */
    public void setContextualMenuHandler(DianaDrawingEditor.ContextualMenuHandler handler) {
        getDianaEditor().setContextualMenuHandler(handler);
    }

    /** The tab title shown in the central pane. */
    public String getTitle() {
        String name = diagram.getName();
        return (name != null && !name.isEmpty()) ? name : "Untitled diagram";
    }
}
