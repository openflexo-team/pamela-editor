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
 * Adds an inheritance link by making an entity extend another
 * {@link SourceModelEntity} (adds a super-interface).
 *
 * <p>Flat action with its own {@code choices}/{@code selected} parameters and
 * form fragment ({@code AddSuperEntityForm.fib}). Candidates exclude the entity
 * itself, its direct super-entities, and its (transitive) sub-entities.</p>
 */
public class AddSuperEntityAction extends ParameteredAction {


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
            ResourceLocator.locateResource("Fib/dialogs/AddSuperEntityForm.fib");

    private List<SourceModelEntity> candidateEntities = Collections.emptyList();
    private SourceModelEntity selectedEntity;
    private SourceMetaModel metaModel;

    @Override
    public String getLabel() {
        return "Add Super-Entity…";
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
        return "Add Super-Entity";
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
        entity.addSuperEntity(selectedEntity);
        return () -> model.getEntity(entityQN);
    }

    // --- bound by AddSuperEntityForm.fib ------------------------------------

    /** Candidate super-entities — the {@code ModelEntitySelector} restriction. */
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
        List<SourceModelEntity> directSupers = entity.getDirectSuperEntities();
        List<SourceModelEntity> result = new ArrayList<>();
        for (SourceModelEntity candidate : model.getEntities().values()) {
            if (candidate == entity || directSupers.contains(candidate)) {
                continue;
            }
            if (isSubEntityOf(candidate, entity)) {
                continue; // would create a cycle
            }
            result.add(candidate);
        }
        return result;
    }

    /** Returns true if {@code candidate} extends {@code ancestor} (transitively). */
    private static boolean isSubEntityOf(SourceModelEntity candidate, SourceModelEntity ancestor) {
        for (SourceModelEntity sup : candidate.getDirectSuperEntities()) {
            if (sup == ancestor || isSubEntityOf(sup, ancestor)) {
                return true;
            }
        }
        return false;
    }
}
