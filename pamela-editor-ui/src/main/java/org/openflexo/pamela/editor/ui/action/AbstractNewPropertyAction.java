/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor.
 *
 */

package org.openflexo.pamela.editor.ui.action;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.swing.ImageIcon;

import org.openflexo.connie.type.CustomTypeManager;
import org.openflexo.gina.controller.CustomTypeEditorProvider;
import org.openflexo.pamela.editor.model.AccessorSpec;
import org.openflexo.pamela.editor.model.AccessorSpec.Role;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.pamela.editor.ui.type.PamelaCustomTypeEditorProvider;
import org.openflexo.pamela.editor.ui.type.PamelaCustomTypeManager;
import org.openflexo.pamela.editor.ui.type.PamelaEntityTypeFactory;
import org.openflexo.pamela.editor.ui.type.PamelaTypes;

import spoon.reflect.declaration.CtMethod;

/**
 * Common mechanics of the "New single property" / "New list property" dialogs
 * (model-editing-design.md §3.3 / §5bis). A concrete subclass only fixes the cardinality
 * ({@link #isList()}) and the sibling accessor roles offered besides the (always present)
 * getter ({@link #siblingRoles()} / {@link #optionFor(Role)}); everything else — dialog
 * seeding, name/candidate refresh, spec collection, validation — is generic here, driven by
 * a per-role naming/default-include table equivalent to the former {@code
 * NewPropertyAction.reconfigureForCardinality()}.
 */
public abstract class AbstractNewPropertyAction extends ParameteredAction {

    @Override
    public ActionGroup getGroup() {
        return ActionGroup.NEW;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.PROPERTY_ICON;
    }

    /** Generated-method-name prefix per role (independent of cardinality). */
    private static String namePrefix(Role role) {
        switch (role) {
            case GETTER:    return "get";
            case SETTER:    return "set";
            case UPDATER:   return "update";
            case ADDER:     return "addTo";
            case REMOVER:   return "removeFrom";
            case REINDEXER: return "reindex";
            default: throw new IllegalArgumentException("Unsupported role: " + role);
        }
    }

    /** Default "include" checkbox state per role (independent of cardinality). */
    private static boolean defaultInclude(Role role) {
        switch (role) {
            case GETTER:
            case SETTER:
            case ADDER:
            case REMOVER:
                return true;
            case UPDATER:
            case REINDEXER:
            default:
                return false;
        }
    }

    /** Signature-matching candidates for the given role. */
    private List<CtMethod<?>> candidatesFor(Role role, List<CtMethod<?>> methods, String typeQN) {
        switch (role) {
            case GETTER:    return AccessorCandidates.noArgReturning(methods, typeQN, isList());
            case REINDEXER: return AccessorCandidates.reindexer(methods, typeQN);
            default:        return AccessorCandidates.oneParam(methods, typeQN);
        }
    }

    private final AccessorOption getterOption = new AccessorOption(Role.GETTER, "Getter");

    private SourceModelEntity entity;
    private String identifier = "";
    private Type type;
    private Set<String> existingIdentifiers = new HashSet<>();
    private CustomTypeManager customTypeManager;
    private CustomTypeEditorProvider customTypeEditorProvider;

    /** SINGLE or LIST — drives {@code createProperty(...)} and the getter's candidate lookup. */
    protected abstract boolean isList();

    /** The sibling roles (besides GETTER) offered by this cardinality, in display order. */
    protected abstract List<Role> siblingRoles();

    /** The {@link AccessorOption} for a sibling role (declared and held by the concrete subclass). */
    protected abstract AccessorOption optionFor(Role role);

    /** Re-fires every {@code data.<role>Option} path so Gina refreshes the nested bindings. */
    protected abstract void fireOptionsChanged();

    private List<AccessorOption> allOptions() {
        List<AccessorOption> options = new ArrayList<>();
        options.add(getterOption);
        for (Role role : siblingRoles()) {
            options.add(optionFor(role));
        }
        return options;
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceModelEntity;
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
        identifier = "";
        configureOptions();
        return true;
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();
        String typeQN = PamelaTypes.qualifiedNameOf(type);

        List<AccessorSpec> specs = new ArrayList<>();
        for (AccessorOption opt : allOptions()) {
            AccessorSpec spec = opt.toSpec();
            if (spec != null) {
                specs.add(spec);
            }
        }
        entity.createProperty(getTrimmedIdentifier(), typeQN, isList(), specs);
        return () -> model.getEntity(entityQN);
    }

    @Override
    public boolean isInputValid() {
        String id = getTrimmedIdentifier();
        if (!ModelEditingSupport.isValidJavaIdentifier(id)
                || existingIdentifiers.contains(id) || type == null) {
            return false;
        }
        for (AccessorOption opt : allOptions()) {
            if (opt.isShow() && !opt.isComplete()) {
                return false;
            }
        }
        return true;
    }

    /**
     * (Re)configures every option for the current identifier/type. Called once on open;
     * identifier/type edits afterwards go through {@link #refreshGeneratedNames()} /
     * {@link #refreshCandidates()} instead, so the user's per-row checkbox/mode choices survive.
     */
    private void configureOptions() {
        String cap = capitalise(getTrimmedIdentifier());
        String typeQN = PamelaTypes.qualifiedNameOf(type);
        List<CtMethod<?>> methods = AccessorCandidates.entityMethods(entity);

        getterOption.configure(true, true, true,
                candidatesFor(Role.GETTER, methods, typeQN), namePrefix(Role.GETTER) + cap, null, true);
        for (Role role : siblingRoles()) {
            optionFor(role).configure(true, false, true,
                    candidatesFor(role, methods, typeQN), namePrefix(role) + cap, null, defaultInclude(role));
        }
        for (AccessorOption opt : allOptions()) {
            opt.setChangeCallback(this::fireInputValidChanged);
        }
    }

    /** Updates the generated names from the current identifier, preserving include/attach choices. */
    private void refreshGeneratedNames() {
        String cap = capitalise(getTrimmedIdentifier());
        getterOption.setGeneratedName(namePrefix(Role.GETTER) + cap);
        for (Role role : siblingRoles()) {
            optionFor(role).setGeneratedName(namePrefix(role) + cap);
        }
    }

    /** Updates the attach-candidate sets from the current type, preserving include/attach choices. */
    private void refreshCandidates() {
        String typeQN = PamelaTypes.qualifiedNameOf(type);
        List<CtMethod<?>> methods = AccessorCandidates.entityMethods(entity);
        getterOption.setCandidates(candidatesFor(Role.GETTER, methods, typeQN));
        for (Role role : siblingRoles()) {
            optionFor(role).setCandidates(candidatesFor(role, methods, typeQN));
        }
    }

    private static String capitalise(String s) {
        if (s == null || s.isEmpty()) {
            return s == null ? "" : s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // --- bound by the concrete FIB forms ------------------------------------

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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
        refreshCandidates();
        getPropertyChangeSupport().firePropertyChange("headerSummary", null, getHeaderSummary());
        fireInputValidChanged();
    }

    /** Title shown at the top of the dialog; updates live as the identifier changes. */
    public String getHeaderSummary() {
        String id = getTrimmedIdentifier();
        return "New " + (isList() ? "list " : "") + "property" + (id.isEmpty() ? "" : " ‘" + id + "’");
    }

    public AccessorOption getGetterOption() {
        return getterOption;
    }

    /** Owning entity — the per-role {@code JavaMethodSelector} context. */
    public SourceModelEntity getEntity() {
        return entity;
    }

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
