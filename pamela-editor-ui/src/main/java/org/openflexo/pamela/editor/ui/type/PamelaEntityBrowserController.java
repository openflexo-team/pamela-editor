package org.openflexo.pamela.editor.ui.type;

import javax.swing.ImageIcon;

import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;

/**
 * FIB controller for the entity-type browser ({@code PamelaEntityTypeEditor.fib}):
 * supplies metamodel / package / entity icons via {@code iconForObject(...)}.
 */
public class PamelaEntityBrowserController extends PamelaEditorFIBController<Object> {

    public PamelaEntityBrowserController(FIBComponent rootComponent) {
        super(rootComponent);
    }

    @Override
    protected ImageIcon retrieveIconForObject(Object object) {
        if (object instanceof SourceMetaModel) {
            return PamelaEditorIconLibrary.METAMODEL_ICON;
        }
        if (object instanceof SourcePackage) {
            return PamelaEditorIconLibrary.PACKAGE_ICON;
        }
        if (object instanceof SourceModelEntity) {
            return ((SourceModelEntity) object).isAbstract()
                    ? PamelaEditorIconLibrary.ABSTRACT_ENTITY_ICON
                    : PamelaEditorIconLibrary.ENTITY_ICON;
        }
        return super.retrieveIconForObject(object);
    }
}
