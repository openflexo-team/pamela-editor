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
import org.openflexo.pamela.annotations.Adder;
import org.openflexo.pamela.annotations.Reindexer;
import org.openflexo.pamela.annotations.Remover;
import org.openflexo.pamela.annotations.Setter;
import org.openflexo.pamela.annotations.Updater;
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

/**
 * Method-level "promote" action: turns a plain no-arg getter into a <em>new</em> PAMELA property
 * (model-editing-design.md §3.3a). Keyed on a {@link PromotableMethod} facet and applicable on
 * <strong>signature</strong> alone (no parameter, non-{@code void} return): a {@code List<T>} return
 * yields a LIST property, anything else a SINGLE property.
 *
 * <p>The dialog also offers to <strong>pull in the property's other accessors</strong> found in the
 * same interface: for a SINGLE getter a setter / updater; for a LIST getter an adder / remover /
 * reindexer. Candidates are gathered by <strong>signature</strong> (parameter types vs the property
 * type / element type) and, when several match, the default is picked by <strong>name</strong>
 * (e.g. {@code getX} → {@code setX}). Each accessor is a "also promote …" checkbox + a dropdown of
 * the matching methods. All annotations are written in a single text edit by
 * {@link SourceModelEntity#promoteGetterToProperty(String, String, boolean, Map)}.</p>
 */
