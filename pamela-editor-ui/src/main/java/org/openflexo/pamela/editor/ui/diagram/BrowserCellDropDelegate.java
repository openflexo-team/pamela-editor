package org.openflexo.pamela.editor.ui.diagram;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.util.logging.Logger;

import org.openflexo.diana.swing.control.tools.DataFlavorDelegate;
import org.openflexo.diana.swing.control.tools.DianaViewDropListener;
import org.openflexo.gina.view.widget.browser.impl.FIBBrowserModel;
import org.openflexo.gina.view.widget.browser.impl.FIBBrowserModel.BrowserCell;
import org.openflexo.pamela.editor.model.SourceModelEntity;

/**
 * {@link DataFlavorDelegate} that accepts a {@link BrowserCell} dragged from a
 * Gina browser (flavor {@link FIBBrowserModel#BROWSER_CELL_FLAVOR}) and, when it
 * represents a {@link SourceModelEntity}, adds it to the diagram at the drop
 * location.
 *
 * <p>Gina's {@code BrowserCell.getTransferData()} returns an <em>empty</em>
 * {@code TransferedBrowserCell} marker — it carries no payload. The actual
 * dragged object is therefore read from the Diana editor's drag source context
 * (populated by the browser drag source's {@code DragSourceListener.dragOver}),
 * not from the drop event. This mirrors the Openflexo diagram editor pattern.</p>
 */
public class BrowserCellDropDelegate extends DataFlavorDelegate {

    private static final Logger logger =
            Logger.getLogger(BrowserCellDropDelegate.class.getPackage().getName());

    public BrowserCellDropDelegate(DianaViewDropListener dropListener) {
        super(dropListener);
    }

    @Override
    public DataFlavor getDataFlavor() {
        return FIBBrowserModel.BROWSER_CELL_FLAVOR;
    }

    @Override
    public int getAcceptableActions() {
        return DnDConstants.ACTION_COPY_OR_MOVE;
    }

    @Override
    public boolean isDragOk(DropTargetDragEvent e) {
        return getDraggedEntity() != null;
    }

    @Override
    public boolean performDrop(DropTargetDropEvent e) {
        SourceModelEntity entity = getDraggedEntity();
        if (entity == null) {
            return false;
        }
        if (!(getDianaEditor() instanceof DianaDrawingEditor)) {
            return false;
        }
        DianaDrawingEditor editor = (DianaDrawingEditor) getDianaEditor();

        // Convert the drop point (view pixels) to logical drawing coordinates.
        double scale = getDrawingView().getScale();
        if (scale <= 0) {
            scale = 1.0;
        }
        Point pt = e.getLocation();
        double logicalX = pt.x / scale;
        double logicalY = pt.y / scale;

        editor.addEntity(entity, logicalX, logicalY);
        logger.fine("Dropped entity " + entity.getQualifiedName() + " on diagram");
        return true;
    }

    /**
     * Resolves the {@link SourceModelEntity} currently being dragged, reading the
     * {@link BrowserCell} from the editor's drag source context (with a fallback
     * to {@code getObjectBeingTransfered()} for OS quirks).
     */
    private SourceModelEntity getDraggedEntity() {
        Transferable transferable = null;
        if (getDianaEditor().getDragSourceContext() != null) {
            transferable = getDianaEditor().getDragSourceContext().getTransferable();
        } else if (getDianaEditor().getObjectBeingTransfered() != null) {
            transferable = getDianaEditor().getObjectBeingTransfered();
        }
        if (transferable instanceof BrowserCell) {
            Object represented = ((BrowserCell) transferable).getRepresentedObject();
            if (represented instanceof SourceModelEntity) {
                return (SourceModelEntity) represented;
            }
        }
        return null;
    }
}
