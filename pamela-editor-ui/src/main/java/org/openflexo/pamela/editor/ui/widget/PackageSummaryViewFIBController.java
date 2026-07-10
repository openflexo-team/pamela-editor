package org.openflexo.pamela.editor.ui.widget;

import javax.swing.ImageIcon;

import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;

/**
 * FIB controller for {@link PackageSummaryView}.
 *
 * <p>Provides icon resolution for the entities table (without it, the FIB falls back to the
 * base {@link org.openflexo.gina.controller.FIBController}, which has no {@code iconForObject}
 * method and the icon column stays empty).</p>
 *
 * <p>Also synchronizes the entities table selection with the application: clicking a row
 * soft-selects the corresponding {@link SourceModelEntity} (updates the inspector and the
 * detailed browser without leaving this view — mirrors the diagram soft-selection,
 * ui-design.md §18.2) and right-clicking it opens the same contextual menu as the browsers
 * and the diagram (ui-design.md §18.4). See {@link EntityTableSupport}.</p>
 */
public class PackageSummaryViewFIBController extends PamelaEditorFIBController<SourcePackage> {

    private PamelaEditorApplication application;

    public PackageSummaryViewFIBController(FIBComponent rootComponent) {
        super(rootComponent);
    }

    public void setApplication(PamelaEditorApplication application) {
        this.application = application;
    }

    @Override
    protected ImageIcon retrieveIconForObject(Object object) {
        if (object instanceof SourceModelEntity) {
            SourceModelEntity entity = (SourceModelEntity) object;
            return entity.isAbstract()
                    ? PamelaEditorIconLibrary.ABSTRACT_ENTITY_ICON
                    : PamelaEditorIconLibrary.ENTITY_ICON;
        }
        return super.retrieveIconForObject(object);
    }

    /**
     * Two-way-bound selection of the entities table ({@code selected="controller.selectedEntity"}),
     * settable programmatically to highlight a row (e.g. a newly created entity).
     */
    private SourceModelEntity selectedEntity;

    public SourceModelEntity getSelectedEntity() {
        return selectedEntity;
    }

    public void setSelectedEntity(SourceModelEntity selectedEntity) {
        SourceModelEntity old = this.selectedEntity;
        this.selectedEntity = selectedEntity;
        getPropertyChangeSupport().firePropertyChange("selectedEntity", old, selectedEntity);
    }

    /** Called when the user clicks a row in the entities table (FIB {@code clickAction}). */
    public void selectEntity(Object selected) {
        EntityTableSupport.selectEntity(application, selected);
    }

    /** Called when the user right-clicks a row in the entities table (FIB {@code rightClickAction}). */
    public void rightClick(Object selected, Object event) {
        EntityTableSupport.rightClick(application, selected, event);
    }
}
