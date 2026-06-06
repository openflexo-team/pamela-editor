package org.openflexo.pamela.editor.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import spoon.reflect.declaration.CtPackage;

/**
 * Groups {@link SourceModelEntity} instances by Java package.
 *
 * <p>A {@code SourcePackage} is created only for packages that contain
 * at least one discovered entity (Phase 1 of {@link SourceMetaModel}
 * construction).</p>
 */
public class SourcePackage implements SourceElement {

    // Internal Spoon reference — never exposed in the public API
    private final CtPackage ctPackage;

    private final SourceMetaModel metaModel;
    private final String qualifiedName;
    private final List<SourceModelEntity> entities;

    /**
     * Constructs a {@code SourcePackage}.
     *
     * @param ctPackage the Spoon package (must not be {@code null})
     * @param metaModel the owning meta-model
     */
    public SourcePackage(CtPackage ctPackage, SourceMetaModel metaModel) {
        this.ctPackage = ctPackage;
        this.metaModel = metaModel;
        this.qualifiedName = ctPackage.getQualifiedName();
        this.entities = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * The fully qualified package name, e.g. {@code "org.example"}.
     * Returns an empty string for the default (unnamed) package.
     */
    public String getQualifiedName() {
        return qualifiedName;
    }

    /** The owning meta-model. */
    public SourceMetaModel getMetaModel() {
        return metaModel;
    }

    /**
     * The entities discovered in this package (in discovery order).
     * The returned list is unmodifiable; use {@link #addEntity} internally.
     */
    public List<SourceModelEntity> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    /**
     * {@code true} if this is the default (unnamed) package,
     * i.e. {@link #getQualifiedName()} returns an empty string.
     */
    public boolean isDefault() {
        return qualifiedName == null || qualifiedName.isEmpty();
    }

    /**
     * Adds a discovered entity to this package.
     * Called internally during Phase 1 of meta-model construction.
     *
     * @param entity the entity to register (must not be {@code null})
     */
    void addEntity(SourceModelEntity entity) {
        if (!entities.contains(entity)) {
            entities.add(entity);
        }
    }

    /**
     * Removes an entity from this package.
     * Called internally when an entity is deleted.
     *
     * @param entity the entity to remove
     */
    void removeEntity(SourceModelEntity entity) {
        entities.remove(entity);
    }

    /**
     * Returns the internal Spoon package.
     * Package-private — used by mutation operations in {@link SourceMetaModel}.
     */
    CtPackage getCtPackage() {
        return ctPackage;
    }

    @Override
    public String toString() {
        return "SourcePackage(" + (isDefault() ? "<default>" : qualifiedName) + ")";
    }
}
