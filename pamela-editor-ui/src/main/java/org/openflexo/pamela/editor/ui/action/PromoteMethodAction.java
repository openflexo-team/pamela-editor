package org.openflexo.pamela.editor.ui.action;
import org.openflexo.icon.IconMarker;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import java.util.ArrayList;
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

import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

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


    @Override
    public ActionGroup getGroup() {
        return ActionGroup.PROMOTE;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.PROPERTY_ICON;
    }

    @Override
    protected IconMarker[] getMarkers() {
        return new IconMarker[] { PamelaEditorIconLibrary.REINJECT };
    }

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/PromoteMethodForm.fib");

    private SourceModelEntity entity;
    private List<CtMethod<?>> getterChoices = Collections.emptyList();
    private CtMethod<?> selectedGetter;
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
        entity = (SourceModelEntity) target;
        getterChoices = promotableGetterMethods(entity);
        if (getterChoices.isEmpty()) {
            return false;
        }
        existingIdentifiers = new HashSet<>(entity.getDeclaredProperties().keySet());
        selectedGetter = getterChoices.get(0);
        propertyIdentifier = derivePropertyName(selectedGetter.getSimpleName());
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
        entity.promoteMethodToProperty(selectedGetter.getSimpleName(), propertyIdentifier.trim(), includeSetter);
        return () -> model.getEntity(entityQN);
    }

    /** Resolves the entity's promotable getter <em>names</em> (core) to their Spoon {@link CtMethod}s. */
    private static List<CtMethod<?>> promotableGetterMethods(SourceModelEntity entity) {
        List<CtMethod<?>> result = new ArrayList<>();
        Set<String> names = new HashSet<>(entity.getPromotableGetterNames());
        if (names.isEmpty() || entity.getCompilationUnit() == null) {
            return result;
        }
        for (CtType<?> type : entity.getCompilationUnit().getRootTypes()) {
            if (!entity.getSimpleName().equals(type.getSimpleName())) {
                continue;
            }
            for (CtMethod<?> m : type.getMethods()) {
                if (m.getParameters().isEmpty() && names.contains(m.getSimpleName())) {
                    result.add(m);
                }
            }
        }
        return result;
    }

    // --- bound by PromoteMethodForm.fib -------------------------------------

    /** Owning entity — the {@code JavaMethodSelector} context. */
    public SourceModelEntity getEntity() {
        return entity;
    }

    public List<CtMethod<?>> getGetterChoices() {
        return getterChoices;
    }

    public CtMethod<?> getSelectedGetter() {
        return selectedGetter;
    }

    public void setSelectedGetter(CtMethod<?> selectedGetter) {
        this.selectedGetter = selectedGetter;
        // Suggest a property name derived from the chosen getter.
        this.propertyIdentifier = selectedGetter == null ? "" : derivePropertyName(selectedGetter.getSimpleName());
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
