package project;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaSource;
import model.PamelaEntity;
import model.PamelaModel;
import org.openflexo.pamela.annotations.ModelEntity;
import qdox.OrderedModelWriter;

import java.io.File;
import java.util.*;

/**
 * Created by adria on 24/01/2017.
 */
public class PamelaProject {

    /**
     * Creates the corresponding Pamela Entity of the targeted file. If it isn't a PamelaEntity, returns null.
     *
     * @param path The path to the entity.
     * @return
     * @throws Exception
     */
    public static PamelaEntity createEntity(File path) throws Exception {
        JavaProjectBuilder builder = new JavaProjectBuilder();
        JavaSource src = builder.addSource(path);
        JavaClass source = src.getClasses().get(0);
        boolean isPamelaEntity = source.getAnnotations()
                .stream()
                .anyMatch(annotation -> Objects.equals(annotation.getType().getCanonicalName(), ModelEntity.class.getCanonicalName()));
        if (source.isInterface() && isPamelaEntity) return new PamelaEntity(source);
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
     * @param headClassNames The canonical names of the targeted head classes.
     * @param directoryPath  The directory path, containing all the entities.
     * @return
     * @throws Exception
     */
    public static List<PamelaModel> createModels(List<String> headClassNames, File directoryPath) throws Exception {
        List<PamelaModel> models = new ArrayList<>();
        for (String headClass : headClassNames) {
            models.add(createModel(headClass, directoryPath));
        }
        return models;
    }

    /**
     * Creates a Pamela Model with the specified directory and head class.
     *
     * @param headClassName The canonical name of the targeted head class.
     * @param directoryPath The directory path, containing all the entities.
     * @return
     * @throws Exception
     */
    public static PamelaModel createModel(String headClassName, File directoryPath) throws Exception {
        List<PamelaEntity> entities = new ArrayList<>();
        for (File path : directoryPath.listFiles()) {
            if (path.getAbsolutePath().endsWith(".java")) {
                PamelaEntity entity = createEntity(path);
                if (entity != null) entities.add(entity);
            }
        }

        /* Populates referenced entities and superEntities */
        for (PamelaEntity entity : entities) {
            entity.resolveDependencies(entities);
        }
        /* Get head entity */
        Optional<PamelaEntity> headEntity = entities.stream()
                .filter(entity -> Objects.equals(entity.getCanonicalName(), headClassName))
                .findFirst();

        /* Computes transitive closure to build PamelaModel */
        if (headEntity.isPresent()) return transitiveClosure(headEntity.get(), entities);
        else throw new Exception("Specified head class " + headClassName + " not found.");
    }

    private static PamelaModel transitiveClosure(PamelaEntity headEntity, List<PamelaEntity> entities) {
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
        return new PamelaModel(new ArrayList<>(reachableEntities));
    }
}
