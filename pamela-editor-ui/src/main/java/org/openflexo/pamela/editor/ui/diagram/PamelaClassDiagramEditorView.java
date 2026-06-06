package org.openflexo.pamela.editor.ui.diagram;

import org.openflexo.diana.swing.view.JDrawingView;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;

/**
 * Swing drawing canvas for a {@link PamelaClassDiagram}.
 *
 * <p>Subclasses {@link JDrawingView} to allow future custom overlay painting
 * (e.g. selection handles, drag feedback).  In the first sprint no custom
 * painting is needed — the standard Diana rendering is sufficient.</p>
 */
public class PamelaClassDiagramEditorView extends JDrawingView<PamelaClassDiagram> {

    public PamelaClassDiagramEditorView(DianaDrawingEditor controller) {
        super(controller);
    }
}
