package org.openflexo.pamela.editor.ui.widget;

import javax.swing.ImageIcon;

import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;

/**
 * FIB controller for {@link PackageSummaryView}.
 *
 * <p>Provides icon resolution for the entities table (without it, the FIB falls back to the
 * base {@link org.openflexo.gina.controller.FIBController}, which has no {@code iconForObject}
 * method and the icon column stays empty).</p>
 */
public class PackageSummaryViewFIBController extends PamelaEditorFIBController<SourcePackage> {

    public PackageSummaryViewFIBController(FIBComponent rootComponent) {
        super(rootComponent);
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
}
