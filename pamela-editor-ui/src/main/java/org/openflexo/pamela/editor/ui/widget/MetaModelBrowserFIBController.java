package org.openflexo.pamela.editor.ui.widget;

import javax.swing.ImageIcon;

import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import org.openflexo.pamela.editor.ui.PamelaEditorSession;

/**
 * FIB controller for the {@link MetaModelBrowser}.
 *
 * <p>Holds the currently selected element ({@code selectedElement}) as an
 * observable property.  The FIB browser writes to it on single-click via the
 * two-way binding {@code selected="controller.selectedElement"}.
 * {@link PamelaEditorApplication} listens to {@code PropertyChangeEvent}s on
 * {@code "selectedElement"} to propagate the selection to the rest of the UI.</p>
 */
public class MetaModelBrowserFIBController extends PamelaEditorFIBController<PamelaEditorApplication> {

    /** Property name fired when the selected element changes. */
    public static final String SELECTED_ELEMENT = "selectedElement";

    private Object selectedElement;

    public MetaModelBrowserFIBController(FIBComponent rootComponent) {
        super(rootComponent);
    }

    // -------------------------------------------------------------------------
    // Observable selection property (two-way bound from the FIB browser)
    // -------------------------------------------------------------------------

    public Object getSelectedElement() {
        return selectedElement;
    }

    public void setSelectedElement(Object selectedElement) {
        Object old = this.selectedElement;
        this.selectedElement = selectedElement;
        getPropertyChangeSupport().firePropertyChange(SELECTED_ELEMENT, old, selectedElement);
    }

    // -------------------------------------------------------------------------
    // Actions invoked from the FIB
    // -------------------------------------------------------------------------

    /** Called when the user double-clicks a node in the browser. */
    public void doubleClick(Object object) {
        PamelaEditorApplication app = getDataObject();
        if (app != null) {
            app.doubleClickInBrowser(object);
        }
    }

    /** Called on right-click (contextual menu — deferred to a later sprint). */
    public void rightClick(Object object, Object event) {
        // TODO: contextual menu
    }

    // -------------------------------------------------------------------------
    // Icon resolution
    // -------------------------------------------------------------------------

    @Override
    protected ImageIcon retrieveIconForObject(Object object) {
        if (object instanceof PamelaEditorSession) {
            return PamelaEditorIconLibrary.SESSION_ICON;
        }
        if (object instanceof PamelaClassDiagram) {
            return PamelaEditorIconLibrary.DIAGRAM_ICON;
        }
        if (object instanceof SourcePackage) {
            return PamelaEditorIconLibrary.PACKAGE_ICON;
        }
        if (object instanceof SourceModelEntity) {
            SourceModelEntity entity = (SourceModelEntity) object;
            return entity.isAbstract()
                    ? PamelaEditorIconLibrary.ABSTRACT_ENTITY_ICON
                    : PamelaEditorIconLibrary.ENTITY_ICON;
        }
        return super.retrieveIconForObject(object);
    }
}
