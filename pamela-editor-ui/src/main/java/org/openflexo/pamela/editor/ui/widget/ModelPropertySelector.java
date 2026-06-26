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
import java.util.List;

import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Selects a {@link SourceModelProperty}, browsing within a single entity, a package, or a whole
 * metamodel (the accepted context is whatever is set; {@link #getRootObject()} picks the most
 * specific one). Completion enumerates the declared properties of every entity in scope.
 *
 * @author sylvain
 */
@SuppressWarnings("serial")
public class ModelPropertySelector extends AbstractSourceElementSelector<SourceModelProperty> {

	public static final Resource FIB_FILE = ResourceLocator.locateResource("Fib/Selector/ModelPropertySelector.fib");

	public ModelPropertySelector(SourceModelProperty editedObject) {
		super(editedObject);
	}

	@Override
	public Resource getFIBResource() {
		return FIB_FILE;
	}

	@Override
	public Class<SourceModelProperty> getRepresentedType() {
		return SourceModelProperty.class;
	}

	@Override
	public String renderedString(SourceModelProperty editedObject) {
		return editedObject != null ? editedObject.getPropertyIdentifier() : "";
	}

	@Override
	protected Collection<SourceModelProperty> getAllSelectableValues() {
		List<SourceModelProperty> result = new ArrayList<>();
		if (getEntity() != null) {
			result.addAll(getEntity().getDeclaredProperties().values());
		} else if (getSourcePackage() != null) {
			for (SourceModelEntity entity : getSourcePackage().getEntities()) {
				result.addAll(entity.getDeclaredProperties().values());
			}
		} else if (getMetaModel() != null) {
			for (SourceModelEntity entity : getMetaModel().getEntities().values()) {
				result.addAll(entity.getDeclaredProperties().values());
			}
		} else {
			return null;
		}
		return result;
	}
}
