package org.openflexo.pamela.editor.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.ModelEntity.InitPolicy;
import org.openflexo.pamela.annotations.XMLElement;

import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtType;

/**
 * Corresponds to PAMELA's {@code ModelEntity<I>}.
 * Represents one {@code @ModelEntity}-annotated type (expected to be an
 * interface; a {@link Error} is fired if it is a class).
 *
 * <p>This class stores only the properties <em>declared directly</em> on this
 * type.  Inherited properties are accessed via {@link #getAllProperties()},
 * which walks {@link #getDirectSuperEntities()} recursively.</p>
 *
 * <p>Validation rules (fired during construction / Phase 2):
 * <ul>
 *   <li>{@code @ModelEntity} on a class → {@link Error}</li>
 *   <li>Non-abstract entity with no {@code @Initializer} and
 *       {@code initPolicy = REQUIRED} → {@link Warning}</li>
 * </ul>
 * </p>
 */
public class SourceModelEntity implements SourceElement {

    // Internal Spoon reference — never exposed in the public API
    private final CtType<?> ctType;

    // Identity
    private final String qualifiedName;
    private final String simpleName;
    private SourcePackage sourcePackage;
    private final SourceMetaModel metaModel;

    // @ModelEntity annotation parameters
    private final boolean abstractEntity;
    private final InitPolicy initPolicy;
    private final boolean inheritInitializers;

    // Source file
    private SourceCompilationUnit compilationUnit;

    // Implementation class (set during Phase 2; null if @ImplementationClass is absent)
    private SourceImplementationClass implementationClass;

    // Properties declared on this type only
    private final Map<String, SourceModelProperty> declaredProperties;

    // Hierarchy — direct parents only (no transitive closure stored)
    private final List<SourceModelEntity> directSuperEntities;

    // Lifecycle
    private final List<SourceModelInitializer> initializers;

    // Serialization
    private final String xmlTag;

    // Diagnostics
    private final List<Issue> issues;

    /**
     * Constructs a {@code SourceModelEntity} from a Spoon type node.
     *
     * @param ctType    the Spoon type (interface or class annotated {@code @ModelEntity})
     * @param metaModel the owning meta-model
     */
    public SourceModelEntity(CtType<?> ctType, SourceMetaModel metaModel) {
        this.ctType = ctType;
        this.metaModel = metaModel;
        this.qualifiedName = ctType.getQualifiedName();
        this.simpleName = ctType.getSimpleName();
        this.declaredProperties = new LinkedHashMap<>();
        this.directSuperEntities = new ArrayList<>();
        this.initializers = new ArrayList<>();
        this.issues = new ArrayList<>();

        // Read @ModelEntity annotation parameters
        ModelEntity annotation = ctType.getAnnotation(ModelEntity.class);
        this.abstractEntity = (annotation != null) && annotation.isAbstract();
        this.initPolicy = (annotation != null) ? annotation.initPolicy() : InitPolicy.REQUIRED;
        this.inheritInitializers = (annotation != null) && annotation.inheritInitializers();

        // Validation: @ModelEntity on a class
        if (!(ctType instanceof CtInterface)) {
            addIssue(new Error("@ModelEntity " + qualifiedName + " must be an interface, not a class"));
        }

        // @XMLElement on the type itself
        XMLElement xmlElement = ctType.getAnnotation(XMLElement.class);
        this.xmlTag = (xmlElement != null && !xmlElement.xmlTag().isEmpty())
                ? xmlElement.xmlTag()
                : null;
    }

    // -------------------------------------------------------------------------
    // Internal setters called by SourceMetaModel during construction
    // -------------------------------------------------------------------------

    void setSourcePackage(SourcePackage pkg) {
        this.sourcePackage = pkg;
    }

    void setCompilationUnit(SourceCompilationUnit cu) {
        this.compilationUnit = cu;
    }

    void setImplementationClass(SourceImplementationClass impl) {
        this.implementationClass = impl;
    }

