package org.openflexo.pamela.editor.ui.widget;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;

import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.model.SourceJavaFile;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.pamela.editor.ui.action.AddAsRootTypeAction;
import org.openflexo.pamela.editor.ui.action.AddSourceFolderAction;

/**
 * FIB controller for {@link MetaModelSummaryView}.
 *
 * <p>Provides add/remove actions for the source directories table. That list is a
 * mutable input that defines what Spoon will analyse when the project is (re)built.</p>
 *
 * <p>Also drives the "all entities" table — icon resolution + click/right-click (see
 * {@link EntityTableSupport}), mirroring {@link PackageSummaryViewFIBController} — plus
 * the read-only "root" checkbox column and the "+" footer action that registers an
 * eligible {@link SourceJavaFile} as a root type, via {@link AddAsRootTypeAction}.</p>
 */
public class MetaModelSummaryViewFIBController extends PamelaEditorFIBController<SourceMetaModel> {

    private static final Logger logger =
            Logger.getLogger(MetaModelSummaryViewFIBController.class.getPackage().getName());

    /** Directory of the .pamela file — used as starting directory for the file chooser. */
    private File projectDirectory;

    private PamelaEditorApplication application;

    public void setProjectDirectory(File projectDirectory) {
        this.projectDirectory = projectDirectory;
    }

    public void setApplication(PamelaEditorApplication application) {
        this.application = application;
    }

    public Icon getSourceFolderIcon(File dir) {
        return PamelaEditorIconLibrary.SOURCE_FOLDER_ICON;
    }

    public MetaModelSummaryViewFIBController(FIBComponent rootComponent) {
        super(rootComponent);
    }

    @Override
    protected ImageIcon retrieveIconForObject(Object object) {
        if (object instanceof SourceModelEntity) {
            SourceModelEntity entity = (SourceModelEntity) object;
            return entity.isAbstract()
                    ? PamelaEditorIconLibrary.ABSTRACT_ENTITY_ICON
                    : PamelaEditorIconLibrary.ENTITY_ICON;
        }
        return super.retrieveIconForObject(object);
    }

    /** Called when the user clicks a row in the entities table (FIB {@code clickAction}). */
    public void selectEntity(Object selected) {
        EntityTableSupport.selectEntity(application, selected);
    }

    /** Called when the user right-clicks a row in the entities table (FIB {@code rightClickAction}). */
    public void rightClick(Object selected, Object event) {
        EntityTableSupport.rightClick(application, selected, event);
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
    // Root type status (entities table)
    // -------------------------------------------------------------------------

    /**
     * Whether {@code entity} is currently registered as a root type of its metamodel
     * (bound as the read-only "root" checkbox column of the entities table).
     */
    public boolean isRootType(SourceModelEntity entity) {
        if (entity == null || entity.getMetaModel() == null) {
            return false;
        }
        return entity.getMetaModel().getRootTypeNames().contains(entity.getQualifiedName());
    }

    /**
     * Footer "+" action of the entities table. Lets the user pick, among the Java
     * files that contain an {@code @ModelEntity} annotation and are not already a
     * root type, the one to register — delegating the actual mutation to
     * {@link AddAsRootTypeAction} so it goes through the same rebuild/reselect
     * machinery as the browser/diagram contextual menu entry.
     */
    public void addRootType() {
        SourceMetaModel model = getDataObject();
        if (model == null || application == null) {
            return;
        }
        Map<String, SourceJavaFile> candidatesByName = new TreeMap<>();
        for (SourcePackage pkg : model.getAllPackages()) {
            for (SourceJavaFile file : pkg.getJavaFiles()) {
                if (file.isPotentialModelEntity()
                        && !model.getRootTypeNames().contains(file.getQualifiedName())) {
                    candidatesByName.put(file.getQualifiedName(), file);
                }
            }
        }
        JFrame parent = getParentFrame();
        if (candidatesByName.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "No eligible Java file found (must declare @ModelEntity and not already be a root type).",
                    "Add root type", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] names = candidatesByName.keySet().toArray(new String[0]);
        String chosen = (String) JOptionPane.showInputDialog(
                parent,
                "Select the Java type to add as a root type:",
                "Add root type",
                JOptionPane.PLAIN_MESSAGE,
                null, names, names[0]);
        if (chosen != null) {
            new AddAsRootTypeAction().perform(candidatesByName.get(chosen), application);
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
