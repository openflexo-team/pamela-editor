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

import org.openflexo.pamela.editor.model.SourceModelInitializer;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Selects a {@link SourceModelInitializer} declared by an entity (the {@code entity} context).
 *
 * @author sylvain
 */
@SuppressWarnings("serial")
public class InitializerSelector extends AbstractSourceElementSelector<SourceModelInitializer> {

	public static final Resource FIB_FILE = ResourceLocator.locateResource("Fib/Selector/InitializerSelector.fib");

	public InitializerSelector(SourceModelInitializer editedObject) {
		super(editedObject);
	}

	@Override
	public Resource getFIBResource() {
		return FIB_FILE;
	}

	@Override
	public Class<SourceModelInitializer> getRepresentedType() {
		return SourceModelInitializer.class;
	}

	@Override
	public String renderedString(SourceModelInitializer editedObject) {
		return editedObject != null ? editedObject.getDisplayLabel() : "";
	}

	@Override
	protected Collection<SourceModelInitializer> getAllSelectableValues() {
		if (getEntity() != null) {
			return new ArrayList<>(getEntity().getInitializers());
		}
		return null;
	}
}
