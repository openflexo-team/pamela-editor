package org.openflexo.pamela.editor.ui.action;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.openflexo.pamela.editor.model.AccessorSpec.Role;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Renames a {@link SourceModelProperty} (C19): changes its PAMELA key and, for each
 * involved accessor, optionally renames the <em>method</em> too. The dialog shows the
 * new-key field plus one row per present accessor (getter/setter/adder/remover/
 * reindexer/updater) — a "rename this method?" checkbox + an editable new-method-name
 * field defaulted to the convention derived from the new key. Inspired by
 * {@link NewSinglePropertyAction} / {@link NewListPropertyAction}. Delegates to
 * {@link SourceModelProperty#rename(String, Map)}.
 */
public class RenamePropertyAction extends ParameteredAction {

    @Override
    public ActionGroup getGroup() {
        return ActionGroup.REFACTOR;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.PROPERTY_ICON;
    }

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/RenamePropertyForm.fib");

    /** Generated-method-name prefix per role (matches {@code AbstractNewPropertyAction}). */
    private static String namePrefix(Role role, String getterMethodName) {
        switch (role) {
            case GETTER:    return getterMethodName != null && getterMethodName.startsWith("is") ? "is" : "get";
            case SETTER:    return "set";
            case UPDATER:   return "update";
            case ADDER:     return "addTo";
            case REMOVER:   return "removeFrom";
            case REINDEXER: return "reindex";
            default: throw new IllegalArgumentException("Unsupported role: " + role);
        }
    }

    private String name = "";
    private Set<String> forbidden = Collections.emptySet();
    private SourceModelProperty targetProperty;

    private final MethodRenameOption getterOption = new MethodRenameOption(Role.GETTER, "Getter");
    private final MethodRenameOption setterOption = new MethodRenameOption(Role.SETTER, "Setter");
    private final MethodRenameOption adderOption = new MethodRenameOption(Role.ADDER, "Adder");
    private final MethodRenameOption removerOption = new MethodRenameOption(Role.REMOVER, "Remover");
    private final MethodRenameOption reindexerOption = new MethodRenameOption(Role.REINDEXER, "Reindexer");
    private final MethodRenameOption updaterOption = new MethodRenameOption(Role.UPDATER, "Updater");

    private String getterMethodName; // for the getter's is/get prefix

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
        targetProperty = property;
        forbidden = new HashSet<>(entity.getDeclaredProperties().keySet());
        forbidden.remove(property.getPropertyIdentifier()); // unchanged stays valid
        name = property.getPropertyIdentifier();
        getterMethodName = property.getGetterMethodName();

        configureOptions();

        for (MethodRenameOption opt : allOptions()) {
            opt.setChangeCallback(this::fireInputValidChanged);
        }
        return true;
    }

    private List<MethodRenameOption> allOptions() {
        List<MethodRenameOption> options = new ArrayList<>();
        options.add(getterOption);
        options.add(setterOption);
        options.add(updaterOption);
        options.add(adderOption);
        options.add(removerOption);
        options.add(reindexerOption);
        return options;
    }

    /** (Re)configures every option's presence + default new name from the current key. */
    private void configureOptions() {
        SourceModelProperty property = targetProperty;
        boolean list = property.isListCardinality();
        String cap = capitalise(getTrimmedName());
        // Rows are gated by cardinality (SINGLE → setter/updater; LIST → adder/remover/reindexer),
        // like the New Single/List Property dialogs — and, within the relevant roles, only the
        // accessors that actually exist on the property are shown. A method rename is offered
        // (checked by default) for each such present accessor.
        configureOption(getterOption, Role.GETTER, property.getGetterMethodName(), cap);
        configureOption(setterOption, Role.SETTER, list ? null : property.getSetterMethodName(), cap);
        configureOption(updaterOption, Role.UPDATER, list ? null : property.getUpdaterMethodName(), cap);
        configureOption(adderOption, Role.ADDER, list ? property.getAdderMethodName() : null, cap);
        configureOption(removerOption, Role.REMOVER, list ? property.getRemoverMethodName() : null, cap);
        configureOption(reindexerOption, Role.REINDEXER, list ? property.getReindexerMethodName() : null, cap);
    }

    private void configureOption(MethodRenameOption option, Role role, String currentName, String cap) {
        if (currentName == null) {
            option.configureAbsent();
        } else {
            String defaultName = namePrefix(role, getterMethodName) + cap;
            option.configurePresent(currentName, defaultName, true);
        }
    }

    /** On key change: refresh each present option's default new name (checkbox choice preserved). */
    private void refreshDefaultNames() {
        String cap = capitalise(getTrimmedName());
        for (MethodRenameOption opt : allOptions()) {
            if (opt.isPresent()) {
                opt.setDefaultName(namePrefix(opt.getRole(), getterMethodName) + cap);
            }
        }
    }

    @Override
    public boolean isInputValid() {
        if (!ModelEditingSupport.isAvailableIdentifier(name, forbidden)) {
            return false;
        }
        for (MethodRenameOption opt : allOptions()) {
            if (!opt.isComplete()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelProperty property = (SourceModelProperty) target;
        SourceModelEntity entity = property.getModelEntity();
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();
        String newName = getTrimmedName();

        Map<Role, String> methodRenames = new EnumMap<>(Role.class);
        for (MethodRenameOption opt : allOptions()) {
            String renameTo = opt.toRenameName();
            if (renameTo != null) {
                methodRenames.put(opt.getRole(), renameTo);
            }
        }
        property.rename(newName, methodRenames);
        return () -> model.getEntity(entityQN);
    }

    private static String capitalise(String s) {
        if (s == null || s.isEmpty()) {
            return s == null ? "" : s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String getTrimmedName() {
        return name == null ? "" : name.trim();
    }

    // --- bound by RenamePropertyForm.fib ------------------------------------

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        refreshDefaultNames();
        fireInputValidChanged();
    }

    public MethodRenameOption getGetterOption() {
        return getterOption;
    }

    public MethodRenameOption getSetterOption() {
        return setterOption;
    }

    public MethodRenameOption getUpdaterOption() {
        return updaterOption;
    }

    public MethodRenameOption getAdderOption() {
        return adderOption;
    }

    public MethodRenameOption getRemoverOption() {
        return removerOption;
    }

    public MethodRenameOption getReindexerOption() {
        return reindexerOption;
    }
}
