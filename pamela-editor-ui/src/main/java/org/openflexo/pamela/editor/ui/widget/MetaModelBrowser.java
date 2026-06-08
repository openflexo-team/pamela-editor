package org.openflexo.pamela.editor.ui.widget;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.FIBJPanel;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Left-top browser panel.  Shows all open sessions and their content as a tree:
 * <pre>
 * PamelaProject (project name)
 * ├── Diagrams
 * │   └── PamelaClassDiagram
 * └── Packages
 *     └── SourcePackage
 *         └── SourceModelEntity
 * </pre>
 *
 * <p>The data object is the {@link PamelaEditorApplication} itself (so the
 * FIB can reach {@code data.sessions}).</p>
 *
 * <p>Single-click propagates via {@link MetaModelBrowserFIBController#getSelectedElement()}
 * property-change events to {@link PamelaEditorApplication#setCurrentSelectedElement(Object)}.</p>
 */
@SuppressWarnings("serial")
public class MetaModelBrowser extends FIBJPanel<PamelaEditorApplication> {

    public static final Resource FIB_FILE =
            ResourceLocator.locateResource("Fib/MetaModelBrowser.fib");

    public MetaModelBrowser(PamelaEditorApplication application) {
        super(FIB_FILE, application,
              ApplicationFIBLibraryImpl.instance(),
              PamelaEditorFIBController.EDITOR_LOCALIZATION);
    }

    @Override
    public MetaModelBrowserFIBController getController() {
        return (MetaModelBrowserFIBController) super.getController();
    }

    @Override
    public Class<PamelaEditorApplication> getRepresentedType() {
        return PamelaEditorApplication.class;
    }

    @Override
    public void delete() {
    }
}
