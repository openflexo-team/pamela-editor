package org.openflexo.pamela.editor.ui.preferences;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.factory.PamelaModelFactory;
import org.openflexo.pamela.model.ModelEntity;
import org.openflexo.pamela.model.ModelProperty;
import org.openflexo.pamela.model.StringConverterLibrary.Converter;

/**
 * Generic JSON (de)serialization of a preferences tree (see {@code preferences-design.md §4}).
 *
 * <p>The <em>structure</em> of the tree is owned by {@link PreferencesRegistry} (built before
 * loading); this serializer only persists / overlays the <em>values</em>. Each node is written
 * as a JSON object keyed by its {@code name}; its string-convertible SINGLE/LIST properties
 * (everything except {@code name}/{@code children}/{@code parent}) are introspected from the
 * PAMELA metamodel — so a new theme needs no serializer code.</p>
 *
 * <p>Unknown JSON keys are ignored (backward-compatible); properties absent from the JSON keep
 * their PAMELA default (forward-compatible). Enum properties are not supported yet.</p>
 */
public final class PreferencesSerializer {

    private static final Logger logger = Logger.getLogger(PreferencesSerializer.class.getPackage().getName());

    private static final int VERSION = 1;
    private static final String VERSION_KEY = "version";

    private final PamelaModelFactory factory;
    private final ObjectMapper mapper;

    public PreferencesSerializer(PamelaModelFactory factory) {
        this.factory = factory;
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    // ----------------------------------------------------------------- save

    public void save(PamelaEditorPreferencesModel model, File file) throws IOException {
        writeTree(toJsonTree(model), file);
    }

    /** Serializes the whole model to an in-memory JSON tree (no file I/O). */
    public ObjectNode toJsonTree(PamelaEditorPreferencesModel model) {
        ObjectNode root = mapper.createObjectNode();
        root.put(VERSION_KEY, VERSION);
        for (PreferencesNode child : model.getChildren()) {
            root.set(child.getName(), serializeNode(child));
        }
        return root;
    }

    /** Copies all persistent values from {@code src} into {@code dst} (same tree shape). */
    public void copyValues(PamelaEditorPreferencesModel src, PamelaEditorPreferencesModel dst) {
        applyJsonTree(dst, toJsonTree(src));
    }

    /**
     * The model's JSON tree without the application-state subtrees ({@code general/window},
     * {@code general/recent}) — the basis for the dialog's dirty/equality checks, since app
     * state is managed outside the Apply/Save flow.
     */
    public ObjectNode toThematicJsonTree(PamelaEditorPreferencesModel model) {
        ObjectNode root = toJsonTree(model);
        JsonNode general = root.get("general");
        if (general instanceof ObjectNode) {
            ((ObjectNode) general).remove("window");
            ((ObjectNode) general).remove("recent");
        }
        return root;
    }

    /** A single node's own scalar / embedded-list values (no child nodes) — for per-node comparison. */
    public ObjectNode scalarsToJson(PreferencesNode node) {
        ObjectNode obj = mapper.createObjectNode();
        writeEntity(node, obj);
        return obj;
    }

    /**
     * Persists <em>only</em> the application-state subtrees ({@code general/window} and
     * {@code general/recent}) into the existing file, leaving the thematic preferences on disk
     * untouched (so an unsaved {@code Apply} of thematic prefs is not leaked by a window move).
     */
    public void saveAppState(PamelaEditorPreferencesModel model, File file) throws IOException {
        ObjectNode root;
        if (file.exists()) {
            JsonNode existing = mapper.readTree(file);
            root = (existing instanceof ObjectNode) ? (ObjectNode) existing : mapper.createObjectNode();
        }
        else {
            root = mapper.createObjectNode();
        }
        root.put(VERSION_KEY, VERSION);
        PreferencesNode general = model.getChild("general");
        if (general != null) {
            ObjectNode generalNode = (root.get("general") instanceof ObjectNode)
                    ? (ObjectNode) root.get("general") : root.putObject("general");
            PreferencesNode window = general.getChild("window");
            if (window != null) {
                generalNode.set("window", serializeNode(window));
            }
            PreferencesNode recent = general.getChild("recent");
            if (recent != null) {
                generalNode.set("recent", serializeNode(recent));
            }
        }
        writeTree(root, file);
    }

    private void writeTree(ObjectNode root, File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        mapper.writeValue(file, root);
    }

    private ObjectNode serializeNode(PreferencesNode node) {
        ObjectNode obj = mapper.createObjectNode();
        writeEntity(node, obj);
        for (PreferencesNode child : node.getChildren()) {
            obj.set(child.getName(), serializeNode(child));
        }
        return obj;
    }

    /** Writes the scalar / convertible-list / embedded-entity-list properties of any PAMELA object. */
    private void writeEntity(org.openflexo.pamela.AccessibleProxyObject obj, ObjectNode json) {
        for (ModelProperty<?> property : persistentProperties(obj)) {
            String key = property.getPropertyIdentifier();
            Class<?> type = property.getType();
            try {
                if (property.getCardinality() == Cardinality.LIST) {
                    Object value = obj.objectForKey(key);
                    if (value instanceof List && !((List<?>) value).isEmpty()) {
                        ArrayNode array = json.putArray(key);
                        for (Object element : (List<?>) value) {
                            if (isModelEntityType(type)) {
                                ObjectNode elementNode = mapper.createObjectNode();
                                writeEntity((org.openflexo.pamela.AccessibleProxyObject) element, elementNode);
                                array.add(elementNode);
                            }
                            else {
                                array.add(scalarToString(type, element));
                            }
                        }
                    }
                }
                else {
                    Object value = obj.objectForKey(key);
                    if (value == null) {
                        continue;
                    }
                    writeSingle(json, key, type, value);
                }
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "Could not serialize preference property " + key, e);
            }
        }
    }

