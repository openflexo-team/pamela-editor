package org.openflexo.pamela.editor.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openflexo.pamela.annotations.Adder;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.ModelEntity.InitPolicy;
import org.openflexo.pamela.annotations.Remover;
import org.openflexo.pamela.annotations.Setter;
import org.openflexo.pamela.annotations.XMLElement;

import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

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

    // Identity — non-final to allow rename
    private String qualifiedName;
    private String simpleName;
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
    // Package-private helpers used by mutation operations
    // -------------------------------------------------------------------------

    /**
     * Returns the internal Spoon type.
     * Package-private — used by mutation operations and by {@link SourceModelProperty#remove()}.
     */
    CtType<?> getCtType() {
        return ctType;
    }

    /**
     * Removes a property from the declared properties map.
     * Called by {@link SourceModelProperty#remove()}.
     *
     * @param id the property identifier to remove
     */
    void removeDeclaredProperty(String id) {
        declaredProperties.remove(id);
    }

    /**
     * Removes a direct super-entity link.
     * Called by {@link #removeSuperEntity(SourceModelEntity)}.
     *
     * @param e the super-entity to remove
     */
    void removeDirectSuperEntity(SourceModelEntity e) {
        directSuperEntities.remove(e);
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

    // =========================================================================
    // Mutation operations
    // =========================================================================

    /**
     * Renames this entity to {@code newSimpleName}.
     *
     * <p>This operation:
     * <ol>
     *   <li>Updates all type references in the Spoon model to use the new name.</li>
     *   <li>Renames the Spoon type itself.</li>
     *   <li>Renames the backing {@code .java} file on disk.</li>
     *   <li>Saves all affected compilation units.</li>
     *   <li>Updates this object's {@code simpleName} and {@code qualifiedName} fields.</li>
     *   <li>Updates the meta-model's entity map key.</li>
     * </ol>
     * </p>
     *
     * @param newSimpleName the new simple name for this entity
     * @throws IOException if a file write fails
     */
    public void rename(String newSimpleName) throws IOException {
        String oldQualifiedName = this.qualifiedName;
        String packagePrefix = sourcePackage != null && !sourcePackage.isDefault()
                ? sourcePackage.getQualifiedName() + "."
                : "";
        String newQualifiedName = packagePrefix + newSimpleName;

        // 1. Scan all types in the model and update every reference to this type
        Set<SourceCompilationUnit> affectedCUs = new HashSet<>();
        for (CtType<?> type : metaModel.getCtModel().getAllTypes()) {
            boolean changed = false;
            List<CtTypeReference<?>> refs = type.getElements(new TypeFilter<>(CtTypeReference.class));
            for (CtTypeReference<?> ref : refs) {
                if (oldQualifiedName.equals(ref.getQualifiedName())) {
                    ref.setSimpleName(newSimpleName);
                    changed = true;
                }
            }
            if (changed && !type.getQualifiedName().equals(oldQualifiedName)) {
                // Look up the compilation unit for this other type and queue it for save
                SourceCompilationUnit otherCU = metaModel.getCompilationUnit(type.getQualifiedName());
                if (otherCU != null) {
                    affectedCUs.add(otherCU);
                }
            }
        }

        // 2. Rename the Spoon type itself
        ctType.setSimpleName(newSimpleName);

        // 2b. Refresh the CU's declared type reference so the pretty-printer finds the
        //     renamed type and not the old compiled shadow class on the classpath.
        if (compilationUnit != null) {
            compilationUnit.updateDeclaredType(ctType);
        }

        // 3. Update the file on disk: rename the .java file
        if (compilationUnit != null && compilationUnit.getFile() != null) {
            File oldFile = compilationUnit.getFile();
            File parentDir = oldFile.getParentFile();
            File newFile = new File(parentDir, newSimpleName + ".java");
            compilationUnit.setFile(newFile);
            compilationUnit.save();
            if (!oldFile.equals(newFile) && oldFile.exists()) {
                oldFile.delete();
            }
        }

        // 4. Save all other affected CUs
        for (SourceCompilationUnit cu : affectedCUs) {
            cu.save();
        }

        // 5. Update fields
        this.simpleName = newSimpleName;
        this.qualifiedName = newQualifiedName;

        // 6. Update the meta-model's entity key
        metaModel.renameEntityKey(oldQualifiedName, newQualifiedName, this);
    }

    /**
     * Adds a SINGLE-cardinality property to this entity.
     *
     * <p>Generates getter and setter methods with the standard naming convention:
     * {@code "get" + capitalize(identifier)} and {@code "set" + capitalize(identifier)}.
     * Writes the change back to disk via {@link SourceCompilationUnit#save()}.</p>
     *
     * @param identifier        the property identifier (e.g. {@code "name"})
     * @param qualifiedTypeName the fully qualified type name (e.g. {@code "java.lang.String"})
     * @return the newly created {@link SourceModelProperty}
     * @throws IOException if the file write fails
     */
    public SourceModelProperty addSingleProperty(String identifier, String qualifiedTypeName) throws IOException {
        Factory factory = ctType.getFactory();
        String capitalised = capitalise(identifier);
        String getterName = "get" + capitalised;
        String setterName = "set" + capitalised;

        // Create the type reference
        CtTypeReference<?> typeRef = factory.Type().createReference(qualifiedTypeName);

        // --- Build getter ---
        CtMethod<Object> getter = factory.Core().createMethod();
        getter.setSimpleName(getterName);
        getter.setType((CtTypeReference<Object>) typeRef.clone());
        getter.addModifier(ModifierKind.PUBLIC);

        CtAnnotation<?> getterAnnotation = factory.Core().createAnnotation();
        getterAnnotation.setAnnotationType(factory.Type().createReference(Getter.class));
        getterAnnotation.addValue("value", identifier);
        getter.addAnnotation(getterAnnotation);

        ctType.addMethod(getter);

        // --- Build setter ---
        CtMethod<Void> setter = factory.Core().createMethod();
        setter.setSimpleName(setterName);
        setter.setType((CtTypeReference<Void>) factory.Type().VOID_PRIMITIVE);
        setter.addModifier(ModifierKind.PUBLIC);

        CtParameter<Object> setterParam = factory.Core().createParameter();
        setterParam.setSimpleName("value");
        setterParam.setType((CtTypeReference<Object>) typeRef.clone());
        setter.addParameter(setterParam);

        CtAnnotation<?> setterAnnotation = factory.Core().createAnnotation();
        setterAnnotation.setAnnotationType(factory.Type().createReference(Setter.class));
        setterAnnotation.addValue("value", identifier);
        setter.addAnnotation(setterAnnotation);

        ctType.addMethod(setter);

        // --- Build SourceModelProperty ---
        SourceModelProperty prop = new SourceModelProperty(getter, this);
        prop.registerSetter(setter);
        addDeclaredProperty(prop);

        // --- Write back ---
        compilationUnit.save();

        return prop;
    }

    /**
     * Adds a LIST-cardinality property to this entity.
     *
     * <p>Generates getter, adder, and remover methods with the standard naming
     * convention. Writes the change back to disk via
     * {@link SourceCompilationUnit#save()}.</p>
     *
     * @param identifier               the property identifier (e.g. {@code "children"})
     * @param qualifiedElementTypeName the fully qualified element type name
     *                                 (e.g. {@code "org.example.Child"})
     * @return the newly created {@link SourceModelProperty}
     * @throws IOException if the file write fails
     */
    public SourceModelProperty addListProperty(String identifier, String qualifiedElementTypeName)
            throws IOException {
        Factory factory = ctType.getFactory();
        String capitalised = capitalise(identifier);
        String getterName = "get" + capitalised;
        String adderName = "addTo" + capitalised;
        String removerName = "removeFrom" + capitalised;

        // Create element type reference
        CtTypeReference<?> elementTypeRef = factory.Type().createReference(qualifiedElementTypeName);

        // Create List<ElementType> reference
        CtTypeReference<Object> listRef = (CtTypeReference<Object>) factory.Type()
                .createReference("java.util.List");
        listRef.addActualTypeArgument(elementTypeRef.clone());

        // --- Build getter: List<T> getXxx() ---
        CtMethod<Object> getter = factory.Core().createMethod();
        getter.setSimpleName(getterName);
        getter.setType(listRef.clone());
        getter.addModifier(ModifierKind.PUBLIC);

        CtAnnotation<?> getterAnnotation = factory.Core().createAnnotation();
        getterAnnotation.setAnnotationType(factory.Type().createReference(Getter.class));
        getterAnnotation.addValue("value", identifier);
        getterAnnotation.addValue("cardinality", Cardinality.LIST);
        getter.addAnnotation(getterAnnotation);

        ctType.addMethod(getter);

        // --- Build adder: void addToXxx(T element) ---
        CtMethod<Void> adder = factory.Core().createMethod();
        adder.setSimpleName(adderName);
        adder.setType((CtTypeReference<Void>) factory.Type().VOID_PRIMITIVE);
        adder.addModifier(ModifierKind.PUBLIC);

        CtParameter<Object> adderParam = factory.Core().createParameter();
        adderParam.setSimpleName("element");
        adderParam.setType((CtTypeReference<Object>) elementTypeRef.clone());
        adder.addParameter(adderParam);

        CtAnnotation<?> adderAnnotation = factory.Core().createAnnotation();
        adderAnnotation.setAnnotationType(factory.Type().createReference(Adder.class));
        adderAnnotation.addValue("value", identifier);
        adder.addAnnotation(adderAnnotation);

        ctType.addMethod(adder);

        // --- Build remover: void removeFromXxx(T element) ---
        CtMethod<Void> remover = factory.Core().createMethod();
        remover.setSimpleName(removerName);
        remover.setType((CtTypeReference<Void>) factory.Type().VOID_PRIMITIVE);
        remover.addModifier(ModifierKind.PUBLIC);

        CtParameter<Object> removerParam = factory.Core().createParameter();
        removerParam.setSimpleName("element");
        removerParam.setType((CtTypeReference<Object>) elementTypeRef.clone());
        remover.addParameter(removerParam);

        CtAnnotation<?> removerAnnotation = factory.Core().createAnnotation();
        removerAnnotation.setAnnotationType(factory.Type().createReference(Remover.class));
        removerAnnotation.addValue("value", identifier);
        remover.addAnnotation(removerAnnotation);

        ctType.addMethod(remover);

        // --- Build SourceModelProperty ---
        SourceModelProperty prop = new SourceModelProperty(getter, this);
        prop.registerAdder(adder);
        prop.registerRemover(remover);
        addDeclaredProperty(prop);

        // --- Write back ---
        compilationUnit.save();

        return prop;
    }

    /**
     * Deletes this entity from the meta-model and removes its backing {@code .java}
     * file from disk.
     *
     * @throws IOException if the file deletion fails
     */
    public void delete() throws IOException {
        // 1. Delete the backing file
        if (compilationUnit != null && compilationUnit.getFile() != null) {
            File f = compilationUnit.getFile();
            if (f.exists()) {
                f.delete();
            }
        }

        // 2. Unregister from meta-model
        metaModel.unregisterEntity(qualifiedName);
        metaModel.unregisterCompilationUnit(qualifiedName);

        // 3. Remove from package
        if (sourcePackage != null) {
            sourcePackage.removeEntity(this);
        }
    }

    /**
     * Adds a super-entity to this entity's {@code extends} clause.
     *
     * @param superEntity the entity to inherit from
     * @throws IOException if saving the compilation unit fails
     * @throws IllegalStateException if this entity is not backed by a {@link CtInterface}
     */
    public void addSuperEntity(SourceModelEntity superEntity) throws IOException {
        if (!(ctType instanceof CtInterface)) {
            throw new IllegalStateException("addSuperEntity is only supported on interfaces");
        }
        CtInterface<?> ctInterface = (CtInterface<?>) ctType;
        ctInterface.addSuperInterface(superEntity.getCtType().getReference());
        addDirectSuperEntity(superEntity);
        compilationUnit.save();
    }

    /**
     * Removes a super-entity from this entity's {@code extends} clause.
     *
     * @param superEntity the super-entity to remove
     * @throws IOException if saving the compilation unit fails
     * @throws IllegalStateException if this entity is not backed by a {@link CtInterface}
     */
    public void removeSuperEntity(SourceModelEntity superEntity) throws IOException {
        if (!(ctType instanceof CtInterface)) {
            throw new IllegalStateException("removeSuperEntity is only supported on interfaces");
        }
        CtInterface<?> ctInterface = (CtInterface<?>) ctType;

        // Find the matching CtTypeReference in the super-interface set
        CtTypeReference<?> toRemove = null;
        String targetName = superEntity.getQualifiedName();
        for (CtTypeReference<?> ref : ctInterface.getSuperInterfaces()) {
            if (targetName.equals(ref.getQualifiedName())) {
                toRemove = ref;
                break;
            }
        }

        if (toRemove != null) {
            ctInterface.removeSuperInterface(toRemove);
        }

        removeDirectSuperEntity(superEntity);
        compilationUnit.save();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Capitalises the first character of a string, e.g. {@code "name"} → {@code "Name"}.
     */
    private static String capitalise(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
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
