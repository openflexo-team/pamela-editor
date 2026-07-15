package org.openflexo.pamela.editor.ui.action;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.openflexo.pamela.editor.model.SourceFolder;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Creates a brand-new, initially empty package under a {@link SourceFolder} —
 * see {@code SourceMetaModel#createPackage} for why an empty package needs a
 * {@code package-info.java} stub to survive the rebuild that follows every
 * model-editing action.
 *
 * <p>Applicable on a {@link SourceFolder} (pre-fills it) or on the project root
 * ({@link SourceMetaModel} / {@link PamelaProject}, nothing pre-filled — the
 * user picks the folder explicitly, matching {@code NewEntityAction}'s
 * convention). See {@code model-editing-design.md} (NewPackageAction section).</p>
 *
 * <p>Flat action: extends {@link ParameteredAction} directly and references its
 * own form fragment ({@code NewPackageForm.fib}).</p>
 */
public class NewPackageAction extends ParameteredAction {

    @Override
    public ActionGroup getGroup() {
        return ActionGroup.NEW;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.PACKAGE_ICON;
    }

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/NewPackageForm.fib");

    private SourceMetaModel metaModel;
    private SourceFolder sourceFolder;
    private String name = "";
    private Set<String> forbidden = Collections.emptySet();

    @Override
    public String getLabel() {
        return loc("new_package_action");
    }

    @Override
    public boolean isApplicable(Object target) {
        return resolveMetaModel(target) != null;
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected String getDialogTitle() {
        return loc("new_package_title");
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        SourceMetaModel model = resolveMetaModel(target);
        if (model == null) {
            return false;
        }
        metaModel = model;
        name = "";
        sourceFolder = target instanceof SourceFolder ? (SourceFolder) target : null;
        recomputeForbidden();
        return true;
    }

    @Override
    public boolean isInputValid() {
        return sourceFolder != null && ModelEditingSupport.isAvailablePackageName(name, forbidden);
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceMetaModel model = project.getMetaModel();
        final String qualifiedName = name.trim();
        model.createPackage(qualifiedName, sourceFolder);
        return () -> model.getPackage(qualifiedName);
    }

    // --- bound by NewPackageForm.fib -----------------------------------------

    public SourceMetaModel getMetaModel() {
        return metaModel;
    }

    public SourceFolder getSourceFolder() {
        return sourceFolder;
    }

    public void setSourceFolder(SourceFolder sourceFolder) {
        SourceFolder old = this.sourceFolder;
        this.sourceFolder = sourceFolder;
        getPropertyChangeSupport().firePropertyChange("sourceFolder", old, sourceFolder);
        recomputeForbidden();
        fireInputValidChanged();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        fireInputValidChanged();
    }

    // -------------------------------------------------------------------------

    private void recomputeForbidden() {
        Set<String> names = new HashSet<>();
        if (sourceFolder != null) {
            for (SourcePackage p : sourceFolder.getPackages()) {
                names.add(p.getQualifiedName());
            }
        }
        forbidden = names;
    }

    private static SourceMetaModel resolveMetaModel(Object target) {
        if (target instanceof SourceFolder) {
            return ((SourceFolder) target).getMetaModel();
        }
        if (target instanceof SourceMetaModel) {
            return (SourceMetaModel) target;
        }
        if (target instanceof PamelaProject) {
            return ((PamelaProject) target).getMetaModel();
        }
        return null;
    }
}
