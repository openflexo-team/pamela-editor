package org.openflexo.pamela.editor.diagram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelInitializer;
import org.openflexo.pamela.editor.model.SourceModelProperty;

/**
 * Implementation class for {@link EntityView}.
 * Manages the transient {@code entity} reference that is not handled by PAMELA.
 */
public abstract class EntityViewImpl implements EntityView {

    /** Transient — not serialized, resolved at load time. */
    private SourceModelEntity entity;

    @Override
    public SourceModelEntity getEntity() {
        return entity;
    }

    @Override
    public void setEntity(SourceModelEntity entity) {
        this.entity = entity;
    }

    @Override
    public String getDisplayLabel() {
        if (entity != null) {
            String simple = entity.getSimpleName();
            return entity.isAbstract() ? ("« abstract »\n" + simple) : simple;
        }
        String qn = getQualifiedName();
        return "« unresolved »\n" + (qn != null ? qn : "?");
    }

    @Override
    public String getEntitySimpleName() {
        if (entity != null) return entity.getSimpleName();
        String qn = getQualifiedName();
        if (qn == null) return "(?)";
        int dot = qn.lastIndexOf('.');
        String simple = dot >= 0 ? qn.substring(dot + 1) : qn;
        return simple + " (?)";
    }

    @Override
    public List<SourceModelProperty> getDisplayedProperties() {
        if (entity == null) return Collections.emptyList();
        return new ArrayList<>(entity.getDeclaredProperties().values());
    }

    @Override
    public List<SourceModelInitializer> getDisplayedInitializers() {
        if (entity == null) return Collections.emptyList();
        return entity.getInitializers();
    }
}
