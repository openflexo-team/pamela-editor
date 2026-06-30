package org.openflexo.pamela.editor.ui.action;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.openflexo.connie.type.CustomTypeManager;
import org.openflexo.gina.controller.CustomTypeEditorProvider;
import org.openflexo.pamela.editor.model.AccessorSpec;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.pamela.editor.ui.type.PamelaCustomTypeEditorProvider;
import org.openflexo.pamela.editor.ui.type.PamelaCustomTypeManager;
import org.openflexo.pamela.editor.ui.type.PamelaEntityTypeFactory;
import org.openflexo.pamela.editor.ui.type.PamelaTypes;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

import spoon.reflect.declaration.CtMethod;

/**
 * Adds a new property (SINGLE or LIST) to a {@link SourceModelEntity} (C4 / C5).
 * The value type is chosen with the Gina {@code TypeSelector} widget (JDK types
 * + PAMELA source entities via {@link PamelaCustomTypeManager}).
 */
public class NewPropertyAction extends ParameteredAction {


    @Override
    public ActionGroup getGroup() {
        return ActionGroup.NEW;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.PROPERTY_ICON;
    }

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/NewPropertyForm.fib");

    private SourceModelEntity entity;
    private String identifier = "";
    private boolean list;
    private Type type;
    private Set<String> existingIdentifiers = new HashSet<>();
    private CustomTypeManager customTypeManager;
    private CustomTypeEditorProvider customTypeEditorProvider;

    // Explicit per-role accessor choices (generate by default; attach an existing method instead).
    private final AccessorOption getterOption    = new AccessorOption(AccessorSpec.Role.GETTER, "Getter");
    private final AccessorOption setterOption     = new AccessorOption(AccessorSpec.Role.SETTER, "Setter");
    private final AccessorOption updaterOption   = new AccessorOption(AccessorSpec.Role.UPDATER, "Updater");
    private final AccessorOption adderOption       = new AccessorOption(AccessorSpec.Role.ADDER, "Adder");
    private final AccessorOption removerOption    = new AccessorOption(AccessorSpec.Role.REMOVER, "Remover");
    private final AccessorOption reindexerOption = new AccessorOption(AccessorSpec.Role.REINDEXER, "Reindexer");

    private final AccessorOption[] allOptions = {
            getterOption, setterOption, updaterOption, adderOption, removerOption, reindexerOption };

    @Override
    public String getLabel() {
        return "New Property…";
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
        return "New Property";
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        entity = (SourceModelEntity) target;
        SourceMetaModel model = entity.getMetaModel();
        existingIdentifiers = new HashSet<>(entity.getDeclaredProperties().keySet());
        PamelaEntityTypeFactory factory = new PamelaEntityTypeFactory(model);
        customTypeManager = new PamelaCustomTypeManager(factory);
        customTypeEditorProvider = new PamelaCustomTypeEditorProvider(factory);
        type = String.class;
        list = false;
        identifier = "";
        reconfigureForCardinality();
        return true;
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();
        String typeQN = PamelaTypes.qualifiedNameOf(type);

        List<AccessorSpec> specs = new ArrayList<>();
        for (AccessorOption opt : allOptions) {
            if (!opt.isShow()) {
                continue;
            }
            AccessorSpec spec = opt.toSpec();
            if (spec != null) {
                specs.add(spec);
            }
        }
        entity.createProperty(getTrimmedIdentifier(), typeQN, list, specs);
        return () -> model.getEntity(entityQN);
    }

    /**
     * (Re)configures the per-role options for the current cardinality: getter mandatory (generate by
     * default), the relevant siblings shown with generate-by-default and an "attach an existing
     * method" option. Called on open and on a list toggle. Identifier/type edits update names and
     * candidates without resetting the user's checkbox/selector choices (see {@link #setIdentifier} /
     * {@link #setType}).
     */
    private void reconfigureForCardinality() {
        String cap = capitalise(getTrimmedIdentifier());
        String typeQN = PamelaTypes.qualifiedNameOf(type);
        List<CtMethod<?>> methods = AccessorCandidates.entityMethods(entity);

        getterOption.configure(true, true, true,
                AccessorCandidates.noArgReturning(methods, typeQN, list), "get" + cap, null, true);
        if (list) {
            adderOption.configure(true, false, true,
                    AccessorCandidates.oneParam(methods, typeQN), "addTo" + cap, null, true);
            removerOption.configure(true, false, true,
                    AccessorCandidates.oneParam(methods, typeQN), "removeFrom" + cap, null, true);
            reindexerOption.configure(true, false, true,
                    AccessorCandidates.reindexer(methods, typeQN), "reindex" + cap, null, false);
            setterOption.configure(false, false, true, java.util.Collections.emptyList(), "set" + cap, null, false);
            updaterOption.configure(false, false, true, java.util.Collections.emptyList(), "update" + cap, null, false);
        } else {
            setterOption.configure(true, false, true,
                    AccessorCandidates.oneParam(methods, typeQN), "set" + cap, null, true);
            updaterOption.configure(true, false, true,
                    AccessorCandidates.oneParam(methods, typeQN), "update" + cap, null, false);
            adderOption.configure(false, false, true, java.util.Collections.emptyList(), "addTo" + cap, null, false);
            removerOption.configure(false, false, true, java.util.Collections.emptyList(), "removeFrom" + cap, null, false);
            reindexerOption.configure(false, false, true, java.util.Collections.emptyList(), "reindex" + cap, null, false);
        }
        for (AccessorOption opt : allOptions) {
            opt.setChangeCallback(this::fireInputValidChanged);
        }
    }

