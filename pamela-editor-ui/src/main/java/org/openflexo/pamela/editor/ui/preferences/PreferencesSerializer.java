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
        ObjectNode root = mapper.createObjectNode();
        root.put(VERSION_KEY, VERSION);
        for (PreferencesNode child : model.getChildren()) {
            root.set(child.getName(), serializeNode(child));
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        mapper.writeValue(file, root);
    }

    private ObjectNode serializeNode(PreferencesNode node) {
        ObjectNode obj = mapper.createObjectNode();
        writeScalars(node, obj);
        for (PreferencesNode child : node.getChildren()) {
            obj.set(child.getName(), serializeNode(child));
        }
        return obj;
    }

    private void writeScalars(PreferencesNode node, ObjectNode obj) {
        for (ModelProperty<?> property : persistentProperties(node)) {
            String key = property.getPropertyIdentifier();
            Class<?> type = property.getType();
            try {
                if (property.getCardinality() == Cardinality.LIST) {
                    Object value = node.objectForKey(key);
                    if (value instanceof List && !((List<?>) value).isEmpty()) {
                        ArrayNode array = obj.putArray(key);
                        for (Object element : (List<?>) value) {
                            array.add(scalarToString(type, element));
                        }
                    }
                }
                else {
                    Object value = node.objectForKey(key);
                    if (value == null) {
                        continue;
                    }
                    writeSingle(obj, key, type, value);
                }
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "Could not serialize preference " + node.getPath() + "#" + key, e);
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
        for (ModelProperty<?> property : persistentProperties(node)) {
            String key = property.getPropertyIdentifier();
            JsonNode valueNode = json.get(key);
            if (valueNode == null || valueNode.isNull()) {
                continue;
            }
            Class<?> type = property.getType();
            try {
                if (property.getCardinality() == Cardinality.LIST) {
                    if (valueNode.isArray()) {
                        applyList(node, key, type, (ArrayNode) valueNode);
                    }
                }
                else {
                    node.setObjectForKey(readSingle(valueNode, type), key);
                }
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "Could not load preference " + node.getPath() + "#" + key, e);
            }
        }
        for (PreferencesNode child : node.getChildren()) {
            apply(child, json.get(child.getName()));
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
        return stringToScalar(type, valueNode.asText());
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
    private void applyList(PreferencesNode node, String key, Class<?> elementType, ArrayNode array)
            throws Exception {
        Object current = node.objectForKey(key);
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

    // ----------------------------------------------------------------- introspection

    /** Persistent (string-convertible, non-structural) properties of a node. */
    private List<ModelProperty<?>> persistentProperties(PreferencesNode node) {
        List<ModelProperty<?>> result = new ArrayList<>();
        try {
            ModelEntity<?> entity = factory.getModelEntityForInstance(node);
            if (entity == null) {
                return result;
            }
            Iterator<? extends ModelProperty<?>> it = entity.getProperties();
            while (it.hasNext()) {
                ModelProperty<?> property = it.next();
                String key = property.getPropertyIdentifier();
                if (PreferencesNode.NAME.equals(key)
                        || PreferencesNode.CHILDREN.equals(key)
                        || PreferencesNode.PARENT.equals(key)) {
                    continue;
                }
                if (property.getGetter() != null && property.getGetter().isDerived()) {
                    continue;
                }
                Class<?> type = property.getType();
                if (type == null || PreferencesNode.class.isAssignableFrom(type)) {
                    continue;
                }
                boolean convertible = type == String.class
                        || factory.getStringEncoder().converterForClass(type) != null;
                if (!convertible) {
                    continue;
                }
                result.add(property);
            }
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Could not introspect preferences node " + node.getPath(), e);
        }
        return result;
    }
}
