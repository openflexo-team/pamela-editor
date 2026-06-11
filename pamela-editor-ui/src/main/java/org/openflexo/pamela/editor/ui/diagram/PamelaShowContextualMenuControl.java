package org.openflexo.pamela.editor.ui.diagram;

import java.awt.Point;

import org.openflexo.diana.Drawing.DrawingTreeNode;
import org.openflexo.diana.control.MouseControlContext;
import org.openflexo.diana.control.actions.MouseClickControlActionImpl;
import org.openflexo.diana.control.actions.MouseClickControlImpl;
import org.openflexo.diana.view.DianaView;
import org.openflexo.pamela.factory.EditingContext;

/**
 * Right mouse button click control that opens the editor's contextual menu on the
 * right-clicked diagram node.
 *
 * <p>Registered on the interactive GRs of {@link PamelaClassDiagramDrawing} (the
 * entity box container, the compartment rows and the drawing background) via
 * {@code gr.addToMouseClickControls(...)}. On a right click it resolves the
 * {@link DianaView} and the in-view point and delegates to
 * {@link DianaDrawingEditor#showContextualMenu(DrawingTreeNode, DianaView, Point)},
 * which maps the node to its model facets and shows the shared {@code JPopupMenu}.</p>
 *
 * <p>Mirrors the reference {@code diana-drawing-editor}'s {@code ShowContextualMenuControl}.</p>
 */
public class PamelaShowContextualMenuControl extends MouseClickControlImpl<DianaDrawingEditor> {

    public PamelaShowContextualMenuControl(EditingContext editingContext) {
        super("Show contextual menu", MouseButton.RIGHT, 1,
                new MouseClickControlActionImpl<DianaDrawingEditor>() {
                    @Override
                    public boolean handleClick(DrawingTreeNode<?, ?> dtn, DianaDrawingEditor controller,
                            MouseControlContext context) {
                        DianaView<?, ?> view = controller.getDrawingView().viewForNode(dtn);
                        Point newPoint = getPointInView(dtn, controller, context);
                        controller.showContextualMenu(dtn, view, newPoint);
                        return false;
                    }
                }, false, false, false, false, editingContext);
    }
}
