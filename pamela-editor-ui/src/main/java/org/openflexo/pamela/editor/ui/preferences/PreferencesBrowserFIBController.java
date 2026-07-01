package org.openflexo.pamela.editor.ui.preferences;

import javax.swing.ImageIcon;

import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;

/**
 * FIB controller for the {@link PreferencesBrowser}. Holds the observable
 * {@code selectedNode} (two-way bound from the browser); {@link PreferencesDialog}
 * listens to it to drive the right-pane property editor.
 */
public class PreferencesBrowserFIBController extends PamelaEditorFIBController<PamelaEditorPreferencesModel> {

    public static final String SELECTED_NODE = "selectedNode";

    private Object selectedNode;

    public PreferencesBrowserFIBController(FIBComponent rootComponent) {
        super(rootComponent);
    }

    /** Browser icon per preferences node, resolved from the node's transient icon key. */
    @Override
    protected ImageIcon retrieveIconForObject(Object object) {
        if (object instanceof PreferencesNode) {
            String key = ((PreferencesNode) object).getIconKey();
            if (key != null) {
                switch (key) {
                    case "general":
                        return PamelaEditorIconLibrary.METAMODEL_ICON;
                    case "window":
                        return PamelaEditorIconLibrary.SESSION_ICON;
                    case "recent":
                        return PamelaEditorIconLibrary.JAVA_FILE_ICON;
                    case "diagram":
                        return PamelaEditorIconLibrary.DIAGRAM_ICON;
                    case "entity":
                        return PamelaEditorIconLibrary.SHAPE_ICON;
                    case "connector":
                        return PamelaEditorIconLibrary.CONNECTOR_ICON;
                    case "analysis":
                        return PamelaEditorIconLibrary.INSPECT_ICON;
                    case "generation":
                        return PamelaEditorIconLibrary.PROPERTY_ICON;
                    default:
                        break;
                }
            }
        }
        return super.retrieveIconForObject(object);
    }

    public Object getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(Object selectedNode) {
        Object old = this.selectedNode;
        this.selectedNode = selectedNode;
        getPropertyChangeSupport().firePropertyChange(SELECTED_NODE, old, selectedNode);
    }
}
