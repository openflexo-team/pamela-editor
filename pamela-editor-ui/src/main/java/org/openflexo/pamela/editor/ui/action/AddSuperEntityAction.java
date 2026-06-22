package org.openflexo.pamela.editor.ui.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Contextual action: adds an inheritance link by making this entity extend
 * another {@link SourceModelEntity} (adds a super-interface).
 *
 * <p>Delegates to {@link SourceModelEntity#addSuperEntity}. Candidates exclude
 * the entity itself, its existing direct super-entities, and any entity that is
 * (transitively) a sub-entity of this one — the last guard avoids obvious
 * inheritance cycles.</p>
 */
public class AddSuperEntityAction implements ContextualAction {

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
    public void perform(Object target, PamelaEditorApplication app) {
        SourceModelEntity entity = (SourceModelEntity) target;
        SourceMetaModel model = entity.getMetaModel();
        PamelaProject project = app.getProjectForElement(entity);
        if (model == null || project == null) {
            return;
        }

        List<SourceModelEntity> candidates = candidates(entity);
        if (candidates.isEmpty()) {
            return;
        }
        String[] names = candidates.stream()
                .map(SourceModelEntity::getQualifiedName)
                .sorted().toArray(String[]::new);

        Object answer = JOptionPane.showInputDialog(app.getFrame(),
                "Super-entity for '" + entity.getSimpleName() + "':",
                "Add Super-Entity", JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (answer == null) {
            return; // cancelled
        }
        SourceModelEntity superEntity = model.getEntity(answer.toString());
        if (superEntity == null) {
            return;
        }

        try {
            entity.addSuperEntity(superEntity);
        } catch (IOException | RuntimeException e) {
            ModelEditingSupport.error(app, "Add Super-Entity",
                    "Could not add super-entity:\n" + e.getMessage());
            return;
        }

        final String qn = entity.getQualifiedName();
        app.rebuildProject(project, () -> {
            SourceModelEntity refreshed = model.getEntity(qn);
            if (refreshed != null) {
                app.selectInBrowser(refreshed);
            }
        });
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
