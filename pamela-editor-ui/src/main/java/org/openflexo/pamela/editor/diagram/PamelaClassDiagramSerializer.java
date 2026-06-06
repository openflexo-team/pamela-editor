package org.openflexo.pamela.editor.diagram;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.openflexo.pamela.editor.ui.PamelaEditorSession;

/**
 * Serializes and deserializes a {@link PamelaClassDiagram} to/from a
 * {@code .diagram.json} sidecar file.
 *
 * <h3>File format</h3>
 * <pre>
 * {
 *   "name": "Overview",
 *   "entityViews": [
 *     {
 *       "qualifiedName": "org.example.Person",
 *       "x": 100.0,
 *       "y": 150.0,
 *       "width": 200.0,
 *       "height": 120.0
 *     }
 *   ]
 * }
 * </pre>
 *
 * <p>Only positional data is stored. The transient {@code entity} reference
 * on {@link EntityView} is resolved at load time via the session's
 * {@link org.openflexo.pamela.editor.model.SourceMetaModel}.</p>
 *
 * <p>The file name is derived from {@link PamelaClassDiagram#getName()} by
 * replacing spaces with underscores and appending {@code .diagram.json}.</p>
 */
public class PamelaClassDiagramSerializer {

    private static final Logger logger =
            Logger.getLogger(PamelaClassDiagramSerializer.class.getPackage().getName());

    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private PamelaClassDiagramSerializer() {
    }

    // -------------------------------------------------------------------------
    // File name convention
    // -------------------------------------------------------------------------

    /**
     * Returns the sidecar file name for a diagram with the given name.
     * Spaces are replaced with underscores; the extension {@code .diagram.json}
     * is appended.
     *
     * @param diagramName the diagram's display name (e.g. {@code "Overview"})
     * @return the file name (e.g. {@code "Overview.diagram.json"})
     */
    public static String sidecarFileName(String diagramName) {
        if (diagramName == null || diagramName.isEmpty()) {
            return "Untitled.diagram.json";
        }
        return diagramName.replace(' ', '_') + ".diagram.json";
    }

    // -------------------------------------------------------------------------
    // Load
    // -------------------------------------------------------------------------

    /**
     * Loads a {@link PamelaClassDiagram} from a {@code .diagram.json} file.
     *
     * <p>Entity views are created via {@link PamelaEditorSession#getDiagramFactory()}.
     * The transient {@code entity} reference is resolved immediately if the
     * session's meta-model is available.</p>
     *
     * @param diagramFile the sidecar file to read; must exist
     * @param session     the session that owns this diagram (provides the factory
     *                    and the meta-model for entity resolution)
     * @return a fully populated {@link PamelaClassDiagram}
     * @throws IOException if the file cannot be read or parsed, or if the
     *                     session's {@link PamelaClassDiagramFactory} is {@code null}
     */
    public static PamelaClassDiagram load(File diagramFile, PamelaEditorSession session)
            throws IOException {
        if (!diagramFile.exists()) {
            throw new IOException("Diagram file not found: "
                    + diagramFile.getAbsolutePath());
        }
        PamelaClassDiagramFactory factory = session.getDiagramFactory();
        if (factory == null) {
            throw new IOException(
                    "Cannot load diagram: session has no diagram factory");
        }

        JsonNode root = MAPPER.readTree(diagramFile);

        PamelaClassDiagram diagram = factory.newInstance(PamelaClassDiagram.class);

        // name
        JsonNode nameNode = root.get("name");
        if (nameNode != null && !nameNode.isNull()) {
            diagram.setName(nameNode.asText());
        }

        // entityViews
        JsonNode viewsNode = root.get("entityViews");
        if (viewsNode != null && viewsNode.isArray()) {
            for (JsonNode viewNode : viewsNode) {
                String qn = viewNode.has("qualifiedName")
                        ? viewNode.get("qualifiedName").asText() : null;
                double x = viewNode.has("x") ? viewNode.get("x").asDouble() : 100.0;
                double y = viewNode.has("y") ? viewNode.get("y").asDouble() : 100.0;
                double w = viewNode.has("width") ? viewNode.get("width").asDouble() : 200.0;
                double h = viewNode.has("height") ? viewNode.get("height").asDouble() : 120.0;

                EntityView ev = factory.newEntityView(qn, x, y, w, h);

                // Resolve transient entity reference
                if (qn != null && session.getMetaModel() != null) {
                    ev.setEntity(session.getMetaModel().getEntity(qn));
                }

                diagram.addToEntityViews(ev);
            }
        }

        return diagram;
    }

    // -------------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------------

    /**
     * Saves a {@link PamelaClassDiagram} to a {@code .diagram.json} file.
     *
     * <p>Parent directories are created as needed. The file is overwritten if
     * it already exists.</p>
     *
     * @param diagram     the diagram to persist
     * @param diagramFile the destination file
     * @throws IOException if the file cannot be written
     */
    public static void save(PamelaClassDiagram diagram, File diagramFile) throws IOException {
        File parent = diagramFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", diagram.getName() != null ? diagram.getName() : "");

        ArrayNode viewsArray = MAPPER.createArrayNode();
        if (diagram.getEntityViews() != null) {
            for (EntityView ev : diagram.getEntityViews()) {
                ObjectNode viewNode = MAPPER.createObjectNode();
                viewNode.put("qualifiedName",
                        ev.getQualifiedName() != null ? ev.getQualifiedName() : "");
                viewNode.put("x", ev.getX());
                viewNode.put("y", ev.getY());
                viewNode.put("width", ev.getWidth());
                viewNode.put("height", ev.getHeight());
                viewsArray.add(viewNode);
            }
        }
        root.set("entityViews", viewsArray);

        MAPPER.writeValue(diagramFile, root);
    }
}
