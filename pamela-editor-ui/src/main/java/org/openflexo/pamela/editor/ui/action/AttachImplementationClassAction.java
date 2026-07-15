package org.openflexo.pamela.editor.ui.action;
import org.openflexo.icon.IconMarker;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.openflexo.pamela.editor.model.SourceCompilationUnit;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtType;

/**
 * Attaches an existing Java class to an entity as its {@code @ImplementationClass} — the
 * "attach existing class" (promote) path of
 * {@code implementation-class-support-design.md §3.1/§4} (the "create a new one" path is
 * {@link CreateImplementationClassAction}).
 *
 * <p>Opens a dialog with a {@link org.openflexo.pamela.editor.ui.widget.JavaClassSelector}
 * scoped to the whole project, restricted to plain classes (interfaces/enums excluded) minus
 * the entity's own current implementation class, if any — mirrors {@link AddAsRootTypeAction}'s
 * permissive style: no "does it already implement the entity" pre-filter, the next rebuild's
 * {@code SourceImplementationClass} validation surfaces a mismatch as an {@code Error}/
 * {@code Warning} instead.</p>
 */
public class AttachImplementationClassAction extends ParameteredAction {

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
            ResourceLocator.locateResource("Fib/dialogs/AttachImplementationClassForm.fib");

    private SourceMetaModel metaModel;
    private List<CtType<?>> candidateTypes = Collections.emptyList();
    private CtType<?> selectedType;

    @Override
    public String getLabel() {
        return loc("attach_implementation_class_action");
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceModelEntity
                && !candidateTypes((SourceModelEntity) target).isEmpty();
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected String getDialogTitle() {
        return loc("attach_implementation_class_title");
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        SourceModelEntity entity = (SourceModelEntity) target;
        metaModel = entity.getMetaModel();
        candidateTypes = candidateTypes(entity);
        if (candidateTypes.isEmpty()) {
            return false;
        }
        // No default pick — the user must explicitly choose a class.
        selectedType = null;
        return true;
    }

    @Override
    public boolean isInputValid() {
        return selectedType != null;
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelEntity entity = (SourceModelEntity) target;
        SourceMetaModel model = entity.getMetaModel();
        if (selectedType == null) {
            return null;
        }
        final String entityQN = entity.getQualifiedName();
        entity.attachImplementationClass(selectedType.getQualifiedName());
        return () -> model.getEntity(entityQN);
    }

    // --- bound by AttachImplementationClassForm.fib -------------------------

    /** Browser scope for the {@code JavaClassSelector} — the whole project. */
    public SourceMetaModel getMetaModel() {
        return metaModel;
    }

    /** Candidate classes — the {@code JavaClassSelector} restriction. */
    public List<CtType<?>> getCandidateTypes() {
        return candidateTypes;
    }

    public CtType<?> getSelectedType() {
        return selectedType;
    }

    public void setSelectedType(CtType<?> selectedType) {
        this.selectedType = selectedType;
        fireInputValidChanged();
    }

    // -------------------------------------------------------------------------

    /**
     * Every plain class (interfaces/enums excluded) Spoon currently knows about, across the
     * whole metamodel, minus the entity's own current implementation class if any. Sorted by
     * qualified name for a stable, predictable browsing/completion order.
     */
    private static List<CtType<?>> candidateTypes(SourceModelEntity entity) {
        SourceMetaModel model = entity.getMetaModel();
        if (model == null) {
            return Collections.emptyList();
        }
        String currentImplQN = entity.getImplementationClass() != null
                ? entity.getImplementationClass().getQualifiedName() : null;
        List<CtType<?>> result = new ArrayList<>();
        for (SourceCompilationUnit cu : model.getAllCompilationUnits()) {
            for (CtType<?> type : cu.getRootTypes()) {
                if (!(type instanceof CtClass)) {
                    continue;
                }
                if (type.getQualifiedName().equals(currentImplQN)) {
                    continue;
                }
                result.add(type);
            }
        }
        result.sort((a, b) -> a.getQualifiedName().compareTo(b.getQualifiedName()));
        return result;
    }
}
