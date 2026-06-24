package org.openflexo.pamela.editor.ui.action;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.openflexo.connie.type.CustomTypeManager;
import org.openflexo.gina.controller.CustomTypeEditorProvider;
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

/**
 * Adds a new property (SINGLE or LIST) to a {@link SourceModelEntity} (C4 / C5).
 * The value type is chosen with the Gina {@code TypeSelector} widget (JDK types
 * + PAMELA source entities via {@link PamelaCustomTypeManager}).
 */
public class NewPropertyAction extends ParameteredAction {

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/NewPropertyForm.fib");

    private String identifier = "";
    private boolean list;
    private Type type;
    private Set<String> existingIdentifiers = new HashSet<>();
    private CustomTypeManager customTypeManager;
    private CustomTypeEditorProvider customTypeEditorProvider;

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
        SourceMetaModel model = entity.getMetaModel();
        existingIdentifiers = new HashSet<>(entity.getDeclaredProperties().keySet());
        PamelaEntityTypeFactory factory = new PamelaEntityTypeFactory(model);
        customTypeManager = new PamelaCustomTypeManager(factory);
        customTypeEditorProvider = new PamelaCustomTypeEditorProvider(factory);
        type = String.class;
        return true;
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelEntity entity = (SourceModelEntity) target;
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();
        String id = getTrimmedIdentifier();
        String typeQN = PamelaTypes.qualifiedNameOf(type);
        if (list) {
            entity.addListProperty(id, typeQN);
        } else {
            entity.addSingleProperty(id, typeQN);
        }
        return () -> model.getEntity(entityQN);
    }

    @Override
    public boolean isInputValid() {
        String id = getTrimmedIdentifier();
        return ModelEditingSupport.isValidJavaIdentifier(id)
                && !existingIdentifiers.contains(id)
                && type != null;
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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
        fireInputValidChanged();
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
