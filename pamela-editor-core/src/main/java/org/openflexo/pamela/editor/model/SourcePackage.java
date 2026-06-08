package org.openflexo.pamela.editor.model;

import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.openflexo.toolbox.HasPropertyChangeSupport;

import spoon.reflect.declaration.CtPackage;

/**
 * Groups {@link SourceModelEntity} and {@link SourceJavaFile} instances by
 * Java package.
 *
 * <p>A {@code SourcePackage} can be created in two ways:
 * <ol>
 *   <li><b>Filesystem scan</b> (triggered by {@link SourceMetaModel#addSourceDirectory}):
 *       built from a directory path alone, before Spoon has run. {@code ctPackage}
 *       is {@code null} at this stage.</li>
 *   <li><b>Phase-1 discovery</b> ({@link SourceMetaModel#buildMetaModel}): the
 *       existing {@code SourcePackage} is enriched with its Spoon {@link CtPackage}
 *       reference and its entity list is populated.</li>
 * </ol>
 * </p>
 *
 * <p>{@link #getNonEntityJavaFiles()} filters lazily: it returns files whose
 * qualified name is not yet a known entity in the owning meta-model. No separate
 * state needs to be maintained.</p>
 */
public class SourcePackage implements SourceElement, HasPropertyChangeSupport {

    private final PropertyChangeSupport pcSupport = new PropertyChangeSupport(this);

    @Override
    public PropertyChangeSupport getPropertyChangeSupport() {
        return pcSupport;
    }

    @Override
    public String getDeletedProperty() {
        return null;
    }

    // May be null before buildMetaModel() has run for this package.
    private CtPackage ctPackage;

    private final SourceMetaModel metaModel;
    private final String qualifiedName;
    private final List<SourceModelEntity> entities;

    /** All .java files discovered by the filesystem scan for this package. */
    private final List<SourceJavaFile> javaFiles;

    /**
     * Filesystem-scan constructor: no Spoon reference yet.
     *
     * @param qualifiedName the Java package name (empty string for the default package)
     * @param metaModel     the owning meta-model
     */
    SourcePackage(String qualifiedName, SourceMetaModel metaModel) {
        this.ctPackage     = null;
        this.metaModel     = metaModel;
        this.qualifiedName = qualifiedName != null ? qualifiedName : "";
        this.entities      = new ArrayList<>();
        this.javaFiles     = new ArrayList<>();
    }

    /**
     * Spoon-backed constructor: used when a package is first seen during
     * Phase-1 discovery and was not pre-created by the filesystem scan.
     */
    SourcePackage(CtPackage ctPackage, SourceMetaModel metaModel) {
        this.ctPackage     = ctPackage;
        this.metaModel     = metaModel;
        this.qualifiedName = ctPackage.getQualifiedName();
        this.entities      = new ArrayList<>();
        this.javaFiles     = new ArrayList<>();
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
     * All {@code @ModelEntity} types discovered in this package (in discovery order).
     */
    public List<SourceModelEntity> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    /**
     * All {@code .java} files found in this package's directory by the
     * filesystem scan (includes both entity and non-entity files).
     */
    public List<SourceJavaFile> getJavaFiles() {
        return Collections.unmodifiableList(javaFiles);
    }

    /**
     * Java files in this package whose qualified name is <em>not</em> a known
     * {@code @ModelEntity} in the owning meta-model.
     *
     * <p>Computed lazily on every call — no separate state is maintained.</p>
     */
    public List<SourceJavaFile> getNonEntityJavaFiles() {
        return javaFiles.stream()
                .filter(f -> metaModel.getEntity(f.getQualifiedName()) == null)
                .collect(Collectors.toList());
    }

    /**
     * All browsable members of this package — {@link SourceModelEntity}s and
     * non-entity {@link SourceJavaFile}s — merged into a single list sorted by
     * simple name (case-insensitive).
     *
     * <p>The single, name-sorted list gives a <em>stable</em> ordering that
     * survives the file → entity transition: when a Java file is declared as a
     * {@code @ModelEntity} it keeps its alphabetical position and only its kind
     * (and icon) changes, instead of jumping between separate "files" and
     * "entities" groups.</p>
     */
    public List<SourceElement> getMembers() {
        List<SourceElement> members = new ArrayList<>();
        members.addAll(entities);
        members.addAll(getNonEntityJavaFiles());
        members.sort(Comparator.comparing(SourcePackage::memberSimpleName,
                String.CASE_INSENSITIVE_ORDER));
        return members;
    }

    private static String memberSimpleName(SourceElement member) {
        if (member instanceof SourceModelEntity) {
            return ((SourceModelEntity) member).getSimpleName();
        }
        if (member instanceof SourceJavaFile) {
            return ((SourceJavaFile) member).getSimpleName();
        }
        return "";
    }

    /** Simple name: last segment of the qualified name, or {@code "<default>"}. */
    public String getSimpleName() {
        if (isDefault()) {
            return "<default>";
        }
        int dot = qualifiedName.lastIndexOf('.');
        return dot < 0 ? qualifiedName : qualifiedName.substring(dot + 1);
    }

    /** Number of {@code @ModelEntity} types in this package. */
    public int getEntitiesCount() {
        return entities.size();
    }

    /** Number of abstract entities in this package. */
    public int getAbstractEntitiesCount() {
        int count = 0;
        for (SourceModelEntity e : entities) {
            if (e.isAbstract()) count++;
        }
        return count;
    }

    /** Total number of declared properties across all entities in this package. */
    public int getTotalPropertiesCount() {
        int count = 0;
        for (SourceModelEntity e : entities) {
            count += e.getDeclaredProperties().size();
        }
        return count;
    }

    /**
     * {@code true} if this is the default (unnamed) package.
     */
    public boolean isDefault() {
        return qualifiedName.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Package-private mutation helpers
    // -------------------------------------------------------------------------

    void addEntity(SourceModelEntity entity) {
        if (!entities.contains(entity)) {
            entities.add(entity);
        }
    }

    void removeEntity(SourceModelEntity entity) {
        entities.remove(entity);
    }

    void addJavaFile(SourceJavaFile file) {
        if (!javaFiles.contains(file)) {
            javaFiles.add(file);
            pcSupport.firePropertyChange("javaFiles", null, Collections.unmodifiableList(javaFiles));
            pcSupport.firePropertyChange("nonEntityJavaFiles", null, null);
        }
    }

    void clearJavaFiles() {
        if (!javaFiles.isEmpty()) {
            javaFiles.clear();
            pcSupport.firePropertyChange("javaFiles", null, Collections.unmodifiableList(javaFiles));
            pcSupport.firePropertyChange("nonEntityJavaFiles", null, null);
        }
    }

    void clearEntities() {
        entities.clear();
    }

    /** Links the Spoon package once {@code buildMetaModel()} has run. */
    void setCtPackage(CtPackage ctPackage) {
        this.ctPackage = ctPackage;
    }

    CtPackage getCtPackage() {
        return ctPackage;
    }

    @Override
    public String toString() {
        return "SourcePackage(" + (isDefault() ? "<default>" : qualifiedName) + ")";
    }
}
