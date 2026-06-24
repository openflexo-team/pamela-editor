package org.openflexo.pamela.editor.ui.action;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Adds a new property (SINGLE or LIST) to a {@link SourceModelEntity} — the
 * "create" path of {@code model-editing-design.md §1.1} for properties (C4 / C5).
 *
 * <p>The action carries the bound parameters (identifier, cardinality, type) and
 * delegates the source generation to {@link SourceModelEntity#addSingleProperty}
 * / {@code addListProperty}. The type is selected from a fixed list (entities +
 * common JDK types); a free-text {@code TypeSelector} widget is a follow-up.</p>
 */
public class NewPropertyAction extends ParameteredAction {

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/NewPropertyForm.fib");

    private String identifier = "";
    private boolean list;
    private String type;
    private List<String> typeChoices = new ArrayList<>();
    private Set<String> existingIdentifiers = new HashSet<>();

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
        SourceModelEntity entity = (SourceModelEntity) target;
        this.existingIdentifiers = new HashSet<>(entity.getDeclaredProperties().keySet());
        this.typeChoices = typeChoices(entity.getMetaModel());
        this.type = typeChoices.isEmpty() ? null : typeChoices.get(0);
        return true;
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelEntity entity = (SourceModelEntity) target;
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();
        String id = getTrimmedIdentifier();
        if (list) {
            entity.addListProperty(id, type);
        } else {
            entity.addSingleProperty(id, type);
        }
        return () -> model.getEntity(entityQN);
    }

    @Override
    public boolean isInputValid() {
        String id = getTrimmedIdentifier();
        return ModelEditingSupport.isValidJavaIdentifier(id)
                && !existingIdentifiers.contains(id)
                && type != null && !type.isEmpty();
    }

    // --- bound by NewPropertyForm.fib ---------------------------------------

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
        fireInputValidChanged();
    }

    public boolean getList() {
        return list;
    }

    public void setList(boolean list) {
        this.list = list;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
        fireInputValidChanged();
    }

    public List<String> getTypeChoices() {
        return typeChoices;
    }

    private String getTrimmedIdentifier() {
        return identifier == null ? "" : identifier.trim();
    }

    /** Common JDK types first, then the metamodel's entity qualified names (sorted). */
    private static List<String> typeChoices(SourceMetaModel model) {
        List<String> choices = new ArrayList<>();
        choices.add("java.lang.String");
        choices.add("boolean");
        choices.add("int");
        choices.add("long");
        choices.add("double");
        choices.add("float");
        choices.add("java.lang.Integer");
        choices.add("java.lang.Boolean");
        choices.add("java.util.Date");
        if (model != null) {
            choices.addAll(new TreeSet<>(model.getEntities().keySet()));
        }
        return choices;
    }
}
