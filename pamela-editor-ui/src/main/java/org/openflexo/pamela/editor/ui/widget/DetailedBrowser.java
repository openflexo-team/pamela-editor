package org.openflexo.pamela.editor.ui.widget;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.FIBJPanel;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Left-bottom panel.  Shows the content of the currently selected element.
 *
 * <p>The data object is the currently selected element itself (typed as
 * {@link Object} since it can be anything).  It is rebound every time the
 * global selection changes via {@link #setEditedObject(Object)}.</p>
 *
 * <p>The FIB file uses conditional visibility on each browser section to
 * display the appropriate content based on the runtime type of the data object.</p>
 */
@SuppressWarnings("serial")
public class DetailedBrowser extends FIBJPanel<Object> {

    public static final Resource FIB_FILE =
            ResourceLocator.locateResource("Fib/DetailedBrowser.fib");

    public DetailedBrowser(PamelaEditorApplication application) {
        super(FIB_FILE, null,
              ApplicationFIBLibraryImpl.instance(),
              PamelaEditorFIBController.EDITOR_LOCALIZATION);
        // Give the controller a back-reference to the application so that
        // single-click in the detailed browser propagates the selection.
        getController().setApplication(application);
    }

    @Override
    public DetailedBrowserFIBController getController() {
        return (DetailedBrowserFIBController) super.getController();
    }

    @Override
    public Class<Object> getRepresentedType() {
        return Object.class;
    }

    @Override
    public void delete() {
    }
}
