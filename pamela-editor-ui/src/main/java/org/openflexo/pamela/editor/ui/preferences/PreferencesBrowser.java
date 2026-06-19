package org.openflexo.pamela.editor.ui.preferences;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.FIBJPanel;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Left-pane tree browser of the preferences themes (see {@code ui-design.md},
 * {@code preferences-design.md §3.1}). Data object is the {@link PamelaEditorPreferencesModel};
 * the root is hidden, its children are the top-level themes.
 */
@SuppressWarnings("serial")
public class PreferencesBrowser extends FIBJPanel<PamelaEditorPreferencesModel> {

    public static final Resource FIB_FILE = ResourceLocator.locateResource("Fib/PreferencesBrowser.fib");

    public PreferencesBrowser(PamelaEditorPreferencesModel data) {
        super(FIB_FILE, data,
                ApplicationFIBLibraryImpl.instance(),
                PamelaEditorFIBController.EDITOR_LOCALIZATION);
    }

    @Override
    public PreferencesBrowserFIBController getController() {
        return (PreferencesBrowserFIBController) super.getController();
    }

    @Override
    public Class<PamelaEditorPreferencesModel> getRepresentedType() {
        return PamelaEditorPreferencesModel.class;
    }

    @Override
    public void delete() {
    }
}
