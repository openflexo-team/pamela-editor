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
 * Removes one of an entity's {@code @Import} entries (drops it from the {@code @Imports}
 * annotation — the whole annotation is removed if it was the last one).
 *
 * <p>Flat action with its own {@code choices}/{@code selected} parameters and form fragment
 * ({@code RemoveImportForm.fib}), mirroring {@link RemoveSuperEntityAction}. Delegates to
 * {@link SourceModelEntity#removeImport}.</p>
 */
public class RemoveImportAction extends ParameteredAction {

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
            ResourceLocator.locateResource("Fib/dialogs/RemoveImportForm.fib");

    private List<SourceModelEntity> candidateEntities = Collections.emptyList();
    private SourceModelEntity selectedEntity;
    private SourceMetaModel metaModel;

    @Override
    public String getLabel() {
        return loc("remove_import_action");
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceModelEntity
                && !((SourceModelEntity) target).getImportedEntities().isEmpty();
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected String getDialogTitle() {
        return loc("remove_import_title");
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        SourceModelEntity entity = (SourceModelEntity) target;
        List<SourceModelEntity> imported = entity.getImportedEntities();
        if (imported.isEmpty()) {
            return false;
        }
        candidateEntities = new ArrayList<>(imported);
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
        entity.removeImport(selectedEntity);
        return () -> model.getEntity(entityQN);
    }

    // --- bound by RemoveImportForm.fib ---------------------------------

    /** The entity's current imports — the {@code ModelEntitySelector} restriction. */
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
