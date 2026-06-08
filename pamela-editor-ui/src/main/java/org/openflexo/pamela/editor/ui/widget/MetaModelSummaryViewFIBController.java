package org.openflexo.pamela.editor.ui.widget;

import java.io.File;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;

import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.pamela.editor.ui.action.AddSourceFolderAction;

/**
 * FIB controller for {@link MetaModelSummaryView}.
 *
 * <p>Provides add/remove actions for the source directories and root type names
 * tables.  Both lists are mutable inputs that define what Spoon will analyse
 * when the project is (re)built.</p>
 */
public class MetaModelSummaryViewFIBController extends PamelaEditorFIBController<SourceMetaModel> {

    private static final Logger logger =
            Logger.getLogger(MetaModelSummaryViewFIBController.class.getPackage().getName());

    /** Directory of the .pamela file — used as starting directory for the file chooser. */
    private File projectDirectory;

    public void setProjectDirectory(File projectDirectory) {
        this.projectDirectory = projectDirectory;
    }

    public Icon getSourceFolderIcon(File dir) {
        return PamelaEditorIconLibrary.SOURCE_FOLDER_ICON;
    }

    public Icon getRootTypeIcon(String typeName) {
        return PamelaEditorIconLibrary.ENTITY_ICON;
    }

    public MetaModelSummaryViewFIBController(FIBComponent rootComponent) {
        super(rootComponent);
    }

    // -------------------------------------------------------------------------
    // Source directory actions
    // -------------------------------------------------------------------------

    /**
     * Opens a directory chooser dialog and adds the selected directory to the
     * metamodel's source directory list.
     * Delegates to {@link AddSourceFolderAction#addSourceFolder} to avoid duplication.
     */
    public void addSourceDirectory() {
        AddSourceFolderAction.addSourceFolder(getDataObject(), projectDirectory, getParentFrame());
    }

    /**
     * Removes {@code directory} from the metamodel's source directory list.
     * Called from the table's remove action with the currently selected row.
     */
    public void removeSourceDirectory(File directory) {
        SourceMetaModel model = getDataObject();
        if (model != null && directory != null) {
            model.removeSourceDirectory(directory);
        }
    }

    // -------------------------------------------------------------------------
    // Root type name actions
    // -------------------------------------------------------------------------

    /**
     * Opens an input dialog asking for a fully qualified class name and adds it
     * to the metamodel's root type list.
     */
    public void addRootTypeName() {
        SourceMetaModel model = getDataObject();
        if (model == null) {
            return;
        }
        JFrame parent = getParentFrame();
        String name = (String) JOptionPane.showInputDialog(
                parent,
                "Enter the fully qualified class name:",
                "Add root type",
                JOptionPane.PLAIN_MESSAGE,
                null, null, "org.example.MyEntity");
        if (name != null && !name.trim().isEmpty() && !model.getRootTypeNames().contains(name.trim())) {
            model.addRootTypeName(name.trim());
        }
    }

    /**
     * Removes {@code typeName} from the metamodel's root type list.
     * Called from the table's remove action with the currently selected row.
     */
    public void removeRootTypeName(String typeName) {
        SourceMetaModel model = getDataObject();
        if (model != null && typeName != null) {
            model.removeRootTypeName(typeName);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private JFrame getParentFrame() {
        try {
            if (getRootView() != null) {
                java.awt.Component comp = (java.awt.Component) getRootView().getTechnologyComponent();
                if (comp != null) {
                    java.awt.Window w = SwingUtilities.getWindowAncestor(comp);
                    if (w instanceof JFrame) {
                        return (JFrame) w;
                    }
                }
            }
        } catch (Exception ignored) {
            // fall through to null
        }
        return null;
    }
}
