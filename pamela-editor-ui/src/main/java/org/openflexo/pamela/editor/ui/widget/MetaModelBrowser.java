package org.openflexo.pamela.editor.ui.widget;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.FIBJPanel;
import org.openflexo.gina.swing.view.widget.DnDJTree;
import org.openflexo.gina.view.widget.browser.impl.FIBBrowserModel.BrowserCell;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.pamela.editor.ui.diagram.PamelaClassDiagramEditor;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Left-top browser panel.  Shows all open sessions and their content as a tree:
 * <pre>
 * PamelaProject (project name)
 * ├── Diagrams
 * │   └── PamelaClassDiagram
 * └── Packages
 *     └── SourcePackage
 *         └── SourceModelEntity
 * </pre>
 *
 * <p>The data object is the {@link PamelaEditorApplication} itself (so the
 * FIB can reach {@code data.sessions}).</p>
 *
 * <p>Single-click propagates via {@link MetaModelBrowserFIBController#getSelectedElement()}
 * property-change events to {@link PamelaEditorApplication#setCurrentSelectedElement(Object)}.</p>
 */
@SuppressWarnings("serial")
public class MetaModelBrowser extends FIBJPanel<PamelaEditorApplication> {

    private static final Logger logger =
            Logger.getLogger(MetaModelBrowser.class.getPackage().getName());

    public static final Resource FIB_FILE =
            ResourceLocator.locateResource("Fib/MetaModelBrowser.fib");

    private final PamelaEditorApplication application;

    public MetaModelBrowser(PamelaEditorApplication application) {
        super(FIB_FILE, application,
              ApplicationFIBLibraryImpl.instance(),
              PamelaEditorFIBController.EDITOR_LOCALIZATION);
        this.application = application;
    }

    @Override
    public MetaModelBrowserFIBController getController() {
        return (MetaModelBrowserFIBController) super.getController();
    }

    @Override
    public Class<PamelaEditorApplication> getRepresentedType() {
        return PamelaEditorApplication.class;
    }

    @Override
    public void delete() {
    }

    /**
     * Enables dragging a {@link org.openflexo.pamela.editor.model.SourceModelEntity}
     * from this browser tree onto a class diagram, using Gina's native external
     * drag-and-drop mechanism.
     *
     * <p>Requires the FIB {@code <Browser>} to declare
     * {@code allowsExternalDragAndDrop="true"} so that Gina instantiates an
     * {@code ExternalDnDJTree}. We locate that {@link DnDJTree}, then register a
     * {@link DGListener} via {@code JFIBBrowserWidget.registerDragGestureListener}.
     * The dragged payload is the Gina {@code BrowserCell} itself (which is a
     * {@link Transferable}); the drop side reads it from the Diana editor's drag
     * source context, populated here by {@link DSListener#dragOver}.</p>
     *
     * @return {@code true} if the browser tree was found and drag was enabled
     */
    public boolean enableEntityDrag() {
        JTree tree = findJTree(this);
        if (!(tree instanceof DnDJTree)) {
            return false;
        }
        DnDJTree dndTree = (DnDJTree) tree;
        try {
            dndTree.getWidget().registerDragGestureListener(new DGListener(dndTree));
            installPressGuard(dndTree);
            return true;
        } catch (Exception e) {
            logger.warning("Failed to register drag gesture listener on browser: " + e);
            return false;
        }
    }

    /**
     * Sets the application's "browser press" guard <em>before</em> the tree processes
     * the press, so the central-view switch can be deferred while a drag may be
     * starting.
     *
     * <p>A plain {@code addMouseListener} on the tree is notified <em>after</em> the
     * tree's own UI mouse handler has already changed the selection (and switched the
     * view). To run first, we use a global {@link java.awt.Toolkit} AWT event listener:
     * {@code notifyAWTEventListeners} fires before the target component's own listeners
     * during event dispatch.</p>
     */
    private void installPressGuard(JTree dndTree) {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (!(event instanceof MouseEvent)) {
                return;
            }
            MouseEvent me = (MouseEvent) event;
            // Only react to presses/releases that target this browser's tree.
            if (me.getSource() != dndTree) {
                return;
            }
            if (me.getID() == MouseEvent.MOUSE_PRESSED) {
                application.onBrowserMousePressed();
            } else if (me.getID() == MouseEvent.MOUSE_RELEASED) {
                application.onBrowserMouseReleased();
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    /**
     * Target and hard-cap row counts for {@link #autoExpandToFillView()} (roughly a
     * screenful without overflowing it).
     */
    private static final int AUTO_EXPAND_TARGET_ROWS = 20;
    private static final int AUTO_EXPAND_MAX_ROWS = 30;

    /**
     * Auto-expands the tree, one level of the whole forest at a time, so the browser opens
     * showing substantially more than just the top level — without turning into an
     * overwhelming wall of rows.
     *
     * <p>Algorithm: while fewer than {@value #AUTO_EXPAND_TARGET_ROWS} rows are visible,
     * expand <em>every</em> currently visible, collapsed, non-leaf row at once (this is "the
     * next available level" — different branches may be at different depths, e.g. once a
     * shallow leaf stops growing while a deeper branch keeps going, but each pass always
     * expands whatever is currently at the collapsed frontier). If that expansion would push
     * the total past {@value #AUTO_EXPAND_MAX_ROWS} rows, undo it and stop; otherwise keep it
     * and try the next level. Also stops as soon as there is nothing left to expand.</p>
     *
     * <p>Safe to call whenever new top-level content just appeared in the tree (e.g. right
     * after a project is opened/created) — a JTree row is only actually populated with its
     * real children the first time it is expanded (Gina's lazy-loading, see
     * {@code gina/CLAUDE.md}), so this both measures and (provisionally) performs each level's
     * expansion in the same pass, relying on the fact that expanding/collapsing a row here
     * runs synchronously on the EDT (no repaint happens in between, so a reverted, over-budget
     * expansion is never actually seen).</p>
     */
    public void autoExpandToFillView() {
        JTree tree = findJTree(this);
        if (tree != null) {
            expandToFillView(tree);
        }
    }

    /**
     * The actual algorithm behind {@link #autoExpandToFillView()}, factored out (and taking a
     * plain {@link JTree}) so it can be exercised directly against a hand-built tree in tests,
     * independently of the Gina/Spoon machinery that backs the real browser.
     */
    static void expandToFillView(JTree tree) {
        while (tree.getRowCount() < AUTO_EXPAND_TARGET_ROWS) {
            List<TreePath> frontier = collapsedExpandableRows(tree);
            if (frontier.isEmpty()) {
                break; // fully expanded already, even if under the target
            }
            for (TreePath path : frontier) {
                tree.expandPath(path);
            }
            if (tree.getRowCount() > AUTO_EXPAND_MAX_ROWS) {
                for (TreePath path : frontier) {
                    tree.collapsePath(path);
                }
                break; // this level alone would overshoot the budget — stop here
            }
        }
    }

    /** Every currently visible row that is collapsed and has at least one child. */
    private static List<TreePath> collapsedExpandableRows(JTree tree) {
        List<TreePath> result = new ArrayList<>();
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            if (path == null || tree.isExpanded(path)) {
                continue;
            }
            if (!tree.getModel().isLeaf(path.getLastPathComponent())) {
                result.add(path);
            }
        }
        return result;
    }

    /** Depth-first search for the first {@link JTree} in a component hierarchy. */
    private static JTree findJTree(Component component) {
        if (component instanceof JTree) {
            return (JTree) component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                JTree found = findJTree(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    // =========================================================================
    // Drag source — exports the selected BrowserCell to a class diagram
    // =========================================================================

    /**
     * Starts a drag with the currently selected {@link BrowserCell} when the user
     * drags a tree node. The {@code BrowserCell} carries the represented object
     * (a {@code SourceModelEntity}) that the diagram drop delegate consumes.
     */
    private final class DGListener implements DragGestureListener {

        private final DnDJTree sourceComponent;
        private final DSListener dsListener = new DSListener();

        DGListener(DnDJTree sourceComponent) {
            this.sourceComponent = sourceComponent;
        }

        @Override
        public void dragGestureRecognized(DragGestureEvent e) {
            BrowserCell dragNode = sourceComponent.getSelectedBrowserCell();
            if (dragNode == null) {
                return;
            }
            // This press is a drag, not a click: tell the app so it does not switch
            // the central view away from the diagram being dropped onto.
            application.onBrowserDragStarted();
            // The BrowserCell itself is the Transferable (supports BROWSER_CELL_FLAVOR).
            Transferable transferable = dragNode;
            try {
                e.startDrag(DragSource.DefaultCopyNoDrop, transferable, dsListener);
            } catch (Exception idoe) {
                logger.warning("Unexpected exception starting drag: " + idoe);
            }
        }
    }

    /**
     * Tracks the drag operation and pushes the {@code DragSourceContext} into the
     * active diagram editor, so its drop delegate can read the dragged
     * {@code BrowserCell} (Gina's drop transfer data is an empty marker).
     */
    private final class DSListener implements DragSourceListener {

        @Override
        public void dragEnter(DragSourceDragEvent e) {
            pushContext(e.getDragSourceContext());
        }

        @Override
        public void dragOver(DragSourceDragEvent e) {
            pushContext(e.getDragSourceContext());
        }

        @Override
        public void dragDropEnd(DragSourceDropEvent e) {
            // Clear the stale context once the drop is over.
            pushContext(null);
            // Release the browser press guard (a JTree may not receive mouseReleased
            // after a DnD operation); do not commit the deferred view switch.
            application.onBrowserDragEnded();
        }

        /** Pushes the current drag source context into the active diagram editor. */
        private void pushContext(DragSourceContext context) {
            PamelaClassDiagramEditor editor = application.getActiveDiagramEditor();
            if (editor != null) {
                editor.getDianaEditor().setDragSourceContext(context);
            }
        }

        @Override
        public void dragExit(DragSourceEvent e) {
        }

        @Override
        public void dropActionChanged(DragSourceDragEvent e) {
        }
    }
}
