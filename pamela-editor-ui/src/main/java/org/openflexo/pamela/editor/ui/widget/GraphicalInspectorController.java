package org.openflexo.pamela.editor.ui.widget;

import java.util.logging.Logger;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.JFIBInspectorController;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.rm.ResourceLocator;

/**
 * Manages the graphical inspector panel (bottom-right column, diagram mode).
 *
 * Mirrors {@link PamelaEditorInspectorController} but points to the
 * {@code Inspectors/Graphical/} directory, which contains inspectors for diagram
 * model objects ({@code EntityView}, {@code PamelaClassDiagram}).
 *
 * Calling {@link #inspectObject(Object)} selects the most specific graphical
 * inspector for the given object's runtime type (e.g. {@code EntityView} →
 * editable x/y/width/height; {@code PamelaClassDiagram} → name and entity count).
 */
public class GraphicalInspectorController {

    @SuppressWarnings("unused")
    private static final Logger logger =
            Logger.getLogger(GraphicalInspectorController.class.getPackage().getName());

    private final JFIBInspectorController inspectorController;

    public GraphicalInspectorController() {
        inspectorController = new JFIBInspectorController(
                ResourceLocator.locateResource("Inspectors/Graphical"),
                ApplicationFIBLibraryImpl.instance(),
                PamelaEditorFIBController.EDITOR_LOCALIZATION);
    }

    /**
     * Switch the graphical inspector to show the most specific inspector for
     * {@code object}. Passing {@code null} (or an unrecognised type) shows the
     * "No selection" empty panel.
     */
    public void inspectObject(Object object) {
        inspectorController.inspectObject(object);
    }

    /**
     * Returns the Swing root pane managed by this controller.
     * Embed this panel in the bottom-right area of the application.
     */
    public javax.swing.JPanel getRootPane() {
        return inspectorController.getRootPane();
    }
}
