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
import java.util.HashSet;
import java.util.Set;

import org.openflexo.gina.model.widget.FIBCustom.FIBCustomComponent.CustomComponentParameter;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Selects a {@link SourceModelEntity}, browsing either a whole metamodel or a single package.
 * Optional constraints: {@code abstractOnly} (only abstract entities) and {@code subTypeOf}
 * (only entities that are, transitively, sub-entities of the given qualified name).
 *
 * @author sylvain
 */
@SuppressWarnings("serial")
public class ModelEntitySelector extends AbstractSourceElementSelector<SourceModelEntity> {

	public static final Resource FIB_FILE = ResourceLocator.locateResource("Fib/Selector/ModelEntitySelector.fib");

	private boolean abstractOnly = false;
	private String subTypeOf;

	public ModelEntitySelector(SourceModelEntity editedObject) {
		super(editedObject);
	}

	@Override
	public Resource getFIBResource() {
		return FIB_FILE;
	}

	@Override
	public Class<SourceModelEntity> getRepresentedType() {
		return SourceModelEntity.class;
	}

	@Override
	public String renderedString(SourceModelEntity editedObject) {
		return editedObject != null ? editedObject.getSimpleName() : "";
	}

	@Override
	protected Collection<SourceModelEntity> getAllSelectableValues() {
		if (getSourcePackage() != null) {
			return new ArrayList<>(getSourcePackage().getEntities());
		}
		if (getMetaModel() != null) {
			return new ArrayList<>(getMetaModel().getEntities().values());
		}
		return null;
	}

	public boolean isAbstractOnly() {
		return abstractOnly;
	}

	@CustomComponentParameter(name = "abstractOnly", type = CustomComponentParameter.Type.OPTIONAL)
	public void setAbstractOnly(boolean abstractOnly) {
		if (this.abstractOnly != abstractOnly) {
			this.abstractOnly = abstractOnly;
			getPropertyChangeSupport().firePropertyChange("abstractOnly", !abstractOnly, abstractOnly);
			refresh();
		}
	}

	public String getSubTypeOf() {
		return subTypeOf;
	}

	@CustomComponentParameter(name = "subTypeOf", type = CustomComponentParameter.Type.OPTIONAL)
	public void setSubTypeOf(String subTypeOf) {
		if (this.subTypeOf == null ? subTypeOf != null : !this.subTypeOf.equals(subTypeOf)) {
			String old = this.subTypeOf;
			this.subTypeOf = subTypeOf;
			getPropertyChangeSupport().firePropertyChange("subTypeOf", old, subTypeOf);
			refresh();
		}
	}

	@Override
	public boolean isAcceptableValue(Object o) {
		if (!super.isAcceptableValue(o)) {
			return false;
		}
		SourceModelEntity entity = (SourceModelEntity) o;
		if (abstractOnly && !entity.isAbstract()) {
			return false;
		}
		if (subTypeOf != null && subTypeOf.length() > 0 && !isSubTypeOf(entity, subTypeOf)) {
			return false;
		}
		return true;
	}

	private static boolean isSubTypeOf(SourceModelEntity entity, String superQualifiedName) {
		Set<SourceModelEntity> visited = new HashSet<>();
		return walk(entity, superQualifiedName, visited);
	}

	private static boolean walk(SourceModelEntity entity, String superQN, Set<SourceModelEntity> visited) {
		if (entity == null || !visited.add(entity)) {
			return false;
		}
		if (superQN.equals(entity.getQualifiedName())) {
			return true;
		}
		for (SourceModelEntity superEntity : entity.getDirectSuperEntities()) {
			if (walk(superEntity, superQN, visited)) {
				return true;
			}
		}
		return false;
	}
}
