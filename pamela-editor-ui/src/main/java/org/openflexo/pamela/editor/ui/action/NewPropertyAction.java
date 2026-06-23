package org.openflexo.pamela.editor.ui.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.pamela.editor.ui.dialog.ModelEditingDialogs;
import org.openflexo.pamela.editor.ui.dialog.NewPropertyParameters;

/**
 * Contextual action: adds a new property (SINGLE or LIST) to a
 * {@link SourceModelEntity} — the "create" path of
 * {@code model-editing-design.md §1.1} for properties (C4 / C5).
 *
 * <p>Parameters (identifier, cardinality, type) are collected by
 * {@link NewPropertyDialog}; the source generation is delegated to
 * {@link SourceModelEntity#addSingleProperty} / {@code addListProperty}.</p>
 *
 * <p>One action covers both cardinalities (the dialog carries the choice),
 * rather than the two separate {@code NewSingleProperty} / {@code NewListProperty}
 * entries sketched in the spec — a single menu item with an in-dialog toggle is
 * the cleaner surface and extends naturally (embedded, options…).</p>
 */
public class NewPropertyAction implements ContextualAction {

    @Override
    public String getLabel() {
        return "New Property…";
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceModelEntity;
    }

    @Override
    public void perform(Object target, PamelaEditorApplication app) {
        SourceModelEntity entity = (SourceModelEntity) target;
        SourceMetaModel model = entity.getMetaModel();
        PamelaProject project = app.getProjectForElement(entity);
        if (model == null || project == null) {
            return;
        }

        Set<String> existingIds = new HashSet<>(entity.getDeclaredProperties().keySet());
        NewPropertyParameters params = new NewPropertyParameters(typeChoices(model), existingIds);
        if (!ModelEditingDialogs.showForm(app.getFrame(),
                ModelEditingDialogs.NEW_PROPERTY_FIB, "New Property", params)) {
            return; // cancelled
        }
        String identifier = params.getTrimmedIdentifier();

        final String entityQN = entity.getQualifiedName();
        try {
            if (params.getList()) {
                entity.addListProperty(identifier, params.getType());
            } else {
                entity.addSingleProperty(identifier, params.getType());
            }
        } catch (IOException | RuntimeException e) {
            ModelEditingSupport.error(app, "New Property",
                    "Could not add property '" + identifier + "':\n" + e.getMessage());
            return;
        }

        // Re-select the owning entity so its source view and detailed browser
        // refresh to show the new accessors / property row.
        app.rebuildProject(project, () -> {
            SourceModelEntity refreshed = model.getEntity(entityQN);
            if (refreshed != null) {
                app.selectInBrowser(refreshed);
            }
        });
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
        choices.addAll(new TreeSet<>(model.getEntities().keySet()));
        return choices;
    }
}
