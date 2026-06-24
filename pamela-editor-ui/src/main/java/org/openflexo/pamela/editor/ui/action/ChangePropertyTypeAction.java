package org.openflexo.pamela.editor.ui.action;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Changes the value type of a {@link SourceModelProperty} (C19 retype). For a
 * LIST property the chosen type is the element type. Delegates to
 * {@link SourceModelProperty#changeType}.
 */
public class ChangePropertyTypeAction extends ParameteredAction {

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/ChangePropertyTypeForm.fib");

    private List<String> typeChoices = Collections.emptyList();
    private String type;

    @Override
    public String getLabel() {
        return "Change Type…";
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
        return "Change Property Type";
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        SourceModelProperty property = (SourceModelProperty) target;
        SourceModelEntity entity = property.getModelEntity();
        typeChoices = ModelEditingSupport.typeChoices(entity.getMetaModel());
        // Pre-select the current type (its qualified name = element type for LIST).
        String current = property.getType() != null ? property.getType().getQualifiedName() : null;
        if (current != null && !typeChoices.contains(current)) {
            typeChoices.add(0, current);
        }
        type = current != null ? current : (typeChoices.isEmpty() ? null : typeChoices.get(0));
        return true;
    }

    @Override
    public boolean isInputValid() {
        return type != null && !type.isEmpty();
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelProperty property = (SourceModelProperty) target;
        SourceModelEntity entity = property.getModelEntity();
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();
        property.changeType(type);
        return () -> model.getEntity(entityQN);
    }

    // --- bound by ChangePropertyTypeForm.fib --------------------------------

    public List<String> getTypeChoices() {
        return typeChoices;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
        fireInputValidChanged();
    }
}