    /** Updates the generated names from the current identifier, preserving include/attach choices. */
    private void refreshGeneratedNames() {
        String cap = capitalise(getTrimmedIdentifier());
        getterOption.setGeneratedName("get" + cap);
        setterOption.setGeneratedName("set" + cap);
        updaterOption.setGeneratedName("update" + cap);
        adderOption.setGeneratedName("addTo" + cap);
        removerOption.setGeneratedName("removeFrom" + cap);
        reindexerOption.setGeneratedName("reindex" + cap);
    }

    /** Updates the attach-candidate sets from the current type, preserving include/attach choices. */
    private void refreshCandidates() {
        String typeQN = PamelaTypes.qualifiedNameOf(type);
        List<CtMethod<?>> methods = AccessorCandidates.entityMethods(entity);
        getterOption.setCandidates(AccessorCandidates.noArgReturning(methods, typeQN, list));
        if (list) {
            adderOption.setCandidates(AccessorCandidates.oneParam(methods, typeQN));
            removerOption.setCandidates(AccessorCandidates.oneParam(methods, typeQN));
            reindexerOption.setCandidates(AccessorCandidates.reindexer(methods, typeQN));
        } else {
            setterOption.setCandidates(AccessorCandidates.oneParam(methods, typeQN));
            updaterOption.setCandidates(AccessorCandidates.oneParam(methods, typeQN));
        }
    }

    private static String capitalise(String s) {
        if (s == null || s.isEmpty()) {
            return s == null ? "" : s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public boolean isInputValid() {
        String id = getTrimmedIdentifier();
        if (!ModelEditingSupport.isValidJavaIdentifier(id)
                || existingIdentifiers.contains(id) || type == null) {
            return false;
        }
        for (AccessorOption opt : allOptions) {
            if (opt.isShow() && !opt.isComplete()) {
                return false;
            }
        }
        return true;
    }

    // --- bound by NewPropertyForm.fib ---------------------------------------

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
        refreshGeneratedNames();
        // Re-resolve each `data.<role>Option.*` binding subtree so the generated-name TextFields
        // refresh from the model (a leaf "generatedName" fire alone does not refresh the nested
        // two-way binding).
        fireOptionsChanged();
        getPropertyChangeSupport().firePropertyChange("headerSummary", null, getHeaderSummary());
        fireInputValidChanged();
    }

    /** Re-fires each option object so Gina re-reads the whole `data.<role>Option.*` path. */
    private void fireOptionsChanged() {
        java.beans.PropertyChangeSupport pcs = getPropertyChangeSupport();
        pcs.firePropertyChange("getterOption", null, getterOption);
        pcs.firePropertyChange("setterOption", null, setterOption);
        pcs.firePropertyChange("updaterOption", null, updaterOption);
        pcs.firePropertyChange("adderOption", null, adderOption);
        pcs.firePropertyChange("removerOption", null, removerOption);
        pcs.firePropertyChange("reindexerOption", null, reindexerOption);
    }

    public boolean getList() {
        return list;
    }

    public void setList(boolean list) {
        this.list = list;
        reconfigureForCardinality();
        getPropertyChangeSupport().firePropertyChange("list", !list, list);
        getPropertyChangeSupport().firePropertyChange("headerSummary", null, getHeaderSummary());
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
        refreshCandidates();
        getPropertyChangeSupport().firePropertyChange("headerSummary", null, getHeaderSummary());
        fireInputValidChanged();
    }

    /** Title shown at the top of the dialog; updates live as the identifier / cardinality change. */
    public String getHeaderSummary() {
        String id = getTrimmedIdentifier();
        return "New " + (list ? "list " : "") + "property" + (id.isEmpty() ? "" : " ‘" + id + "’");
    }

    public AccessorOption getGetterOption()    { return getterOption; }
    public AccessorOption getSetterOption()    { return setterOption; }
    public AccessorOption getUpdaterOption()   { return updaterOption; }
    public AccessorOption getAdderOption()     { return adderOption; }
    public AccessorOption getRemoverOption()   { return removerOption; }
    public AccessorOption getReindexerOption() { return reindexerOption; }

    /** Owning entity — the per-role {@code JavaMethodSelector} context. */
    public SourceModelEntity getEntity() { return entity; }

    public CustomTypeManager getCustomTypeManager() {
        return customTypeManager;
    }

    public CustomTypeEditorProvider getCustomTypeEditorProvider() {
        return customTypeEditorProvider;
    }

    private String getTrimmedIdentifier() {
        return identifier == null ? "" : identifier.trim();
    }
}
