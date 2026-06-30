package org.openflexo.pamela.editor.ui.action;

import javax.swing.ImageIcon;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.openflexo.icon.IconMarker;
import org.openflexo.pamela.editor.model.AccessorSpec;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

/**
 * Promotes an existing plain getter of a {@link SourceModelEntity} into a PAMELA property — the
 * "promote" path of {@code model-editing-design.md §1.1} (C4). The developer writes ordinary Java
 * in the IDE, then lifts a getter into the model from the editor.
 *
 * <p>Generalised on the shared {@link AccessorOption} panel (same as the New-property / Promote-getter
 * dialogs): the getter is chosen from the entity's promotable getters, and each relevant sibling
 * accessor (setter/updater for SINGLE, adder/remover/reindexer for LIST, by signature) is offered as
 * an "also promote" checkbox + an existing-method selector + an explicit summary. Attach-only (a
 * promote refines an existing entity); delegates to {@link SourceModelEntity#promoteGetterToProperty}.</p>
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
    private Set<String> existingIdentifiers = new HashSet<>();

    private boolean list;
    private String propertyTypeQN;

    private final AccessorOption setterOption     = new AccessorOption(AccessorSpec.Role.SETTER, "Setter");
    private final AccessorOption updaterOption   = new AccessorOption(AccessorSpec.Role.UPDATER, "Updater");
    private final AccessorOption adderOption       = new AccessorOption(AccessorSpec.Role.ADDER, "Adder");
    private final AccessorOption removerOption    = new AccessorOption(AccessorSpec.Role.REMOVER, "Remover");
    private final AccessorOption reindexerOption = new AccessorOption(AccessorSpec.Role.REINDEXER, "Reindexer");

    private final AccessorOption[] siblings = {
            setterOption, updaterOption, adderOption, removerOption, reindexerOption };

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
        reconfigureFromGetter();
        return true;
    }

    @Override
    public boolean isInputValid() {
        if (selectedGetter == null
                || !ModelEditingSupport.isAvailableIdentifier(propertyIdentifier, existingIdentifiers)) {
            return false;
        }
        for (AccessorOption opt : siblings) {
            if (opt.isShow() && !opt.isComplete()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();

        List<AccessorSpec> siblingSpecs = new ArrayList<>();
        boolean anyGenerate = false;
        for (AccessorOption opt : siblings) {
            if (!opt.isShow() || !opt.getInclude()) {
                continue;
            }
            AccessorSpec spec = opt.toSpec();
            if (spec == null) {
                continue;
            }
            siblingSpecs.add(spec);
            anyGenerate |= spec.isGenerate();
        }

        if (anyGenerate) {
            List<AccessorSpec> all = new ArrayList<>();
            all.add(AccessorSpec.attach(AccessorSpec.Role.GETTER, selectedGetter.getSimpleName()));
            all.addAll(siblingSpecs);
            entity.createProperty(propertyIdentifier.trim(), propertyTypeQN, list, all);
        } else {
            Map<Class<? extends Annotation>, String> extras = new LinkedHashMap<>();
            for (AccessorSpec spec : siblingSpecs) {
                extras.put(AccessorOption.annotationFor(spec.getRole()), spec.getMethodName());
            }
            entity.promoteGetterToProperty(selectedGetter.getSimpleName(), propertyIdentifier.trim(),
                    list, extras);
        }
        return () -> model.getEntity(entityQN);
    }

    /** Recomputes cardinality / type from the chosen getter and configures the sibling rows. */
    private void reconfigureFromGetter() {
        if (selectedGetter == null) {
            for (AccessorOption opt : siblings) {
                hide(opt);
            }
            return;
        }
        CtTypeReference<?> ret = selectedGetter.getType();
        list = ret != null && "java.util.List".equals(ret.getQualifiedName());
        if (list) {
            propertyTypeQN = ret.getActualTypeArguments().isEmpty() ? "java.lang.Object"
                    : ret.getActualTypeArguments().get(0).getQualifiedName();
        } else {
            propertyTypeQN = ret == null ? null : ret.getQualifiedName();
        }

        List<CtMethod<?>> methods = AccessorCandidates.entityMethods(entity);
        String base = capitalisedBase(selectedGetter.getSimpleName());
        if (list) {
            hide(setterOption);
            hide(updaterOption);
            AccessorOption.configureSibling(adderOption, true,
                    AccessorCandidates.oneParam(methods, propertyTypeQN), "addTo" + base, "add" + base);
            AccessorOption.configureSibling(removerOption, true,
                    AccessorCandidates.oneParam(methods, propertyTypeQN), "removeFrom" + base, "remove" + base);
            AccessorOption.configureSibling(reindexerOption, true,
                    AccessorCandidates.reindexer(methods, propertyTypeQN), "reindex" + base, "setIndexFor" + base);
        } else {
            AccessorOption.configureSibling(setterOption, true,
                    AccessorCandidates.oneParam(methods, propertyTypeQN), "set" + base);
            AccessorOption.configureSibling(updaterOption, true,
                    AccessorCandidates.oneParam(methods, propertyTypeQN), "update" + base);
            hide(adderOption);
            hide(removerOption);
            hide(reindexerOption);
        }
        for (AccessorOption opt : siblings) {
            opt.setChangeCallback(this::fireInputValidChanged);
        }
    }

    private static void hide(AccessorOption opt) {
        opt.configure(false, false, false, Collections.emptyList(), "", null, false);
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
        this.propertyIdentifier = selectedGetter == null ? "" : derivePropertyName(selectedGetter.getSimpleName());
        reconfigureFromGetter();
        getPropertyChangeSupport().firePropertyChange("propertyIdentifier", null, propertyIdentifier);
        getPropertyChangeSupport().firePropertyChange("cardinalityLabel", null, getCardinalityLabel());
        getPropertyChangeSupport().firePropertyChange("promotionSummary", null, getPromotionSummary());
        fireInputValidChanged();
    }

    public String getPropertyIdentifier() {
        return propertyIdentifier;
    }

    public void setPropertyIdentifier(String propertyIdentifier) {
        this.propertyIdentifier = propertyIdentifier;
        getPropertyChangeSupport().firePropertyChange("promotionSummary", null, getPromotionSummary());
        fireInputValidChanged();
    }

    /** Read-only label shown in the form (SINGLE / LIST), inferred from the chosen getter. */
    public String getCardinalityLabel() {
        return list ? "LIST (collection)" : "SINGLE";
    }

    /** Title shown at the top of the dialog; updates live as the getter / property name change. */
    public String getPromotionSummary() {
        String getterName = selectedGetter == null ? "?" : selectedGetter.getSimpleName();
        return "Promote " + getterName + "() → property" + (propertyIdentifier.isEmpty() ? "" : " ‘" + propertyIdentifier + "’");
    }

    public AccessorOption getSetterOption()    { return setterOption; }
    public AccessorOption getUpdaterOption()   { return updaterOption; }
    public AccessorOption getAdderOption()     { return adderOption; }
    public AccessorOption getRemoverOption()   { return removerOption; }
    public AccessorOption getReindexerOption() { return reindexerOption; }

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

    /** {@code getName} → {@code Name} (capitalised base, for building sibling accessor names). */
    private static String capitalisedBase(String getterName) {
        String base = getterName.startsWith("is") && getterName.length() > 2 ? getterName.substring(2)
                : (getterName.startsWith("get") && getterName.length() > 3 ? getterName.substring(3) : getterName);
        return base.isEmpty() ? getterName : Character.toUpperCase(base.charAt(0)) + base.substring(1);
    }
}
