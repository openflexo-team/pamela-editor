package org.openflexo.pamela.editor.model;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openflexo.pamela.annotations.Adder;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.Initializer;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Reindexer;
import org.openflexo.pamela.annotations.Remover;
import org.openflexo.pamela.annotations.Setter;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

/**
 * Root object of the pamela-editor model layer.
 *
 * <p>Mirrors PAMELA's runtime {@code PamelaMetaModel} but is built by
 * <em>analysing Java source files</em> (via Spoon) instead of compiled
 * bytecode.</p>
 *
 * <p>Construction takes two inputs:
 * <ul>
 *   <li>A {@linkplain #addSourceDirectory list of source directories} — all
 *       given to the Spoon launcher before {@code buildModel()}, so that Spoon
 *       can resolve types across directories transparently.</li>
 *   <li>A {@linkplain #addRootTypeName list of root type names} (fully
 *       qualified) — the entry points for the bottom-up BFS traversal.</li>
 * </ul>
 * </p>
 *
 * <h3>Construction algorithm (3 phases)</h3>
 *
 * <b>Phase 1 — Discovery</b><br>
 * Seed the work queue with root type names. For each type in the queue:
 * resolve it in the Spoon model; if annotated {@code @ModelEntity} create a
 * {@link SourceModelEntity} (fire an {@link Error} if it is a class, a
 * {@link Warning} if it is not {@code @ModelEntity}); follow
 * {@code getSuperInterfaces()} and {@code @Getter} property types to enqueue
 * further types.
 *
 * <b>Phase 2 — Property and impl-class construction</b><br>
 * For each discovered entity: scan its declared methods, group by property
 * identifier, build {@link SourceModelProperty} instances; handle
 * {@link SourceImplementationClass} if {@code @ImplementationClass} is present.
 *
 * <b>Phase 3 — Resolution</b><br>
 * Wire {@code directSuperEntities}; resolve {@code inverse} links between
 * {@link SourceModelProperty} pairs; resolve
 * {@link SourceType#getModelEntity()} by qualified-name lookup.
 */
public class SourceMetaModel implements SourceElement {

    // Internal Spoon reference — never exposed in the public API
    private CtModel ctModel;

    // Construction inputs
    private final List<File> sourceDirectories;
    private final List<String> rootTypeNames;

    // Public model
    private String name;
    private final Map<String, SourcePackage> packages;            // key = qualified package name
    private final Map<String, SourceCompilationUnit> compilationUnits; // key = primary type qualified name
    private final Map<String, SourceModelEntity> entities;        // key = qualified type name
    private final List<Issue> issues;

