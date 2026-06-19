package org.openflexo.pamela.editor.ui.preferences;

import javax.swing.JPanel;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.JFIBInspectorController;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.rm.ResourceLocator;

/**
 * Right-pane property editor for the preferences window. Loads every
 * {@code Inspectors/Preferences/*.inspector} once (indexed by {@code dataClassName}, merged
 * by Java type hierarchy — gina §19), so each theme type gets its editable inspector and the
 * base {@code PreferencesNode.inspector} is merged into all. Editable: writes go straight into
 * the live PAMELA node (immediate apply).
 */
public class PreferencesInspectorController {

    private final JFIBInspectorController inspectorController;

    public PreferencesInspectorController() {
        inspectorController = new JFIBInspectorController(
                ResourceLocator.locateResource("Inspectors/Preferences"),
                ApplicationFIBLibraryImpl.instance(),
                PamelaEditorFIBController.EDITOR_LOCALIZATION);
    }

    public void inspectObject(Object object) {
        inspectorController.inspectObject(object);
    }

    public JPanel getRootPane() {
        return inspectorController.getRootPane();
    }
}
