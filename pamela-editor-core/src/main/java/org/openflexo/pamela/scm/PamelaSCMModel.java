package org.openflexo.pamela.scm;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.directorywalker.FileVisitor;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaSource;

/**
 * Represents a PAMELA model as a source code
 * 
 * A {@link PamelaSCMModel} is defined from a collection of directories and a collection of head classes
 * 
 */
public class PamelaSCMModel {

	private Collection<File> sourceDirectories;
	private Collection<String> headClassNames;

	private Map<String, PamelaEntity> entities;
	private Collection<PamelaEntity> headEntities;
	private List<PamelaEntity> accessibleEntities;

	private boolean isUpToDate = false;

	public PamelaSCMModel() {
		sourceDirectories = new ArrayList<>();
		headClassNames = new ArrayList<>();
		isUpToDate = false;
	}

	public PamelaSCMModel(Collection<File> sourceDirectories, Collection<String> headClassNames) {
		this();
		this.sourceDirectories.addAll(sourceDirectories);
		this.headClassNames.addAll(headClassNames);
		ensureUpToDate();
	}

	public Collection<File> getSourceDirectories() {
		return sourceDirectories;
	}

	public Collection<String> getHeadClassNames() {
		return headClassNames;
	}

	public void setHeadClassName(String uniqueHeadClassName) {
		setHeadClassNames(Collections.singletonList(uniqueHeadClassName));
	}

	public void setHeadClassNames(String... headClassNames) {
		setHeadClassNames(Arrays.asList(headClassNames));
	}

	public void setHeadClassNames(Collection<String> headClassNames) {
		if (!this.headClassNames.equals(headClassNames)) {
			this.headClassNames = headClassNames;
			// Reset head entities
			headEntities = null;
			// Reset accessible entities
			accessibleEntities = null;
		}
	}

	public void ensureUpToDate() {
		if (!isUpToDate) {
			updateModel();
		}
	}

	/**
	 * Internally called to rebuild the whole model
	 */
	private void updateModel() {
		JavaProjectBuilder builder = new PamelaJavaProjectBuilder();
		for (File sourceDirectory : sourceDirectories) {
			builder.addSourceTree(sourceDirectory, new FileVisitor() {
				@Override
				public void visitFile(File file) {
					System.out.println("Probleme avec le fichier " + file);
				}
			});
		}

		List<PamelaEntity> entitiesToRemove = new ArrayList<>();
		entitiesToRemove.addAll(getEntities());

		for (JavaSource javaSource : builder.getSources()) {
			for (JavaClass javaClass : javaSource.getClasses()) {
				if (PamelaEntity.isPamelaEntity(javaClass)) {
					PamelaEntity entity = entities.get(javaClass.getCanonicalName());
					if (entity == null) {
						entity = new PamelaEntity(javaClass);
						System.out.println("New entity: " + javaClass.getCanonicalName());
						entities.put(javaClass.getCanonicalName(), entity);
					}
					else {
						System.out.println("Update entity: " + javaClass.getCanonicalName());
						entity.updateWith(javaClass);
						entitiesToRemove.remove(entity);
					}
				}
			}
		}

		for (PamelaEntity entityToRemove : entitiesToRemove) {
			System.out.println("Remove entity: " + entityToRemove.getCanonicalName());
			entities.remove(entityToRemove.getCanonicalName());
		}

		for (PamelaEntity entity : entities.values()) {
			entity.resolveDependencies(entities.values());
		}

		// Reset head entities
		headEntities = null;

		// Reset accessible entities
		accessibleEntities = null;

		isUpToDate = true;

	}

	public Collection<PamelaEntity> getEntities() {
		if (entities == null) {
			entities = new HashMap<>();
		}
		return entities.values();
	}

	public PamelaEntity getEntityNamed(String fullQualifiedClassName) {
		return entities.get(fullQualifiedClassName);
	}

	public Collection<PamelaEntity> getHeadEntities() {
		if (headEntities == null) {
			buildHeadEntities();
		}
		return headEntities;
	}

	private void buildHeadEntities() {
		headEntities = new ArrayList<>();

		for (String headClassName : headClassNames) {
			PamelaEntity entity = entities.get(headClassName);
			if (entity != null) {
				headEntities.add(entity);
			}
			else {
				System.err.println("Cannot find entity: " + headClassName);
			}
		}

		System.out.println("headEntities=" + headEntities);
	}

	public Collection<PamelaEntity> getAccessibleEntities() {
		if (accessibleEntities == null) {
			buildAccessibleEntities();
		}
		return accessibleEntities;
	}

	private void buildAccessibleEntities() {
		MutableGraph<PamelaEntity> graph = GraphBuilder.directed().build();
		entities.values().forEach(graph::addNode);
		for (PamelaEntity entity : graph.nodes()) {
			for (PamelaEntity refEntity : entity.getReferencedEntities()) {
				// System.out.println("entity " + entity + " references: " + refEntity);
				graph.putEdge(entity, refEntity);
			}
			for (PamelaEntity superEntity : entity.getSuperEntities()) {
				// System.out.println("entity " + entity + " has super: " + superEntity);
				graph.putEdge(entity, superEntity);
			}
		}

		// Graph<PamelaEntity> closure = Graphs.transitiveClosure(graph);

		if (getHeadEntities().size() == 0) {
			accessibleEntities = new ArrayList<>();
		}
		else if (getHeadEntities().size() == 1) {
			PamelaEntity uniqueHeadEntity = getHeadEntities().iterator().next();
			Set<PamelaEntity> reachableEntities = graph.successors(uniqueHeadEntity);
			accessibleEntities = new ArrayList<>();
			accessibleEntities.add(uniqueHeadEntity);
			accessibleEntities.addAll(reachableEntities);
		}
		else {
			Set<PamelaEntity> reachableEntities = new HashSet<>();
			for (PamelaEntity headEntity : getHeadEntities()) {
				Set<PamelaEntity> reachedEntities = graph.successors(headEntity);
				reachableEntities.add(headEntity);
				reachableEntities.addAll(reachedEntities);
			}
			accessibleEntities = new ArrayList<>(reachableEntities);
		}
	}

}
