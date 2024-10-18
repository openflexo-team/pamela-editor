package org.openflexo.pamela.editor;

import java.util.ArrayList;
import java.util.List;

import org.openflexo.toolbox.StringUtils;

import spoon.reflect.declaration.CtPackage;

public class SourcePackage implements SourceElement {

	private final CtPackage ctPackage;
	private final SourceMetaModel metaModel;
	private final List<SourceModelEntity<?>> entities;

	public SourcePackage(CtPackage ctPackage, SourceMetaModel metaModel) {
		this.ctPackage = ctPackage;
		this.metaModel = metaModel;
		entities = new ArrayList<>();
	}

	public boolean isDefault() {
		return StringUtils.isEmpty(getQualifiedName());
	}

	public String getQualifiedName() {
		return ctPackage.getQualifiedName();
	}

	public CtPackage getCtPackage() {
		return ctPackage;
	}

	public SourceMetaModel getMetaModel() {
		return metaModel;
	}

	public List<SourceModelEntity<?>> getEntities() {
		return entities;
	}

	public void addToEntities(SourceModelEntity<?> entity) {
		entities.add(entity);
	}

}
