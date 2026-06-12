package org.openflexo.pamela.editor.ui.widget;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import org.openflexo.pamela.editor.model.SourceCompilationUnit;
import org.openflexo.pamela.editor.model.SourceJavaFile;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;

import spoon.Launcher;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Context-panel view that shows a Java Outline for the currently displayed source
 * file (ui-design.md §19.3).
 *
 * <p>This is a plain Swing {@link JScrollPane} wrapping a {@link JTree}. It does
 * <em>not</em> use a Gina {@code FIBJPanel} because Gina's eager tree construction
 * (one {@code BrowserCell} + two Connie evaluations per node, all on the EDT) took
 * ~315 ms per method on large types such as {@code FlexoConcept} (168 methods →
 * 53 seconds). The plain-Swing approach with lazy child loading is O(visible rows)
 * and typically finishes in under 50 ms.</p>
 *
 * <p>Children of a type node are created <em>lazily</em>: they are populated in
 * {@link LazyTypeNode} when the user (or code) first expands the node, via a
 * {@link TreeWillExpandListener}. Creating 168 {@link DefaultMutableTreeNode}s is
 * a few milliseconds; there is no Connie reflection cost.</p>
 *
 * <p>Label and icon logic lives in {@link SpoonOutlineController} as static helpers
 * and is called directly from the cell renderer — no reflection, no proxy.</p>
 */
/**
 * Pure-Swing backup of {@link SpoonOutlineView}.
 *
 * <p>Kept as a reference implementation while the Gina {@code FIBBrowserModel}
 * performance issue (O(n²) tree rebuild, ~315 ms/node for large types) is being
 * fixed in Gina itself. Once the Gina fix lands this class can be deleted or
 * promoted back to {@code SpoonOutlineView} if preferred.</p>
 *
 * <p>Key characteristics of this approach:
 * <ul>
 *   <li>Plain {@link JScrollPane} + {@link JTree} — no Gina/Connie overhead.</li>
 *   <li>Lazy child loading via {@link TreeWillExpandListener}: children of a type
 *       node are created only when the node is first expanded, so the initial
 *       render is O(number of top-level types), not O(total members).</li>
 *   <li>Label/icon resolution via {@link SpoonOutlineController} static helpers
 *       — direct method call, no reflection proxy.</li>
 * </ul>
 * Measured improvement on {@code FlexoConcept} (168 methods):
 * Gina version = 53 000 ms; this version = &lt; 50 ms.</p>
 */
@SuppressWarnings("serial")
public class SpoonOutlineViewSwing extends JScrollPane {

    private static final Logger logger =
            Logger.getLogger(SpoonOutlineViewSwing.class.getPackage().getName());

    private final JTree tree;
    private PamelaEditorApplication app;

    public SpoonOutlineViewSwing(PamelaEditorApplication app) {
        this.app = app;

        DefaultMutableTreeNode emptyRoot = new DefaultMutableTreeNode();
        tree = new JTree(new DefaultTreeModel(emptyRoot));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(18);
        tree.setCellRenderer(new SpoonTreeCellRenderer());
        tree.addTreeWillExpandListener(new LazyExpandListener());
        tree.addMouseListener(new NavigateClickListener());

        setViewportView(tree);
        setBorder(null);
    }

    // -------------------------------------------------------------------------
    // Public API — called by ContextPanel
    // -------------------------------------------------------------------------

    /**
     * Shows the outline for the given {@link SourceModelEntity}.
     * Always synchronous — the CU is guaranteed to be present.
     */
    public void showEntity(SourceModelEntity entity) {
        SourceCompilationUnit cu = entity != null ? entity.getCompilationUnit() : null;
        List<CtType<?>> types = (cu != null) ? cu.getRootTypes() : Collections.emptyList();
        long t0 = System.currentTimeMillis();
        rebuildTree(types, true);
        long t1 = System.currentTimeMillis();
        logger.info("[PERF] SpoonOutlineView.rebuildTree (plain JTree) = " + (t1 - t0) + " ms");
    }

