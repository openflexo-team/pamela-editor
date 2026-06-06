package org.openflexo.pamela.editor.ui.diagram;

import java.util.logging.Logger;

import org.openflexo.diana.swing.JDianaInteractiveEditor;
import org.openflexo.diana.swing.control.SwingToolFactory;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagramFactory;

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
}