public class PromoteGetterAction extends ParameteredAction {

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
        return new IconMarker[] { PamelaEditorIconLibrary.PLUS };
    }

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/PromoteGetterForm.fib");

    private SourceModelEntity entity;
    private String methodName;
    private boolean list;
    private String propertyTypeQN;          // property type (SINGLE) / element type (LIST)
    private String propertyIdentifier = "";
    private Set<String> existingIdentifiers = new HashSet<>();

    // One option per candidate accessor role (shown only when relevant + candidates exist).
    private final AccessorOption setterOption    = new AccessorOption(AccessorSpec.Role.SETTER, "Setter");
    private final AccessorOption updaterOption   = new AccessorOption(AccessorSpec.Role.UPDATER, "Updater");
    private final AccessorOption adderOption      = new AccessorOption(AccessorSpec.Role.ADDER, "Adder");
    private final AccessorOption removerOption   = new AccessorOption(AccessorSpec.Role.REMOVER, "Remover");
    private final AccessorOption reindexerOption = new AccessorOption(AccessorSpec.Role.REINDEXER, "Reindexer");

    @Override
    public String getLabel() {
        return loc("promote_as_new_property_action");
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof PromotableMethod
                && ((PromotableMethod) target).isValueGetterShape();
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected String getDialogTitle() {
        return loc("promote_method_to_new_property_title");
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        PromotableMethod pm = (PromotableMethod) target;
        entity = pm.getEntity();
        methodName = pm.getMethodName();
        list = pm.returnsList();
        propertyTypeQN = list ? pm.getListElementTypeQualifiedName() : pm.getReturnTypeQualifiedName();
        existingIdentifiers = new HashSet<>(entity.getDeclaredProperties().keySet());
        propertyIdentifier = derivePropertyName(methodName);

        List<CtMethod<?>> candidates = entityCtMethods(entity);
        String base = capitalisedBase(methodName);
        if (list) {
            // LIST property: adder / remover (1 param = element type), reindexer (element, int).
            AccessorOption.configureSibling(adderOption, true,
                    oneParamCandidates(candidates, propertyTypeQN), "addTo" + base, "add" + base);
            AccessorOption.configureSibling(removerOption, true,
                    oneParamCandidates(candidates, propertyTypeQN), "removeFrom" + base, "remove" + base);
            AccessorOption.configureSibling(reindexerOption, true,
                    reindexerCandidates(candidates, propertyTypeQN), "reindex" + base, "setIndexFor" + base);
        } else {
            // SINGLE property: setter / updater (1 param = property type).
            AccessorOption.configureSibling(setterOption, true,
                    oneParamCandidates(candidates, propertyTypeQN), "set" + base);
            AccessorOption.configureSibling(updaterOption, true,
                    oneParamCandidates(candidates, propertyTypeQN), "update" + base);
        }
        for (AccessorOption opt : siblings) {
            opt.setChangeCallback(this::fireInputValidChanged);
        }
        return true;
    }

    @Override
    public boolean isInputValid() {
        if (!ModelEditingSupport.isAvailableIdentifier(propertyIdentifier, existingIdentifiers)) {
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
            // A generated sibling adds a new method → go through createProperty (getter is attached).
            List<AccessorSpec> all = new ArrayList<>();
            all.add(AccessorSpec.attach(AccessorSpec.Role.GETTER, methodName));
            all.addAll(siblingSpecs);
            entity.createProperty(propertyIdentifier.trim(), propertyTypeQN, list, all);
        } else {
            // All-attach → minimal-diff text-edit promote.
            Map<Class<? extends Annotation>, String> extras = new LinkedHashMap<>();
            for (AccessorSpec spec : siblingSpecs) {
                extras.put(AccessorOption.annotationFor(spec.getRole()), spec.getMethodName());
            }
            entity.promoteGetterToProperty(methodName, propertyIdentifier.trim(), list, extras);
        }
        return () -> model.getEntity(entityQN);
    }

    private final AccessorOption[] siblings = {
            setterOption, updaterOption, adderOption, removerOption, reindexerOption };

    // --- candidate computation (by signature, over the entity's Spoon methods) -

    /** Methods of the entity's primary interface (the {@code JavaMethodSelector} candidate pool). */
    private static List<CtMethod<?>> entityCtMethods(SourceModelEntity entity) {
        List<CtMethod<?>> result = new ArrayList<>();
        if (entity.getCompilationUnit() == null) {
            return result;
        }
        for (CtType<?> type : entity.getCompilationUnit().getRootTypes()) {
            if (entity.getSimpleName().equals(type.getSimpleName())) {
                result.addAll(type.getMethods());
            }
        }
        return result;
    }

    /** Methods with exactly one parameter whose type matches {@code paramTypeQN}. */
    private static List<CtMethod<?>> oneParamCandidates(List<CtMethod<?>> all, String paramTypeQN) {
        List<CtMethod<?>> result = new ArrayList<>();
        if (paramTypeQN == null) {
            return result;
        }
        for (CtMethod<?> m : all) {
            if (m.getParameters().size() == 1
                    && paramTypeQN.equals(m.getParameters().get(0).getType().getQualifiedName())) {
                result.add(m);
            }
        }
        return result;
    }

    /** Methods {@code (T, int)} where {@code T} matches {@code elementTypeQN}. */
    private static List<CtMethod<?>> reindexerCandidates(List<CtMethod<?>> all, String elementTypeQN) {
        List<CtMethod<?>> result = new ArrayList<>();
        if (elementTypeQN == null) {
            return result;
        }
        for (CtMethod<?> m : all) {
            if (m.getParameters().size() == 2
                    && elementTypeQN.equals(m.getParameters().get(0).getType().getQualifiedName())
                    && isInt(m.getParameters().get(1).getType().getQualifiedName())) {
                result.add(m);
            }
        }
        return result;
    }

    private static boolean isInt(String qn) {
        return "int".equals(qn) || "java.lang.Integer".equals(qn);
    }

    // --- bound by PromoteGetterForm.fib -------------------------------------

    public String getPropertyIdentifier() {
        return propertyIdentifier;
    }

    public void setPropertyIdentifier(String propertyIdentifier) {
        this.propertyIdentifier = propertyIdentifier;
        getPropertyChangeSupport().firePropertyChange("promotionSummary", null, getPromotionSummary());
        fireInputValidChanged();
    }

    /** Read-only label shown in the form (SINGLE / LIST), inferred from the return type. */
    public String getCardinalityLabel() {
        return list ? "LIST (collection)" : "SINGLE";
    }

    /** Owning entity — the per-role {@code JavaMethodSelector} context. */
    public SourceModelEntity getEntity()       { return entity; }

    public AccessorOption getSetterOption()    { return setterOption; }
    public AccessorOption getUpdaterOption()   { return updaterOption; }
    public AccessorOption getAdderOption()     { return adderOption; }
    public AccessorOption getRemoverOption()   { return removerOption; }
    public AccessorOption getReindexerOption() { return reindexerOption; }

    /** Read-only summary of the property being created (for the form header). */
    public String getPromotionSummary() {
        return "Promote " + methodName + "() → property '" + propertyIdentifier + "'";
    }

    // -------------------------------------------------------------------------

    /** {@code getName} → {@code name}, {@code isActive} → {@code active}, else the name as-is. */
    private static String derivePropertyName(String getterName) {
        String base = stripAccessorPrefix(getterName);
        if (base == null) {
            return getterName;
        }
        return base.isEmpty() ? getterName
                : Character.toLowerCase(base.charAt(0)) + base.substring(1);
    }

    /** {@code getName} → {@code Name} (capitalised base, for building sibling accessor names). */
    private static String capitalisedBase(String getterName) {
        String base = stripAccessorPrefix(getterName);
        if (base == null || base.isEmpty()) {
            return Character.toUpperCase(getterName.charAt(0)) + getterName.substring(1);
        }
        return Character.toUpperCase(base.charAt(0)) + base.substring(1);
    }

    private static String stripAccessorPrefix(String name) {
        if (name.startsWith("get") && name.length() > 3) {
            return name.substring(3);
        }
        if (name.startsWith("is") && name.length() > 2) {
            return name.substring(2);
        }
        return null;
    }
}
