package org.openflexo.pamela.editor.ui.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.swing.ImageIcon;

import org.openflexo.icon.IconMarker;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.editor.model.SourceCompilationUnit;
import org.openflexo.pamela.editor.model.SourceJavaFile;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

import spoon.reflect.declaration.CtType;

/**
 * Registers a {@code @ModelEntity}-annotated Java type as a root type of its metamodel, so it
 * enters the model on the next rebuild (the bootstrap entry point for a type that is not yet
 * reachable from any existing root — {@code model-editing-design.md §3.1}).
 *
 * <p>Opens a dialog with a {@link org.openflexo.pamela.editor.ui.widget.JavaClassSelector} scoped
 * to the <em>whole project</em> — not just the clicked file's package — so the user can search or
 * browse (source folder → package → class) across every {@code @ModelEntity} candidate. Nothing is
 * pre-selected: the user must always explicitly pick a type before the OK button enables, even when
 * the dialog was opened from a specific candidate file. Reachable two ways, both landing on the same
 * dialog:</p>
 * <ul>
 * <li>right-click a {@link SourceJavaFile} that is a potential entity and not yet a root;</li>
 * <li>the entities table's "+" footer action ({@code MetaModelSummaryViewFIBController#addRootType()}),
 * targeting the {@link SourceMetaModel} directly — there is no specific file in that context.</li>
 * </ul>
 */
public class AddAsRootTypeAction extends ParameteredAction {

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/AddAsRootTypeForm.fib");

    private SourceMetaModel metaModel;
    private List<CtType<?>> candidateTypes = Collections.emptyList();
    private CtType<?> selectedType;

    @Override
    public ActionGroup getGroup() {
        return ActionGroup.PROMOTE;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.ENTITY_ICON;
    }

    @Override
    protected IconMarker[] getMarkers() {
        return new IconMarker[] { PamelaEditorIconLibrary.REINJECT };
    }

    @Override
    public String getLabel() {
        return "Add as Root Type…";
    }

    @Override
    public boolean isApplicable(Object target) {
        if (target instanceof SourceMetaModel) {
            return !candidateTypes((SourceMetaModel) target).isEmpty();
        }
        if (target instanceof SourceJavaFile) {
            SourceJavaFile file = (SourceJavaFile) target;
            if (!file.isPotentialModelEntity()) {
                return false;
            }
            SourceMetaModel model = file.getMetaModel();
            return model != null && !model.getRootTypeNames().contains(file.getQualifiedName());
        }
        return false;
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected String getDialogTitle() {
        return "Add as Root Type";
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        metaModel = target instanceof SourceJavaFile ? ((SourceJavaFile) target).getMetaModel()
                : (target instanceof SourceMetaModel ? (SourceMetaModel) target : null);
        if (metaModel == null) {
            return false;
        }
        candidateTypes = candidateTypes(metaModel);
        if (candidateTypes.isEmpty()) {
            return false;
        }
        // No default pick — the user must explicitly choose a type in the JavaClassSelector,
        // even when the dialog was opened on a specific candidate SourceJavaFile.
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
        if (selectedType == null) {
            return null;
        }
        final String qualifiedName = selectedType.getQualifiedName();
        final SourceMetaModel model = metaModel;
        model.addRootTypeName(qualifiedName);
        return () -> model.getEntity(qualifiedName);
    }

    // --- bound by AddAsRootTypeForm.fib -------------------------------------

    /** Browser scope for the {@code JavaClassSelector} — the whole project. */
    public SourceMetaModel getMetaModel() {
        return metaModel;
    }

    /** Candidate types — the {@code JavaClassSelector} restriction. */
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
     * Every {@code @ModelEntity}-annotated type Spoon currently knows about, across the whole
     * metamodel, that is not already registered as a root type. Sorted by qualified name for a
     * stable, predictable browsing/completion order.
     */
    public static List<CtType<?>> candidateTypes(SourceMetaModel model) {
        Set<String> roots = new HashSet<>(model.getRootTypeNames());
        List<CtType<?>> result = new ArrayList<>();
        for (SourceCompilationUnit cu : model.getAllCompilationUnits()) {
            for (CtType<?> type : cu.getRootTypes()) {
                if (roots.contains(type.getQualifiedName())) {
                    continue;
                }
                if (type.getAnnotation(ModelEntity.class) == null) {
                    continue;
                }
                result.add(type);
            }
        }
        result.sort((a, b) -> a.getQualifiedName().compareTo(b.getQualifiedName()));
        return result;
    }
}
