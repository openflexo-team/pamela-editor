package org.openflexo.pamela.editor.ui.action;

import java.util.function.Supplier;

import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Base class for actions that mutate the source of a project and need a
 * re-analysis afterwards. Factors the common body shared by every such action
 * (parameterized or confirmation-gated): resolve the owning project, apply the
 * mutation (with error reporting), rebuild off the EDT, and reselect.
 *
 * <p>Subclasses implement {@link #applyMutation}, which performs the source
 * change and returns a {@link Supplier} of the element to reselect once the
 * (background) rebuild completes ({@code null} = no reselection). The optional
 * gate ({@link #confirmPerform}) is where a confirmation or a parameter dialog
 * lives — see {@link ParameteredAction}.</p>
 */
public abstract class SourceEditingAction extends ContextualAction {

    @Override
    protected final void doPerform(Object target, PamelaEditorApplication app) {
        PamelaProject project = app.getProjectForElement(target);
        if (project == null) {
            return;
        }
        Supplier<Object> reselect;
        try {
            reselect = applyMutation(target, app, project);
        } catch (Exception e) {
        	e.printStackTrace();
            ModelEditingSupport.error(app, getLabel(),
                    "Operation failed:\n" + e.getMessage());
            return;
        }
        app.rebuildProject(project, () -> {
            if (reselect != null) {
                Object sel = reselect.get();
                if (sel != null) {
                    app.selectInBrowser(sel);
                }
            }
        });
    }

    /**
     * Performs the source mutation. Returns a supplier of the element to
     * reselect after the rebuild, or {@code null}.
     */
    protected abstract Supplier<Object> applyMutation(Object target,
            PamelaEditorApplication app, PamelaProject project) throws Exception;
}
