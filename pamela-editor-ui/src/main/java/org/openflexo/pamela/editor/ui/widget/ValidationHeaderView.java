package org.openflexo.pamela.editor.ui.widget;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.FIBJPanel;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Always-visible strip at the very bottom of the center column, below the
 * {@code JSplitPane} holding the collapsible {@link ValidationPanel} issues table
 * (validation-log-panel-design.md §6.6). Shows the per-severity issue summary and a
 * Revalidate hyperlink regardless of whether the table is collapsed or expanded.
 */
@SuppressWarnings("serial")
public class ValidationHeaderView extends FIBJPanel<SourceMetaModel> {

    public static final Resource FIB_FILE =
            ResourceLocator.locateResource("Fib/ValidationHeaderView.fib");

    public ValidationHeaderView(SourceMetaModel metaModel) {
        super(FIB_FILE, metaModel,
              ApplicationFIBLibraryImpl.instance(),
              ValidationFIBController.EDITOR_LOCALIZATION);
    }

    /**
     * Wires the application so the Revalidate link can act on the owning project (and expand
     * the issues table to show the fresh results). Call this right after construction.
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
