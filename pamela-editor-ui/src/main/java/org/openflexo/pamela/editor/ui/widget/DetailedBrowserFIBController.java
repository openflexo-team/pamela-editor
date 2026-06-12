package org.openflexo.pamela.editor.ui.widget;

import javax.swing.ImageIcon;

import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.diagram.EntityView;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.model.SourceImplementationClass;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.ui.PamelaProject;
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
        // Propagate to application only for genuine user selections (non-null).
        //
        // Gina calls setSelectedElement(null) internally when the browser is rebound to a
        // new data object and the previously-selected node no longer exists in the new tree.
        // Propagating that null would cause setCurrentSelectedElement(null) →
        // detailedBrowser.setEditedObject(null) → empty browser.
        // This null is an internal Gina artefact, not a user action, so we filter it here.
        if (application != null && selectedElement != null) {
            // Route through the dedicated entry point so that clicks inside the
            // DetailedBrowser while a diagram is active are treated as soft-selections
            // (inspector + context panel update, but the browser root stays fixed on
            // the diagram). Only MetaModelBrowser selections trigger a full rebind.
            application.onDetailedBrowserSelectionChanged(selectedElement);
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

    /** Called on right-click — builds and shows the contextual menu. */
    public void rightClick(Object object, Object event) {
        showContextualMenu(object, event, application);
    }

    // -------------------------------------------------------------------------
    // Icon resolution
    // -------------------------------------------------------------------------

    @Override
    protected ImageIcon retrieveIconForObject(Object object) {
        if (object instanceof PamelaProject) {
            return PamelaEditorIconLibrary.SESSION_ICON;
        }
        if (object instanceof SourceMetaModel) {
            return PamelaEditorIconLibrary.METAMODEL_ICON;
        }
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
        if (object instanceof org.openflexo.pamela.editor.model.SourceJavaFile) {
            return PamelaEditorIconLibrary.JAVA_FILE_ICON;
        }
        return super.retrieveIconForObject(object);
    }
}
