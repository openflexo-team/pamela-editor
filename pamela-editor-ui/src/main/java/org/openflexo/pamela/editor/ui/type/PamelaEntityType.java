package org.openflexo.pamela.editor.ui.type;

import java.beans.PropertyChangeSupport;
import java.lang.reflect.Type;

import org.openflexo.connie.type.CustomType;
import org.openflexo.connie.type.ConnieType;
import org.openflexo.connie.type.TypingSpace;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.toolbox.HasPropertyChangeSupport;

/**
 * A Connie {@link CustomType} representing a PAMELA <b>source</b> entity (a
 * {@code @ModelEntity} interface that is parsed by Spoon but not compiled, so it
 * cannot be a {@code java.lang.Class}). This lets the Gina {@code TypeSelector}
 * widget offer and edit source entity types alongside real Java types.
 *
 * <p>The type is identified by its qualified name; resolution against the
 * metamodel is best-effort (the entity may not yet exist).</p>
 */
public class PamelaEntityType implements CustomType, HasPropertyChangeSupport {

    private final PropertyChangeSupport pcSupport = new PropertyChangeSupport(this);

    private final String qualifiedName;
    private final SourceMetaModel metaModel;

    public PamelaEntityType(String qualifiedName, SourceMetaModel metaModel) {
        this.qualifiedName = qualifiedName;
        this.metaModel = metaModel;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public SourceModelEntity getEntity() {
        return metaModel != null ? metaModel.getEntity(qualifiedName) : null;
    }

    // --- CustomType ---------------------------------------------------------

    @Override
    public Class<?> getBaseClass() {
        // Source entities have no compiled class; Object is the safe upper bound.
        return Object.class;
    }

    @Override
    public boolean isTypeAssignableFrom(Type aType, boolean permissive) {
        return aType instanceof PamelaEntityType
                && qualifiedName != null
                && qualifiedName.equals(((PamelaEntityType) aType).qualifiedName);
    }

    @Override
    public boolean isOfType(Object object, boolean permissive) {
        return false;
    }

    @Override
    public String simpleRepresentation() {
        if (qualifiedName == null) {
            return "?";
        }
        int dot = qualifiedName.lastIndexOf('.');
        return dot >= 0 ? qualifiedName.substring(dot + 1) : qualifiedName;
    }

    @Override
    public String fullQualifiedRepresentation() {
        return qualifiedName;
    }

    @Override
    public String getSerializationRepresentation() {
        return qualifiedName;
    }

    @Override
    public boolean isResolved() {
        return getEntity() != null;
    }

    @Override
    public void resolve() {
        // No-op: resolution is computed live against the metamodel.
    }

    @Override
    public ConnieType translateTo(TypingSpace typingSpace) {
        return this;
    }

    @Override
    public String getTypeName() {
        return qualifiedName;
    }

    @Override
    public String toString() {
        return simpleRepresentation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PamelaEntityType)) {
            return false;
        }
        PamelaEntityType other = (PamelaEntityType) o;
        return qualifiedName == null ? other.qualifiedName == null
                : qualifiedName.equals(other.qualifiedName);
    }

    @Override
    public int hashCode() {
        return qualifiedName == null ? 0 : qualifiedName.hashCode();
    }

    // --- HasPropertyChangeSupport -------------------------------------------

    @Override
    public PropertyChangeSupport getPropertyChangeSupport() {
        return pcSupport;
    }

    @Override
    public String getDeletedProperty() {
        return null;
    }
}
