/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor, a component of the software infrastructure
 * developed at Openflexo.
 *
 */

package org.openflexo.pamela.editor.ui.widget;

import org.openflexo.gina.model.widget.FIBCustom.FIBCustomComponent.CustomComponentParameter;
import org.openflexo.pamela.editor.model.SourceElement;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourcePackage;

/**
 * Base for the selectors whose value is a {@link SourceElement} and whose enclosing context is some
 * combination of a {@link SourceMetaModel}, a {@link SourcePackage} and a {@link SourceModelEntity}.
 * Factors the context plumbing: three optional {@code @CustomComponentParameter} setters and the
 * {@link #getRootObject()} resolution (most specific non-null context: entity → package → metamodel).
 *
 * <p>Subclasses still provide {@link #getFIBResource()}, {@link #getRepresentedType()},
 * {@link #renderedString(Object)} and {@link #getAllSelectableValues()}, and declare which of the
 * three contexts they actually accept (by leaving the others always null).</p>
 *
 * @param <T> the {@link SourceElement} type selected by this widget
 * @author sylvain
 */
@SuppressWarnings("serial")
public abstract class AbstractSourceElementSelector<T extends SourceElement> extends FIBPamelaObjectSelector<T> {

	private SourceMetaModel metaModel;
	private SourcePackage sourcePackage;
	private SourceModelEntity entity;

	protected AbstractSourceElementSelector(T editedObject) {
		super(editedObject);
	}

	public SourceMetaModel getMetaModel() {
		// Fall back to the most specific context's metamodel when set directly elsewhere
		if (metaModel != null) {
			return metaModel;
		}
		if (sourcePackage != null) {
			return sourcePackage.getMetaModel();
		}
		if (entity != null) {
			return entity.getMetaModel();
		}
		return null;
	}

	@CustomComponentParameter(name = "metaModel", type = CustomComponentParameter.Type.OPTIONAL)
	public void setMetaModel(SourceMetaModel metaModel) {
		if (this.metaModel != metaModel) {
			SourceMetaModel old = this.metaModel;
			this.metaModel = metaModel;
			getPropertyChangeSupport().firePropertyChange("metaModel", old, metaModel);
			refresh();
		}
	}

	public SourcePackage getSourcePackage() {
		return sourcePackage;
	}

	@CustomComponentParameter(name = "sourcePackage", type = CustomComponentParameter.Type.OPTIONAL)
	public void setSourcePackage(SourcePackage sourcePackage) {
		if (this.sourcePackage != sourcePackage) {
			SourcePackage old = this.sourcePackage;
			this.sourcePackage = sourcePackage;
			getPropertyChangeSupport().firePropertyChange("sourcePackage", old, sourcePackage);
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

	/** Most specific non-null context: entity → package → metamodel. */
	@Override
	public Object getRootObject() {
		if (entity != null) {
			return entity;
		}
		if (sourcePackage != null) {
			return sourcePackage;
		}
		return metaModel;
	}
}
