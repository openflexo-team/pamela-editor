package org.openflexo.pamela.editor.ui.widget;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.FIBJPanel;
import org.openflexo.pamela.editor.model.SourcePackage;
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

    @Override
    public Class<SourcePackage> getRepresentedType() {
        return SourcePackage.class;
    }

    @Override
    public void delete() {
    }
}
