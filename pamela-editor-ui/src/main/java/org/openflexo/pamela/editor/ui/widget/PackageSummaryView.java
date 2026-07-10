package org.openflexo.pamela.editor.ui.widget;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.FIBJPanel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Central tab view that displays a summary of a {@link SourcePackage}.
 *
 * <p>Shows the package name and the list of its entities with basic stats.</p>
 */
@SuppressWarnings("serial")
public class PackageSummaryView extends FIBJPanel<SourcePackage> {

    public static final Resource FIB_FILE =
            ResourceLocator.locateResource("Fib/PackageSummaryView.fib");

    public PackageSummaryView(SourcePackage pkg) {
        super(FIB_FILE, pkg,
              ApplicationFIBLibraryImpl.instance(),
              PamelaEditorFIBController.EDITOR_LOCALIZATION);
    }

    /**
     * Wires the application so that clicking/right-clicking a row in the entities
     * table can soft-select the entity and open the shared contextual menu
     * (ui-design.md §18.2/§18.4). Call this right after construction.
     */
    public void setApplication(PamelaEditorApplication application) {
        PackageSummaryViewFIBController ctrl =
                (PackageSummaryViewFIBController) getController();
        if (ctrl != null) {
            ctrl.setApplication(application);
        }
    }

    /** Highlights {@code entity}'s row in the entities table (no-op if it is not listed). */
    public void selectEntity(SourceModelEntity entity) {
        PackageSummaryViewFIBController ctrl =
                (PackageSummaryViewFIBController) getController();
        if (ctrl != null) {
            ctrl.setSelectedEntity(entity);
        }
    }

    @Override
    public Class<SourcePackage> getRepresentedType() {
        return SourcePackage.class;
    }

    @Override
    public void delete() {
    }
}
