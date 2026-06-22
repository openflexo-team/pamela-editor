package org.openflexo.pamela.editor.ui.action;

import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Contextual action: removes an inheritance link by dropping one of this
 * entity's direct super-entities (removes a super-interface).
 *
 * <p>Delegates to {@link SourceModelEntity#removeSuperEntity}. Only applicable
 * when the entity has at least one direct super-entity.</p>
 */
public class RemoveSuperEntityAction implements ContextualAction {

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
    public void perform(Object target, PamelaEditorApplication app) {
        SourceModelEntity entity = (SourceModelEntity) target;
        SourceMetaModel model = entity.getMetaModel();
        PamelaProject project = app.getProjectForElement(entity);
        if (model == null || project == null) {
            return;
        }

        List<SourceModelEntity> directSupers = entity.getDirectSuperEntities();
        if (directSupers.isEmpty()) {
            return;
        }
        String[] names = directSupers.stream()
                .map(SourceModelEntity::getQualifiedName)
                .sorted().toArray(String[]::new);

        Object answer = JOptionPane.showInputDialog(app.getFrame(),
                "Remove super-entity of '" + entity.getSimpleName() + "':",
                "Remove Super-Entity", JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (answer == null) {
            return; // cancelled
        }
        SourceModelEntity superEntity = model.getEntity(answer.toString());
        if (superEntity == null) {
            return;
        }

        try {
            entity.removeSuperEntity(superEntity);
        } catch (IOException | RuntimeException e) {
            ModelEditingSupport.error(app, "Remove Super-Entity",
                    "Could not remove super-entity:\n" + e.getMessage());
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
}
