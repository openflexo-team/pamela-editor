package org.openflexo.pamela.editor.ui.diagram;

import javax.swing.JComponent;

import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
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

    /** Returns the Swing component to embed in the central tab pane. */
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

    /** The tab title shown in the central pane. */
    public String getTitle() {
        String name = diagram.getName();
        return (name != null && !name.isEmpty()) ? name : "Untitled diagram";
    }
}
