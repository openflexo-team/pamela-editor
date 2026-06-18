package org.openflexo.pamela.editor.diagram;

import org.openflexo.pamela.editor.model.SourceModelEntity;

/**
 * Implementation of {@link InheritanceView}. Holds the transient resolved sub/super
 * entities; inheritance links have no label.
 */
public abstract class InheritanceViewImpl extends ConnectorViewImpl implements InheritanceView {

    private SourceModelEntity subEntity;
    private SourceModelEntity superEntity;

    @Override
    public ConnectorStyle.Category getStyleCategory() {
        return ConnectorStyle.Category.INHERITANCE;
    }

    @Override
    public SourceModelEntity getSubEntity() {
        return subEntity;
    }

    @Override
    public void setSubEntity(SourceModelEntity subEntity) {
        this.subEntity = subEntity;
    }

    @Override
    public SourceModelEntity getSuperEntity() {
        return superEntity;
    }

    @Override
    public void setSuperEntity(SourceModelEntity superEntity) {
        this.superEntity = superEntity;
    }
}
