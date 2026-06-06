package org.openflexo.pamela.editor.ui.widget;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.FIBJPanel;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
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
              PamelaEditorFIBController.EDITOR_LOCALIZATION);
    }

    @Override
    public Class<SourceMetaModel> getRepresentedType() {
        return SourceMetaModel.class;
    }

    @Override
    public void delete() {
    }
}