    public SourceMetaModel() {
        this.sourceDirectories = new ArrayList<>();
        this.rootTypeNames = new ArrayList<>();
        this.packages = new LinkedHashMap<>();
        this.compilationUnits = new LinkedHashMap<>();
        this.entities = new LinkedHashMap<>();
        this.issues = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Construction inputs
    // -------------------------------------------------------------------------

    /**
     * Adds a source directory to be scanned by Spoon.
     * All directories must be added before calling {@link #buildMetaModel()}.
     *
     * @param directory a directory containing {@code .java} source files
     */
    public void addSourceDirectory(File directory) {
        sourceDirectories.add(directory);
    }

    /**
     * Adds a root type name (fully qualified) as an entry point for the
     * bottom-up traversal.  At least one root type is required.
     *
     * @param qualifiedName fully qualified name, e.g. {@code "org.example.Person"}
     */
    public void addRootTypeName(String qualifiedName) {
        rootTypeNames.add(qualifiedName);
    }

    // -------------------------------------------------------------------------
    // Build
    // -------------------------------------------------------------------------

    /**
     * Runs the 3-phase construction algorithm.
     * Must be called after all source directories and root type names have been
     * registered.
     */
    public void buildMetaModel() {
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setAutoImports(true);
        for (File dir : sourceDirectories) {
            launcher.addInputResource(dir.getAbsolutePath());
        }
        ctModel = launcher.buildModel();

        // Phase 1 — discovery
        phase1Discovery();

        // Phase 2 — properties and impl classes
        phase2Properties();

        // Phase 3 — resolution
        phase3Resolution();
    }

    // =========================================================================
    // Phase 1 — Discovery
    // =========================================================================

    private void phase1Discovery() {
        // Create SourceCompilationUnit for every compilation unit Spoon found
        for (Map.Entry<String, CompilationUnit> entry : ctModel.getRootPackage().getFactory().CompilationUnit().getMap().entrySet()) {
            CompilationUnit cu = entry.getValue();
            if (cu != null && !cu.getDeclaredTypes().isEmpty()) {
                String primaryName = cu.getDeclaredTypes().get(0).getQualifiedName();
                SourceCompilationUnit sourcecu = new SourceCompilationUnit(cu, this);
                compilationUnits.put(primaryName, sourcecu);
            }
        }

        // BFS from root types
        Deque<String> queue = new ArrayDeque<>();
        for (String name : rootTypeNames) {
            queue.add(name);
        }

        while (!queue.isEmpty()) {
            String typeName = queue.poll();
            if (entities.containsKey(typeName)) {
                continue; // already processed
            }

            CtType<?> ctType = findType(typeName);
            if (ctType == null) {
                fireIssue(new Warning("Root type '" + typeName + "' not found in Spoon model — skipped"));
                continue;
            }

            ModelEntity modelEntityAnnotation = ctType.getAnnotation(ModelEntity.class);
            if (modelEntityAnnotation == null) {
                fireIssue(new Warning("Type '" + typeName + "' is not annotated @ModelEntity — skipped"));
                continue;
            }

            // Create entity (constructor fires Error if it is a class)
            SourceModelEntity entity = new SourceModelEntity(ctType, this);
            entities.put(typeName, entity);

            // Enqueue super-interfaces that are @ModelEntity
            if (ctType instanceof CtInterface) {
                for (CtTypeReference<?> superRef : ((CtInterface<?>) ctType).getSuperInterfaces()) {
                    CtType<?> superType = resolveType(superRef);
                    if (superType != null && superType.getAnnotation(ModelEntity.class) != null) {
                        String superName = superRef.getQualifiedName();
                        if (!entities.containsKey(superName)) {
                            queue.add(superName);
                        }
                    }
                }
            }

            // Enqueue property types: @Getter with ignoreType=false
            for (CtMethod<?> method : ctType.getMethods()) {
                Getter getter = method.getAnnotation(Getter.class);
                if (getter != null && !getter.ignoreType()) {
                    CtTypeReference<?> returnType = method.getType();
                    CtTypeReference<?> elementType = extractElementType(returnType);
                    if (elementType != null) {
                        CtType<?> propType = resolveType(elementType);
                        if (propType != null && propType.getAnnotation(ModelEntity.class) != null) {
                            String propName = elementType.getQualifiedName();
                            if (!entities.containsKey(propName)) {
                                queue.add(propName);
                            }
                        }
                    }
                }
            }
        }

        // Create SourcePackage instances for packages with at least one entity
        for (SourceModelEntity entity : entities.values()) {
            CtType<?> ctType = findType(entity.getQualifiedName());
            if (ctType == null) {
                continue;
            }
            CtPackage ctPackage = ctType.getPackage();
            String pkgName = (ctPackage != null) ? ctPackage.getQualifiedName() : "";
            SourcePackage sourcePkg = packages.get(pkgName);
            if (sourcePkg == null && ctPackage != null) {
                sourcePkg = new SourcePackage(ctPackage, this);
                packages.put(pkgName, sourcePkg);
            }
            if (sourcePkg != null) {
                entity.setSourcePackage(sourcePkg);
                sourcePkg.addEntity(entity);
            }
        }

        // Link entities to their compilation units
        for (SourceModelEntity entity : entities.values()) {
            SourceCompilationUnit cu = compilationUnits.get(entity.getQualifiedName());
            if (cu != null) {
                entity.setCompilationUnit(cu);
            }
        }
    }

    // =========================================================================
    // Phase 2 — Properties and impl classes
    // =========================================================================

    private void phase2Properties() {
        for (SourceModelEntity entity : entities.values()) {
            CtType<?> ctType = findType(entity.getQualifiedName());
            if (ctType == null) {
                continue;
            }

            // Build properties from declared methods
            buildProperties(entity, ctType);

            // Build initializers
            buildInitializers(entity, ctType);

            // Build implementation class if @ImplementationClass is present
            ImplementationClass implAnnotation = ctType.getAnnotation(ImplementationClass.class);
            if (implAnnotation != null) {
                String implClassName = implAnnotation.value().getName();
                buildImplementationClass(entity, implClassName);
            }

            // Validate initPolicy = REQUIRED + no initializer + not abstract
            if (!entity.isAbstract()
                    && entity.getInitializers().isEmpty()
                    && entity.getInitPolicy() == ModelEntity.InitPolicy.REQUIRED) {
                fireIssue(new Warning("Non-abstract entity '" + entity.getQualifiedName()
                        + "' declares initPolicy=REQUIRED but has no @Initializer method"));
            }
        }
    }

    /**
     * Groups methods by property identifier and builds {@link SourceModelProperty}s.
     */
    private void buildProperties(SourceModelEntity entity, CtType<?> ctType) {
        // First pass: create SourceModelProperty for each @Getter
        for (CtMethod<?> method : ctType.getMethods()) {
            Getter getter = method.getAnnotation(Getter.class);
            if (getter != null) {
                SourceModelProperty prop = new SourceModelProperty(method, entity);
                entity.addDeclaredProperty(prop);
            }
        }

        // Second pass: attach @Setter / @Adder / @Remover / @Reindexer to existing properties
        for (CtMethod<?> method : ctType.getMethods()) {
            Setter setter = method.getAnnotation(Setter.class);
            if (setter != null) {
                SourceModelProperty prop = entity.getDeclaredProperties().get(setter.value());
                if (prop != null) {
                    prop.registerSetter(method);
                } else {
                    fireIssue(new Error("@Setter '" + method.getSimpleName()
                            + "' on entity '" + entity.getQualifiedName()
                            + "' has no corresponding @Getter for property '" + setter.value() + "'"));
                }
            }

            Adder adder = method.getAnnotation(Adder.class);
            if (adder != null) {
                SourceModelProperty prop = entity.getDeclaredProperties().get(adder.value());
                if (prop != null) {
                    prop.registerAdder(method);
                } else {
                    fireIssue(new Error("@Adder '" + method.getSimpleName()
                            + "' on entity '" + entity.getQualifiedName()
                            + "' has no corresponding @Getter for property '" + adder.value() + "'"));
                }
            }

            Remover remover = method.getAnnotation(Remover.class);
            if (remover != null) {
                SourceModelProperty prop = entity.getDeclaredProperties().get(remover.value());
                if (prop != null) {
                    prop.registerRemover(method);
                } else {
                    fireIssue(new Error("@Remover '" + method.getSimpleName()
                            + "' on entity '" + entity.getQualifiedName()
                            + "' has no corresponding @Getter for property '" + remover.value() + "'"));
                }
            }

            Reindexer reindexer = method.getAnnotation(Reindexer.class);
            if (reindexer != null) {
                SourceModelProperty prop = entity.getDeclaredProperties().get(reindexer.value());
                if (prop != null) {
                    prop.registerReindexer(method);
                } else {
                    fireIssue(new Error("@Reindexer '" + method.getSimpleName()
                            + "' on entity '" + entity.getQualifiedName()
                            + "' has no corresponding @Getter for property '" + reindexer.value() + "'"));
                }
            }
        }

        // Validate adder/remover consistency per property
        for (SourceModelProperty prop : entity.getDeclaredProperties().values()) {
            prop.validateAdderRemoverConsistency();
        }
    }

    /**
     * Collects {@code @Initializer}-annotated methods and builds
     * {@link SourceModelInitializer}s.
     */
    private void buildInitializers(SourceModelEntity entity, CtType<?> ctType) {
        for (CtMethod<?> method : ctType.getMethods()) {
            if (method.getAnnotation(Initializer.class) != null) {
                entity.addInitializer(new SourceModelInitializer(method, entity));
            }
        }
    }

    /**
     * Locates the implementation class in the Spoon model and builds a
     * {@link SourceImplementationClass}.
     */
    private void buildImplementationClass(SourceModelEntity entity, String implClassName) {
        CtType<?> implType = findType(implClassName);
        if (implType == null) {
            fireIssue(new Warning("Implementation class '" + implClassName
                    + "' referenced by @ImplementationClass on '" + entity.getQualifiedName()
                    + "' was not found in the Spoon model — skipped"));
            return;
        }
        if (!(implType instanceof CtClass)) {
            fireIssue(new Error("@ImplementationClass value '" + implClassName + "' is not a class"));
            return;
        }
        CtClass<?> implClass = (CtClass<?>) implType;
        SourceCompilationUnit implCu = compilationUnits.get(implClassName);
        SourceImplementationClass impl = new SourceImplementationClass(implClass, entity, implCu);
        entity.setImplementationClass(impl);
    }

    // =========================================================================
    // Phase 3 — Resolution
    // =========================================================================

    private void phase3Resolution() {
        for (SourceModelEntity entity : entities.values()) {
            CtType<?> ctType = findType(entity.getQualifiedName());
            if (ctType == null) {
                continue;
            }

            // 1. Wire directSuperEntities (direct parents only)
            if (ctType instanceof CtInterface) {
                for (CtTypeReference<?> superRef : ((CtInterface<?>) ctType).getSuperInterfaces()) {
                    SourceModelEntity superEntity = entities.get(superRef.getQualifiedName());
                    if (superEntity != null) {
                        entity.addDirectSuperEntity(superEntity);
                    }
                }
            }

            // 2. Resolve SourceType.modelEntity for each declared property type
            for (SourceModelProperty prop : entity.getDeclaredProperties().values()) {
                String typeName = prop.getType().getQualifiedName();
                SourceModelEntity typeEntity = entities.get(typeName);
                if (typeEntity != null) {
                    prop.getType().setModelEntity(typeEntity);
                }
            }
        }

        // 3. Resolve inverse property links
        for (SourceModelEntity entity : entities.values()) {
            for (SourceModelProperty prop : entity.getDeclaredProperties().values()) {
                String inverseId = prop.getInversePropertyIdentifier();
                if (inverseId == null || inverseId.isEmpty()) {
                    continue;
                }
                // The inverse is on the entity corresponding to the property type
                SourceModelEntity typeEntity = prop.getType().getModelEntity();
                if (typeEntity == null) {
                    // Try to resolve from entity map using type qualified name
                    typeEntity = entities.get(prop.getType().getQualifiedName());
                }
                if (typeEntity == null) {
                    fireIssue(new Error("Property '" + prop.getPropertyIdentifier()
                            + "' on '" + entity.getQualifiedName()
                            + "' declares inverse='" + inverseId
                            + "' but the property type is not a known entity"));
                    continue;
                }
                // Look for the inverse property in all properties of the target entity
                Map<String, SourceModelProperty> allProps = typeEntity.getAllProperties();
                SourceModelProperty inverseProperty = allProps.get(inverseId);
                if (inverseProperty == null) {
                    fireIssue(new Error("Property '" + prop.getPropertyIdentifier()
                            + "' on '" + entity.getQualifiedName()
                            + "' declares inverse='" + inverseId
                            + "' but no such property found on '" + typeEntity.getQualifiedName() + "'"));
                } else {
                    prop.setInverseProperty(inverseProperty);
                }
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Resolves a fully qualified type name to a Spoon {@code CtType},
     * searching the entire Spoon model.
     *
     * @return the Spoon type or {@code null} if not found
     */
    private CtType<?> findType(String qualifiedName) {
        return ctModel.getAllTypes().stream()
                .filter(t -> t.getQualifiedName().equals(qualifiedName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves a {@code CtTypeReference} to its {@code CtType} declaration
     * using Spoon's {@code getTypeDeclaration()}.
     * Returns {@code null} for shadow classes (compiled from JARs).
     */
    private CtType<?> resolveType(CtTypeReference<?> ref) {
        try {
            spoon.reflect.declaration.CtType<?> decl = ref.getTypeDeclaration();
            if (decl == null || decl.isShadow()) {
                return null;
            }
            return decl;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Given a method's return type reference, extracts the element type:
     * for {@code List<T>} returns the reference to {@code T};
     * for non-list types returns the reference itself.
     *
     * @return the element type reference, or {@code null} if it cannot be determined
     */
    private static CtTypeReference<?> extractElementType(CtTypeReference<?> typeRef) {
        if (typeRef == null) {
            return null;
        }
        String qn = typeRef.getQualifiedName();
        if ("java.util.List".equals(qn) || "List".equals(qn)) {
            List<CtTypeReference<?>> args = typeRef.getActualTypeArguments();
            if (args != null && !args.isEmpty()) {
                return args.get(0);
            }
            return null; // raw List
        }
        return typeRef;
    }

    // =========================================================================
    // Issue reporting
    // =========================================================================

    /**
     * Records and logs an issue.  Called by entity and property constructors
     * as well as the 3-phase construction algorithm.
     *
     * @param issue the issue to record
     */
    public void fireIssue(Issue issue) {
        issues.add(issue);
        System.err.println(" [pamela-editor] " + issue);
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /** Optional display name for this meta-model. */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The source directories registered as input resources.
     *
     * @return an unmodifiable view of the list
     */
    public List<File> getSourceDirectories() {
        return Collections.unmodifiableList(sourceDirectories);
    }

    /**
     * The root type names (fully qualified) used as traversal entry points.
     *
     * @return an unmodifiable view of the list
     */
    public List<String> getRootTypeNames() {
        return Collections.unmodifiableList(rootTypeNames);
    }

    /**
     * All packages discovered during Phase 1 (i.e., packages containing at
     * least one {@code @ModelEntity}).
     *
     * @return an unmodifiable collection
     */
    public Collection<SourcePackage> getAllPackages() {
        return Collections.unmodifiableCollection(packages.values());
    }

    /**
     * Looks up a package by fully qualified name.
     *
     * @param qualifiedName the package name, or empty string for the default package
     * @return the package, or {@code null} if not found
     */
    public SourcePackage getPackage(String qualifiedName) {
        return packages.get(qualifiedName != null ? qualifiedName : "");
    }

    /**
     * Returns the default (unnamed) package, or {@code null} if no entities
     * belong to it.
     */
    public SourcePackage getDefaultPackage() {
        return packages.get("");
    }

    /**
     * All compilation units discovered during Phase 1, keyed by primary type
     * qualified name.
     *
     * @return an unmodifiable collection
     */
    public Collection<SourceCompilationUnit> getAllCompilationUnits() {
        return Collections.unmodifiableCollection(compilationUnits.values());
    }

    /**
     * Looks up a compilation unit by its primary type's qualified name.
     *
     * @return the compilation unit, or {@code null} if not found
     */
    public SourceCompilationUnit getCompilationUnit(String primaryTypeName) {
        return compilationUnits.get(primaryTypeName);
    }

    /**
     * All entities discovered during Phase 1, keyed by qualified type name.
     *
     * @return an unmodifiable map
     */
    public Map<String, SourceModelEntity> getEntities() {
        return Collections.unmodifiableMap(entities);
    }

    /**
     * Looks up an entity by fully qualified type name.
     *
     * @return the entity, or {@code null} if not found
     */
    public SourceModelEntity getEntity(String qualifiedName) {
        return entities.get(qualifiedName);
    }

    /**
     * All diagnostics collected during construction, in the order they were
     * fired.
     *
     * @return an unmodifiable list
     */
    public List<Issue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    /**
     * Returns a human-readable, indented tree-style ASCII representation of
     * this meta-model.
     *
     * <p>The output groups entities by package (packages sorted alphabetically),
     * with entities sorted alphabetically within each package.  For each entity
     * the output shows: file name, abstract flag, init policy, super entities,
     * implementation class (with custom methods), initializers, and declared
     * properties.  For each property it shows: cardinality, type (annotated with
     * {@code ◀entity▶} or {@code ◀primitive▶}), getter/setter/adder/remover,
     * embedded flag, inverse, and default value.  Issues are listed at the end
     * of each section when non-empty.</p>
     *
     * @return the pretty-printed model dump
     */
    public String prettyPrint() {
        return new PrettyPrinter().print(this);
    }

    @Override
    public String toString() {
        return "SourceMetaModel(" + name + ", entities=" + entities.size() + ")";
    }
}
