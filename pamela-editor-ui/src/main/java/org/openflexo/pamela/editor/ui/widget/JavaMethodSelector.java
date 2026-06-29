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
import java.util.Comparator;
import java.util.List;

import org.openflexo.gina.model.widget.FIBCustom.FIBCustomComponent.CustomComponentParameter;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

/**
 * Selects a Java <b>method</b> as a Spoon {@link CtMethod}, browsing the methods of a single class.
 * The class is given either directly as a {@link CtType} ({@code declaringType}) or as a
 * {@link SourceModelEntity} ({@code entity}), whose primary {@code CtType} is then resolved via
 * {@code SourceCompilationUnit.getRootTypes()}. Surfacing Spoon values is the documented
 * encapsulation exception (ui-design.md §19.3).
 *
 * <p>Optional constraint {@code staticOnly} (only static methods).</p>
 *
 * @author sylvain
 */
@SuppressWarnings("serial")
public class JavaMethodSelector extends FIBPamelaObjectSelector<CtMethod<?>> {

	public static final Resource FIB_FILE = ResourceLocator.locateResource("Fib/Selector/JavaMethodSelector.fib");

	private CtType<?> declaringType;
	private SourceModelEntity entity;
	private boolean staticOnly = false;

	public JavaMethodSelector(CtMethod<?> editedObject) {
		super(editedObject);
	}

	@Override
	public Resource getFIBResource() {
		return FIB_FILE;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Class<CtMethod<?>> getRepresentedType() {
		return (Class) CtMethod.class;
	}

	@Override
	public String renderedString(CtMethod<?> editedObject) {
		if (editedObject == null) {
			return "";
		}
		String label = SpoonOutlineController.labelFor(editedObject);
		return label != null ? label : editedObject.getSimpleName();
	}

	/** The resolved class is the browser root; its methods are its children. */
	@Override
	public Object getRootObject() {
		return getResolvedType();
	}

	@Override
	protected Collection<CtMethod<?>> computeAllSelectableValues() {
		return getDeclaredMethods();
	}

	// --- context ------------------------------------------------------------

	public CtType<?> getDeclaringType() {
		return declaringType;
	}

	@CustomComponentParameter(name = "declaringType", type = CustomComponentParameter.Type.OPTIONAL)
	public void setDeclaringType(CtType<?> declaringType) {
		if (this.declaringType != declaringType) {
			CtType<?> old = this.declaringType;
			this.declaringType = declaringType;
			getPropertyChangeSupport().firePropertyChange("declaringType", old, declaringType);
			refresh();
		}
	}

	public SourceModelEntity getEntity() {
		return entity;
	}

	@CustomComponentParameter(name = "entity", type = CustomComponentParameter.Type.OPTIONAL)
	public void setEntity(SourceModelEntity entity) {
		if (this.entity != entity) {
			SourceModelEntity old = this.entity;
			this.entity = entity;
			getPropertyChangeSupport().firePropertyChange("entity", old, entity);
			refresh();
		}
	}

	public boolean isStaticOnly() {
		return staticOnly;
	}

	@CustomComponentParameter(name = "staticOnly", type = CustomComponentParameter.Type.OPTIONAL)
	public void setStaticOnly(boolean staticOnly) {
		if (this.staticOnly != staticOnly) {
			this.staticOnly = staticOnly;
			getPropertyChangeSupport().firePropertyChange("staticOnly", !staticOnly, staticOnly);
			refresh();
		}
	}

	// --- resolution / constraint --------------------------------------------

	/** The class whose methods are browsed: explicit {@code declaringType}, else the entity's type. */
	public CtType<?> getResolvedType() {
		if (declaringType != null) {
			return declaringType;
		}
		if (entity != null && entity.getCompilationUnit() != null) {
			String simpleName = entity.getSimpleName();
			for (CtType<?> type : entity.getCompilationUnit().getRootTypes()) {
				if (simpleName.equals(type.getSimpleName())) {
					return type;
				}
			}
		}
		return null;
	}

	/** Methods of the resolved class (sorted by signature), honouring {@code staticOnly}. */
	public List<CtMethod<?>> getDeclaredMethods() {
		List<CtMethod<?>> result = new ArrayList<>();
		CtType<?> type = getResolvedType();
		if (type == null) {
			return result;
		}
		for (CtMethod<?> m : type.getMethods()) {
			if (!staticOnly || m.isStatic()) {
				result.add(m);
			}
		}
		result.sort(Comparator.comparing(SpoonOutlineController::labelFor,
				Comparator.nullsLast(Comparator.naturalOrder())));
		return result;
	}

	@Override
	public boolean isAcceptableValue(Object o) {
		if (!(o instanceof CtMethod)) {
			return false;
		}
		return (!staticOnly || ((CtMethod<?>) o).isStatic()) && isAmongRestricted(o);
	}
}
