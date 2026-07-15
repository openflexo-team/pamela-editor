package org.openflexo.pamela.editor.ui.action;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

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
    public ActionGroup getGroup() {
        return ActionGroup.NEW;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.SOURCE_FOLDER_ICON;
    }

    @Override
    public String getLabel() {
        return loc("add_source_folder_action");
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
     * <p>The chooser carries a "New folder…" accessory button so a brand-new
     * source directory can be created on the fly. The system (Aqua) file
     * chooser offers no folder-creation affordance in open mode, hence the
     * explicit accessory.</p>
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
        chooser.setAccessory(makeNewFolderAccessory(chooser));
        if (chooser.showOpenDialog(parentComponent) == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            if (dir != null && !model.getSourceDirectories().contains(dir)) {
                model.addSourceDirectory(dir);
            }
        }
    }

    /**
     * Builds the chooser accessory holding the "New folder…" button: prompts
     * for a name, creates the directory under the chooser's current location
     * (or under the selected directory when one is selected), and selects it
     * so that OK adds it directly.
     */
    private static JPanel makeNewFolderAccessory(JFileChooser chooser) {
        JButton newFolderButton = new JButton(loc("new_folder_button"));
        newFolderButton.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(chooser,
                    loc("new_folder_name_prompt"), loc("new_folder_title"), JOptionPane.PLAIN_MESSAGE);
            if (name == null) {
                return; // cancelled
            }
            name = name.trim();
            if (name.isEmpty() || name.contains(File.separator) || name.contains("/")) {
                JOptionPane.showMessageDialog(chooser,
                        loc("error_invalid_folder_name") + " " + name, loc("new_folder_title"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            File selected = chooser.getSelectedFile();
            File base = (selected != null && selected.isDirectory()) ? selected : chooser.getCurrentDirectory();
            File newDir = new File(base, name);
            if (newDir.exists()) {
                JOptionPane.showMessageDialog(chooser,
                        loc("error_folder_already_exists_prefix") + " '" + name + "' "
                                + loc("error_folder_already_exists_suffix"),
                        loc("new_folder_title"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!newDir.mkdirs()) {
                JOptionPane.showMessageDialog(chooser,
                        loc("error_creating_folder") + " " + newDir.getAbsolutePath(),
                        loc("new_folder_title"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            chooser.setSelectedFile(newDir);
            chooser.rescanCurrentDirectory();
        });
        JPanel accessory = new JPanel(new BorderLayout());
        accessory.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        accessory.add(newFolderButton, BorderLayout.SOUTH);
        return accessory;
    }
}
