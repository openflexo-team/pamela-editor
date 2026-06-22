package org.openflexo.pamela.editor.diagram;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.openflexo.diana.connectors.ConnectorSpecification.ConnectorType;
import org.openflexo.diana.connectors.RectPolylinConnectorSpecification.RectPolylinAdjustability;
import org.openflexo.diana.connectors.RectPolylinConnectorSpecification.RectPolylinConstraints;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.pamela.editor.ui.preferences.ConnectorStylePreference;
import org.openflexo.pamela.model.StringConverterLibrary;
import org.openflexo.pamela.model.StringConverterLibrary.Converter;

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

                // hidden member lists — absent in legacy files → none hidden.
                readStringList(viewNode, "hiddenProperties", ev::addToHiddenProperties);
                readStringList(viewNode, "hiddenInitializers", ev::addToHiddenInitializers);
                readStringList(viewNode, "hiddenMethods", ev::addToHiddenMethods);

                // Resolve transient entity reference
                if (qn != null && session.getMetaModel() != null) {
                    ev.setEntity(session.getMetaModel().getEntity(qn));
                }

                diagram.addToEntityViews(ev);
            }
        }

        // connectorViews — connector overrides (label position and/or style). Absent in
        // legacy files. Entries with no "type" default to a property view (legacy format).
        JsonNode connectorsNode = root.get("connectorViews");
        if (connectorsNode != null && connectorsNode.isArray()) {
            for (JsonNode cvNode : connectorsNode) {
                String source = cvNode.has("source") ? cvNode.get("source").asText() : null;
                String target = cvNode.has("target") ? cvNode.get("target").asText() : null;
                String type = cvNode.has("type") ? cvNode.get("type").asText() : "property";
                String style = cvNode.has("style") ? cvNode.get("style").asText() : null;

                ConnectorView cv;
                if ("inheritance".equals(type)) {
                    cv = factory.newInheritanceView(source, target);
                } else {
                    String property = cvNode.has("property") ? cvNode.get("property").asText() : null;
                    PropertyView pv = factory.newPropertyView(source, target, property);
                    pv.setLabelX(cvNode.has("labelX") ? cvNode.get("labelX").asDouble() : 0.0);
                    pv.setLabelY(cvNode.has("labelY") ? cvNode.get("labelY").asDouble() : 0.0);
                    cv = pv;
                }
                if (style != null && !style.isEmpty()) {
                    cv.setStyleId(style);
                }
                diagram.addToConnectorViews(cv);
            }
        }

        // Embedded styles (entity look + used connector styles) — §6bis.
        JsonNode entityStyleNode = root.get("entityStyle");
        if (entityStyleNode != null && entityStyleNode.isObject()) {
            DiagramEntityStyle es = factory.newInstance(DiagramEntityStyle.class);
            readEntityStyle(entityStyleNode, es);
            diagram.setEntityStyle(es);
        }
        JsonNode stylesNode = root.get("connectorStyles");
        if (stylesNode != null && stylesNode.isArray()) {
            for (JsonNode sn : stylesNode) {
                diagram.addToConnectorStyles(readConnectorStyle(sn, factory));
            }
        }
        if (root.has("defaultInheritanceStyleId")) {
            diagram.setDefaultInheritanceStyleId(root.get("defaultInheritanceStyleId").asText());
        }
        if (root.has("defaultAssociationStyleId")) {
            diagram.setDefaultAssociationStyleId(root.get("defaultAssociationStyleId").asText());
        }
        // Legacy file with no embedded styles → snapshot the current preference defaults.
        if (diagram.getEntityStyle() == null && diagram.getConnectorStyles().isEmpty()) {
            DiagramStyleSnapshot.initializeFromDefaults(diagram, factory);
        }

        // Ensure every style explicitly referenced by a connector is embedded, so the diagram is
        // self-contained (§6bis) and the renderer resolves it as an embedded style. The defaults
        // snapshot above only embeds the two per-kind default styles; a connector may reference a
        // non-default catalogue style (e.g. inheritance-rect-polylin while the default is
        // inheritance-line). Without this, diagramStyleFor() would fall back to the global
        // catalogue at render time but the chosen style would never be persisted.
        for (ConnectorView cv : diagram.getConnectorViews()) {
            String id = cv.getStyleId();
            if (id != null && !id.isEmpty()) {
                DiagramStyleSnapshot.captureConnectorStyle(diagram, id, factory);
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
                // hidden member lists — only written when non-empty (default = none hidden).
                writeStringList(viewNode, "hiddenProperties", ev.getHiddenProperties());
                writeStringList(viewNode, "hiddenInitializers", ev.getHiddenInitializers());
                writeStringList(viewNode, "hiddenMethods", ev.getHiddenMethods());
                viewsArray.add(viewNode);
            }
        }
        root.set("entityViews", viewsArray);

        // connectorViews — only connectors carrying non-default data are persisted: a moved
        // label and/or a non-default style. PropertyViews store the label offset; both kinds
        // store their style reference. Connectors with default appearance are recomputed.
        ArrayNode connectorsArray = MAPPER.createArrayNode();
        if (diagram.getConnectorViews() != null) {
            for (ConnectorView cv : diagram.getConnectorViews()) {
                if (!cv.isPersistable()) {
                    continue;
                }
                ObjectNode cvNode = MAPPER.createObjectNode();
                cvNode.put("source",
                        cv.getSourceQualifiedName() != null ? cv.getSourceQualifiedName() : "");
                cvNode.put("target",
                        cv.getTargetQualifiedName() != null ? cv.getTargetQualifiedName() : "");
                if (cv instanceof PropertyView) {
                    PropertyView pv = (PropertyView) cv;
                    cvNode.put("type", "property");
                    cvNode.put("property",
                            pv.getPropertyIdentifier() != null ? pv.getPropertyIdentifier() : "");
                    cvNode.put("labelX", pv.getLabelX());
                    cvNode.put("labelY", pv.getLabelY());
                } else if (cv instanceof InheritanceView) {
                    cvNode.put("type", "inheritance");
                }
                if (cv.getStyleId() != null && !cv.getStyleId().isEmpty()) {
                    cvNode.put("style", cv.getStyleId());
                }
                connectorsArray.add(cvNode);
            }
        }
        root.set("connectorViews", connectorsArray);

        // Embedded styles (entity look + used connector styles) — §6bis.
        root.set("entityStyle", writeEntityStyle(diagram.getEntityStyle()));
        ArrayNode stylesArray = MAPPER.createArrayNode();
        if (diagram.getConnectorStyles() != null) {
            for (ConnectorStylePreference s : diagram.getConnectorStyles()) {
                stylesArray.add(writeConnectorStyle(s));
            }
        }
        root.set("connectorStyles", stylesArray);
        if (diagram.getDefaultInheritanceStyleId() != null) {
            root.put("defaultInheritanceStyleId", diagram.getDefaultInheritanceStyleId());
        }
        if (diagram.getDefaultAssociationStyleId() != null) {
            root.put("defaultAssociationStyleId", diagram.getDefaultAssociationStyleId());
        }

        MAPPER.writeValue(diagramFile, root);
    }

    /** Writes a non-empty string list as a JSON array field (skipped when null/empty). */
    private static void writeStringList(ObjectNode node, String field, java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        ArrayNode array = MAPPER.createArrayNode();
        for (String v : values) {
            array.add(v);
        }
        node.set(field, array);
    }

    /** Reads a string-array field (if present) and feeds each value to the consumer. */
    private static void readStringList(JsonNode node, String field, java.util.function.Consumer<String> sink) {
        JsonNode array = node.get(field);
        if (array != null && array.isArray()) {
            for (JsonNode v : array) {
                sink.accept(v.asText());
            }
        }
    }

    // ---- Embedded styles (entity look + used connector styles) — §6bis -------------------

    private static final Converter<Color> COLOR_CONV =
            StringConverterLibrary.getInstance().getConverter(Color.class);
    private static final Converter<Font> FONT_CONV =
            StringConverterLibrary.getInstance().getConverter(Font.class);

    private static void putColor(ObjectNode node, String field, Color c) {
        if (c != null) {
            node.put(field, COLOR_CONV.convertToString(c));
        }
    }

    private static Color readColor(JsonNode node, String field) {
        try {
            return node.has(field) ? COLOR_CONV.convertFromString(node.get(field).asText(), null) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void putFont(ObjectNode node, String field, Font f) {
        if (f != null) {
            node.put(field, FONT_CONV.convertToString(f));
        }
    }

    private static Font readFont(JsonNode node, String field) {
        try {
            return node.has(field) ? FONT_CONV.convertFromString(node.get(field).asText(), null) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static ObjectNode writeEntityStyle(DiagramEntityStyle es) {
        ObjectNode node = MAPPER.createObjectNode();
        if (es != null) {
            putColor(node, "headerBackgroundColor", es.getHeaderBackgroundColor());
            putColor(node, "bodyBackgroundColor", es.getBodyBackgroundColor());
            putColor(node, "borderColor", es.getBorderColor());
            putFont(node, "titleFont", es.getTitleFont());
            putFont(node, "memberFont", es.getMemberFont());
        }
        return node;
    }

    private static void readEntityStyle(JsonNode node, DiagramEntityStyle es) {
        Color hb = readColor(node, "headerBackgroundColor");
        if (hb != null) es.setHeaderBackgroundColor(hb);
        Color bb = readColor(node, "bodyBackgroundColor");
        if (bb != null) es.setBodyBackgroundColor(bb);
        Color bc = readColor(node, "borderColor");
        if (bc != null) es.setBorderColor(bc);
        Font tf = readFont(node, "titleFont");
        if (tf != null) es.setTitleFont(tf);
        Font mf = readFont(node, "memberFont");
        if (mf != null) es.setMemberFont(mf);
    }

    private static ObjectNode writeConnectorStyle(ConnectorStylePreference s) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("id", s.getId() != null ? s.getId() : "");
        node.put("name", s.getName() != null ? s.getName() : "");
        if (s.getConnectorType() != null) node.put("connectorType", s.getConnectorType().name());
        putColor(node, "color", s.getColor());
        node.put("lineWidth", s.getLineWidth());
        node.put("straightLineWhenPossible", s.getStraightLineWhenPossible());
        node.put("rounded", s.getRounded());
        node.put("arcSize", s.getArcSize());
        if (s.getAdjustability() != null) node.put("adjustability", s.getAdjustability().name());
        if (s.getConstraints() != null) node.put("constraints", s.getConstraints().name());
        return node;
    }

    private static ConnectorStylePreference readConnectorStyle(JsonNode node,
            PamelaClassDiagramFactory factory) {
        ConnectorStylePreference s = factory.newInstance(ConnectorStylePreference.class);
        if (node.has("id")) s.setId(node.get("id").asText());
        if (node.has("name")) s.setName(node.get("name").asText());
        if (node.has("connectorType")) {
            try { s.setConnectorType(ConnectorType.valueOf(node.get("connectorType").asText())); }
            catch (Exception ignored) { }
        }
        Color c = readColor(node, "color");
        if (c != null) s.setColor(c);
        if (node.has("lineWidth")) s.setLineWidth(node.get("lineWidth").asDouble());
        if (node.has("straightLineWhenPossible")) s.setStraightLineWhenPossible(node.get("straightLineWhenPossible").asBoolean());
        if (node.has("rounded")) s.setRounded(node.get("rounded").asBoolean());
        if (node.has("arcSize")) s.setArcSize(node.get("arcSize").asInt());
        if (node.has("adjustability")) {
            try { s.setAdjustability(RectPolylinAdjustability.valueOf(node.get("adjustability").asText())); }
            catch (Exception ignored) { }
        }
        if (node.has("constraints")) {
            try { s.setConstraints(RectPolylinConstraints.valueOf(node.get("constraints").asText())); }
            catch (Exception ignored) { }
        }
        return s;
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
