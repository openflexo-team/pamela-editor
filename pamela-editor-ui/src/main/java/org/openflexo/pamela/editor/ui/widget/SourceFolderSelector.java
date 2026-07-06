/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor, a component of the software infrastructure
 * developed at Openflexo.
 *
 */

package org.openflexo.pamela.editor.ui.widget;

import java.util.ArrayList;
import java.util.Collection;

import org.openflexo.pamela.editor.model.SourceFolder;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Selects a {@link SourceFolder} (a registered source directory), browsing the whole metamodel.
 * Only the {@code metaModel} context (inherited from {@link AbstractSourceElementSelector}) is
 * used — {@code sourcePackage} / {@code entity} are not meaningful scopes for a folder and are
 * left unset by callers.
 *
 * @author sylvain
 */
@SuppressWarnings("serial")
public class SourceFolderSelector extends AbstractSourceElementSelector<SourceFolder> {

	public static final Resource FIB_FILE = ResourceLocator.locateResource("Fib/Selector/SourceFolderSelector.fib");

	public SourceFolderSelector(SourceFolder editedObject) {
		super(editedObject);
	}

	@Override
	public Resource getFIBResource() {
		return FIB_FILE;
	}

	@Override
	public Class<SourceFolder> getRepresentedType() {
		return SourceFolder.class;
	}

	@Override
	public String renderedString(SourceFolder editedObject) {
		return editedObject != null ? editedObject.getName() : "";
	}

	@Override
	protected Collection<SourceFolder> computeAllSelectableValues() {
		return getMetaModel() != null ? new ArrayList<>(getMetaModel().getSourceFolders()) : null;
	}
}
