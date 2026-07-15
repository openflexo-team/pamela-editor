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
 * Adds an {@code @Import} entry to an entity's {@code @Imports} annotation, embedding the
 * chosen target entity in the PAMELA meta-model computation even though it may not be
 * reachable via inheritance or a property type (see {@code imports-support-design.md}).
 *
 * <p>Flat action with its own {@code choices}/{@code selected} parameters and form fragment
 * ({@code AddImportForm.fib}), mirroring {@link AddSuperEntityAction}. Candidates exclude the
 * entity itself and its already-imported entities.</p>
 */
public class AddImportAction extends ParameteredAction {

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
        return new IconMarker[] { PamelaEditorIconLibrary.PLUS };
    }

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/AddImportForm.fib");

    private List<SourceModelEntity> candidateEntities = Collections.emptyList();
    private SourceModelEntity selectedEntity;
    private SourceMetaModel metaModel;

    @Override
    public String getLabel() {
        return loc("add_import_action");
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceModelEntity
                && !candidates((SourceModelEntity) target).isEmpty();
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected String getDialogTitle() {
        return loc("add_import_title");
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        SourceModelEntity entity = (SourceModelEntity) target;
        List<SourceModelEntity> candidates = candidates(entity);
        if (candidates.isEmpty()) {
            return false;
        }
        candidateEntities = candidates;
        metaModel = entity.getMetaModel();
        selectedEntity = candidates.get(0);
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
        entity.addImport(selectedEntity);
        return () -> model.getEntity(entityQN);
    }

    // --- bound by AddImportForm.fib ------------------------------------

    /** Candidate entities to import — the {@code ModelEntitySelector} restriction. */
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

    // -------------------------------------------------------------------------

    private static List<SourceModelEntity> candidates(SourceModelEntity entity) {
        SourceMetaModel model = entity.getMetaModel();
        if (model == null) {
            return Collections.emptyList();
        }
        List<SourceModelEntity> alreadyImported = entity.getImportedEntities();
        List<SourceModelEntity> result = new ArrayList<>();
        for (SourceModelEntity candidate : model.getEntities().values()) {
            if (candidate == entity || alreadyImported.contains(candidate)) {
                continue;
            }
            result.add(candidate);
        }
        return result;
    }
}
