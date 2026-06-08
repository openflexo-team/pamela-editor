package org.openflexo.pamela.editor.ui.widget;

import java.util.logging.Logger;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.JFIBInspectorController;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.rm.ResourceLocator;

/**
 * Manages the inspector panel (right top column) for the PAMELA editor.
 *
 * Uses the gina {@link InspectorGroup} / {@link JFIBInspectorController} mechanism:
 * all {@code .inspector} files from {@code Inspectors/PamelaModel/} are loaded once,
 * indexed by their {@code dataClassName}, and merged automatically according to the
 * Java type hierarchy of the inspected object. Calling {@link #inspectObject(Object)}
 * switches to the most specific available inspector for that object's runtime type.
 */
public class PamelaEditorInspectorController {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(PamelaEditorInspectorController.class.getPackage().getName());

    private final JFIBInspectorController inspectorController;

    public PamelaEditorInspectorController() {
        inspectorController = new JFIBInspectorController(
                ResourceLocator.locateResource("Inspectors/PamelaModel"),
                ApplicationFIBLibraryImpl.instance(),
                PamelaEditorFIBController.EDITOR_LOCALIZATION);
    }

    /**
     * Switch the inspector panel to show the most specific inspector for {@code object}.
     * If no inspector is registered for the object's type (or any of its supertypes),
     * the panel shows "No selection".
     */
    public void inspectObject(Object object) {
        inspectorController.inspectObject(object);
    }

    /**
     * Returns the Swing root pane managed by this controller.
     * Embed this panel in the right-top area of the application.
     */
    public javax.swing.JPanel getRootPane() {
        return inspectorController.getRootPane();
    }
}
