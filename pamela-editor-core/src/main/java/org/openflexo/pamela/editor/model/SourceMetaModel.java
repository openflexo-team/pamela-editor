package org.openflexo.pamela.editor.model;

import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
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
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
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
public class SourceMetaModel implements SourceElement, org.openflexo.toolbox.HasPropertyChangeSupport {

    private final PropertyChangeSupport pcSupport = new PropertyChangeSupport(this);

    @Override
    public PropertyChangeSupport getPropertyChangeSupport() {
        return pcSupport;
    }

    @Override
    public String getDeletedProperty() {
        return null;
    }

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
        if (sourceDirectories.contains(directory)) {
            return;
        }
        List<File> old = new ArrayList<>(sourceDirectories);
        sourceDirectories.add(directory);
        scanForPackages(directory);
        pcSupport.firePropertyChange("sourceDirectories", old, Collections.unmodifiableList(sourceDirectories));
        pcSupport.firePropertyChange("allPackages", null, new ArrayList<>(packages.values()));
    }

    /**
     * Removes a previously registered source directory.
     * Rebuilds the package/file index from the remaining source directories.
     */
    public void removeSourceDirectory(File directory) {
        List<File> old = new ArrayList<>(sourceDirectories);
        if (!sourceDirectories.remove(directory)) {
            return;
        }
        rescanPackages();
        pcSupport.firePropertyChange("sourceDirectories", old, Collections.unmodifiableList(sourceDirectories));
        pcSupport.firePropertyChange("allPackages", null, new ArrayList<>(packages.values()));
    }

    /**
     * Clears all filesystem-scanned data (java files, empty packages) and
     * re-scans from the current source directories.
     * Entity links are preserved.
     */
    private void rescanPackages() {
        for (SourcePackage pkg : packages.values()) {
            pkg.clearJavaFiles();
        }
        for (File dir : sourceDirectories) {
            scanForPackages(dir);
        }
        packages.entrySet().removeIf(e ->
                e.getValue().getJavaFiles().isEmpty() && e.getValue().getEntities().isEmpty());
    }

    /**
     * Recursively scans {@code rootDir} for Java packages.
     * A directory is a package if it contains at least one {@code .java} file.
     * For each such directory, a {@link SourcePackage} is created (or reused if
     * already present) and a {@link SourceJavaFile} is added for each
     * {@code .java} file found.
     *
     * <p>The package qualified name is derived from the relative path of the
     * directory from {@code rootDir}: {@code foo/bar} → {@code "foo.bar"}.
     * The default package (files directly in {@code rootDir}) uses an empty string.</p>
     *
     * @param rootDir a source directory already registered via {@link #addSourceDirectory}
     */
    private void scanForPackages(File rootDir) {
        if (!rootDir.isDirectory()) {
            return;
        }
        scanPackageDir(rootDir, rootDir, "");
    }

    private void scanPackageDir(File dir, File rootDir, String ignored) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }

        List<File> javaFiles = new ArrayList<>();
        List<File> subdirs = new ArrayList<>();
        for (File child : children) {
            if (child.isDirectory()) {
                subdirs.add(child);
            } else if (child.getName().endsWith(".java")) {
                javaFiles.add(child);
            }
        }

        // If this directory has .java files, determine their package from source
        if (!javaFiles.isEmpty()) {
            String packageName = readPackageDeclaration(javaFiles.get(0));
            SourcePackage pkg = packages.get(packageName);
            if (pkg == null) {
                pkg = new SourcePackage(packageName, this);
                packages.put(packageName, pkg);
            }
            for (File jf : javaFiles) {
                // Avoid duplicates across multiple scans
                final SourcePackage finalPkg = pkg;
                boolean alreadyPresent = finalPkg.getJavaFiles().stream()
                        .anyMatch(f -> f.getFile().equals(jf));
                if (!alreadyPresent) {
                    pkg.addJavaFile(new SourceJavaFile(jf, rootDir, this));
                }
            }
        }

        for (File subdir : subdirs) {
            scanPackageDir(subdir, rootDir, "");
        }
    }

    /**
     * Reads the {@code package} declaration from the first few lines of a
     * {@code .java} file. Returns an empty string if no package declaration
     * is found (default package) or if the file cannot be read.
     */
    private static String readPackageDeclaration(File javaFile) {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.io.FileInputStream(javaFile),
                        java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            int linesRead = 0;
            while ((line = reader.readLine()) != null && linesRead < 50) {
                linesRead++;
                line = line.trim();
                if (line.startsWith("package ") && line.endsWith(";")) {
                    return line.substring("package ".length(), line.length() - 1).trim();
                }
                // Stop early if we've passed the header area
                if (line.startsWith("public ") || line.startsWith("class ")
                        || line.startsWith("interface ") || line.startsWith("@")) {
                    if (!line.startsWith("@")) break;
                }
            }
        } catch (IOException e) {
            // ignore — return default package
        }
        return "";
    }

    /**
     * Adds a root type name (fully qualified) as an entry point for the
     * bottom-up traversal.  At least one root type is required.
     *
     * @param qualifiedName fully qualified name, e.g. {@code "org.example.Person"}
     */
    public void addRootTypeName(String qualifiedName) {
        List<String> old = new ArrayList<>(rootTypeNames);
        rootTypeNames.add(qualifiedName);
        pcSupport.firePropertyChange("rootTypeNames", old, Collections.unmodifiableList(rootTypeNames));
    }

    /**
     * Removes a previously registered root type name.
     * Has no effect if the name is not in the list.
     */
    public void removeRootTypeName(String qualifiedName) {
        List<String> old = new ArrayList<>(rootTypeNames);
        if (rootTypeNames.remove(qualifiedName)) {
            pcSupport.firePropertyChange("rootTypeNames", old, Collections.unmodifiableList(rootTypeNames));
        }
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

        pcSupport.firePropertyChange("entities", null, Collections.unmodifiableMap(entities));
        pcSupport.firePropertyChange("allPackages", null, new ArrayList<>(packages.values()));
    }

    /**
     * Resets all derived state (entities, packages, compilation units, issues)
     * and runs the full 3-phase construction algorithm again from the current
     * source directories and root type names.
     *
     * <p>Call this after modifying the inputs via {@link #addSourceDirectory},
     * {@link #removeSourceDirectory}, {@link #addRootTypeName}, or
     * {@link #removeRootTypeName} to bring the model in sync with the new inputs.</p>
     */
    public void rebuildMetaModel() {
        packages.clear();
        compilationUnits.clear();
        entities.clear();
        issues.clear();
        // Re-scan all source directories so packages and java files are populated
        // before Spoon runs (phase1Discovery will then enrich them with CtPackage refs)
        for (File dir : sourceDirectories) {
            scanForPackages(dir);
        }
        buildMetaModel();
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

        // Wire SourcePackage instances for packages containing entities.
        // Packages may already exist from the filesystem scan (scanForPackages);
        // in that case we enrich them with the Spoon CtPackage reference.
        for (SourceModelEntity entity : entities.values()) {
            CtType<?> ctType = findType(entity.getQualifiedName());
            if (ctType == null) {
                continue;
            }
            CtPackage ctPackage = ctType.getPackage();
            String pkgName = (ctPackage != null) ? ctPackage.getQualifiedName() : "";
            SourcePackage sourcePkg = packages.get(pkgName);
            if (sourcePkg == null) {
                // Package not pre-created by scan (e.g., no .java files were scanned
                // yet, or this is a Spoon-only run)
                if (ctPackage != null) {
                    sourcePkg = new SourcePackage(ctPackage, this);
                } else {
                    sourcePkg = new SourcePackage("", this);
                }
                packages.put(pkgName, sourcePkg);
            } else if (sourcePkg.getCtPackage() == null && ctPackage != null) {
                // Pre-created by scan without Spoon ref — enrich it now
                sourcePkg.setCtPackage(ctPackage);
            }
            entity.setSourcePackage(sourcePkg);
            sourcePkg.addEntity(entity);
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

            // Build implementation class if @ImplementationClass is present.
            // We read via CtAnnotation.getValue() (raw AST) instead of the Java proxy because
            // invoking .value().getName() on the proxy triggers Spoon's VisitorPartialEvaluator,
            // which tries to load the class at runtime — failing when it has not been compiled yet.
            // @ImplementationClass(Foo.class) is stored in the AST as a CtFieldRead whose target
            // is a CtTypeAccess<Foo>; we extract the qualified name from that type access.
            CtAnnotation<?> implCtAnnotation = ctType.getAnnotations().stream()
                    .filter(a -> a.getAnnotationType().getQualifiedName()
                            .equals(ImplementationClass.class.getName()))
                    .findFirst().orElse(null);
            if (implCtAnnotation != null) {
                spoon.reflect.code.CtExpression<?> valueExpr = implCtAnnotation.getValue("value");
                String implClassName = null;
                if (valueExpr instanceof spoon.reflect.code.CtFieldRead<?>) {
                    spoon.reflect.code.CtFieldRead<?> fieldRead =
                            (spoon.reflect.code.CtFieldRead<?>) valueExpr;
                    if (fieldRead.getTarget() instanceof spoon.reflect.code.CtTypeAccess<?>) {
                        implClassName = ((spoon.reflect.code.CtTypeAccess<?>) fieldRead.getTarget())
                                .getAccessedType().getQualifiedName();
                    }
                }
                if (implClassName != null) {
                    buildImplementationClass(entity, implClassName);
                }
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
    // Package-private helpers used by mutation operations
    // =========================================================================

    /**
     * Returns the internal Spoon model.
     * Package-private — used by {@link SourceModelEntity#rename}.
     */
    CtModel getCtModel() {
        return ctModel;
    }

    /**
     * Updates the entities map and compilationUnits map when an entity is renamed.
     * Removes the old key and inserts the new key.
     * Package-private — called by {@link SourceModelEntity#rename}.
     *
     * @param oldKey the old qualified name
     * @param newKey the new qualified name
     * @param entity the entity being renamed
     */
    void renameEntityKey(String oldKey, String newKey, SourceModelEntity entity) {
        entities.remove(oldKey);
        entities.put(newKey, entity);

        SourceCompilationUnit cu = compilationUnits.remove(oldKey);
        if (cu != null) {
            compilationUnits.put(newKey, cu);
        }
    }

    /**
     * Removes an entity from the entities map.
     * Package-private — called by {@link SourceModelEntity#delete}.
     *
     * @param qualifiedName the qualified name of the entity to remove
     */
    void unregisterEntity(String qualifiedName) {
        entities.remove(qualifiedName);
    }

    /**
     * Removes a compilation unit from the compilationUnits map.
     * Package-private — called by {@link SourceModelEntity#delete}.
     *
     * @param primaryTypeName the primary type name key
     */
    void unregisterCompilationUnit(String primaryTypeName) {
        compilationUnits.remove(primaryTypeName);
    }

    // =========================================================================
    // Mutation operations
    // =========================================================================

    /**
     * Creates a new {@code @ModelEntity} interface in the given package and
     * registers it in this meta-model.
     *
     * <p>The new interface is written to a {@code .java} file named
     * {@code simpleName + ".java"} placed in the directory that corresponds to
     * the package inside the first source directory (or, if the package already
     * contains an entity, in the same directory as that entity's source file).</p>
     *
     * @param simpleName    the simple name for the new interface
     * @param sourcePackage the package in which to place the new entity
     * @return the newly created {@link SourceModelEntity}
     * @throws IOException if the file cannot be written
     */
    public SourceModelEntity createEntity(String simpleName, SourcePackage sourcePackage) throws IOException {
        String pkgName = sourcePackage.getQualifiedName();
        String qualifiedName = (pkgName == null || pkgName.isEmpty())
                ? simpleName
                : pkgName + "." + simpleName;

        // Determine the target directory
        File targetDir = resolveTargetDirectory(sourcePackage);
        targetDir.mkdirs();
        File newFile = new File(targetDir, simpleName + ".java");

        // Use the Spoon factory from the existing model
        Factory factory = ctModel.getRootPackage().getFactory();

        // Create the CtInterface
        CtInterface<Object> newInterface = factory.Core().createInterface();
        newInterface.setSimpleName(simpleName);
        newInterface.addModifier(ModifierKind.PUBLIC);

        // Add @ModelEntity annotation
        CtAnnotation<?> modelEntityAnnotation = factory.Core().createAnnotation();
        modelEntityAnnotation.setAnnotationType(
                factory.Type().createReference(ModelEntity.class));
        newInterface.addAnnotation(modelEntityAnnotation);

        // Place the interface in the correct package
        sourcePackage.getCtPackage().addType(newInterface);

        // Create the compilation unit wrapper
        SourceCompilationUnit scu = SourceCompilationUnit.forNewEntity(newInterface, newFile, this);

        // Save the file to disk
        scu.save();

        // Build the SourceModelEntity
        SourceModelEntity entity = new SourceModelEntity(newInterface, this);
        entity.setSourcePackage(sourcePackage);
        entity.setCompilationUnit(scu);

        // Register in the meta-model
        entities.put(qualifiedName, entity);
        compilationUnits.put(qualifiedName, scu);
        sourcePackage.addEntity(entity);

        return entity;
    }

    /**
     * Determines the file-system directory where a new entity in the given
     * package should be created.
     *
     * <p>Strategy:
     * <ol>
     *   <li>If the package already contains at least one entity whose source
     *       file is known, use that entity's parent directory.</li>
     *   <li>Otherwise, derive the path from the first source directory plus
     *       the package path segments.</li>
     * </ol>
     * </p>
     */
    private File resolveTargetDirectory(SourcePackage sourcePackage) {
        // Strategy 1: use an existing entity's directory
        for (SourceModelEntity existing : sourcePackage.getEntities()) {
            SourceCompilationUnit cu = existing.getCompilationUnit();
            if (cu != null && cu.getFile() != null) {
                File parent = cu.getFile().getParentFile();
                if (parent != null) {
                    return parent;
                }
            }
        }

        // Strategy 2: derive from first source directory + package path
        File baseDir = sourceDirectories.isEmpty() ? new File(".") : sourceDirectories.get(0);
        String pkgName = sourcePackage.getQualifiedName();
        if (pkgName != null && !pkgName.isEmpty()) {
            String pkgPath = pkgName.replace('.', File.separatorChar);
            return new File(baseDir, pkgPath);
        }
        return baseDir;
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
    public List<SourcePackage> getAllPackages() {
        return Collections.unmodifiableList(new ArrayList<>(packages.values()));
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

    /** Returns the number of packages in this metamodel. */
    public int getPackagesCount() {
        return packages.size();
    }

    /** Returns the number of entities in this metamodel. */
    public int getEntitiesCount() {
        return entities.size();
    }

    /** Returns the number of issues in this metamodel. */
    public int getIssuesCount() {
        return issues.size();
    }

    /**
     * Returns the total number of declared properties across all entities in this metamodel.
     * Useful as a quick metric in summary views.
     */
    public int getTotalPropertiesCount() {
        int count = 0;
        for (SourceModelEntity entity : entities.values()) {
            count += entity.getDeclaredProperties().size();
        }
        return count;
    }

    /**
     * Returns the number of abstract entities ({@code @ModelEntity(isAbstract=true)})
     * in this metamodel.
     */
    public int getAbstractEntitiesCount() {
        int count = 0;
        for (SourceModelEntity entity : entities.values()) {
            if (entity.isAbstract()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the number of initializers across all entities in this metamodel.
     */
    public int getTotalInitializersCount() {
        int count = 0;
        for (SourceModelEntity entity : entities.values()) {
            count += entity.getInitializers().size();
        }
        return count;
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
