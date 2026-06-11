package org.openflexo.pamela.editor.ui.widget;

import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.List;

import org.openflexo.toolbox.HasPropertyChangeSupport;

import spoon.reflect.declaration.CtType;

/**
 * Lightweight data model for the {@link SpoonOutlineView}.
 *
 * <p>Holds the list of top-level {@link CtType}s to display and fires
 * {@link java.beans.PropertyChangeEvent}s so Gina bindings refresh when the
 * content changes (e.g. after an on-demand mini-parse or a rebuild).
 * Implements {@link HasPropertyChangeSupport} so Gina registers as a listener
 * and picks up {@code rootTypes} changes automatically (gina-analysis.md §18.5).</p>
 */
public class SpoonOutlineModel implements HasPropertyChangeSupport {

    public static final String ROOT_TYPES = "rootTypes";
    public static final String LOADING    = "loading";

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private List<CtType<?>> rootTypes = Collections.emptyList();
    private boolean loading = false;

    @Override
    public PropertyChangeSupport getPropertyChangeSupport() {
        return pcs;
    }

    @Override
    public String getDeletedProperty() {
        return null;
    }

    public List<CtType<?>> getRootTypes() {
        return rootTypes;
    }

    public void setRootTypes(List<CtType<?>> types) {
        List<CtType<?>> old = this.rootTypes;
        this.rootTypes = types != null ? types : Collections.emptyList();
        pcs.firePropertyChange(ROOT_TYPES, old, this.rootTypes);
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        boolean old = this.loading;
        this.loading = loading;
        pcs.firePropertyChange(LOADING, old, loading);
    }
}
