package org.openflexo.pamela.editor.diagram;

import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.editor.model.SourceModelEntity;

/**
 * Connector view representing an inheritance link between two entities present on the
 * diagram (the source entity extends the target entity).
 *
 * <p>Inheritance links carry no label and cannot be hidden, so an
 * {@code InheritanceView} has no non-default data and is <em>never</em> persisted — it
 * exists only as a transient, computed drawable. The transient sub/super references are
 * resolved at load/rebuild time.</p>
 */
@ModelEntity
@ImplementationClass(InheritanceViewImpl.class)
public interface InheritanceView extends ConnectorView {

    /**
     * Transient reference to the resolved sub-entity (the source, which extends the
     * super-entity). Not managed by PAMELA — implemented in {@link InheritanceViewImpl}.
     */
    SourceModelEntity getSubEntity();

    void setSubEntity(SourceModelEntity subEntity);

    /**
     * Transient reference to the resolved super-entity (the target).
     * Not managed by PAMELA — implemented in {@link InheritanceViewImpl}.
     */
    SourceModelEntity getSuperEntity();

    void setSuperEntity(SourceModelEntity superEntity);
}
