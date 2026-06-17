package org.openflexo.pamela.editor.diagram;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Serializes and deserializes a {@link PamelaClassDiagram} to/from a
 * {@code .diagram} sidecar file (JSON content).
 *
 * <h3>File format</h3>
 * <pre>
 * {
 *   "id": "overview",
 *   "name": "Overview",
 *   "entityViews": [
 *     {
 *       "qualifiedName": "org.example.Person",
 *       "x": 100.0,
 *       "y": 150.0,
 *       "width": 200.0,
 *       "height": 120.0
 *     }
 *   ],
 *   "connectorViews": [
 *     {
 *       "source": "org.example.Order",
 *       "target": "org.example.Person",
 *       "property": "customer",
 *       "labelX": 12.0,
 *       "labelY": -4.0
 *     }
 *   ]
 * }
 * </pre>
 *
 * <p>{@code connectorViews} stores association-label position overrides; entries
 * exist only for connectors whose label was moved.</p>
 *
 * <p>Only the stable {@code id}, the display {@code name} and positional data are
 * stored. The transient {@code entity} reference on {@link EntityView} is resolved
 * at load time via the session's
 * {@link org.openflexo.pamela.editor.model.SourceMetaModel}.</p>
 *
 * <p>The file name is {@code <id>.diagram}, where {@code id} is the diagram's stable
 * identifier (decoupled from its display name — see {@link #sidecarFileName(String)}).</p>
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

    /** Sidecar file extension (JSON content). */
    public static final String EXTENSION = ".diagram";

    /**
     * Returns the sidecar file name for a diagram with the given stable id:
     * {@code "<id>.diagram"}.
     *
     * @param diagramId the diagram's stable id (e.g. {@code "overview"})
     * @return the file name (e.g. {@code "overview.diagram"})
     */
    public static String sidecarFileName(String diagramId) {
        String id = (diagramId == null || diagramId.isEmpty()) ? "untitled" : diagramId;
        return id + EXTENSION;
    }

    /**
     * Turns a display name into a filesystem/VCS-friendly base id: lower-cased,
     * non-alphanumeric runs collapsed to a single {@code '-'}, trimmed of leading
     * and trailing dashes. Never returns an empty string.
     *
     * @param name the diagram display name (may be null/empty)
     * @return a slug such as {@code "my-overview"} (or {@code "diagram"} as a fallback)
     */
    public static String slugify(String name) {
        if (name == null) {
            return "diagram";
        }
        String slug = name.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        return slug.isEmpty() ? "diagram" : slug;
    }

    // -------------------------------------------------------------------------
    // Load
    // -------------------------------------------------------------------------

    /**
     * Loads a {@link PamelaClassDiagram} from a {@code .diagram} file.
     *
     * <p>Entity views are created via {@link PamelaProject#getDiagramFactory()}.
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
    public static PamelaClassDiagram load(File diagramFile, PamelaProject session)
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

        // id — stable identifier; fall back to the file-name stem for legacy files
        JsonNode idNode = root.get("id");
        if (idNode != null && !idNode.isNull() && !idNode.asText().isEmpty()) {
            diagram.setId(idNode.asText());
        } else {
            diagram.setId(stemOf(diagramFile));
        }

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

                // Compartment visibility flags — absent in legacy files → default true.
                ev.setDisplayInitializers(
                        !viewNode.has("displayInitializers") || viewNode.get("displayInitializers").asBoolean());
                ev.setDisplayProperties(
                        !viewNode.has("displayProperties") || viewNode.get("displayProperties").asBoolean());
                ev.setDisplayMethods(
                        !viewNode.has("displayMethods") || viewNode.get("displayMethods").asBoolean());

                // hiddenProperties — absent in legacy files → none hidden.
                JsonNode hiddenNode = viewNode.get("hiddenProperties");
                if (hiddenNode != null && hiddenNode.isArray()) {
                    for (JsonNode hid : hiddenNode) {
                        ev.addToHiddenProperties(hid.asText());
                    }
                }

                // Resolve transient entity reference
                if (qn != null && session.getMetaModel() != null) {
                    ev.setEntity(session.getMetaModel().getEntity(qn));
                }

                diagram.addToEntityViews(ev);
            }
        }

        // connectorViews — PropertyView label-position overrides (absent in legacy files).
        JsonNode connectorsNode = root.get("connectorViews");
        if (connectorsNode != null && connectorsNode.isArray()) {
            for (JsonNode cvNode : connectorsNode) {
                String source = cvNode.has("source") ? cvNode.get("source").asText() : null;
                String target = cvNode.has("target") ? cvNode.get("target").asText() : null;
                String property = cvNode.has("property") ? cvNode.get("property").asText() : null;
                PropertyView pv = factory.newPropertyView(source, target, property);
                pv.setLabelX(cvNode.has("labelX") ? cvNode.get("labelX").asDouble() : 0.0);
                pv.setLabelY(cvNode.has("labelY") ? cvNode.get("labelY").asDouble() : 0.0);
                diagram.addToConnectorViews(pv);
            }
        }

        return diagram;
    }

    // -------------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------------

    /**
     * Saves a {@link PamelaClassDiagram} to a {@code .diagram} file.
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
        root.put("id", diagram.getId() != null ? diagram.getId() : "");
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
                viewNode.put("displayInitializers", ev.getDisplayInitializers());
                viewNode.put("displayProperties", ev.getDisplayProperties());
                viewNode.put("displayMethods", ev.getDisplayMethods());
                // hiddenProperties — only written when non-empty (default = none hidden).
                if (ev.getHiddenProperties() != null && !ev.getHiddenProperties().isEmpty()) {
                    ArrayNode hidden = MAPPER.createArrayNode();
                    for (String id : ev.getHiddenProperties()) {
                        hidden.add(id);
                    }
                    viewNode.set("hiddenProperties", hidden);
                }
                viewsArray.add(viewNode);
            }
        }
        root.set("entityViews", viewsArray);

        // connectorViews — only PropertyViews whose label was moved (non-default) are
        // persisted; inheritance links and default-positioned labels are recomputed.
        ArrayNode connectorsArray = MAPPER.createArrayNode();
        if (diagram.getConnectorViews() != null) {
            for (ConnectorView cv : diagram.getConnectorViews()) {
                if (!(cv instanceof PropertyView) || !cv.isPersistable()) {
                    continue;
                }
                PropertyView pv = (PropertyView) cv;
                ObjectNode cvNode = MAPPER.createObjectNode();
                cvNode.put("source",
                        pv.getSourceQualifiedName() != null ? pv.getSourceQualifiedName() : "");
                cvNode.put("target",
                        pv.getTargetQualifiedName() != null ? pv.getTargetQualifiedName() : "");
                cvNode.put("property",
                        pv.getPropertyIdentifier() != null ? pv.getPropertyIdentifier() : "");
                cvNode.put("labelX", pv.getLabelX());
                cvNode.put("labelY", pv.getLabelY());
                connectorsArray.add(cvNode);
            }
        }
        root.set("connectorViews", connectorsArray);

        MAPPER.writeValue(diagramFile, root);
    }

    /** Returns the file name without its {@code .diagram} (or any) extension. */
    private static String stemOf(File file) {
        String n = file.getName();
        if (n.endsWith(EXTENSION)) {
            return n.substring(0, n.length() - EXTENSION.length());
        }
        int dot = n.lastIndexOf('.');
        return (dot > 0) ? n.substring(0, dot) : n;
    }
}