    /**
     * Shows the outline for the given {@link SourceJavaFile}.
     *
     * <p>If the file has a known {@link SourceCompilationUnit} in the metamodel its
     * AST is used directly. Otherwise an on-demand Spoon mini-parse is launched in a
     * {@link SwingWorker} (decision D3, ui-design.md §19.3.1).</p>
     */
    public void showJavaFile(SourceJavaFile file, SourceMetaModel metaModel) {
        String qualifiedName = file.getQualifiedName();
        SourceCompilationUnit cu = (qualifiedName != null && !qualifiedName.isEmpty())
                ? metaModel.getCompilationUnit(qualifiedName) : null;

        if (cu != null) {
            rebuildTree(cu.getRootTypes(), true);
            return;
        }

        java.io.File javaFile = file.getFile();
        if (javaFile == null || !javaFile.exists()) {
            rebuildTree(Collections.emptyList(), false);
            return;
        }

        // Show an empty tree while parsing
        rebuildTree(Collections.emptyList(), false);

        new SwingWorker<List<CtType<?>>, Void>() {
            @Override
            protected List<CtType<?>> doInBackground() {
                try {
                    Launcher launcher = new Launcher();
                    launcher.getEnvironment().setNoClasspath(true);
                    launcher.getEnvironment().setCommentEnabled(false);
                    launcher.addInputResource(javaFile.getAbsolutePath());
                    launcher.buildModel();
                    return new ArrayList<>(launcher.getModel().getAllTypes());
                } catch (Exception e) {
                    logger.warning("Mini-parse failed for " + javaFile + ": " + e.getMessage());
                    return Collections.emptyList();
                }
            }

            @Override
            protected void done() {
                try {
                    rebuildTree(get(), true);
                } catch (Exception e) {
                    rebuildTree(Collections.emptyList(), false);
                }
            }
        }.execute();
    }

    /** Clears the outline (e.g. when no source-code element is selected). */
    public void clear() {
        rebuildTree(Collections.emptyList(), false);
    }

    /**
     * Selects, in the outline tree, all methods that belong to the given
     * {@link SourceModelProperty} (getter, setter, adder, remover, reindexer,
     * updater — whichever are present).
     *
     * <p>Uses {@link JTree#setSelectionPaths} directly — does not fire any
     * click/navigate callback, so there is no notification loop risk.</p>
     */
    public void selectMethodsForProperty(SourceModelProperty prop) {
        if (prop == null) return;

        Set<String> methodNames = new HashSet<>();
        if (prop.getGetterMethodName()    != null) methodNames.add(prop.getGetterMethodName());
        if (prop.getSetterMethodName()    != null) methodNames.add(prop.getSetterMethodName());
        if (prop.getAdderMethodName()     != null) methodNames.add(prop.getAdderMethodName());
        if (prop.getRemoverMethodName()   != null) methodNames.add(prop.getRemoverMethodName());
        if (prop.getReindexerMethodName() != null) methodNames.add(prop.getReindexerMethodName());
        if (prop.getUpdaterMethodName()   != null) methodNames.add(prop.getUpdaterMethodName());

        if (methodNames.isEmpty()) return;

        SwingUtilities.invokeLater(() -> {
            List<TreePath> matchingPaths = new ArrayList<>();
            for (int row = 0; row < tree.getRowCount(); row++) {
                TreePath path = tree.getPathForRow(row);
                if (path == null) continue;
                Object last = path.getLastPathComponent();
                if (last instanceof DefaultMutableTreeNode) {
                    Object obj = ((DefaultMutableTreeNode) last).getUserObject();
                    if (obj instanceof CtMethod) {
                        String name = ((CtMethod<?>) obj).getSimpleName();
                        if (methodNames.contains(name)) {
                            matchingPaths.add(path);
                        }
                    }
                }
            }
            if (matchingPaths.isEmpty()) {
                tree.clearSelection();
            } else {
                tree.setSelectionPaths(matchingPaths.toArray(new TreePath[0]));
                tree.scrollPathToVisible(matchingPaths.get(0));
            }
        });
    }