    private void writeSingle(ObjectNode obj, String key, Class<?> type, Object value) {
        if (type == boolean.class || type == Boolean.class) {
            obj.put(key, (Boolean) value);
        }
        else if (type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == short.class || type == Short.class) {
            obj.put(key, ((Number) value).longValue());
        }
        else if (type == double.class || type == Double.class
                || type == float.class || type == Float.class) {
            obj.put(key, ((Number) value).doubleValue());
        }
        else if (type == String.class) {
            obj.put(key, (String) value);
        }
        else if (type.isEnum()) {
            obj.put(key, ((Enum<?>) value).name());
        }
        else {
            obj.put(key, scalarToString(type, value));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String scalarToString(Class<?> type, Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        Converter converter = factory.getStringEncoder().converterForClass(type);
        if (converter == null) {
            return String.valueOf(value);
        }
        return converter.convertToString(value);
    }

    // ----------------------------------------------------------------- load

    /**
     * Overlays the values from {@code file} onto the already-built {@code model} (structure
     * comes from {@link PreferencesRegistry}). Missing file / unknown keys are tolerated.
     */
    public void applyJson(PamelaEditorPreferencesModel model, File file) throws IOException {
        if (file == null || !file.exists()) {
            return;
        }
        JsonNode root = mapper.readTree(file);
        applyJsonTree(model, root);
    }

    /** Overlays values from an in-memory JSON tree onto the model (no file I/O). */
    public void applyJsonTree(PamelaEditorPreferencesModel model, JsonNode root) {
        if (root == null || !root.isObject()) {
            return;
        }
        for (PreferencesNode child : model.getChildren()) {
            apply(child, root.get(child.getName()));
        }
    }

    private void apply(PreferencesNode node, JsonNode json) {
        if (json == null || !json.isObject()) {
            return;
        }
        applyEntity(node, json);
        for (PreferencesNode child : node.getChildren()) {
            apply(child, json.get(child.getName()));
        }
    }

    /** Overlays scalar / convertible-list / embedded-entity-list values onto any PAMELA object. */
    private void applyEntity(org.openflexo.pamela.AccessibleProxyObject obj, JsonNode json) {
        if (json == null || !json.isObject()) {
            return;
        }
        for (ModelProperty<?> property : persistentProperties(obj)) {
            String key = property.getPropertyIdentifier();
            JsonNode valueNode = json.get(key);
            if (valueNode == null || valueNode.isNull()) {
                continue;
            }
            Class<?> type = property.getType();
            try {
                if (property.getCardinality() == Cardinality.LIST) {
                    if (valueNode.isArray()) {
                        if (isModelEntityType(type)) {
                            applyEntityList(obj, key, type, (ArrayNode) valueNode);
                        }
                        else {
                            applyList(obj, key, type, (ArrayNode) valueNode);
                        }
                    }
                }
                else {
                    obj.setObjectForKey(readSingle(valueNode, type), key);
                }
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "Could not load preference property " + key, e);
            }
        }
    }

    private Object readSingle(JsonNode valueNode, Class<?> type) throws Exception {
        if (type == boolean.class || type == Boolean.class) {
            return valueNode.asBoolean();
        }
        if (type == int.class || type == Integer.class) {
            return valueNode.asInt();
        }
        if (type == long.class || type == Long.class) {
            return valueNode.asLong();
        }
        if (type == short.class || type == Short.class) {
            return (short) valueNode.asInt();
        }
        if (type == double.class || type == Double.class) {
            return valueNode.asDouble();
        }
        if (type == float.class || type == Float.class) {
            return (float) valueNode.asDouble();
        }
        if (type == String.class) {
            return valueNode.asText();
        }
        if (type.isEnum()) {
            return enumValue(type, valueNode.asText());
        }
        return stringToScalar(type, valueNode.asText());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object enumValue(Class<?> type, String name) {
        return Enum.valueOf((Class<? extends Enum>) type, name);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object stringToScalar(Class<?> type, String text) throws Exception {
        Converter converter = factory.getStringEncoder().converterForClass(type);
        if (converter == null) {
            return text;
        }
        return converter.convertFromString(text, factory);
    }

    @SuppressWarnings("unchecked")
    private void applyList(org.openflexo.pamela.AccessibleProxyObject obj, String key,
            Class<?> elementType, ArrayNode array) throws Exception {
        Object current = obj.objectForKey(key);
        if (!(current instanceof List)) {
            return;
        }
        List<Object> list = (List<Object>) current;
        list.clear();
        for (JsonNode element : array) {
            if (elementType == String.class) {
                list.add(element.asText());
            }
            else {
                list.add(stringToScalar(elementType, element.asText()));
            }
        }
    }

    /** Recreates an embedded-entity LIST from JSON: one factory instance per array element. */
    @SuppressWarnings("unchecked")
    private void applyEntityList(org.openflexo.pamela.AccessibleProxyObject obj, String key,
            Class<?> elementType, ArrayNode array) throws Exception {
        Object current = obj.objectForKey(key);
        if (!(current instanceof List)) {
            return;
        }
        List<Object> list = (List<Object>) current;
        list.clear();
        for (JsonNode element : array) {
            if (!element.isObject()) {
                continue;
            }
            Object instance = factory.newInstance(elementType);
            applyEntity((org.openflexo.pamela.AccessibleProxyObject) instance, element);
            list.add(instance);
        }
    }

    /** True when {@code type} is a PAMELA {@code @ModelEntity} interface (an embeddable element). */
    private static boolean isModelEntityType(Class<?> type) {
        return type != null
                && type.getAnnotation(org.openflexo.pamela.annotations.ModelEntity.class) != null;
    }

    // ----------------------------------------------------------------- introspection

    /** Persistent (string-convertible, enum, or embedded-entity-list) properties of any PAMELA object. */
    private List<ModelProperty<?>> persistentProperties(org.openflexo.pamela.AccessibleProxyObject obj) {
        List<ModelProperty<?>> result = new ArrayList<>();
        boolean isNode = obj instanceof PreferencesNode;
        try {
            ModelEntity<?> entity = factory.getModelEntityForInstance(obj);
            if (entity == null) {
                return result;
            }
            Iterator<? extends ModelProperty<?>> it = entity.getProperties();
            while (it.hasNext()) {
                ModelProperty<?> property = it.next();
                String key = property.getPropertyIdentifier();
                // Structural relations of the tree are handled by JSON nesting, not as properties.
                if (PreferencesNode.CHILDREN.equals(key) || PreferencesNode.PARENT.equals(key)) {
                    continue;
                }
                // A node's name is the JSON nesting key; an embedded entity's name is real data.
                if (isNode && PreferencesNode.NAME.equals(key)) {
                    continue;
                }
                if (property.getGetter() != null && property.getGetter().isDerived()) {
                    continue;
                }
                Class<?> type = property.getType();
                if (type == null) {
                    continue;
                }
                // Embedded-entity LIST (e.g. connector styles) — serialized as nested objects.
                if (property.getCardinality() == Cardinality.LIST && isModelEntityType(type)
                        && !PreferencesNode.class.isAssignableFrom(type)) {
                    result.add(property);
                    continue;
                }
                if (PreferencesNode.class.isAssignableFrom(type)) {
                    continue;
                }
                boolean convertible = type == String.class
                        || type.isEnum()
                        || factory.getStringEncoder().converterForClass(type) != null;
                if (!convertible) {
                    continue;
                }
                result.add(property);
            }
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Could not introspect preferences object " + obj, e);
        }
        return result;
    }
}
