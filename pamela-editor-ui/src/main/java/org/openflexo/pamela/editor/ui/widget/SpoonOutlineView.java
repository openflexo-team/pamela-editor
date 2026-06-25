package org.openflexo.pamela.editor.ui.widget;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.tree.TreePath;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.FIBJPanel;
import org.openflexo.gina.view.widget.browser.impl.FIBBrowserModel.BrowserCell;
import org.openflexo.pamela.editor.model.SourceCompilationUnit;
import org.openflexo.pamela.editor.model.SourceJavaFile;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

import spoon.Launcher;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

/**
 * Context-panel view that shows a Java Outline for the currently displayed source
 * file (ui-design.md §19.3).
 *
 * <p>The outline mirrors Eclipse's Java Outline: types → members in source order,
 * no PAMELA-specific grouping.  It is driven by the Spoon AST directly — no
 * wrapper objects are created (decision D4).</p>
 *
 * <p>If the file has already been parsed by the main Spoon analysis (i.e. it is
 * a known {@link SourceCompilationUnit}), the AST is reused.  Otherwise an
 * on-demand mini-parse is launched in a {@link SwingWorker} (decision D3).</p>
 *
 * <p><strong>Note on performance:</strong> Gina's {@code FIBBrowserModel} rebuilds
 * the whole tree eagerly on every data change, evaluating label and icon bindings
 * via Connie for every node.  For large types (e.g. 168 methods) this can take
 * ~53 seconds.  A pure-Swing alternative with lazy child loading is available in
 * {@link SpoonOutlineViewSwing} and can be substituted once the Gina performance
 * issue is resolved.</p>
 */
@SuppressWarnings("serial")
public class SpoonOutlineView extends FIBJPanel<SpoonOutlineModel> {

    private static final Logger logger =
            Logger.getLogger(SpoonOutlineView.class.getPackage().getName());

    public static final Resource FIB_FILE =
            ResourceLocator.locateResource("Fib/SpoonOutlineView.fib");

    private final SpoonOutlineModel model;

    public SpoonOutlineView(PamelaEditorApplication app) {
        super(FIB_FILE, new SpoonOutlineModel(),
              ApplicationFIBLibraryImpl.instance(),
              PamelaEditorFIBController.EDITOR_LOCALIZATION);
        this.model = (SpoonOutlineModel) getEditedObject();
        SpoonOutlineController ctrl = (SpoonOutlineController) getController();
        if (ctrl != null) {
            ctrl.setApplication(app);
        }
    }

    @Override
    public Class<SpoonOutlineModel> getRepresentedType() {
        return SpoonOutlineModel.class;
    }

    @Override
    public void delete() {}

    // -------------------------------------------------------------------------
    // Public API — called by ContextPanel
    // -------------------------------------------------------------------------

    /**
     * Shows the outline for the given {@link SourceModelEntity}.
     * Always synchronous — the CU is guaranteed to be present.
     */
    public void showEntity(SourceModelEntity entity) {
        SourceCompilationUnit cu = entity.getCompilationUnit();
        if (cu != null) {
            model.setLoading(false);
            model.setRootTypes(cu.getRootTypes());
        } else {
            model.setLoading(false);
            model.setRootTypes(Collections.emptyList());
        }
        scheduleExpandFirstLevel();
    }

