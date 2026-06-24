package org.openflexo.pamela.editor.ui.action;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Contextual action: opens a directory chooser and adds the selected directory
 * as a source folder of the target metamodel.
 *
 * <p>Applies to {@link PamelaProject} and {@link SourceMetaModel} nodes.</p>
 *
 * <p>The core logic lives in the static helper
 * {@link #addSourceFolder(SourceMetaModel, File, Component)} so that
 * {@code MetaModelSummaryViewFIBController} can delegate to it without
 * duplicating code.</p>
 */
public class AddSourceFolderAction extends ContextualAction {

    @Override
    public String getLabel() {
        return "Add Source Folder…";
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof PamelaProject
                || target instanceof SourceMetaModel;
    }

    @Override
    protected void doPerform(Object target, PamelaEditorApplication app) {
        SourceMetaModel model;
        File projectDir = null;

        if (target instanceof PamelaProject) {
            PamelaProject project = (PamelaProject) target;
            model = project.getMetaModel();
            if (project.getPamelaFile() != null) {
                projectDir = project.getPamelaFile().getParentFile();
            }
        } else {
            model = (SourceMetaModel) target;
            // Resolve the project directory from the owning project
            for (PamelaProject s : app.getProjects()) {
                if (s.getMetaModel() == model && s.getPamelaFile() != null) {
                    projectDir = s.getPamelaFile().getParentFile();
                    break;
                }
            }
        }

        addSourceFolder(model, projectDir, app.getFrame());
    }

    // =========================================================================
    // Shared static helper — also used by MetaModelSummaryViewFIBController
    // =========================================================================

    /**
     * Opens a directory-chooser dialog and adds the chosen directory to
     * {@code model} if it is not already registered.
     *
     * @param model          the metamodel to update (must not be null)
     * @param projectDir     starting directory for the chooser (may be null)
     * @param parentComponent parent Swing component for dialog centering (may be null)
     */
    public static void addSourceFolder(SourceMetaModel model,
                                       File projectDir,
                                       Component parentComponent) {
        if (model == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select source directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (projectDir != null && projectDir.exists()) {
            chooser.setCurrentDirectory(projectDir);
        }
        if (chooser.showOpenDialog(parentComponent) == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            if (dir != null && !model.getSourceDirectories().contains(dir)) {
                model.addSourceDirectory(dir);
            }
        }
    }
}
