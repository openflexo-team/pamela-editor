package org.openflexo.pamela.editor.diagram;

import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.preferences.PreferencesManager;

/**
 * Implementation of {@link InheritanceView}. Holds the transient resolved sub/super
 * entities; inheritance links have no label.
 */
public abstract class InheritanceViewImpl extends ConnectorViewImpl implements InheritanceView {

    private SourceModelEntity subEntity;
    private SourceModelEntity superEntity;

    @Override
    public String getDefaultStyleId() {
        PamelaClassDiagram d = getOwningDiagram();
        if (d != null && d.getDefaultInheritanceStyleId() != null) {
            return d.getDefaultInheritanceStyleId();
        }
        return PreferencesManager.getInstance().connectors().getDefaultInheritanceStyleId();
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
