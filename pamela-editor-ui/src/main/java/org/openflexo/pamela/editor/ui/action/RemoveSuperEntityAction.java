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

    private List<SourceModelEntity> candidateEntities = Collections.emptyList();
    private SourceModelEntity selectedEntity;
    private SourceMetaModel metaModel;

    @Override
    public String getLabel() {
        return loc("remove_super_entity_action");
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
        return loc("remove_super_entity_title");
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        SourceModelEntity entity = (SourceModelEntity) target;
        List<SourceModelEntity> directSupers = entity.getDirectSuperEntities();
        if (directSupers.isEmpty()) {
            return false;
        }
        candidateEntities = new ArrayList<>(directSupers);
        metaModel = entity.getMetaModel();
        selectedEntity = candidateEntities.get(0);
        return true;
    }

    @Override
    public boolean isInputValid() {
        return selectedEntity != null;
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelEntity entity = (SourceModelEntity) target;
        SourceMetaModel model = entity.getMetaModel();
        if (selectedEntity == null) {
            return null;
        }
        final String entityQN = entity.getQualifiedName();
        entity.removeSuperEntity(selectedEntity);
        return () -> model.getEntity(entityQN);
    }

    // --- bound by RemoveSuperEntityForm.fib ---------------------------------

    /** The entity's direct super-entities — the {@code ModelEntitySelector} restriction. */
    public List<SourceModelEntity> getCandidateEntities() {
        return candidateEntities;
    }

    /** Browser scope for the {@code ModelEntitySelector}. */
    public SourceMetaModel getMetaModel() {
        return metaModel;
    }

    public SourceModelEntity getSelectedEntity() {
        return selectedEntity;
    }

    public void setSelectedEntity(SourceModelEntity selectedEntity) {
        this.selectedEntity = selectedEntity;
        fireInputValidChanged();
    }
}