    /**
     * Shows the outline for the given {@link SourceJavaFile}.
     *
     * <p>If the file has a known {@link SourceCompilationUnit} in the metamodel,
     * its AST is used directly.  Otherwise an on-demand Spoon mini-parse is
     * launched in a background thread (decision D3).</p>
     */
    public void showJavaFile(SourceJavaFile file, SourceMetaModel metaModel) {
        // Try to find the CU from the metamodel
        String qualifiedName = file.getQualifiedName();
        SourceCompilationUnit cu = (qualifiedName != null && !qualifiedName.isEmpty())
                ? metaModel.getCompilationUnit(qualifiedName) : null;

        if (cu != null) {
            model.setLoading(false);
            model.setRootTypes(cu.getRootTypes());
            scheduleExpandFirstLevel();
            return;
        }

        // On-demand mini-parse
        java.io.File javaFile = file.getFile();
        if (javaFile == null || !javaFile.exists()) {
            model.setLoading(false);
            model.setRootTypes(Collections.emptyList());
            return;
        }

        model.setLoading(true);
        model.setRootTypes(Collections.emptyList());
        scheduleExpandFirstLevel();

        new SwingWorker<List<CtType<?>>, Void>() {
            @Override
            protected List<CtType<?>> doInBackground() {
                try {
                    Launcher launcher = new Launcher();
                    launcher.getEnvironment().setNoClasspath(true);
                    launcher.getEnvironment().setCommentEnabled(false);
                    launcher.addInputResource(javaFile.getAbsolutePath());
                    launcher.buildModel();
                    return new java.util.ArrayList<>(launcher.getModel().getAllTypes());
                } catch (Exception e) {
                    logger.warning("Mini-parse failed for " + javaFile + ": " + e.getMessage());
                    return Collections.emptyList();
                }
            }

            @Override
            protected void done() {
                try {
                    model.setLoading(false);
                    model.setRootTypes(get());
                } catch (Exception e) {
                    model.setLoading(false);
                    model.setRootTypes(Collections.emptyList());
                }
                scheduleExpandFirstLevel();
            }
        }.execute();
    }

    /** Clears the outline (e.g. when no source-code element is selected). */
    public void clear() {
        model.setLoading(false);
        model.setRootTypes(Collections.emptyList());
    }

    /**
     * Selects, in the outline tree, all methods that belong to the given
     * {@link SourceModelProperty} (getter, setter, adder, remover, reindexer,
     * updater — whichever are present).
     *
     * <p>Uses {@link JTree#setSelectionPaths} directly — does not fire the FIB
     * {@code clickAction} (only triggered by mouse clicks) — so there is no
     * notification loop risk.</p>
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
        selectMatchingMethods(m -> methodNames.contains(m.getSimpleName()));
    }

    /**
     * Selects, in the outline tree, the single method declaration at the given 1-based source
     * line. Unambiguous for overloaded initializers / operations (same name, distinct lines).
     * Used for an initializer or a custom method (operation) selected in the DetailedBrowser.
     *
     * @param line 1-based declaration line; values &lt; 1 are a no-op
     */
    public void selectMethodAtLine(int line) {
        if (line < 1) return;
        selectMatchingMethods(m -> m.getPosition() != null
                && m.getPosition().isValidPosition()
                && m.getPosition().getLine() == line);
    }

    /** Selects every outline node that is a {@link CtMethod} matching the predicate. */
    private void selectMatchingMethods(java.util.function.Predicate<CtMethod<?>> match) {
        SwingUtilities.invokeLater(() -> {
            JTree tree = findTree(SpoonOutlineView.this);
            if (tree == null) return;

            List<TreePath> matchingPaths = new ArrayList<>();
            for (int row = 0; row < tree.getRowCount(); row++) {
                TreePath path = tree.getPathForRow(row);
                if (path == null) continue;
                Object last = path.getLastPathComponent();
                if (last instanceof BrowserCell) {
                    Object obj = ((BrowserCell) last).getRepresentedObject();
                    if (obj instanceof CtMethod && match.test((CtMethod<?>) obj)) {
                        matchingPaths.add(path);
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
    // Auto-expand helpers
    // -------------------------------------------------------------------------

    /**
     * Schedules expansion of all top-level rows via {@link SwingUtilities#invokeLater}
     * so it runs after Gina has rebuilt the tree model from the updated rootTypes.
     */
    private void scheduleExpandFirstLevel() {
        SwingUtilities.invokeLater(() -> {
            JTree tree = findTree(SpoonOutlineView.this);
            if (tree == null) return;
            int row = 0;
            while (row < tree.getRowCount()) {
                if (tree.getPathForRow(row).getPathCount() <= 2) {
                    tree.expandRow(row);
                }
                row++;
            }
        });
    }

    /** Depth-first search for the first {@link JTree} inside this panel. */
    private static JTree findTree(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JTree) return (JTree) c;
            if (c instanceof Container) {
                JTree found = findTree((Container) c);
                if (found != null) return found;
            }
        }
        return null;
    }
}
