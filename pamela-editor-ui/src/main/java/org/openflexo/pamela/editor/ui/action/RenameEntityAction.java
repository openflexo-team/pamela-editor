package org.openflexo.pamela.editor.ui.action;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Renames a {@link SourceModelEntity} (its simple type name), updating the
 * {@code .java} file name and every reference to the type.
 *
 * <p>Flat action with its own {@code name} parameter and form fragment
 * ({@code RenameEntityForm.fib}). Delegates to {@link SourceModelEntity#rename};
 * if the entity was a root type the root list is re-keyed.</p>
 */
public class RenameEntityAction extends ParameteredAction {

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/RenameEntityForm.fib");

    private String name = "";
    private Set<String> forbidden = Collections.emptySet();

    @Override
    public String getLabel() {
        return "Rename Entity…";
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceModelEntity;
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected String getDialogTitle() {
        return "Rename Entity";
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        SourceModelEntity entity = (SourceModelEntity) target;
        // Forbid sibling entity names except the current one (so "unchanged" stays valid).
        forbidden = new HashSet<>();
        SourcePackage pkg = entity.getSourcePackage();
        if (pkg != null) {
            for (SourceModelEntity e : pkg.getEntities()) {
                if (e != entity) {
                    forbidden.add(e.getSimpleName());
                }
            }
        }
        name = entity.getSimpleName();
        return true;
    }

    @Override
    public boolean isInputValid() {
        return ModelEditingSupport.isAvailableIdentifier(name, forbidden);
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelEntity entity = (SourceModelEntity) target;
        SourceMetaModel model = entity.getMetaModel();
        String newName = name.trim();
        if (newName.equals(entity.getSimpleName())) {
            return null; // unchanged
        }
        final String oldQualifiedName = entity.getQualifiedName();
        String pkgPrefix = oldQualifiedName.contains(".")
                ? oldQualifiedName.substring(0, oldQualifiedName.lastIndexOf('.') + 1)
                : "";
        final String newQualifiedName = pkgPrefix + newName;

        boolean wasRoot = model.getRootTypeNames().contains(oldQualifiedName);
        entity.rename(newName);
        if (wasRoot) {
            model.removeRootTypeName(oldQualifiedName);
            model.addRootTypeName(newQualifiedName);
        }
        return () -> model.getEntity(newQualifiedName);
    }

    // --- bound by RenameEntityForm.fib --------------------------------------

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        fireInputValidChanged();
    }
}
