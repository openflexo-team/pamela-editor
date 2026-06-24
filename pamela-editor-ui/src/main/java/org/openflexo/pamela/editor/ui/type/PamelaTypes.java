package org.openflexo.pamela.editor.ui.type;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.openflexo.connie.type.CustomType;
import org.openflexo.pamela.editor.model.SourceMetaModel;

/**
 * Conversion helpers between a {@code java.lang.reflect.Type} (as edited by the
 * Gina {@code TypeSelector}) and the qualified-name strings the core mutation
 * primitives consume.
 */
public final class PamelaTypes {

    private static final Map<String, Class<?>> PRIMITIVES = new HashMap<>();
    static {
        PRIMITIVES.put("boolean", boolean.class);
        PRIMITIVES.put("byte", byte.class);
        PRIMITIVES.put("char", char.class);
        PRIMITIVES.put("short", short.class);
        PRIMITIVES.put("int", int.class);
        PRIMITIVES.put("long", long.class);
        PRIMITIVES.put("float", float.class);
        PRIMITIVES.put("double", double.class);
    }

    private PamelaTypes() {
    }

    /** The qualified name to pass to the core primitives for the given type. */
    public static String qualifiedNameOf(Type type) {
        if (type == null) {
            return null;
        }
        if (type instanceof PamelaEntityType) {
            return ((PamelaEntityType) type).getQualifiedName();
        }
        if (type instanceof Class) {
            return ((Class<?>) type).getName();
        }
        if (type instanceof CustomType) {
            return ((CustomType) type).getSerializationRepresentation();
        }
        return type.getTypeName();
    }

    /**
     * Best-effort reverse: builds a {@code Type} from a qualified name —
     * a {@link PamelaEntityType} if the name is a known entity, a primitive
     * {@code Class}, a loadable {@code Class}, or an (unresolved) entity type.
     */
    public static Type forQualifiedName(String qualifiedName, SourceMetaModel metaModel) {
        if (qualifiedName == null || qualifiedName.isEmpty()) {
            return null;
        }
        if (metaModel != null && metaModel.getEntity(qualifiedName) != null) {
            return new PamelaEntityType(qualifiedName, metaModel);
        }
        Class<?> primitive = PRIMITIVES.get(qualifiedName);
        if (primitive != null) {
            return primitive;
        }
        try {
            return Class.forName(qualifiedName);
        } catch (Throwable t) {
            return new PamelaEntityType(qualifiedName, metaModel);
        }
    }
}
