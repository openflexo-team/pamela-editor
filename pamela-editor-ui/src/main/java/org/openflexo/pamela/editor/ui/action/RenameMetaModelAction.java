package org.openflexo.pamela.editor.ui.action;

import java.io.File;
import java.util.function.Supplier;

import javax.swing.ImageIcon;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Renames a {@link SourceMetaModel}'s display name (persisted in the {@code .pamela} project
 * file), and — optionally — the {@code .pamela} file on disk as well.
 *
 * <p>A regular {@link ParameteredAction} (same FIB-dialog shell + Validate/Cancel plumbing as
 * every other parameter dialog, {@code model-editing-design.md §5}), with one difference: it
 * renames <em>project metadata</em>, not {@code .java} source, so it overrides
 * {@link #needsRebuild()} to {@code false} — no Spoon re-analysis is needed after the rename.</p>
 *
 * <p>The dialog ({@code RenameMetaModelForm.fib}) offers: the new display name; a checkbox to
 * also rename the {@code .pamela} file; and the new file base-name (extension shown as a
 * trailing {@code .pamela} label), defaulted from the new display name and editable, enabled
 * only while the checkbox is ticked. The apply itself lives in
 * {@link PamelaEditorApplication#applyMetaModelRename}.</p>
 */
public class RenameMetaModelAction extends ParameteredAction {

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/RenameMetaModelForm.fib");

    // Resolved in prepareDialog, reused in applyMutation (same fresh instance).
    private SourceMetaModel metaModel;
    private PamelaProject project;

    // Bound parameters.
    private String newName = "";
    private boolean renameFile = false;
    private String newFileName = "";
    // Tracks the last auto-suggested file name, so we stop overriding a user-customized one.
    private String lastAutoFileName = "";

    @Override
    public ActionGroup getGroup() {
        return ActionGroup.REFACTOR;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.METAMODEL_ICON;
    }

    @Override
    public String getLabel() {
        return loc("rename_metamodel_action");
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceMetaModel || target instanceof PamelaProject;
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected String getDialogTitle() {
        return loc("rename_metamodel_dialog_title");
    }

    /** Project metadata, not source — no Spoon re-analysis needed after the rename. */
    @Override
    protected boolean needsRebuild() {
        return false;
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        metaModel = target instanceof PamelaProject
                ? ((PamelaProject) target).getMetaModel()
                : (SourceMetaModel) target;
        project = app.getProjectForElement(metaModel);
        if (metaModel == null || project == null) {
            return false;
        }
        newName = metaModel.getName() != null ? metaModel.getName() : "";
        renameFile = false;
        newFileName = defaultFileName(newName);
        lastAutoFileName = newFileName;
        return true;
    }

    @Override
    public boolean isInputValid() {
        if (newName.trim().isEmpty()) {
            return false;
        }
        if (renameFile) {
            String base = newFileName.trim();
            if (base.isEmpty()) {
                return false;
            }
            // Reject a name that collides with an existing, different file. (project is set in
            // prepareDialog before the dialog is shown; guard for null so the validity check is
            // exercisable in isolation.)
            if (project != null && project.getPamelaFile() != null) {
                File target = new File(project.getPamelaFile().getParentFile(), base + ".pamela");
                if (!target.equals(project.getPamelaFile()) && target.exists()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        String name = newName.trim();
        File newFile = null;
        if (renameFile) {
            String base = newFileName.trim();
            if (!base.isEmpty()) {
                newFile = new File(project.getPamelaFile().getParentFile(), base + ".pamela");
            }
        }
        app.applyMetaModelRename(metaModel, name, newFile);
        return null; // no reselection (the project stays selected)
    }

    // --- bound by RenameMetaModelForm.fib -----------------------------------

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName != null ? newName : "";
        // Keep the file base-name in sync with the display name until the user customizes it.
        String auto = defaultFileName(this.newName);
        if (newFileName == null || newFileName.equals(lastAutoFileName)) {
            newFileName = auto;
            getPropertyChangeSupport().firePropertyChange("newFileName", null, newFileName);
        }
        lastAutoFileName = auto;
        fireInputValidChanged();
    }

    public boolean getRenameFile() {
        return renameFile;
    }

    public void setRenameFile(boolean renameFile) {
        this.renameFile = renameFile;
        getPropertyChangeSupport().firePropertyChange("renameFile", !renameFile, renameFile);
        fireInputValidChanged();
    }

    public String getNewFileName() {
        return newFileName;
    }

    public void setNewFileName(String newFileName) {
        this.newFileName = newFileName != null ? newFileName : "";
        fireInputValidChanged();
    }

    // -------------------------------------------------------------------------

    /** Default {@code .pamela} base-name (no extension) computed from the display name. */
    private static String defaultFileName(String displayName) {
        return displayName != null ? displayName.trim() : "";
    }
}
