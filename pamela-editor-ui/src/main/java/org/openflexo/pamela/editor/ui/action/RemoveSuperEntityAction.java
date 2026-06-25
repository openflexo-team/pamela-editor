package org.openflexo.pamela.editor.ui.action;
import org.openflexo.icon.IconMarker;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Removes an inheritance link by dropping one of an entity's direct
 * super-entities (removes a super-interface).
 *
 * <p>Flat action with its own {@code choices}/{@code selected} parameters and
 * form fragment ({@code RemoveSuperEntityForm.fib}). Delegates to
 * {@link SourceModelEntity#removeSuperEntity}.</p>
 */
public class RemoveSuperEntityAction extends ParameteredAction {


    @Override
    public ActionGroup getGroup() {
        return ActionGroup.REFACTOR;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.ENTITY_ICON;
    }

    @Override
    protected IconMarker[] getMarkers() {
        return new IconMarker[] { PamelaEditorIconLibrary.MINUS };
    }

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/RemoveSuperEntityForm.fib");

    private List<String> choices = Collections.emptyList();
    private String selected;

    @Override
    public String getLabel() {
        return "Remove Super-Entity…";
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceModelEntity
                && !((SourceModelEntity) target).getDirectSuperEntities().isEmpty();
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected String getDialogTitle() {
        return "Remove Super-Entity";
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        SourceModelEntity entity = (SourceModelEntity) target;
        List<SourceModelEntity> directSupers = entity.getDirectSuperEntities();
        if (directSupers.isEmpty()) {
            return false;
        }
        choices = new ArrayList<>();
        directSupers.stream().map(SourceModelEntity::getQualifiedName).sorted()
                .forEach(choices::add);
        selected = choices.get(0);
        return true;
    }

    @Override
    public boolean isInputValid() {
        return selected != null && !selected.isEmpty();
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelEntity entity = (SourceModelEntity) target;
        SourceMetaModel model = entity.getMetaModel();
        SourceModelEntity superEntity = model.getEntity(selected);
        if (superEntity == null) {
            return null;
        }
        final String entityQN = entity.getQualifiedName();
        entity.removeSuperEntity(superEntity);
        return () -> model.getEntity(entityQN);
    }

    // --- bound by RemoveSuperEntityForm.fib ---------------------------------

    public List<String> getChoices() {
        return choices;
    }

    public String getSelected() {
        return selected;
    }

    public void setSelected(String selected) {
        this.selected = selected;
        fireInputValidChanged();
    }
}
