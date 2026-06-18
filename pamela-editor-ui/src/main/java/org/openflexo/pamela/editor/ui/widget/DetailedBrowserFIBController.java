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

    /** Property name fired when the lead selected element changes. */
    public static final String SELECTED_ELEMENT = "selectedElement";

    /** Property name fired when the (multi-)selection list changes. */
    public static final String SELECTION = "selection";

    private Object selectedElement;
    private java.util.List<Object> selection = new java.util.ArrayList<>();
    private PamelaEditorApplication application;

    public DetailedBrowserFIBController(FIBComponent rootComponent) {
        super(rootComponent);
    }

    // -------------------------------------------------------------------------
    // Application reference (set by DetailedBrowser after construction)
    // -------------------------------------------------------------------------

    public void setApplication(PamelaEditorApplication application) {
        this.application = application;
        // Route user (multi-)selection from the browser tree to the application.
        //
        // The full selection list is delivered through Gina's FIBSelectionListener
        // mechanism: the per-node `selected` two-way binding only carries the lead
        // (it is NOT written back for the multi-selection list — see
        // FIBBrowserWidgetImpl.valueChanged). The lead is read from getSelectedElement(),
        // which Gina has already updated (via the `selected` binding) before firing the
        // selection change.
        addSelectionListener(sel -> {
            if (this.application == null) {
                return;
            }
            Object lead = getSelectedElement();
            // Ignore the internal empty selection Gina fires while rebinding the tree to
            // a new data object (not a user action — would clear the inspector/diagram).
            if (lead == null && (sel == null || sel.isEmpty())) {
                return;
            }
            java.util.List<Object> list =
                    (sel == null) ? new java.util.ArrayList<>() : new java.util.ArrayList<>(sel);
            this.application.onDetailedBrowserSelectionChanged(lead, list);
        });
    }

    // -------------------------------------------------------------------------
    // Observable selection properties
    // -------------------------------------------------------------------------

    public Object getSelectedElement() {
        return selectedElement;
    }

    /**
     * Updates the lead selected element. This is the {@code selected} two-way binding
     * used to highlight the lead node and to feed the double/right-click actions.
     *
     * <p>It does <em>not</em> notify the application: user selection is propagated through
     * the {@link org.openflexo.gina.model.listener.FIBSelectionListener} registered in
     * {@link #setApplication} (which carries the full multi-selection list, not only the
     * lead). Keeping the notification here too would double every event.</p>
     */
    public void setSelectedElement(Object selectedElement) {
        Object old = this.selectedElement;
        this.selectedElement = selectedElement;
        getPropertyChangeSupport().firePropertyChange(SELECTED_ELEMENT, old, selectedElement);
    }

    public java.util.List<Object> getSelection() {
        return selection;
    }

    /**
     * Two-way {@code selection} binding. Setting it programmatically (from the
     * application, during a diagram → browser sync) highlights the corresponding tree
     * nodes; the user's own multi-selection is reported via the
     * {@link org.openflexo.gina.model.listener.FIBSelectionListener} (see
     * {@link #setApplication}).
     */
    public void setSelection(java.util.List<Object> selection) {
        java.util.List<Object> old = this.selection;
        this.selection = selection;
        getPropertyChangeSupport().firePropertyChange(SELECTION, old, selection);
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
        if (object instanceof org.openflexo.pamela.editor.model.SourceCustomMethod) {
            return PamelaEditorIconLibrary.METHOD_ICON;
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
