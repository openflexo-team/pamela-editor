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

import org.openflexo.pamela.editor.model.SourceCustomMethod;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Selects an operation ({@link SourceCustomMethod}) declared by an entity (the {@code entity}
 * context). Operations are the entity's non-accessor, non-initializer methods (finders, deleters,
 * {@code @Operation}-marked and plain interface methods, per {@code custom-method-design.md}).
 *
 * @author sylvain
 */
@SuppressWarnings("serial")
public class OperationSelector extends AbstractSourceElementSelector<SourceCustomMethod> {

	public static final Resource FIB_FILE = ResourceLocator.locateResource("Fib/Selector/OperationSelector.fib");

	public OperationSelector(SourceCustomMethod editedObject) {
		super(editedObject);
	}

	@Override
	public Resource getFIBResource() {
		return FIB_FILE;
	}

	@Override
	public Class<SourceCustomMethod> getRepresentedType() {
		return SourceCustomMethod.class;
	}

	@Override
	public String renderedString(SourceCustomMethod editedObject) {
		return editedObject != null ? editedObject.getSignature() : "";
	}

	@Override
	protected Collection<SourceCustomMethod> getAllSelectableValues() {
		if (getEntity() != null) {
			return new ArrayList<>(getEntity().getDeclaredCustomMethods());
		}
		return null;
	}
}
