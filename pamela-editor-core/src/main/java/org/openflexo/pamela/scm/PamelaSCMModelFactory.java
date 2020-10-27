package org.openflexo.pamela.scm;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.openflexo.pamela.annotations.ModelEntity;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaSource;

import qdox.OrderedModelWriter;

/**
 * Created by adria on 24/01/2017.
 */
public class PamelaSCMModelFactory {

	/*public static void main(String[] args) {
		File currentDir = new File(System.getProperty("user.dir"));
		File resource3dir = new File(currentDir, "src/test/java/org/openflexo/pamela/scm/test/resources3");
		System.out.println("currentdir=" + currentDir);
		System.out.println("resource3dir=" + resource3dir + " exist: " + resource3dir.exists());
		makePamelaModel(resource3dir, "org.openflexo.pamela.scm.test.resources3.ChildEntity2");
	}*/

	public static void main(String[] args) {
		File currentDir = new File(System.getProperty("user.dir"));
		File resource4dir1 = new File(currentDir, "src/test/java/org/openflexo/pamela/scm/test/resources4/package1");
		File resource4dir2 = new File(currentDir, "src/test/java/org/openflexo/pamela/scm/test/resources4/package2");

		makePamelaModel(Arrays.asList(resource4dir1, resource4dir2), Arrays.asList(
				"org.openflexo.pamela.scm.test.resources4.package1.Entity1"/*, "org.openflexo.pamela.scm.test.resources4.package2.Entity2"*/));
	}

	public static PamelaSCMModel makePamelaModel(File sourceDirectory, String headClassName) {
		return makePamelaModel(Collections.singletonList(sourceDirectory), Collections.singletonList(headClassName));
	}

	public static PamelaSCMModel makePamelaModel(Collection<File> sourceDirectories, String headClassName) {
		return makePamelaModel(sourceDirectories, Collections.singletonList(headClassName));
	}

	public static PamelaSCMModel makePamelaModel(File sourceDirectory, Collection<String> headClassNames) {
		return makePamelaModel(Collections.singletonList(sourceDirectory), headClassNames);
	}

	public static PamelaSCMModel makePamelaModel(Collection<File> sourceDirectories, Collection<String> headClassNames) {

		PamelaSCMModel returned = new PamelaSCMModel(sourceDirectories, headClassNames);
		return returned;
	}

	/**
	 * Creates the corresponding Pamela Entity of the targeted file. If it isn't a PamelaEntity, returns null.
	 *
	 * @param path
	 *            The path to the entity.
	 * @return
	 * @throws Exception
	 */
	public static PamelaEntity createEntity(File path) throws Exception {
		JavaProjectBuilder builder = new JavaProjectBuilder();
		JavaSource src = builder.addSource(path);
		JavaClass source = src.getClasses().get(0);
		boolean isPamelaEntity = source.getAnnotations().stream()
				.anyMatch(annotation -> Objects.equals(annotation.getType().getCanonicalName(), ModelEntity.class.getCanonicalName()));
		if (source.isInterface() && isPamelaEntity)
			return new PamelaEntity(source);
		return null;
	}

	public static String serialize(PamelaEntity entity) {
		OrderedModelWriter writer = new OrderedModelWriter();
		writer.writeClass(entity.getContainingInterface());
		return writer.toString();
	}

	/**
	 * Creates the Pamela Models with the files contained by the directory, with the targeted head classes.
	 *
	 * @param headClassNames
	 *            The canonical names of the targeted head classes.
	 * @param directoryPath
	 *            The directory path, containing all the entities.
	 * @return
	 * @throws Exception
	 */
	public static List<PamelaSCMModel> createModels(List<String> headClassNames, File directoryPath) throws Exception {
		List<PamelaSCMModel> models = new ArrayList<>();
		for (String headClass : headClassNames) {
			models.add(createModel(headClass, directoryPath));
		}
		return models;
	}

	/**
	 * Creates a Pamela Model with the specified directory and head class.
	 *
	 * @param headClassName
	 *            The canonical name of the targeted head class.
	 * @param directoryPath
	 *            The directory path, containing all the entities.
	 * @return
	 * @throws Exception
	 */
	public static PamelaSCMModel createModel(String headClassName, File directoryPath) throws Exception {
		List<PamelaEntity> entities = new ArrayList<>();
		for (File path : directoryPath.listFiles()) {
			if (path.getAbsolutePath().endsWith(".java")) {
				PamelaEntity entity = createEntity(path);
				if (entity != null)
					entities.add(entity);
			}
		}

		/* Populates referenced entities and superEntities */
		for (PamelaEntity entity : entities) {
			entity.resolveDependencies(entities);
		}
		/* Get head entity */
		Optional<PamelaEntity> headEntity = entities.stream().filter(entity -> Objects.equals(entity.getCanonicalName(), headClassName))
				.findFirst();

		/* Computes transitive closure to build PamelaModel */
		if (headEntity.isPresent())
			return transitiveClosure(headEntity.get(), entities);
		else
			throw new Exception("Specified head class " + headClassName + " not found.");
	}

	private static PamelaSCMModel transitiveClosure(PamelaEntity headEntity, List<PamelaEntity> entities) {
		MutableGraph<PamelaEntity> graph = GraphBuilder.undirected().build();
		entities.forEach(graph::addNode);
		for (PamelaEntity entity : graph.nodes()) {
			for (PamelaEntity refEntity : entity.getReferencedEntities()) {
				graph.putEdge(entity, refEntity);
			}
			for (PamelaEntity superEntity : entity.getSuperEntities()) {
				graph.putEdge(entity, superEntity);
			}
		}
		Graph<PamelaEntity> closure = Graphs.transitiveClosure(graph);
		Set<PamelaEntity> reachableEntities = closure.adjacentNodes(headEntity);
		// return new PamelaSCMModel(new ArrayList<>(reachableEntities));
		return null;
	}
}