    void addDeclaredProperty(SourceModelProperty property) {
        declaredProperties.put(property.getPropertyIdentifier(), property);
    }

    void addDirectSuperEntity(SourceModelEntity superEntity) {
        if (!directSuperEntities.contains(superEntity)) {
            directSuperEntities.add(superEntity);
        }
    }

    void addInitializer(SourceModelInitializer initializer) {
        initializers.add(initializer);
    }

    private void addIssue(Issue issue) {
        issues.add(issue);
        metaModel.fireIssue(issue);
    }

    // -------------------------------------------------------------------------
    // Key methods
    // -------------------------------------------------------------------------

    /**
     * Returns all properties visible on this entity: those declared by ancestor
     * entities (parents-first, depth-first) merged with the properties declared
     * directly on this entity.  Child properties override parent properties with
     * the same identifier.
     *
     * @return an unmodifiable, insertion-ordered map keyed by property identifier
     */
    public Map<String, SourceModelProperty> getAllProperties() {
        Map<String, SourceModelProperty> result = new LinkedHashMap<>();
        // Parents first (depth-first)
        for (SourceModelEntity parent : directSuperEntities) {
            result.putAll(parent.getAllProperties());
        }
        // Own properties override parent properties
        result.putAll(declaredProperties);
        return Collections.unmodifiableMap(result);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Fully qualified name of the interface/class,
     * e.g. {@code "org.example.Person"}.
     */
    public String getQualifiedName() {
        return qualifiedName;
    }

    /**
     * Simple name, e.g. {@code "Person"}.
     */
    public String getSimpleName() {
        return simpleName;
    }

    /** The package containing this entity. */
    public SourcePackage getSourcePackage() {
        return sourcePackage;
    }

    /** The owning meta-model. */
    public SourceMetaModel getMetaModel() {
        return metaModel;
    }

    /**
     * {@code true} if {@code @ModelEntity.isAbstract()} is set, meaning the
     * entity cannot be instantiated directly.
     */
    public boolean isAbstract() {
        return abstractEntity;
    }

    /** Initialization policy declared on {@code @ModelEntity}. */
    public InitPolicy getInitPolicy() {
        return initPolicy;
    }

    /** {@code true} if {@code @ModelEntity.inheritInitializers()} is set. */
    public boolean isInheritInitializers() {
        return inheritInitializers;
    }

    /**
     * The {@code .java} file that declares this type, or {@code null} if
     * not yet resolved.
     */
    public SourceCompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

    /**
     * The {@code @ImplementationClass} for this entity, or {@code null} if
     * the annotation is absent.
     */
    public SourceImplementationClass getImplementationClass() {
        return implementationClass;
    }

    /**
     * Properties declared directly on this type (does not include inherited
     * properties).
     *
     * @return an unmodifiable map keyed by property identifier
     */
    public Map<String, SourceModelProperty> getDeclaredProperties() {
        return Collections.unmodifiableMap(declaredProperties);
    }

    /**
     * Direct (non-transitive) super-entities,
     * i.e. the {@code @ModelEntity} interfaces that this type extends directly.
     *
     * @return an unmodifiable list in declaration order
     */
    public List<SourceModelEntity> getDirectSuperEntities() {
        return Collections.unmodifiableList(directSuperEntities);
    }

    /**
     * Initializer methods declared on this entity.
     *
     * @return an unmodifiable list in discovery order
     */
    public List<SourceModelInitializer> getInitializers() {
        return Collections.unmodifiableList(initializers);
    }

    /**
     * The XML tag from {@code @XMLElement} on this type, or {@code null} if
     * the annotation is absent or has no explicit tag.
     */
    public String getXmlTag() {
        return xmlTag;
    }

    /**
     * Diagnostics detected on this entity during construction and validation.
     */
    public List<Issue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    @Override
    public String toString() {
        return "SourceModelEntity(" + qualifiedName + ")";
    }
}
