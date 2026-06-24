package org.openflexo.pamela.editor.ui.type;

import java.beans.PropertyChangeSupport;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.controller.CustomTypeEditor;
import org.openflexo.gina.controller.FIBController;
import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;
import org.openflexo.toolbox.HasPropertyChangeSupport;

/**
 * {@link CustomTypeEditor} for {@link PamelaEntityType}: a FIB <b>browser</b> of
 * the current metamodel (entities organised by package), shown in the
 * {@code TypeSelector} popup when the "PAMELA Entity" category is chosen.
 * Selecting an entity in the tree drives the edited type — just like the Java
 * class browser drives a Java type.
 *
 * <p>The choice reaches {@code TypeSelector} through the <b>shared factory</b>:
 * {@link #setSelectedElement} configures the factory, which fires; TypeSelector
 * listens to the factory (not to this editor) and regenerates its edited type.</p>
 */
public class PamelaEntityTypeEditor
        implements CustomTypeEditor<PamelaEntityType>, HasPropertyChangeSupport {

    public static final Resource FIB_FILE =
            ResourceLocator.locateResource("Fib/CustomType/PamelaEntityTypeEditor.fib");

    private final PropertyChangeSupport pcSupport = new PropertyChangeSupport(this);
    private final PamelaEntityTypeFactory factory;
    private Object selectedElement; // a SourceModelEntity (or a SourcePackage — ignored)

    public PamelaEntityTypeEditor(PamelaEntityTypeFactory factory) {
        this.factory = factory;
    }

    @Override
    public String getPresentationName() {
        return "PAMELA Entity";
    }

    @Override
    public Class<PamelaEntityType> getCustomType() {
        return PamelaEntityType.class;
    }

    @Override
    public Resource getFIBComponentResource() {
        return FIB_FILE;
    }

    @Override
    public PamelaEntityType getEditedType() {
        if (selectedElement instanceof SourceModelEntity) {
            return new PamelaEntityType(((SourceModelEntity) selectedElement).getQualifiedName(),
                    factory.getMetaModel());
        }
        return null;
    }

    @Override
    public void updateEditedType(PamelaEntityType type) {
        // Adopt an incoming type only when it names an existing entity (e.g. the
        // dialog pre-filling the current type) — never the name-less placeholder
        // produced when the category is first chosen.
        if (type != null && type.getQualifiedName() != null && factory.getMetaModel() != null) {
            SourceModelEntity entity = factory.getMetaModel().getEntity(type.getQualifiedName());
            if (entity != null && entity != selectedElement) {
                setSelectedElement(entity);
            }
        }
    }

    @Override
    public FIBController makeFIBController() {
        FIBComponent component = ApplicationFIBLibraryImpl.instance()
                .retrieveFIBComponent(getFIBComponentResource());
        return new PamelaEntityBrowserController(component);
    }

    // --- bound by PamelaEntityTypeEditor.fib --------------------------------

    /** Browser root. */
    public SourceMetaModel getMetaModel() {
        return factory.getMetaModel();
    }

    public Object getSelectedElement() {
        return selectedElement;
    }

    public void setSelectedElement(Object selectedElement) {
        Object old = this.selectedElement;
        this.selectedElement = selectedElement;
        pcSupport.firePropertyChange("selectedElement", old, selectedElement);
        // Propagate to TypeSelector via the shared factory it listens to.
        String qn = selectedElement instanceof SourceModelEntity
                ? ((SourceModelEntity) selectedElement).getQualifiedName() : null;
        factory.setConfiguredQualifiedName(qn);
    }

    // --- HasPropertyChangeSupport -------------------------------------------

    @Override
    public PropertyChangeSupport getPropertyChangeSupport() {
        return pcSupport;
    }

    @Override
    public String getDeletedProperty() {
        return null;
    }
}
