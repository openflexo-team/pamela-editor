package org.openflexo.pamela.editor.ui.diagram;

import java.util.logging.Logger;

import org.openflexo.diana.swing.JDianaInteractiveEditor;
import org.openflexo.diana.swing.control.SwingToolFactory;
import org.openflexo.diana.swing.control.tools.DianaViewDropListener;
import org.openflexo.diana.swing.view.JDianaView;
import org.openflexo.pamela.editor.diagram.EntityView;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagramFactory;
import org.openflexo.pamela.editor.model.SourceModelEntity;

/**
 * Diana interactive editor for a {@link PamelaClassDiagram}.
 *
 * <p>In the first sprint the editor supports selection and drag (the default
 * {@code SelectionTool}).  Drawing new shapes from the canvas is intentionally
 * not wired yet — entities are added through the browser or the menu, not by
 * drawing rectangles on screen.</p>
 *
 * <p>Connectors are computed by {@link PamelaClassDiagramDrawing} from the
 * meta-model; they cannot be drawn by the user.</p>
 */
public class DianaDrawingEditor extends JDianaInteractiveEditor<PamelaClassDiagram> {

    private static final Logger logger =
            Logger.getLogger(DianaDrawingEditor.class.getPackage().getName());

    public DianaDrawingEditor(PamelaClassDiagramDrawing drawing,
                              PamelaClassDiagramFactory factory,
                              SwingToolFactory toolFactory) {
        super(drawing, factory, toolFactory);
    }

    @Override
    public PamelaClassDiagramDrawing getDrawing() {
        return (PamelaClassDiagramDrawing) super.getDrawing();
    }

    @Override
    public PamelaClassDiagramFactory getFactory() {
        return (PamelaClassDiagramFactory) super.getFactory();
    }

    @Override
    public PamelaClassDiagramEditorView makeDrawingView() {
        return new PamelaClassDiagramEditorView(this);
    }

    @Override
    public PamelaClassDiagramEditorView getDrawingView() {
        return (PamelaClassDiagramEditorView) super.getDrawingView();
    }

    /**
     * Hooks the browser-to-diagram drag-and-drop into Diana's drop machinery.
     * Adds a {@link BrowserCellDropDelegate} (handling Gina's
     * {@code BROWSER_CELL_FLAVOR}) alongside Diana's default palette delegate.
     */
    @Override
    public DianaViewDropListener makeDropListener(JDianaView<?, ?> view) {
        DianaViewDropListener listener = new DianaViewDropListener(view, this);
        listener.addFlavor(new BrowserCellDropDelegate(listener));
        return listener;
    }

    /**
     * Adds an entity to the diagram at the given logical position and refreshes
     * the drawing so the new shape and its connectors towards already-present
     * entities appear. Never duplicates an entity already on the diagram.
     *
     * @param entity the source entity to represent (must not be null)
     * @param x      logical X position (drawing coordinates)
     * @param y      logical Y position (drawing coordinates)
     * @return the {@link EntityView} representing the entity (new or existing)
     */
    public EntityView addEntity(SourceModelEntity entity, double x, double y) {
        if (entity == null) {
            return null;
        }
        PamelaClassDiagram diagram = getDrawing().getModel();
        String qualifiedName = entity.getQualifiedName();

        // Dedupe: never add the same entity twice.
        for (EntityView existing : diagram.getEntityViews()) {
            if (qualifiedName.equals(existing.getQualifiedName())) {
                return existing;
            }
        }

        EntityView ev = getFactory().newEntityView(qualifiedName, x, y, 200.0, 120.0);
        ev.setEntity(entity);
        // Default to the optimal (content-fitting) size for the dropped entity.
        ev.setWidth(getDrawing().optimalWidth(ev));
        ev.setHeight(getDrawing().optimalHeight(ev));
        diagram.addToEntityViews(ev);
        getDrawing().updateGraphicalObjectsHierarchy();
        logger.fine("Added entity " + qualifiedName + " to diagram at " + x + "," + y);
        return ev;
    }
}
