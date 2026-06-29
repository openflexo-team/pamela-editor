/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor, a component of the software infrastructure
 * developed at Openflexo.
 *
 */

package org.openflexo.pamela.editor.ui.widget;

import javax.swing.ImageIcon;

import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.model.SourceJavaFile;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelInitializer;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;

/**
 * Controller for the PAMELA-model {@code .inspector} files that need controller-side
 * icon resolution (e.g. an {@code <IconColumn data="controller.iconForObject(iterator)">}
 * in the {@code SourceModelEntity} super-entities table). Declared via
 * {@code controllerClassName} on those inspectors; instantiated by gina through the
 * {@code (FIBComponent)} constructor.
 */
@SuppressWarnings("serial")
public class PamelaModelInspectorFIBController extends PamelaEditorFIBController<Object> {

    public PamelaModelInspectorFIBController(FIBComponent rootComponent) {
        super(rootComponent);
    }

    @Override
    protected ImageIcon retrieveIconForObject(Object object) {
        if (object instanceof SourceModelEntity) {
            return ((SourceModelEntity) object).isAbstract()
                    ? PamelaEditorIconLibrary.ABSTRACT_ENTITY_ICON
                    : PamelaEditorIconLibrary.ENTITY_ICON;
        }
        if (object instanceof SourcePackage) {
            return PamelaEditorIconLibrary.PACKAGE_ICON;
        }
        if (object instanceof SourceJavaFile) {
            return PamelaEditorIconLibrary.JAVA_FILE_ICON;
        }
        if (object instanceof SourceModelProperty) {
            return PamelaEditorIconLibrary.PROPERTY_ICON;
        }
        if (object instanceof SourceModelInitializer) {
            return PamelaEditorIconLibrary.INITIALIZER_ICON;
        }
        return super.retrieveIconForObject(object);
    }
}
