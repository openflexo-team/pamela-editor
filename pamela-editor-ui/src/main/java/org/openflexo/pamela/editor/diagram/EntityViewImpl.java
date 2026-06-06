package org.openflexo.pamela.editor.diagram;

import org.openflexo.pamela.editor.model.SourceModelEntity;

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
}
