package org.openflexo.pamela.editor.ui.action;

import java.io.IOException;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Contextual action: renames a {@link SourceModelEntity} (its simple type
 * name), updating the {@code .java} file name and every reference to the type
 * across the model.
 *
 * <p>Delegates to {@link SourceModelEntity#rename}. The rename changes the
 * entity's qualified name; if it was a registered root type, the root list is
 * re-keyed here so the entity is still discovered after the rebuild.</p>
 */
public class RenameEntityAction implements ContextualAction {

    @Override
    public String getLabel() {
        return "Rename Entity…";
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceModelEntity;
    }

    @Override
    public void perform(Object target, PamelaEditorApplication app) {
        SourceModelEntity entity = (SourceModelEntity) target;
        SourceMetaModel model = entity.getMetaModel();
        PamelaProject project = app.getProjectForElement(entity);
        if (model == null || project == null) {
            return;
        }

        final String oldQualifiedName = entity.getQualifiedName();
        String newName = ModelEditingSupport.promptForName(app, "Rename Entity",
                "New entity name:", entity.getSimpleName());
        if (newName == null || newName.equals(entity.getSimpleName())) {
            return; // cancelled or unchanged
        }
        if (!ModelEditingSupport.isValidJavaIdentifier(newName)) {
            ModelEditingSupport.error(app, "Rename Entity",
                    "'" + newName + "' is not a valid Java type name.");
            return;
        }

        String pkgPrefix = oldQualifiedName.contains(".")
                ? oldQualifiedName.substring(0, oldQualifiedName.lastIndexOf('.') + 1)
                : "";
        final String newQualifiedName = pkgPrefix + newName;
        if (model.getEntity(newQualifiedName) != null) {
            ModelEditingSupport.error(app, "Rename Entity",
                    "An entity named '" + newQualifiedName + "' already exists.");
            return;
        }

        try {
            boolean wasRoot = model.getRootTypeNames().contains(oldQualifiedName);
            entity.rename(newName);
            // Keep the root-type list in sync with the new qualified name so the
            // renamed entity is rediscovered on rebuild.
            if (wasRoot) {
                model.removeRootTypeName(oldQualifiedName);
                model.addRootTypeName(newQualifiedName);
            }
        } catch (IOException | RuntimeException e) {
            ModelEditingSupport.error(app, "Rename Entity",
                    "Could not rename '" + oldQualifiedName + "':\n" + e.getMessage());
            return;
        }

        app.rebuildProject(project, () -> {
            SourceModelEntity renamed = model.getEntity(newQualifiedName);
            if (renamed != null) {
                app.selectInBrowser(renamed);
            }
        });
    }
}
