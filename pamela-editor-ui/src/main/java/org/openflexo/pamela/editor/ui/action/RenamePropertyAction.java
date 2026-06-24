package org.openflexo.pamela.editor.ui.action;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Renames a {@link SourceModelProperty} (C19): changes its PAMELA key and the
 * accessor method names, and updates the inverse side if any. Delegates to
 * {@link SourceModelProperty#rename}.
 */
public class RenamePropertyAction extends ParameteredAction {

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/RenamePropertyForm.fib");

    private String name = "";
    private Set<String> forbidden = Collections.emptySet();

    @Override
    public String getLabel() {
        return "Rename Property…";
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceModelProperty;
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected String getDialogTitle() {
        return "Rename Property";
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        SourceModelProperty property = (SourceModelProperty) target;
        SourceModelEntity entity = property.getModelEntity();
        forbidden = new HashSet<>(entity.getDeclaredProperties().keySet());
        forbidden.remove(property.getPropertyIdentifier()); // unchanged stays valid
        name = property.getPropertyIdentifier();
        return true;
    }

    @Override
    public boolean isInputValid() {
        return ModelEditingSupport.isAvailableIdentifier(name, forbidden);
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelProperty property = (SourceModelProperty) target;
        SourceModelEntity entity = property.getModelEntity();
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();
        String newName = name.trim();
        if (!newName.equals(property.getPropertyIdentifier())) {
            property.rename(newName);
        }
        return () -> model.getEntity(entityQN);
    }

    // --- bound by RenamePropertyForm.fib ------------------------------------

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        fireInputValidChanged();
    }
}
