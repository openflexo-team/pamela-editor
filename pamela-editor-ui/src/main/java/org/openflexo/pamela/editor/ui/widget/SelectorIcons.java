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

import org.openflexo.pamela.editor.model.SourceJavaFile;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelInitializer;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.model.SourceCustomMethod;
import org.openflexo.pamela.editor.model.SourceFolder;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;

import spoon.reflect.declaration.CtElement;

/**
 * Icon resolution shared by all selector popup browsers: maps {@code Source*} model elements to the
 * {@link PamelaEditorIconLibrary} icons and delegates Spoon nodes ({@code CtType}, {@code CtMethod},
 * …) to {@link SpoonOutlineController#iconFor(Object)}. Returns {@code null} when no icon applies, so
 * callers can fall back to the default controller behaviour.
 */
final class SelectorIcons {

	private SelectorIcons() {
	}

	static ImageIcon iconFor(Object object) {
		if (object instanceof SourceMetaModel) {
			return PamelaEditorIconLibrary.METAMODEL_ICON;
		}
		if (object instanceof SourceFolder) {
			return PamelaEditorIconLibrary.SOURCE_FOLDER_ICON;
		}
		if (object instanceof SourcePackage) {
			return PamelaEditorIconLibrary.PACKAGE_ICON;
		}
		if (object instanceof SourceModelEntity) {
			return ((SourceModelEntity) object).isAbstract()
					? PamelaEditorIconLibrary.ABSTRACT_ENTITY_ICON
					: PamelaEditorIconLibrary.ENTITY_ICON;
		}
		if (object instanceof SourceModelProperty) {
			return PamelaEditorIconLibrary.PROPERTY_ICON;
		}
		if (object instanceof SourceModelInitializer) {
			return PamelaEditorIconLibrary.INITIALIZER_ICON;
		}
		if (object instanceof SourceCustomMethod) {
			return PamelaEditorIconLibrary.METHOD_ICON;
		}
		if (object instanceof SourceJavaFile) {
			return PamelaEditorIconLibrary.JAVA_FILE_ICON;
		}
		if (object instanceof CtElement) {
			return SpoonOutlineController.iconFor(object);
		}
		return null;
	}
}
