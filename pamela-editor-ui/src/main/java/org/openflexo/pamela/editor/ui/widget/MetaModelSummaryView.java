package org.openflexo.pamela.editor.ui.widget;

import java.io.File;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.FIBJPanel;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.widget.MetaModelSummaryViewFIBController;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Central tab view that displays a summary of a {@link SourceMetaModel}.
 *
 * <p>Shown when a session node or the meta-model node is selected in the
 * browser.  Displays project name, package list, entity count, and issue
 * summary.</p>
 */
@SuppressWarnings("serial")
public class MetaModelSummaryView extends FIBJPanel<SourceMetaModel> {

    public static final Resource FIB_FILE =
            ResourceLocator.locateResource("Fib/MetaModelSummaryView.fib");

    public MetaModelSummaryView(SourceMetaModel metaModel) {
        super(FIB_FILE, metaModel,
              ApplicationFIBLibraryImpl.instance(),
              MetaModelSummaryViewFIBController.EDITOR_LOCALIZATION);
    }

    /**
     * Sets the project directory used as the starting point for the source
     * directory file chooser.  Call this after construction when the view is
     * bound to a {@code PamelaProject} so the chooser opens at the right place.
     */
    public void setProjectDirectory(File projectDirectory) {
        MetaModelSummaryViewFIBController ctrl =
                (MetaModelSummaryViewFIBController) getController();
        if (ctrl != null) {
            ctrl.setProjectDirectory(projectDirectory);
        }
    }

    /**
     * Wires the application so that clicking/right-clicking a row in the entities table
     * can soft-select the entity and open the shared contextual menu (ui-design.md
     * §18.2/§18.4). Call this right after construction.
     */
    public void setApplication(PamelaEditorApplication application) {
        MetaModelSummaryViewFIBController ctrl =
                (MetaModelSummaryViewFIBController) getController();
        if (ctrl != null) {
            ctrl.setApplication(application);
        }
    }

    @Override
    public Class<SourceMetaModel> getRepresentedType() {
        return SourceMetaModel.class;
    }

    @Override
    public void delete() {
    }
}
