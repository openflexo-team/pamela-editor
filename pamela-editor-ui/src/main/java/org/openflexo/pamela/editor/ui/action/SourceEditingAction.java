package org.openflexo.pamela.editor.ui.action;

import java.util.function.Supplier;

import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Base class for actions that apply a change to a project and (usually) need a Spoon
 * re-analysis afterwards. Factors the common body shared by every such action
 * (parameterized or confirmation-gated): resolve the owning project, apply the
 * mutation (with error reporting), rebuild off the EDT, and reselect.
 *
 * <p>Subclasses implement {@link #applyMutation}, which performs the change and returns a
 * {@link Supplier} of the element to reselect afterwards ({@code null} = no reselection). The
 * optional gate ({@link #confirmPerform}) is where a confirmation or a parameter dialog lives
 * — see {@link ParameteredAction}.</p>
 *
 * <p>The re-analysis is skipped when {@link #needsRebuild()} returns {@code false} — for the
 * rare action that edits <em>project metadata</em> (the meta-model display name, the
 * {@code .pamela} file location) rather than {@code .java} source, and therefore has nothing
 * for Spoon to re-parse (e.g. {@code RenameMetaModelAction}). In that case {@code applyMutation}
 * runs and any reselection is applied directly, without the (potentially multi-second) rebuild.</p>
 */
public abstract class SourceEditingAction extends ContextualAction {

    @Override
    protected final void doPerform(Object target, PamelaEditorApplication app) {
        PamelaProject project = app.getProjectForElement(target);
        if (project == null) {
            return;
        }
        if (needsRebuild() && app.isRebuilding(project)) {
            // A background rebuild is already iterating/rewriting this project's SourceMetaModel
            // collections (entities, packages…). Running applyMutation now would race it on the
            // EDT and can throw ConcurrentModificationException — refuse and let the user retry
            // once the in-flight rebuild has finished. (A metadata-only edit, needsRebuild()
            // == false, touches none of those collections, so it need not wait.)
            ModelEditingSupport.error(app, getLabel(),
                    "A rebuild is already in progress for this project.\n"
                            + "Please wait for it to finish, then try again.");
            return;
        }
        Supplier<Object> reselect;
        app.beginSourceMutation();
        try {
            reselect = applyMutation(target, app, project);
        } catch (Exception e) {
        	e.printStackTrace();
            ModelEditingSupport.error(app, getLabel(),
                    "Operation failed:\n" + e.getMessage());
            return;
        } finally {
            app.endSourceMutation();
        }
        if (needsRebuild()) {
            app.rebuildProject(project, () -> reselect(app, reselect));
        } else {
            reselect(app, reselect);
        }
    }

    private void reselect(PamelaEditorApplication app, Supplier<Object> reselect) {
        Object sel = (reselect != null) ? reselect.get() : null;
        if (sel != null) {
            presentResult(app, sel);
        }
    }

    /**
     * Presents the element produced by {@link #applyMutation} once the (optional) rebuild has
     * completed. The default anchors it in the main browser — which cascades to the central
     * view, the inspector and the detailed browser. An action may override this to present its
     * result differently depending on context (e.g. {@code NewEntityAction} keeps the active
     * tabular/diagram view instead of always switching to the entity's source code).
     */
    protected void presentResult(PamelaEditorApplication app, Object result) {
        app.selectInBrowser(result);
    }

    /**
     * Whether a Spoon re-analysis of the project must run after {@link #applyMutation}.
     * Default {@code true} (the action edited {@code .java} source). Override to {@code false}
     * for an action that only edits project metadata (see the class javadoc).
     */
    protected boolean needsRebuild() {
        return true;
    }

    /**
     * Performs the mutation. Returns a supplier of the element to reselect afterwards, or
     * {@code null}.
     */
    protected abstract Supplier<Object> applyMutation(Object target,
            PamelaEditorApplication app, PamelaProject project) throws Exception;
}
