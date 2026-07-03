package org.openflexo.pamela.editor.ui.widget;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.FIBJPanel;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Bottom-of-center-column strip showing the active project's validation issues
 * (validation-log-panel-design.md). Bound to a {@link SourceMetaModel}; rebound whenever the
 * central view switches to an element of a different project.
 */
@SuppressWarnings("serial")
public class ValidationPanel extends FIBJPanel<SourceMetaModel> {

    public static final Resource FIB_FILE =
            ResourceLocator.locateResource("Fib/ValidationPanel.fib");

    public ValidationPanel(SourceMetaModel metaModel) {
        super(FIB_FILE, metaModel,
              ApplicationFIBLibraryImpl.instance(),
              ValidationFIBController.EDITOR_LOCALIZATION);
    }

    /**
     * Wires the application so the issues table can navigate to an issue's source element and
     * the Revalidate/Hide links can act on the owning project. Call this right after
     * construction.
     */
    public void setApplication(PamelaEditorApplication application) {
        ValidationFIBController ctrl = (ValidationFIBController) getController();
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
