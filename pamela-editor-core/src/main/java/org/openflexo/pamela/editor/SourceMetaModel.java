package org.openflexo.pamela.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openflexo.pamela.annotations.ModelEntity;

import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.AnnotationFilter;

/**
 * Represents a PAMELA meta-model beeing encoded in source files
 * 
 * TODO... explain
 */
public class SourceMetaModel implements SourceElement {

	private String name;
	private final SpoonAPI spoon;
	private CtModel model;
	private final Map<CtPackage, SourcePackage> packages;
	private final Map<CtType<?>, SourceModelEntity<?>> entities;
	private final List<Issue> issues;

	public SourceMetaModel() {
		spoon = new Launcher();
		packages = new HashMap<>();
		entities = new HashMap<>();
		issues = new ArrayList<>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Add in input resource to the {@link SourceMetaModel} : this is a location where source files can be found (not all the classes being
	 * part of the Pamela meta model)<br>
	 * 
	 * @param inputResource
	 *            a path or a single file
	 */
	public void addInputResource(File inputResource) {
		spoon.addInputResource(inputResource.getAbsolutePath());
	}

	public void buildMetaModel() {
		model = spoon.buildModel();
		// debug();

		for (CtPackage ctPackage : model.getAllPackages()) {
			buildOrRetrievePackage(ctPackage);
		}

		for (CtPackage ctPackage : model.getAllPackages()) {
			for (CtElement ctElement : ctPackage.getElements(new AnnotationFilter<CtElement>(ModelEntity.class))) {
				if (ctElement instanceof CtType) {
					buildOrRetrieveModelEntity((CtType) ctElement);
				}
				else {
					fireIssue(new Error("Unexpected @ModelEntity found on " + ctElement));
				}
			}
		}
	}

	private SourcePackage buildOrRetrievePackage(CtPackage aPackage) {
		SourcePackage returned = getPackage(aPackage);
		if (returned == null) {
			returned = new SourcePackage(aPackage, this);
			packages.put(aPackage, returned);
		}
		return returned;

	}

	public SourcePackage getPackage(CtPackage aPackage) {
		return packages.get(aPackage);
	}

	public SourcePackage getPackage(String qualifiedName) {
		for (SourcePackage sourcePackage : packages.values()) {
			if (sourcePackage.getQualifiedName().equals(qualifiedName)) {
				return sourcePackage;
			}
		}
		return null;
	}

	public SourcePackage getDefaultPackage() {
		for (SourcePackage sourcePackage : packages.values()) {
			if (sourcePackage.isDefault()) {
				return sourcePackage;
			}
		}
		return null;
	}

	public Collection<SourcePackage> getAllPackages() {
		return packages.values();
	}

	private <T> SourceModelEntity<T> buildOrRetrieveModelEntity(CtType<T> ctType) {
		SourceModelEntity<T> returned = (SourceModelEntity<T>) entities.get(ctType);
		if (returned == null) {
			returned = new SourceModelEntity<T>(ctType, this);
			entities.put(ctType, returned);
		}
		return returned;
	}

	public CtModel getModel() {
		return model;
	}

	protected void fireIssue(Issue issue) {
		issues.add(issue);
		System.err.println(" >>> " + issue.getMessage());
	}

	protected void debug() {
		System.out.println("Debugging SourceMetaModel");
		for (CtPackage ctPackage : model.getAllPackages()) {
			System.out.println("Package: " + ctPackage);
			for (CtElement ctElement : ctPackage.getElements(new AnnotationFilter<CtElement>(ModelEntity.class))) {
				System.out.println("-----> " + ctElement + " of " + ctElement.getClass().getSimpleName());
			}
		}
	}

}
