package org.openflexo.pamela.editor.ui.widget;

import java.io.File;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;

import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
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
 * the read-only "root" checkbox column and the "+" footer action that opens
 * {@link AddAsRootTypeAction}'s own dialog to register a {@code @ModelEntity} type as a root type.</p>
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
     * Footer "+" action of the entities table. Delegates straight to
     * {@link AddAsRootTypeAction}, targeting the metamodel itself (there is no specific file in
     * this context) — its own dialog offers a project-wide {@code JavaClassSelector} over every
     * {@code @ModelEntity} candidate not already a root type, and goes through the same
     * rebuild/reselect machinery as the browser/diagram contextual menu entry.
     */
    public void addRootType() {
        SourceMetaModel model = getDataObject();
        if (model == null || application == null) {
            return;
        }
        if (AddAsRootTypeAction.candidateTypes(model).isEmpty()) {
            JOptionPane.showMessageDialog(getParentFrame(),
                    "No eligible Java type found (must declare @ModelEntity and not already be a root type).",
                    "Add root type", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new AddAsRootTypeAction().perform(model, application);
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
