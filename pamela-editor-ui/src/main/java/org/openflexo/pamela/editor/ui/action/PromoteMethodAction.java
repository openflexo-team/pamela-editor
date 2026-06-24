package org.openflexo.pamela.editor.ui.action;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Promotes an existing plain getter of a {@link SourceModelEntity} into a PAMELA
 * property — the "promote" path of {@code model-editing-design.md §1.1} (C4),
 * the heart of co-construction: a developer writes ordinary Java in the IDE, then
 * lifts a getter into the model from the editor.
 *
 * <p>Carries its own parameters (the chosen getter, the property identifier,
 * whether to also annotate the matching setter) and references its own form
 * fragment ({@code PromoteMethodForm.fib}). Delegates to
 * {@link SourceModelEntity#promoteMethodToProperty}.</p>
 */
public class PromoteMethodAction extends ParameteredAction {

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/PromoteMethodForm.fib");

    private List<String> getterChoices = Collections.emptyList();
    private String selectedGetter;
    private String propertyIdentifier = "";
    private boolean includeSetter = true;
    private Set<String> existingIdentifiers = new HashSet<>();

    @Override
    public String getLabel() {
        return "Promote Method to Property…";
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceModelEntity
                && !((SourceModelEntity) target).getPromotableGetterNames().isEmpty();
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected String getDialogTitle() {
        return "Promote Method to Property";
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        SourceModelEntity entity = (SourceModelEntity) target;
        getterChoices = entity.getPromotableGetterNames();
        if (getterChoices.isEmpty()) {
            return false;
        }
        existingIdentifiers = new HashSet<>(entity.getDeclaredProperties().keySet());
        selectedGetter = getterChoices.get(0);
        propertyIdentifier = derivePropertyName(selectedGetter);
        includeSetter = true;
        return true;
    }

    @Override
    public boolean isInputValid() {
        return selectedGetter != null
                && ModelEditingSupport.isAvailableIdentifier(propertyIdentifier, existingIdentifiers);
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelEntity entity = (SourceModelEntity) target;
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();
        entity.promoteMethodToProperty(selectedGetter, propertyIdentifier.trim(), includeSetter);
        return () -> model.getEntity(entityQN);
    }

    // --- bound by PromoteMethodForm.fib -------------------------------------

    public List<String> getGetterChoices() {
        return getterChoices;
    }

    public String getSelectedGetter() {
        return selectedGetter;
    }

    public void setSelectedGetter(String selectedGetter) {
        this.selectedGetter = selectedGetter;
        // Suggest a property name derived from the chosen getter.
        this.propertyIdentifier = selectedGetter == null ? "" : derivePropertyName(selectedGetter);
        getPropertyChangeSupport().firePropertyChange("propertyIdentifier", null, propertyIdentifier);
        fireInputValidChanged();
    }

    public String getPropertyIdentifier() {
        return propertyIdentifier;
    }

    public void setPropertyIdentifier(String propertyIdentifier) {
        this.propertyIdentifier = propertyIdentifier;
        fireInputValidChanged();
    }

    public boolean getIncludeSetter() {
        return includeSetter;
    }

    public void setIncludeSetter(boolean includeSetter) {
        this.includeSetter = includeSetter;
    }

    // -------------------------------------------------------------------------

    /** {@code getName} → {@code name}, {@code isActive} → {@code active}. */
    private static String derivePropertyName(String getterName) {
        String base = getterName.startsWith("is")
                ? getterName.substring(2) : getterName.substring(3);
        if (base.isEmpty()) {
            return "";
        }
        return Character.toLowerCase(base.charAt(0)) + base.substring(1);
    }
}
