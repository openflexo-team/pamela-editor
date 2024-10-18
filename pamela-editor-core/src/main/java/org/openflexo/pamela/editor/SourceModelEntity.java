package org.openflexo.pamela.editor;

import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtType;

public class SourceModelEntity<T> {

	private final CtType<T> ctInterface;
	private SourceMetaModel metaModel;
	private SourcePackage sourcePackage;

	public SourceModelEntity(CtType<T> ctInterface, SourceMetaModel metaModel) {
		this.ctInterface = ctInterface;
		this.metaModel = metaModel;
		buildMetaModel();
	}

	public SourceMetaModel getMetaModel() {
		return metaModel;
	}

	private void buildMetaModel() {
		System.out.println("Hop, on builde " + ctInterface.getQualifiedName());

		if (ctInterface instanceof CtClass) {
			getMetaModel().fireIssue(new Error("@ModelEntity " + ctInterface.getQualifiedName() + " must be an interface"));
		}

		sourcePackage = metaModel.getPackage(ctInterface.getPackage());
		sourcePackage.addToEntities(this);
	}

}
