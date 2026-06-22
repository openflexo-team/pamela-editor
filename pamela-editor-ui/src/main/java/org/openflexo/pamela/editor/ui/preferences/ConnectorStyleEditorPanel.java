package org.openflexo.pamela.editor.ui.preferences;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.FIBJPanel;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Master-detail editor for the connector styles ({@link ConnectorStylePreferences}): a table of
 * styles with add/remove, a detail editor for the selected style, and the inheritance/association
 * default-style assignments. Shown in the preferences window's right pane in place of the generic
 * inspector for this node type (immediate apply — every edit writes through to the model).
 */
@SuppressWarnings("serial")
public class ConnectorStyleEditorPanel extends FIBJPanel<ConnectorStylePreferences> {

    public static final Resource FIB_FILE = ResourceLocator.locateResource("Fib/ConnectorStyleEditor.fib");

    public ConnectorStyleEditorPanel(ConnectorStylePreferences data) {
        super(FIB_FILE, data,
                ApplicationFIBLibraryImpl.instance(),
                PamelaEditorFIBController.EDITOR_LOCALIZATION);
    }

    @Override
    public Class<ConnectorStylePreferences> getRepresentedType() {
        return ConnectorStylePreferences.class;
    }

    @Override
    public void delete() {
    }
}
