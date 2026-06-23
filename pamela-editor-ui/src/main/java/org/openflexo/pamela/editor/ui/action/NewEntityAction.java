package org.openflexo.pamela.editor.ui.action;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.pamela.editor.ui.dialog.ModelEditingDialogs;
import org.openflexo.pamela.editor.ui.dialog.SingleNameParameters;

/**
 * Contextual action: creates a brand-new PAMELA entity (a new annotated
 * {@code interface}) in a given {@link SourcePackage} — the "create" path of
 * {@code model-editing-design.md §1.1}.
 *
 * <p>Delegates the source generation to {@link SourceMetaModel#createEntity}.
 * Because a freshly created entity has no incoming references yet, it must be
 * registered as a root type so it survives the next analysis rebuild
 * ({@code createEntity} leaves root registration to the caller — see
 * {@code TestMutations.testCreateEntity}).</p>
 */
public class NewEntityAction implements ContextualAction {

    @Override
    public String getLabel() {
        return "New Entity…";
    }

    @Override
    public boolean isApplicable(Object target) {
        // Lot 1 scope: creation is anchored on a package node. Creation from the
        // metamodel root (with a package picker) is deferred — see §11 Q6.
        return target instanceof SourcePackage
                && ((SourcePackage) target).getMetaModel() != null;
    }

    @Override
    public void perform(Object target, PamelaEditorApplication app) {
        SourcePackage pkg = (SourcePackage) target;
        SourceMetaModel model = pkg.getMetaModel();
        PamelaProject project = app.getProjectForElement(model);
        if (project == null) {
            return;
        }

        // Forbid names that already exist as entities in this package.
        Set<String> forbidden = new HashSet<>();
        for (SourceModelEntity e : pkg.getEntities()) {
            forbidden.add(e.getSimpleName());
        }
        SingleNameParameters params = new SingleNameParameters(
                "Entity name (simple Java type name):", "NewEntity", forbidden);
        if (!ModelEditingDialogs.showForm(app.getFrame(),
                ModelEditingDialogs.SINGLE_NAME_FIB, "New Entity", params)) {
            return; // cancelled
        }
        String name = params.getTrimmedName();

        String pkgPrefix = pkg.getQualifiedName() == null || pkg.getQualifiedName().isEmpty()
                ? "" : pkg.getQualifiedName() + ".";
        final String qualifiedName = pkgPrefix + name;

        try {
            model.createEntity(name, pkg);
            // Register as a root so the new (reference-less) entity is rediscovered
            // by the rebuild below.
            model.addRootTypeName(qualifiedName);
        } catch (IOException | RuntimeException e) {
            ModelEditingSupport.error(app, "New Entity",
                    "Could not create entity '" + qualifiedName + "':\n" + e.getMessage());
            return;
        }

        app.rebuildProject(project, () -> {
            SourceModelEntity created = model.getEntity(qualifiedName);
            if (created != null) {
                app.selectInBrowser(created);
            }
        });
    }
}
