package org.openflexo.pamela.editor.ui.action;
import org.openflexo.icon.IconMarker;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import java.lang.reflect.Type;
import java.util.function.Supplier;

import org.openflexo.connie.type.CustomTypeManager;
import org.openflexo.gina.controller.CustomTypeEditorProvider;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.pamela.editor.ui.type.PamelaCustomTypeEditorProvider;
import org.openflexo.pamela.editor.ui.type.PamelaCustomTypeManager;
import org.openflexo.pamela.editor.ui.type.PamelaEntityTypeFactory;
import org.openflexo.pamela.editor.ui.type.PamelaTypes;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Changes the value type of a {@link SourceModelProperty} (C19 retype). For a
 * LIST property the chosen type is the element type. The type is edited with the
 * Gina {@code TypeSelector} widget.
 */
public class ChangePropertyTypeAction extends ParameteredAction {


    @Override
    public ActionGroup getGroup() {
        return ActionGroup.REFACTOR;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.PROPERTY_ICON;
    }

    @Override
    protected IconMarker[] getMarkers() {
        return new IconMarker[] { PamelaEditorIconLibrary.SYNC };
    }

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/ChangePropertyTypeForm.fib");

    private Type type;
    private CustomTypeManager customTypeManager;
    private CustomTypeEditorProvider customTypeEditorProvider;

    @Override
    public String getLabel() {
        return loc("change_type_action");
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
        return loc("change_property_type_title");
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        SourceModelProperty property = (SourceModelProperty) target;
        SourceMetaModel model = property.getModelEntity().getMetaModel();
        PamelaEntityTypeFactory factory = new PamelaEntityTypeFactory(model);
        customTypeManager = new PamelaCustomTypeManager(factory);
        customTypeEditorProvider = new PamelaCustomTypeEditorProvider(factory);
        String current = property.getType() != null ? property.getType().getQualifiedName() : null;
        type = PamelaTypes.forQualifiedName(current, model);
        return true;
    }

    @Override
    public boolean isInputValid() {
        return type != null;
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelProperty property = (SourceModelProperty) target;
        SourceModelEntity entity = property.getModelEntity();
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();
        property.changeType(PamelaTypes.qualifiedNameOf(type));
        return () -> model.getEntity(entityQN);
    }

    // --- bound by ChangePropertyTypeForm.fib --------------------------------

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
}
