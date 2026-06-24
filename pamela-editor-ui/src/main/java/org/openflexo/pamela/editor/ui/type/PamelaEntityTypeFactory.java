package org.openflexo.pamela.editor.ui.type;

import java.beans.PropertyChangeSupport;

import org.openflexo.connie.type.CustomTypeFactory;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.toolbox.HasPropertyChangeSupport;

/**
 * {@link CustomTypeFactory} that builds {@link PamelaEntityType}s.
 *
 * <p>It also holds the <b>current configuration</b> (the qualified name selected
 * in the editor). This is how the user's choice reaches the {@code TypeSelector}:
 * the editor calls {@link #setConfiguredQualifiedName(String)}, which fires a
 * change; {@code TypeSelector} listens to the factory and regenerates its edited
 * object via {@code makeCustomType(null)} — which reads this configuration.</p>
 */
public class PamelaEntityTypeFactory
        implements CustomTypeFactory<PamelaEntityType>, HasPropertyChangeSupport {

    private final PropertyChangeSupport pcSupport = new PropertyChangeSupport(this);
    private final SourceMetaModel metaModel;
    private String configuredQualifiedName;

    public PamelaEntityTypeFactory(SourceMetaModel metaModel) {
        this.metaModel = metaModel;
    }

    public SourceMetaModel getMetaModel() {
        return metaModel;
    }

    /** Set by the editor when the user picks an entity; fires so TypeSelector regenerates. */
    public void setConfiguredQualifiedName(String qualifiedName) {
        String old = this.configuredQualifiedName;
        this.configuredQualifiedName = qualifiedName;
        pcSupport.firePropertyChange("configuredQualifiedName", old, qualifiedName);
    }

    public String getConfiguredQualifiedName() {
        return configuredQualifiedName;
    }

    @Override
    public PamelaEntityType makeCustomType(String configuration) {
        String qn = configuration != null ? configuration : configuredQualifiedName;
        return new PamelaEntityType(qn, metaModel);
    }

    @Override
    public Class<PamelaEntityType> getCustomType() {
        return PamelaEntityType.class;
    }

    @Override
    public void configureFactory(PamelaEntityType type) {
        if (type != null) {
            this.configuredQualifiedName = type.getQualifiedName();
        }
    }

    @Override
    public PropertyChangeSupport getPropertyChangeSupport() {
        return pcSupport;
    }

    @Override
    public String getDeletedProperty() {
        return null;
    }
}
