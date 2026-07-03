/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor, a component of the software infrastructure
 * developed at Openflexo.
 *
 */

package org.openflexo.pamela.editor.ui.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.junit.Test;

/**
 * Regression tests for {@link MetaModelBrowser#expandToFillView(JTree)} — the level-by-level
 * auto-expand algorithm described in the session: while fewer than 20 rows are visible,
 * expand the next available level of the whole forest at once; if that expansion would push
 * the total past 30 rows, undo it and stop.
 *
 * <p>Exercised against a hand-built {@code javax.swing.JTree} (a {@link DefaultTreeModel} over
 * plain {@link DefaultMutableTreeNode}s), independently of the Gina/Spoon machinery that backs
 * the real {@link MetaModelBrowser} — the algorithm only depends on the plain {@link JTree} API
 * (row count, expand/collapse, leaf test).</p>
 */
public class TestMetaModelBrowserAutoExpand {

    /** Builds a uniform tree: {@code branching} children per node, {@code depth} levels deep. */
    private static JTree uniformTree(int branching, int depth) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        buildUniform(root, branching, depth);
        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        return tree;
    }

    private static void buildUniform(DefaultMutableTreeNode parent, int branching, int remainingDepth) {
        if (remainingDepth <= 0) {
            return;
        }
        for (int i = 0; i < branching; i++) {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode("n" + remainingDepth + "-" + i);
            parent.add(child);
            buildUniform(child, branching, remainingDepth - 1);
        }
    }

    /**
     * Builds a "comb": {@code topBranches} top-level nodes, each the head of its own
     * single-child chain {@code chainDepth} nodes deep (branching factor 1 below the top
     * level) — so each level opened adds exactly {@code topBranches} new rows.
     */
    private static JTree chainForest(int topBranches, int chainDepth) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        for (int i = 0; i < topBranches; i++) {
            DefaultMutableTreeNode top = new DefaultMutableTreeNode("top" + i);
            root.add(top);
            DefaultMutableTreeNode current = top;
            for (int d = 0; d < chainDepth; d++) {
                DefaultMutableTreeNode next = new DefaultMutableTreeNode("top" + i + "-" + d);
                current.add(next);
                current = next;
            }
        }
        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        return tree;
    }

    @Test
    public void testInvisibleRootShowsTopLevelByDefault() {
        // Sanity precondition the whole algorithm relies on: with rootVisible=false, the
        // top-level nodes are visible without any explicit expansion.
        JTree tree = uniformTree(5, 1);
        assertEquals(5, tree.getRowCount());
    }

    @Test
    public void testDeepNarrowChainExpandsExactlyToTarget() {
        // 5 top-level branches, each a chain 1-wide and deep enough to keep growing.
        // Rows grow 5, 10, 15, 20 as each level is opened; the loop stops as soon as it
        // reaches 20 (the target), never going further even though more levels remain.
        JTree tree = chainForest(5, 10);
        MetaModelBrowser.expandToFillView(tree);

        assertEquals(20, tree.getRowCount());
        // The 5th level down should still be collapsed — the algorithm stopped early.
        TreePath deepPath = pathToRow(tree, tree.getRowCount() - 1);
        assertFalse("should not have expanded past the 20-row target",
                tree.isExpanded(deepPath));
    }

    @Test
    public void testOverBudgetExpansionIsRevertedAndStops() {
        // 2 top-level nodes, each with 50 children: expanding either one alone already
        // overshoots the 30-row cap, so the expansion must be undone and the tree left
        // exactly as it was found (2 visible rows, both still collapsed).
        JTree tree = uniformTree(2, 1);
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) root.getFirstChild();
        for (int i = 0; i < 50; i++) {
            firstChild.add(new DefaultMutableTreeNode("leaf" + i));
        }
        ((DefaultTreeModel) tree.getModel()).reload();

        int rowsBefore = tree.getRowCount();
        assertEquals(2, rowsBefore);

        MetaModelBrowser.expandToFillView(tree);

        assertEquals("row count must be unchanged after a reverted over-budget expansion",
                rowsBefore, tree.getRowCount());
        assertFalse(tree.isExpanded(new TreePath(new Object[] { root, firstChild })));
    }

    @Test
    public void testExpansionLandingExactlyOnTheCapIsKept() {
        // 3 top-level -> 3 children each (12 rows) -> 2 children each (12 + 18 = 30 rows):
        // exactly the hard cap, which must be committed (the check is a strict ">").
        JTree tree = uniformTree(3, 1);
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode level1 = (DefaultMutableTreeNode) root.getChildAt(i);
            for (int j = 0; j < 3; j++) {
                DefaultMutableTreeNode level2 = new DefaultMutableTreeNode("l2-" + i + "-" + j);
                level1.add(level2);
                for (int k = 0; k < 2; k++) {
                    level2.add(new DefaultMutableTreeNode("l3-" + i + "-" + j + "-" + k));
                }
            }
        }
        ((DefaultTreeModel) tree.getModel()).reload();

        MetaModelBrowser.expandToFillView(tree);

        assertEquals(30, tree.getRowCount());
    }

    @Test
    public void testSmallTreeFullyExpandsWithoutReachingTarget() {
        // A tree that can never reach 20 rows even fully expanded: the algorithm should
        // expand everything there is and stop (frontier becomes empty), not loop forever.
        JTree tree = uniformTree(2, 3); // 2 + 4 + 8 = 14 rows fully expanded
        MetaModelBrowser.expandToFillView(tree);

        assertEquals(14, tree.getRowCount());
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = pathToRow(tree, row);
            if (!tree.getModel().isLeaf(path.getLastPathComponent())) {
                assertTrue("every non-leaf row should end up expanded", tree.isExpanded(path));
            }
        }
    }

    @Test
    public void testAlreadyAtOrAboveTargetDoesNothing() {
        // 25 top-level leaves: already >= 20 rows before any expansion is attempted, and
        // there is nothing to expand anyway (all leaves).
        JTree tree = uniformTree(25, 1);
        int rowsBefore = tree.getRowCount();
        MetaModelBrowser.expandToFillView(tree);
        assertEquals(rowsBefore, tree.getRowCount());
    }

    @Test
    public void testNeverExpandRowStaysCollapsedAcrossEveryPass() {
        // Regression for MetaModelBrowser.focusNewlyOpenedProject (ui-design.md §4.1): a row
        // passed in neverExpand must never be re-expanded, even though a freshly-collapsed,
        // visible, non-leaf row is otherwise an entirely ordinary candidate for the very next
        // pass — this is exactly what silently undid the "collapse the other open project"
        // step when the plain (unscoped) expandToFillView(tree) was called right after.
        JTree tree = chainForest(2, 10); // top0 = "other project" (kept collapsed), top1 = "new project"
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        DefaultMutableTreeNode top0 = (DefaultMutableTreeNode) root.getChildAt(0);
        DefaultMutableTreeNode top1 = (DefaultMutableTreeNode) root.getChildAt(1);
        TreePath top0Path = new TreePath(new Object[] { root, top0 });
        TreePath top1Path = new TreePath(new Object[] { root, top1 });

        MetaModelBrowser.expandToFillView(tree, java.util.Collections.singleton(top0Path));

        assertFalse("the excluded row must stay collapsed after every pass", tree.isExpanded(top0Path));
        assertTrue("the other row is free to expand and use the freed budget", tree.isExpanded(top1Path));
        // top1's whole 10-deep single-child chain has nowhere else to go (never overshoots the
        // 30 cap since it grows by exactly 1 row per level), so it fully unfolds: top0 (1) +
        // top1 + its 10 descendants (11) = 12 total visible rows.
        assertEquals(12, tree.getRowCount());
    }

    private static TreePath pathToRow(JTree tree, int row) {
        TreePath path = tree.getPathForRow(row);
        assertTrue("row " + row + " should resolve to a path", path != null);
        return path;
    }
}
