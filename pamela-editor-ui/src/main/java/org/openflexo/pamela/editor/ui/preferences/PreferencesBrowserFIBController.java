package org.openflexo.pamela.editor.ui.preferences;

import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;

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

    public Object getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(Object selectedNode) {
        Object old = this.selectedNode;
        this.selectedNode = selectedNode;
        getPropertyChangeSupport().firePropertyChange(SELECTED_NODE, old, selectedNode);
    }
}