    // -------------------------------------------------------------------------
    // Tree construction
    // -------------------------------------------------------------------------

    /**
     * Replaces the tree model with nodes for the given types.
     *
     * <p>Each type gets a {@link LazyTypeNode} with a placeholder child so Swing
     * shows an expand handle. The actual method/field children are added by
     * {@link LazyExpandListener} the first time the node is expanded.</p>
     *
     * @param types      the top-level types to show
     * @param autoExpand if true, immediately expand every type node (lazy listener
     *                   will fire, populating children; this is fast because it is
     *                   plain {@link DefaultMutableTreeNode} creation with no
     *                   Connie/reflection overhead)
     */
    private void rebuildTree(List<CtType<?>> types, boolean autoExpand) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        for (CtType<?> type : types) {
            root.add(new LazyTypeNode(type));
        }
        tree.setModel(new DefaultTreeModel(root));

        if (autoExpand && root.getChildCount() > 0) {
            // Expand every type node (depth 1). The LazyExpandListener populates
            // children synchronously but cheaply (plain object creation, no Connie).
            for (int i = 0; i < root.getChildCount(); i++) {
                tree.expandRow(i);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Inner classes
    // -------------------------------------------------------------------------

    /**
     * A tree node for a Spoon {@link CtType} whose children are loaded on demand.
     * A placeholder child is present from the start so Swing shows an expand arrow.
     */
    private static final class LazyTypeNode extends DefaultMutableTreeNode {
        boolean childrenLoaded = false;

        LazyTypeNode(CtType<?> type) {
            super(type);
            // Placeholder so the expand arrow appears before children are loaded
            add(new DefaultMutableTreeNode("..."));
        }
    }

    /**
     * Populates the children of a {@link LazyTypeNode} the first time it is expanded.
     * Children are plain {@link DefaultMutableTreeNode}s wrapping Spoon members
     * ({@link CtTypeMember}).  Creating them is O(n) in member count and takes
     * only a few milliseconds even for 168 members.
     */
    private final class LazyExpandListener implements TreeWillExpandListener {
        @Override
        public void treeWillExpand(TreeExpansionEvent event) {
            Object last = event.getPath().getLastPathComponent();
            if (!(last instanceof LazyTypeNode)) return;
            LazyTypeNode node = (LazyTypeNode) last;
            if (node.childrenLoaded) return;

            node.childrenLoaded = true;
            node.removeAllChildren();
            CtType<?> type = (CtType<?>) node.getUserObject();
            for (CtTypeMember member : SpoonOutlineController.memberList(type)) {
                node.add(new DefaultMutableTreeNode(member));
            }
            ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(node);
        }

        @Override
        public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {}
    }

    /**
     * On mouse click, navigates the active source-code view to the clicked element's
     * line (click-to-navigate, ui-design.md §19.3, decision D6).
     */
    private final class NavigateClickListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (app == null) return;
            int row = tree.getRowForLocation(e.getX(), e.getY());
            if (row < 0) return;
            TreePath path = tree.getPathForRow(row);
            if (path == null) return;
            Object last = path.getLastPathComponent();
            if (!(last instanceof DefaultMutableTreeNode)) return;
            Object obj = ((DefaultMutableTreeNode) last).getUserObject();
            if (obj instanceof CtElement) {
                SourcePosition pos = ((CtElement) obj).getPosition();
                if (pos != null && pos.isValidPosition()) {
                    app.scrollSourceViewToLine(pos.getLine());
                }
            }
        }
    }

    /**
     * Cell renderer for Spoon elements: delegates label and icon resolution to
     * {@link SpoonOutlineController} static helpers (no reflection overhead).
     */
    private static final class SpoonTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(
                JTree tree, Object value, boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode) {
                Object obj = ((DefaultMutableTreeNode) value).getUserObject();
                if (obj != null && !(obj instanceof String)) {
                    setText(SpoonOutlineController.labelFor(obj));
                    Icon icon = SpoonOutlineController.iconFor(obj);
                    if (icon != null) setIcon(icon);
                }
            }
            return this;
        }
    }
}
