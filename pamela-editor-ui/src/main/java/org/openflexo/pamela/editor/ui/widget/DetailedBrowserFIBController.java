package org.openflexo.pamela.editor.ui.widget;

import javax.swing.ImageIcon;

import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.diagram.EntityView;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.model.SourceImplementationClass;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelInitializer;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;

/**
 * FIB controller for the {@link DetailedBrowser}.
 *
 * <p>Shows the content of the currently selected element.  The data object
 * changes every time the selection changes in the {@link MetaModelBrowser};
 * {@link DetailedBrowser#setEditedObject(Object)} triggers an in-place rebind.</p>
 *
 * <p>Holds a {@code selectedElement} observable property so that single-click
 * inside the DetailedBrowser also propagates to
 * {@link PamelaEditorApplication#setCurrentSelectedElement(Object)}.</p>
 */
public class DetailedBrowserFIBController extends PamelaEditorFIBController<Object> {

    /** Property name fired when the selected element changes. */
    public static final String SELECTED_ELEMENT = "selectedElement";

    private Object selectedElement;
    private PamelaEditorApplication application;

    public DetailedBrowserFIBController(FIBComponent rootComponent) {
        super(rootComponent);
    }

    // -------------------------------------------------------------------------
    // Application reference (set by DetailedBrowser after construction)
    // -------------------------------------------------------------------------

    public void setApplication(PamelaEditorApplication application) {
        this.application = application;
    }

    // -------------------------------------------------------------------------
    // Observable selection property
    // -------------------------------------------------------------------------

    public Object getSelectedElement() {
        return selectedElement;
    }

    public void setSelectedElement(Object selectedElement) {
        Object old = this.selectedElement;
        this.selectedElement = selectedElement;
        getPropertyChangeSupport().firePropertyChange(SELECTED_ELEMENT, old, selectedElement);
        // Propagate to application
        if (application != null) {
            application.setCurrentSelectedElement(selectedElement);
        }
    }

    // -------------------------------------------------------------------------
    // Type predicates used by FIB visible= bindings
    // (Connie does not support Java instanceof expressions)
    // -------------------------------------------------------------------------

    public boolean isSourceModelEntity(Object obj) {
        return obj instanceof SourceModelEntity;
    }

    public boolean isSourcePackage(Object obj) {
        return obj instanceof SourcePackage;
    }

    public boolean isPamelaClassDiagram(Object obj) {
        return obj instanceof PamelaClassDiagram;
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    /** Called when the user double-clicks a node in the detailed browser. */
    public void doubleClick(Object object) {
        if (application != null) {
            application.doubleClickInBrowser(object);
        }
    }

    // -------------------------------------------------------------------------
    // Icon resolution
    // -------------------------------------------------------------------------

    @Override
    protected ImageIcon retrieveIconForObject(Object object) {
        if (object instanceof SourceModelEntity) {
            SourceModelEntity entity = (SourceModelEntity) object;
            return entity.isAbstract()
                    ? PamelaEditorIconLibrary.ABSTRACT_ENTITY_ICON
                    : PamelaEditorIconLibrary.ENTITY_ICON;
        }
        if (object instanceof SourceModelProperty) {
            return PamelaEditorIconLibrary.PROPERTY_ICON;
        }
        if (object instanceof SourceModelInitializer) {
            return PamelaEditorIconLibrary.INITIALIZER_ICON;
        }
        if (object instanceof SourceImplementationClass) {
            return PamelaEditorIconLibrary.IMPL_CLASS_ICON;
        }
        if (object instanceof SourcePackage) {
            return PamelaEditorIconLibrary.PACKAGE_ICON;
        }
        if (object instanceof PamelaClassDiagram) {
            return PamelaEditorIconLibrary.DIAGRAM_ICON;
        }
        if (object instanceof EntityView) {
            return PamelaEditorIconLibrary.ENTITY_ICON;
        }
        return super.retrieveIconForObject(object);
    }
}
