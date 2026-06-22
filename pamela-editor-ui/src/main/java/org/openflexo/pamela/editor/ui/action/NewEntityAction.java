package org.openflexo.pamela.editor.ui.action;

import java.io.IOException;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;

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

        String name = ModelEditingSupport.promptForName(app, "New Entity",
                "Entity name (simple Java type name):", "NewEntity");
        if (name == null) {
            return; // cancelled
        }
        if (!ModelEditingSupport.isValidJavaIdentifier(name)) {
            ModelEditingSupport.error(app, "New Entity",
                    "'" + name + "' is not a valid Java type name.");
            return;
        }

        String pkgPrefix = pkg.getQualifiedName() == null || pkg.getQualifiedName().isEmpty()
                ? "" : pkg.getQualifiedName() + ".";
        final String qualifiedName = pkgPrefix + name;
        if (model.getEntity(qualifiedName) != null) {
            ModelEditingSupport.error(app, "New Entity",
                    "An entity named '" + qualifiedName + "' already exists.");
            return;
        }

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
