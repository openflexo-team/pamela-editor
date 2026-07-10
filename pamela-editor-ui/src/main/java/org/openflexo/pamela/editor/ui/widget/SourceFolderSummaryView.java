package org.openflexo.pamela.editor.ui.widget;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.FIBJPanel;
import org.openflexo.pamela.editor.model.SourceFolder;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Central tab view that displays a summary of a {@link SourceFolder}.
 *
 * <p>Shows the source directory name/path and the list of all entities discovered across
 * every package it contributes to, with basic stats. Mirrors {@link PackageSummaryView}.</p>
 */
@SuppressWarnings("serial")
public class SourceFolderSummaryView extends FIBJPanel<SourceFolder> {

    public static final Resource FIB_FILE =
            ResourceLocator.locateResource("Fib/SourceFolderSummaryView.fib");

    public SourceFolderSummaryView(SourceFolder folder) {
        super(FIB_FILE, folder,
              ApplicationFIBLibraryImpl.instance(),
              PamelaEditorFIBController.EDITOR_LOCALIZATION);
    }

    /**
     * Wires the application so that clicking/right-clicking a row in the entities
     * table can soft-select the entity and open the shared contextual menu
     * (ui-design.md §18.2/§18.4). Call this right after construction.
     */
    public void setApplication(PamelaEditorApplication application) {
        SourceFolderSummaryViewFIBController ctrl =
                (SourceFolderSummaryViewFIBController) getController();
        if (ctrl != null) {
            ctrl.setApplication(application);
        }
    }

    /** Highlights {@code entity}'s row in the entities table (no-op if it is not listed). */
    public void selectEntity(SourceModelEntity entity) {
        SourceFolderSummaryViewFIBController ctrl =
                (SourceFolderSummaryViewFIBController) getController();
        if (ctrl != null) {
            ctrl.setSelectedEntity(entity);
        }
    }

    @Override
    public Class<SourceFolder> getRepresentedType() {
        return SourceFolder.class;
    }

    @Override
    public void delete() {
    }
}
