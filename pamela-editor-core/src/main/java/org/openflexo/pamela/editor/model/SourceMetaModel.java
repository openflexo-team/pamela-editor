package org.openflexo.pamela.editor.model;

import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openflexo.pamela.annotations.Adder;
import org.openflexo.pamela.annotations.Deleter;
import org.openflexo.pamela.annotations.Finder;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.Import;
import org.openflexo.pamela.annotations.Imports;
import org.openflexo.pamela.annotations.Initializer;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Operation;
import org.openflexo.pamela.annotations.Reindexer;
import org.openflexo.pamela.annotations.Remover;
import org.openflexo.pamela.annotations.Setter;
import org.openflexo.pamela.annotations.Updater;

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

    // Index of all top-level Spoon types by qualified name, built once after
    // buildModel(). Replaces the previous O(types) linear scan of
    // ctModel.getAllTypes() that findType() ran on every call. See findType().
    private Map<String, CtType<?>> typeIndex;

    // Construction inputs
    private final List<File> sourceDirectories;
    private final List<String> rootTypeNames;

    // One stable SourceFolder wrapper per registered source directory (browser node,
    // see ui-design.md §4.1). Kept in sync with sourceDirectories in addSourceDirectory /
    // removeSourceDirectory.
    private final Map<File, SourceFolder> sourceFolders;

    // Build cache (approach B, see source-metamodel-design.md §18). When a cache
    // file is set, buildMetaModel() restricts the Spoon parse to the reachable
    // entity files recorded by a previous full build, as long as the directory
    // fingerprint is unchanged. Null = no caching (always full parse).
    private File buildCacheFile;

    // Set by buildMetaModel() for the current build: the subset of source files
    // Spoon should parse, or null to parse the whole source directories.
    private java.util.Set<File> restrictedInputFiles;

    // Optional progress listener, notified during buildMetaModel() (parse + phases).
    // Null = no progress reporting. Called on the build thread (never the EDT).
    private BuildProgressListener progressListener;

    // Which interface methods are surfaced as SourceCustomMethods (custom-method-design.md).
    private CustomMethodFilter customMethodFilter = CustomMethodFilter.DEFAULT;

    // Style used when generating identifiers on an entity with no decisive style
    // (property-identifier-constant-design.md §5). Populated from the Analysis preference by the UI.
    private PropertyIdentifierStyle defaultPropertyIdentifierStyle = PropertyIdentifierStyle.DEFAULT;

    // Source style / templating preferences (source-style-preferences-design.md). Populated
    // from the Generation > Style preference by the UI; take effect on the next buildMetaModel().
    private boolean useTabulations = true;
    private int tabulationSize = 4;
    private boolean blankLineAfterPackage = true;
    private boolean blankLineAfterImports = true;
    private boolean expandEmptyBody = true;

    // Public model
    private String name;
    private final Map<String, SourcePackage> packages;            // key = qualified package name
    private final Map<String, SourceCompilationUnit> compilationUnits; // key = primary type qualified name
    private final Map<String, SourceModelEntity> entities;        // key = qualified type name
    private final List<Issue> issues;

    // Deferred (unsaved) source edits — see editable-source-dirty-buffer-design.md.
    // Keyed by primary type qualified name so a buffer survives rebuildMetaModel()
    // (which replaces every SourceCompilationUnit) and can be fed to Spoon as an
    // in-memory VirtualFile (parse-from-buffer) instead of the on-disk file. Not
    // cleared by rebuildMetaModel(); cleared only by flushAll().
    private final Map<String, PendingSource> pendingSources = new LinkedHashMap<>();
    // Files to remove from disk on the next flush (deferred entity delete / rename old file).
    private final java.util.Set<File> pendingDeletions = new java.util.LinkedHashSet<>();

    /** An unsaved source edit: the target file (may not exist on disk yet) plus the pending text. */
    static final class PendingSource {
        final File file;
        final String text;
        PendingSource(File file, String text) {
            this.file = file;
            this.text = text;
        }
    }

    public SourceMetaModel() {
        this.sourceDirectories = new ArrayList<>();
        this.sourceFolders = new LinkedHashMap<>();
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
        sourceFolders.put(directory, new SourceFolder(directory, this));
        scanForPackages(directory);
        pcSupport.firePropertyChange("sourceDirectories", old, Collections.unmodifiableList(sourceDirectories));
        pcSupport.firePropertyChange("allPackages", null, new ArrayList<>(packages.values()));
        pcSupport.firePropertyChange("sourceFolders", null, getSourceFolders());
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
        sourceFolders.remove(directory);
        rescanPackages();
        pcSupport.firePropertyChange("sourceDirectories", old, Collections.unmodifiableList(sourceDirectories));
        pcSupport.firePropertyChange("allPackages", null, new ArrayList<>(packages.values()));
        pcSupport.firePropertyChange("sourceFolders", null, getSourceFolders());
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
        if (rootTypeNames.contains(qualifiedName)) {
            // Same entity cannot be declared as a root type more than once.
            return;
        }
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
        // Build-cache decision (approach B): if a fresh cache exists, restrict the
        // Spoon parse to the reachable entity files it recorded; otherwise parse the
        // whole source directories and (re)write the cache after the build.
        String fingerprint = null;
        boolean cacheHit = false;
        if (buildCacheFile != null) {
            fingerprint = SourceBuildCache.fingerprint(sourceDirectories, rootTypeNames);
            SourceBuildCache cache = SourceBuildCache.load(buildCacheFile);
            if (cache != null && cache.matches(fingerprint)) {
                java.util.Set<File> files = cache.resolveFiles(buildCacheFile.getParentFile());
                if (files != null) {
                    restrictedInputFiles = files;
                    cacheHit = true;
                }
            }
        }
        if (!cacheHit) {
            restrictedInputFiles = null; // full parse
        }

        reportProgress(0.0, cacheHit ? "Loading from cache…" : "Parsing source files…");

        Launcher launcher = new Launcher();
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setAutoImports(true);
        // Source style / templating preferences (source-style-preferences-design.md): applies to
        // every AST pretty-print through this environment, not only new-entity creation.
        launcher.getEnvironment().useTabulations(useTabulations);
        launcher.getEnvironment().setTabulationSize(tabulationSize);
        if (progressListener != null) {
            launcher.getEnvironment().setSpoonProgress(new SpoonProgressAdapter(progressListener));
        }
        feedInputResources(launcher);
        ctModel = launcher.buildModel();

        // Index every top-level type by qualified name once, so findType() is O(1)
        // instead of rebuilding and scanning ctModel.getAllTypes() on each call.
        typeIndex = new HashMap<>();
        for (CtType<?> t : ctModel.getAllTypes()) {
            typeIndex.put(t.getQualifiedName(), t);
        }

        // Phase 1 — discovery
        reportProgress(SpoonProgressAdapter.COMPLETE_AT, "Discovering entities…");
        phase1Discovery();

        // Phase 2 — properties and impl classes
        reportProgress(0.97, "Building properties…");
        phase2Properties();

        // Phase 3 — resolution
        reportProgress(0.99, "Resolving references…");
        phase3Resolution();

        // On a full (cache-miss) build, record the resolved reachable file set so the
        // next open with an unchanged fingerprint can take the fast path. Skip while any
        // buffer is dirty: the cache mirrors disk (fingerprint = disk mtimes), but a dirty
        // build resolves from in-memory buffers, so the recorded set would not match disk.
        if (buildCacheFile != null && !cacheHit
                && pendingSources.isEmpty() && pendingDeletions.isEmpty()) {
            SourceBuildCache.save(buildCacheFile, fingerprint,
                    getResolvedSourceFiles(), buildCacheFile.getParentFile());
        }

        reportProgress(1.0, "Done");

        pcSupport.firePropertyChange("entities", null, Collections.unmodifiableMap(entities));
        pcSupport.firePropertyChange("allPackages", null, new ArrayList<>(packages.values()));

        // A rebuild discards and recreates every SourcePackage; an already-expanded browser
        // node bound to "folder.packages" listens on the SourceFolder itself (stable identity
        // across rebuilds), not on this meta-model, so it must be notified directly here or it
        // keeps showing the stale, pre-rebuild package objects (gina-analysis.md §18.5).
        for (SourceFolder folder : sourceFolders.values()) {
            folder.getPropertyChangeSupport().firePropertyChange("packages", null, folder.getPackages());
        }
    }

    /**
     * Sets an optional listener notified of build progress during
     * {@link #buildMetaModel()} (Spoon parse stages + the three resolution phases).
     * The listener is invoked on the build thread; a Swing client must marshal updates
     * onto the EDT. Pass {@code null} to disable progress reporting.
     */
    public void setProgressListener(BuildProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    private void reportProgress(double fraction, String message) {
        if (progressListener != null) {
            progressListener.progress(fraction, message);
        }
    }

    /**
     * Sets the build-cache sidecar file (approach B, see
     * {@code source-metamodel-design.md §18}). When set, {@link #buildMetaModel()}
     * parses only the reachable entity files recorded by a previous full build as
     * long as the source-directory fingerprint is unchanged, falling back to a full
     * parse (and rewriting the cache) otherwise. Pass {@code null} to disable
     * caching.
     */
    public void setBuildCacheFile(File buildCacheFile) {
        this.buildCacheFile = buildCacheFile;
    }

    /**
     * Sets the filter deciding which interface methods are surfaced as
     * {@link SourceCustomMethod}s. Must be set before {@code buildMetaModel()} to take effect;
     * defaults to {@link CustomMethodFilter#DEFAULT}. A {@code null} value resets to the default.
     */
    public void setCustomMethodFilter(CustomMethodFilter filter) {
        this.customMethodFilter = (filter != null) ? filter : CustomMethodFilter.DEFAULT;
    }

    /** The current custom-method visibility filter. */
    public CustomMethodFilter getCustomMethodFilter() {
        return customMethodFilter;
    }

    /**
     * Sets the identifier style used when generating on an entity with no decisive style
     * ({@link PropertyIdentifierStyle#UNDETERMINED}). A {@code null} value, or {@code MIXED}/
     * {@code UNDETERMINED}, resets to {@link PropertyIdentifierStyle#DEFAULT}. Populated from the
     * Analysis preference by the UI. See {@code property-identifier-constant-design.md §5}.
     */
    public void setDefaultPropertyIdentifierStyle(PropertyIdentifierStyle style) {
        this.defaultPropertyIdentifierStyle =
                (style == PropertyIdentifierStyle.CONSTANT || style == PropertyIdentifierStyle.LITERAL)
                        ? style : PropertyIdentifierStyle.DEFAULT;
    }

    /** The default identifier style for generation (always {@code CONSTANT} or {@code LITERAL}). */
    public PropertyIdentifierStyle getDefaultPropertyIdentifierStyle() {
        return defaultPropertyIdentifierStyle;
    }

    /**
     * Whether {@link #buildMetaModel()} configures the shared Spoon environment to indent with
     * tabs (as opposed to spaces). Applies to <b>every</b> AST pretty-print (rename, add
     * property, new entity…), not only new files. Populated from the Generation &gt; Style
     * preference by the UI; takes effect on the next {@link #buildMetaModel()}. See
     * {@code source-style-preferences-design.md}.
     */
    public void setUseTabulations(boolean useTabulations) {
        this.useTabulations = useTabulations;
    }

    public boolean isUseTabulations() {
        return useTabulations;
    }

    /** Indentation width in spaces, used only when {@link #isUseTabulations()} is {@code false}. */
    public void setTabulationSize(int tabulationSize) {
        this.tabulationSize = tabulationSize;
    }

    public int getTabulationSize() {
        return tabulationSize;
    }

    /**
     * Whether a brand-new entity's source gets a blank line after the {@code package}
     * statement. Only affects the new-entity print path
     * ({@link SourceCompilationUnit#printFromAst()}), never a re-printed existing file. See
     * {@code source-style-preferences-design.md}.
     */
    public void setBlankLineAfterPackage(boolean blankLineAfterPackage) {
        this.blankLineAfterPackage = blankLineAfterPackage;
    }

    public boolean isBlankLineAfterPackage() {
        return blankLineAfterPackage;
    }

    /** Whether a brand-new entity's source gets a blank line after the last import. */
    public void setBlankLineAfterImports(boolean blankLineAfterImports) {
        this.blankLineAfterImports = blankLineAfterImports;
    }

    public boolean isBlankLineAfterImports() {
        return blankLineAfterImports;
    }

    /**
     * Whether a brand-new entity with no members gets its empty body expanded onto its own
     * lines ({@code "{\n\n}"}) instead of the printer's collapsed {@code "{}"}.
     */
    public void setExpandEmptyBody(boolean expandEmptyBody) {
        this.expandEmptyBody = expandEmptyBody;
    }

    public boolean isExpandEmptyBody() {
        return expandEmptyBody;
    }

    /** The build-cache sidecar file, or {@code null} if caching is disabled. */
    public File getBuildCacheFile() {
        return buildCacheFile;
    }

    /**
     * The set of source files the analysis actually resolved a {@code CtType} from —
     * the compilation-unit file of every entity plus that of every resolved
     * implementation class. This is the minimal input set that reproduces the
     * meta-model and is what the build cache stores (see §18.2).
     */
    public java.util.Set<File> getResolvedSourceFiles() {
        java.util.LinkedHashSet<File> files = new java.util.LinkedHashSet<>();
        for (SourceModelEntity entity : entities.values()) {
            SourceCompilationUnit cu = entity.getCompilationUnit();
            if (cu != null && cu.getFile() != null) {
                files.add(cu.getFile());
            }
            SourceImplementationClass impl = entity.getImplementationClass();
            if (impl != null && impl.getCompilationUnit() != null
                    && impl.getCompilationUnit().getFile() != null) {
                files.add(impl.getCompilationUnit().getFile());
            }
        }
        return files;
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
                // Parse-from-buffer: a unit parsed from a VirtualFile has no file path and
                // its content is a still-unsaved edit. Recover the real file and re-adopt the
                // dirty buffer so the edit survives the rebuild it triggered.
                PendingSource pending = pendingSources.get(primaryName);
                if (pending != null) {
                    sourcecu.seedFromPending(pending.file, pending.text);
                }
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
                fireIssue(new Warning("Root type '" + typeName + "' not found in Spoon model — skipped", this));
                continue;
            }

            ModelEntity modelEntityAnnotation = ctType.getAnnotation(ModelEntity.class);
            if (modelEntityAnnotation == null) {
                fireIssue(new Warning("Type '" + typeName + "' is not annotated @ModelEntity — skipped", this));
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

            // Enqueue @Imports/@Import targets: types PAMELA embeds in the meta-model
            // even though they are not reachable via inheritance or a property type
            // (e.g. concrete subtypes of an abstract entity registered only via @Import —
            // see imports-support-design.md and pamela-editor-ui's ConnectorView). Mirrors
            // PAMELA's own ModelEntity.init() embeddedEntities resolution of @Imports.
            for (String importedName : readImportedTypeNames(ctType)) {
                CtType<?> importedType = findType(importedName);
                if (importedType != null && !importedType.isShadow()
                        && importedType.getAnnotation(ModelEntity.class) != null
                        && !entities.containsKey(importedName)) {
                    queue.add(importedName);
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

            // Build custom methods (operations) from the interface, after the impl class
            // is resolved so the IMPLEMENTED_IN_IMPL filter and the impl link work.
            buildCustomMethods(entity, ctType);

            // Capture the raw @Imports/@Import declaration and validate each entry
            // (imports-support-design.md §4.3). Resolution into SourceModelEntity
            // references happens in Phase 3, once every entity is known.
            List<String> importedNames = readImportedTypeNames(ctType);
            entity.setImportedEntityNames(importedNames);
            for (String importedName : importedNames) {
                CtType<?> importedType = findType(importedName);
                if (importedType == null || importedType.isShadow()
                        || importedType.getAnnotation(ModelEntity.class) == null) {
                    fireIssue(new Warning("Entity '" + entity.getQualifiedName()
                            + "' declares @Import(" + importedName
                            + ") but it does not resolve to a known @ModelEntity in this project",
                            entity));
                }
            }

            // Validate initPolicy = REQUIRED + no initializer + not abstract
            if (!entity.isAbstract()
                    && entity.getInitializers().isEmpty()
                    && entity.getInitPolicy() == ModelEntity.InitPolicy.REQUIRED) {
                fireIssue(new Warning("Non-abstract entity '" + entity.getQualifiedName()
                        + "' declares initPolicy=REQUIRED but has no @Initializer method", entity));
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
                            + "' has no corresponding @Getter for property '" + setter.value() + "'", entity));
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
                            + "' has no corresponding @Getter for property '" + adder.value() + "'", entity));
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
                            + "' has no corresponding @Getter for property '" + remover.value() + "'", entity));
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
                            + "' has no corresponding @Getter for property '" + reindexer.value() + "'", entity));
                }
            }

            Updater updater = method.getAnnotation(Updater.class);
            if (updater != null) {
                SourceModelProperty prop = entity.getDeclaredProperties().get(updater.value());
                if (prop != null) {
                    prop.registerUpdater(method);
                } else {
                    fireIssue(new Error("@Updater '" + method.getSimpleName()
                            + "' on entity '" + entity.getQualifiedName()
                            + "' has no corresponding @Getter for property '" + updater.value() + "'", entity));
                }
            }
        }

        // Validate adder/remover and identifier-style consistency per property
        for (SourceModelProperty prop : entity.getDeclaredProperties().values()) {
            prop.validateAdderRemoverConsistency();
            prop.validateIdentifierStyleConsistency();
        }

        // Aggregate the entity's identifier style (literal vs constant) from its properties.
        entity.computeIdentifierStyle();
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
     * Builds the {@link SourceCustomMethod}s (operations) for an entity from its
     * <em>interface</em> methods, applying the {@link CustomMethodFilter}.
     *
     * <p>Candidates = interface methods that are neither a property accessor nor an
     * {@code @Initializer}. The kind is derived from the carried annotation
     * ({@code @Finder}/{@code @Deleter}/{@code @Operation}, else PLAIN). See
     * {@code custom-method-design.md}.</p>
     */
    private void buildCustomMethods(SourceModelEntity entity, CtType<?> ctType) {
        SourceImplementationClass impl = entity.getImplementationClass();
        for (CtMethod<?> method : ctType.getMethods()) {
            if (isPropertyAccessor(method) || method.getAnnotation(Initializer.class) != null) {
                continue;
            }
            if (method.getModifiers().contains(spoon.reflect.declaration.ModifierKind.STATIC)) {
                continue;
            }

            SourceCustomMethod.Kind kind;
            if (method.getAnnotation(Finder.class) != null) {
                kind = SourceCustomMethod.Kind.FINDER;
            } else if (method.getAnnotation(Deleter.class) != null) {
                kind = SourceCustomMethod.Kind.DELETER;
            } else if (method.getAnnotation(Operation.class) != null) {
                kind = SourceCustomMethod.Kind.OPERATION;
            } else {
                kind = SourceCustomMethod.Kind.PLAIN;
            }

            CtMethod<?> implMethod = (impl != null) ? impl.findImplementingMethod(method) : null;
            boolean implemented = implMethod != null && implMethod.getBody() != null;

            boolean include;
            switch (customMethodFilter) {
                case ALL_INTERFACE_METHODS:
                    include = true;
                    break;
                case IMPLEMENTED_IN_IMPL:
                    include = implemented;
                    break;
                case PAMELA_ANNOTATED:
                default:
                    include = kind != SourceCustomMethod.Kind.PLAIN;
                    break;
            }
            if (include) {
                entity.addCustomMethod(new SourceCustomMethod(method, entity, kind, implMethod));
            }
        }
    }

    /** {@code true} if the method carries a PAMELA property-accessor annotation. */
    private static boolean isPropertyAccessor(CtMethod<?> method) {
        return method.getAnnotation(Getter.class) != null
                || method.getAnnotation(Setter.class) != null
                || method.getAnnotation(Adder.class) != null
                || method.getAnnotation(Remover.class) != null
                || method.getAnnotation(Reindexer.class) != null
                || method.getAnnotation(Updater.class) != null;
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
                    + "' was not found in the Spoon model — skipped", entity));
            return;
        }
        if (!(implType instanceof CtClass)) {
            fireIssue(new Error("@ImplementationClass value '" + implClassName + "' is not a class", entity));
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

            // 1b. Resolve @Imports/@Import links (raw names captured in Phase 2). An entry
            // that failed to resolve to a known @ModelEntity already fired a Warning there
            // and is simply absent from the entities map, so it is silently skipped here.
            for (String importedName : entity.getImportedEntityNames()) {
                SourceModelEntity importedEntity = entities.get(importedName);
                if (importedEntity != null) {
                    entity.addImportedEntity(importedEntity);
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
                            + "' but the property type is not a known entity", prop));
                    continue;
                }
                // Look for the inverse property in all properties of the target entity
                Map<String, SourceModelProperty> allProps = typeEntity.getAllProperties();
                SourceModelProperty inverseProperty = allProps.get(inverseId);
                if (inverseProperty == null) {
                    fireIssue(new Error("Property '" + prop.getPropertyIdentifier()
                            + "' on '" + entity.getQualifiedName()
                            + "' declares inverse='" + inverseId
                            + "' but no such property found on '" + typeEntity.getQualifiedName() + "'", prop));
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
     * Feeds the Spoon {@code Launcher} with this build's input resources.
     *
     * <p>Clean build (no dirty buffers): the original behaviour — the restricted
     * file set from the build cache, or the whole source directories.</p>
     *
     * <p>Dirty build (parse-from-buffer): an explicit file list so that dirty units
     * are fed as in-memory {@link spoon.support.compiler.VirtualFile}s (their disk
     * versions skipped) and units pending deletion are excluded. Adding a directory
     * <em>and</em> a {@code VirtualFile} for a file inside it would make Spoon see
     * the type twice — hence the file-list mode while dirty.</p>
     */
    private void feedInputResources(Launcher launcher) {
        if (pendingSources.isEmpty() && pendingDeletions.isEmpty()) {
            if (restrictedInputFiles != null) {
                for (File f : restrictedInputFiles) {
                    launcher.addInputResource(f.getAbsolutePath());
                }
            } else {
                for (File dir : sourceDirectories) {
                    launcher.addInputResource(dir.getAbsolutePath());
                }
            }
            return;
        }

        // Disk base = the file set this build would otherwise parse.
        java.util.LinkedHashSet<File> diskFiles = new java.util.LinkedHashSet<>();
        if (restrictedInputFiles != null) {
            diskFiles.addAll(restrictedInputFiles);
        } else {
            collectJavaFiles(sourceDirectories, diskFiles);
        }

        java.util.Set<String> pendingPaths = new java.util.HashSet<>();
        for (PendingSource ps : pendingSources.values()) {
            if (ps.file != null) {
                pendingPaths.add(canon(ps.file));
            }
        }
        java.util.Set<String> deletePaths = new java.util.HashSet<>();
        for (File f : pendingDeletions) {
            deletePaths.add(canon(f));
        }

        for (File f : diskFiles) {
            String c = canon(f);
            if (pendingPaths.contains(c) || deletePaths.contains(c)) {
                continue; // dirty unit fed from memory below; deleted unit excluded
            }
            launcher.addInputResource(f.getAbsolutePath());
        }
        for (PendingSource ps : pendingSources.values()) {
            String vname = (ps.file != null) ? ps.file.getName() : "Pending.java";
            launcher.addInputResource(new spoon.support.compiler.VirtualFile(ps.text, vname));
        }
    }

    /** Canonical path of a file, falling back to the absolute path on error. */
    private static String canon(File f) {
        try {
            return f.getCanonicalPath();
        } catch (IOException e) {
            return f.getAbsolutePath();
        }
    }

    /** Recursively collects every {@code .java} file under the given directories. */
    private static void collectJavaFiles(List<File> dirs, java.util.Set<File> out) {
        for (File dir : dirs) {
            collectJavaFiles(dir, out);
        }
    }

    private static void collectJavaFiles(File dir, java.util.Set<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectJavaFiles(child, out);
            } else if (child.getName().endsWith(".java")) {
                out.add(child);
            }
        }
    }

    /**
     * Resolves a fully qualified type name to a Spoon {@code CtType},
     * searching the entire Spoon model.
     *
     * @return the Spoon type or {@code null} if not found
     */
    private CtType<?> findType(String qualifiedName) {
        if (typeIndex != null) {
            return typeIndex.get(qualifiedName);
        }
        // Fallback (index not yet built): linear scan.
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
     * Reads the qualified class names declared via {@code @Imports}/{@code @Import} on
     * {@code ctType}, in declaration order. PAMELA runtime only ever reads {@code @Imports}
     * (never a bare {@code @Import}) — see {@code imports-support-design.md §1}.
     *
     * <p>Read from the raw AST (not the Java proxy) for the same reason as
     * {@code @ImplementationClass}: invoking {@code annotation.value()} on the proxy triggers
     * Spoon's {@code VisitorPartialEvaluator}, which tries to load the class at runtime —
     * failing when it has not been compiled yet. An {@code @Imports} value is either a
     * {@code CtNewArray} of nested {@code CtAnnotation}s (explicit array, any size) or a bare
     * {@code CtAnnotation} (the single-element sugar {@code @Imports(@Import(X.class))},
     * verified empirically against Spoon 9.1.0). Each nested {@code @Import}'s own value is a
     * {@code CtFieldRead} targeting a {@code CtTypeAccess}, exactly like
     * {@code @ImplementationClass}.</p>
     *
     * @return the imported qualified names, in declaration order (possibly empty)
     */
    private static List<String> readImportedTypeNames(CtType<?> ctType) {
        CtAnnotation<?> importsAnnotation = ctType.getAnnotations().stream()
                .filter(a -> a.getAnnotationType().getQualifiedName().equals(Imports.class.getName()))
                .findFirst().orElse(null);
        if (importsAnnotation == null) {
            return Collections.emptyList();
        }
        spoon.reflect.code.CtExpression<?> valueExpr = importsAnnotation.getValue("value");
        List<CtAnnotation<?>> entries = new ArrayList<>();
        if (valueExpr instanceof spoon.reflect.code.CtNewArray) {
            for (Object elt : ((spoon.reflect.code.CtNewArray<?>) valueExpr).getElements()) {
                if (elt instanceof CtAnnotation) {
                    entries.add((CtAnnotation<?>) elt);
                }
            }
        } else if (valueExpr instanceof CtAnnotation) {
            entries.add((CtAnnotation<?>) valueExpr);
        }

        List<String> names = new ArrayList<>();
        for (CtAnnotation<?> entry : entries) {
            spoon.reflect.code.CtExpression<?> importValue = entry.getValue("value");
            if (importValue instanceof spoon.reflect.code.CtFieldRead) {
                spoon.reflect.code.CtFieldRead<?> fieldRead = (spoon.reflect.code.CtFieldRead<?>) importValue;
                if (fieldRead.getTarget() instanceof spoon.reflect.code.CtTypeAccess) {
                    names.add(((spoon.reflect.code.CtTypeAccess<?>) fieldRead.getTarget())
                            .getAccessedType().getQualifiedName());
                }
            }
        }
        return names;
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

        // Re-key the type index: the same CtType instance now reports newKey as
        // its qualified name, so move its entry from oldKey to newKey.
        if (typeIndex != null) {
            CtType<?> ctType = typeIndex.remove(oldKey);
            if (ctType != null) {
                typeIndex.put(newKey, ctType);
            }
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

    // -------------------------------------------------------------------------
    // Deferred-save plumbing (pending buffers) — see editable-source-dirty-buffer-design.md.
    // Package-private; called by SourceCompilationUnit / SourceModelEntity mutations.
    // -------------------------------------------------------------------------

    /** Records (or replaces) an unsaved source edit for the unit with this primary type name. */
    void registerPendingSource(String primaryTypeName, File file, String text) {
        pendingSources.put(primaryTypeName, new PendingSource(file, text));
    }

    /** Drops the pending edit for this primary type name (called after a flush). */
    void clearPendingSource(String primaryTypeName) {
        pendingSources.remove(primaryTypeName);
    }

    /** The pending (unsaved) source text for this primary type name, or {@code null} if none. */
    String getPendingSourceText(String primaryTypeName) {
        PendingSource ps = pendingSources.get(primaryTypeName);
        return ps != null ? ps.text : null;
    }

    /** Re-keys a pending buffer when an entity (and its file) is renamed. */
    void renamePendingSource(String oldPrimaryTypeName, String newPrimaryTypeName, File newFile) {
        PendingSource ps = pendingSources.remove(oldPrimaryTypeName);
        if (ps != null) {
            pendingSources.put(newPrimaryTypeName, new PendingSource(newFile, ps.text));
        }
    }

    /** Marks a file for deletion on the next flush (deferred entity delete / rename old file). */
    void registerPendingDeletion(File f) {
        if (f != null) {
            pendingDeletions.add(f);
        }
    }

    // -------------------------------------------------------------------------
    // Public deferred-save API
    // -------------------------------------------------------------------------

    /** {@code true} if any compilation unit has unsaved (dirty) source edits or pending deletions. */
    public boolean isDirty() {
        return !pendingSources.isEmpty() || !pendingDeletions.isEmpty();
    }

    /** The compilation units whose buffers are dirty (unsaved). */
    public List<SourceCompilationUnit> getDirtyCompilationUnits() {
        List<SourceCompilationUnit> result = new ArrayList<>();
        for (String qn : pendingSources.keySet()) {
            SourceCompilationUnit cu = compilationUnits.get(qn);
            if (cu != null) {
                result.add(cu);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Writes every dirty buffer to disk and applies deferred deletions, then clears the
     * dirty state. This is the only disk write of the deferred-save flow; call it on an
     * explicit <em>Save project</em>.
     *
     * @throws IOException if a file write or delete fails
     */
    public void flushAll() throws IOException {
        boolean wasDirty = isDirty();

        // 1. Write pending buffers.
        java.util.Set<String> writeTargets = new java.util.HashSet<>();
        for (PendingSource ps : pendingSources.values()) {
            if (ps.file == null) {
                continue;
            }
            File parent = ps.file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            java.nio.file.Files.write(ps.file.toPath(),
                    ps.text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            writeTargets.add(canon(ps.file));
        }

        // 2. Apply deferred deletions (never delete a file that is also a write target).
        for (File f : pendingDeletions) {
            if (!writeTargets.contains(canon(f)) && f.exists()) {
                f.delete();
            }
        }

        // 3. Clear dirty flags on the live compilation units, then drop the pending state.
        for (String qn : pendingSources.keySet()) {
            SourceCompilationUnit cu = compilationUnits.get(qn);
            if (cu != null) {
                cu.markClean();
            }
        }
        pendingSources.clear();
        pendingDeletions.clear();

        if (wasDirty) {
            pcSupport.firePropertyChange("dirty", true, false);
        }
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
        return createEntity(simpleName, sourcePackage, null, false);
    }

    /**
     * Creates a new {@code @ModelEntity} interface in the given package and
     * registers it in this meta-model, with explicit placement and abstractness.
     *
     * <p>{@code isAbstract} is baked directly into the {@code @ModelEntity}
     * annotation's AST at construction time (not applied afterwards via
     * {@link SourceModelEntity#setAbstract(boolean)}, which requires a resolved
     * source position that a freshly-created, never-parsed interface does not
     * have yet).</p>
     *
     * @param simpleName    the simple name for the new interface
     * @param sourcePackage the package in which to place the new entity
     * @param sourceFolder  the source directory to place the file under, or
     *                      {@code null} to use the same heuristic as the two-arg
     *                      overload (reuse an existing entity's directory, else
     *                      the first registered source directory)
     * @param isAbstract    whether the new entity is declared abstract
     * @return the newly created {@link SourceModelEntity}
     * @throws IOException if the file cannot be written
     */
    public SourceModelEntity createEntity(String simpleName, SourcePackage sourcePackage,
            SourceFolder sourceFolder, boolean isAbstract) throws IOException {
        String pkgName = sourcePackage.getQualifiedName();
        String qualifiedName = (pkgName == null || pkgName.isEmpty())
                ? simpleName
                : pkgName + "." + simpleName;

        // Determine the target directory
        File targetDir = resolveTargetDirectory(sourcePackage, sourceFolder);
        targetDir.mkdirs();
        File newFile = new File(targetDir, simpleName + ".java");

        // Use the Spoon factory from the existing model
        Factory factory = ctModel.getRootPackage().getFactory();

        // Create the CtInterface
        CtInterface<Object> newInterface = factory.Core().createInterface();
        newInterface.setSimpleName(simpleName);
        newInterface.addModifier(ModifierKind.PUBLIC);

        // Add @ModelEntity annotation (AST-based: this is a brand-new file with no
        // existing text to edit, generated via DefaultJavaPrettyPrinter).
        CtAnnotation<?> modelEntityAnnotation = factory.Core().createAnnotation();
        modelEntityAnnotation.setAnnotationType(
                factory.Type().createReference(ModelEntity.class));
        if (isAbstract) {
            modelEntityAnnotation.addValue("isAbstract", true);
        }
        newInterface.addAnnotation(modelEntityAnnotation);

        // Place the interface in the correct package. sourcePackage.getCtPackage() may still be
        // null here: a SourcePackage only gets Spoon-linked once it has at least one entity (see
        // the package-wiring loop in buildProperties), so a brand-new, still-empty package
        // (e.g. just created via createPackage, with only a package-info.java) has no CtPackage
        // yet. Resolve/create it via the factory instead of assuming it is already set, and keep
        // the SourcePackage linked for good so later calls don't hit the same gap.
        CtPackage ctPackage = sourcePackage.getCtPackage();
        if (ctPackage == null) {
            ctPackage = (pkgName == null || pkgName.isEmpty())
                    ? factory.Package().getRootPackage()
                    : factory.Package().getOrCreate(pkgName);
            sourcePackage.setCtPackage(ctPackage);
        }
        ctPackage.addType(newInterface);

        // Create the compilation unit wrapper
        SourceCompilationUnit scu = SourceCompilationUnit.forNewType(newInterface, newFile, this);

        // Buffer the new source (deferred): the .java file is written on the next flush
        // (Save project). The new entity is dirty from creation, like a new diagram.
        scu.regenerateFromAST();

        // Build the SourceModelEntity
        SourceModelEntity entity = new SourceModelEntity(newInterface, this);
        entity.setSourcePackage(sourcePackage);
        entity.setCompilationUnit(scu);

        // Register in the meta-model
        entities.put(qualifiedName, entity);
        compilationUnits.put(qualifiedName, scu);
        sourcePackage.addEntity(entity);

        // Keep the type index in sync so a subsequent findType() resolves it
        // without falling back to a full scan.
        if (typeIndex != null) {
            typeIndex.put(qualifiedName, newInterface);
        }

        return entity;
    }

    /**
     * Generates a brand-new abstract implementation class for {@code entity} and attaches it
     * via {@code @ImplementationClass} — the "create" path of
     * {@code implementation-class-support-design.md §3.2} (the "attach" path for an
     * <em>existing</em> class is {@link SourceModelEntity#attachImplementationClass}).
     *
     * <p>Placed in the same directory as the entity's own file — the universal convention in
     * this codebase ({@code ConnectorViewImpl} next to {@code ConnectorView}, etc.); no
     * folder/package picker. Public, abstract, implementing the entity's interface. Unlike
     * {@code attachImplementationClass} (which only edits text, since the target may be an
     * arbitrary, not-yet-tracked class), this method has everything needed to eagerly
     * construct and register a fully valid {@link SourceImplementationClass} in the same call
     * (own compilation unit; freshly generated code is by construction abstract and
     * implements the entity, so neither of {@code SourceImplementationClass}'s validation
     * rules can fire) — mirrors {@link #createEntity}'s eager, non-deferred registration.</p>
     *
     * @param entity     the entity to attach the new implementation class to
     * @param simpleName the simple name for the new class, e.g. {@code "FooImpl"}
     * @return the newly created {@link SourceImplementationClass}
     * @throws IOException if the file cannot be written
     */
    public SourceImplementationClass createImplementationClass(SourceModelEntity entity, String simpleName)
            throws IOException {
        SourcePackage sourcePackage = entity.getSourcePackage();
        String pkgName = sourcePackage != null ? sourcePackage.getQualifiedName() : "";
        String qualifiedName = (pkgName == null || pkgName.isEmpty())
                ? simpleName
                : pkgName + "." + simpleName;

        File entityFile = entity.getCompilationUnit().getFile();
        File targetDir = entityFile.getParentFile();
        targetDir.mkdirs();
        File newFile = new File(targetDir, simpleName + ".java");

        Factory factory = ctModel.getRootPackage().getFactory();

        CtClass<Object> newClass = factory.Core().createClass();
        newClass.setSimpleName(simpleName);
        newClass.addModifier(ModifierKind.PUBLIC);
        newClass.addModifier(ModifierKind.ABSTRACT);
        newClass.addSuperInterface(entity.getCtType().getReference());

        CtPackage ctPackage = sourcePackage.getCtPackage();
        if (ctPackage == null) {
            ctPackage = (pkgName == null || pkgName.isEmpty())
                    ? factory.Package().getRootPackage()
                    : factory.Package().getOrCreate(pkgName);
            sourcePackage.setCtPackage(ctPackage);
        }
        ctPackage.addType(newClass);

        // Create the compilation unit wrapper (deferred-save: buffer only, see createEntity).
        SourceCompilationUnit scu = SourceCompilationUnit.forNewType(newClass, newFile, this);
        scu.regenerateFromAST();

        // Build and register the SourceImplementationClass, and wire it on the entity.
        SourceImplementationClass impl = new SourceImplementationClass(newClass, entity, scu);
        entity.setImplementationClass(impl);
        entity.attachImplementationClass(qualifiedName);

        compilationUnits.put(qualifiedName, scu);
        if (typeIndex != null) {
            typeIndex.put(qualifiedName, newClass);
        }

        return impl;
    }

    /**
     * Turns an existing Java type — currently not a PAMELA entity — into one, by
     * inserting an {@code @ModelEntity} annotation into its source, then
     * registering it as a root type so it enters the meta-model on the next
     * rebuild.
     *
     * <p>The target {@code CtType} is resolved from the already-built Spoon model
     * (every file in the source directories was parsed by {@code buildModel()},
     * not only the types reachable from the roots), so no re-parsing happens here.
     * Only the affected {@code .java} file is rewritten. This is fast and safe to
     * call on the Event Dispatch Thread; the caller is responsible for triggering
     * the (background) {@link #rebuildMetaModel()} that actually re-analyses the
     * source and materialises the new {@link SourceModelEntity}.</p>
     *
     * <p>Applicable to both interfaces and classes. {@code @ModelEntity} on a
     * class is not yet fully supported and will surface a validation
     * {@link Error} after the rebuild — this is intentional and handled by the
     * normal validation flow.</p>
     *
     * @param file the Java file to promote (must not be {@code null})
     * @throws IOException           if the source file cannot be written
     * @throws IllegalStateException if the type cannot be located in the Spoon model
     */
    public void declareAsEntity(SourceJavaFile file) throws IOException {
        String qualifiedName = file.getQualifiedName();
        CtType<?> ctType = findType(qualifiedName);
        if (ctType == null) {
            throw new IllegalStateException(
                    "Cannot declare entity: type not found in Spoon model: " + qualifiedName);
        }
        if (ctType.getPosition() == null || !ctType.getPosition().isValidPosition()) {
            throw new IllegalStateException(
                    "Cannot declare entity: no source position for " + qualifiedName);
        }

        File javaFile = file.getFile();

        // Edit the source text (buffered, deferred): insert @ModelEntity above the
        // declaration (using Spoon's position) and add the import. Minimal-diff and
        // immune to the Sniper annotation-insertion defect. Written on the next flush.
        SourceCompilationUnit cu = compilationUnits.get(qualifiedName);
        String source = (cu != null) ? cu.getText()
                : new String(java.nio.file.Files.readAllBytes(javaFile.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8);
        int declarationStart = ctType.getPosition().getSourceStart();
        String edited = SourceAnnotationEditor.addAnnotation(
                source, declarationStart, "ModelEntity", ModelEntity.class.getName());
        if (cu != null) {
            cu.setText(edited);
        } else {
            registerPendingSource(qualifiedName, javaFile, edited);
        }

        // Register as a root type so the next rebuild materialises the entity,
        // and drop the stale textual cache on the lightweight file wrapper.
        if (!rootTypeNames.contains(qualifiedName)) {
            addRootTypeName(qualifiedName);
        }
        file.invalidateContent();
    }

    /**
     * Creates a new, initially empty package under the given {@link SourceFolder} and registers
     * it in this meta-model.
     *
     * <p>A directory is only recognised as a package once it contains at least one {@code .java}
     * file (see {@link #scanForPackages}) — an empty directory would vanish on the very next
     * rebuild, which always follows a model-editing action. A minimal {@code package-info.java}
     * stub (just the {@code package} declaration) is therefore written <b>immediately</b> — not
     * deferred like entity content — so the package survives. This mirrors {@code mkdirs()}
     * already being an immediate, non-buffered side effect in {@link #createEntity}.</p>
     *
     * <p>If the same qualified package name already exists (e.g. under a different source
     * folder — a package may legitimately span several source directories), the existing
     * {@link SourcePackage} is reused and simply gains this new directory's {@code package-info.java}
     * as one more of its {@linkplain SourcePackage#getJavaFiles() java files}.</p>
     *
     * <p><b>Placement is anchored on the longest already-known ancestor package, not naively
     * derived from the folder's own root.</b> A registered source folder does not necessarily
     * correspond to the empty/default package: it may already be positioned at a non-trivial
     * ambient package (e.g. a folder literally named {@code model2} whose files declare
     * {@code package test.model2;} — a deliberately non-conventional layout used by this
     * project's own test fixtures, but the same reasoning applies to any folder registered below
     * a project's true source root). Naively computing
     * {@code folder.getDirectory() + qualifiedName.replace('.', separator)} would then double up
     * the ambient prefix (creating {@code test.model2.foo} under a folder that is *already*
     * {@code test/model2} would land at {@code model2/test/model2/foo} instead of
     * {@code model2/foo}). Instead, {@link #resolvePackageDirectory} finds the longest package
     * already known to this folder whose qualified name is a dotted prefix of the new one, and
     * nests only the remaining segments under that package's own (already resolved) directory —
     * mirroring {@link #resolveTargetDirectory(SourcePackage)}'s "reuse a sibling's real directory
     * rather than assume a path convention" strategy for entities.</p>
     *
     * @param qualifiedName the new package's qualified name, e.g. {@code "org.example.sub"}
     * @param sourceFolder  the source directory to create the package under
     * @return the (possibly pre-existing) {@link SourcePackage}
     * @throws IOException if the directory or the {@code package-info.java} file cannot be written
     */
    public SourcePackage createPackage(String qualifiedName, SourceFolder sourceFolder) throws IOException {
        File targetDir = resolvePackageDirectory(qualifiedName, sourceFolder);
        targetDir.mkdirs();

        File packageInfoFile = new File(targetDir, "package-info.java");
        if (!packageInfoFile.exists()) {
            Files.write(packageInfoFile.toPath(),
                    ("package " + qualifiedName + ";\n").getBytes(StandardCharsets.UTF_8));
        }

        SourcePackage pkg = packages.get(qualifiedName);
        if (pkg == null) {
            pkg = new SourcePackage(qualifiedName, this);
            packages.put(qualifiedName, pkg);
        }
        final SourcePackage finalPkg = pkg;
        boolean alreadyPresent = finalPkg.getJavaFiles().stream()
                .anyMatch(f -> f.getFile().equals(packageInfoFile));
        if (!alreadyPresent) {
            pkg.addJavaFile(new SourceJavaFile(packageInfoFile, sourceFolder.getDirectory(), this));
        }
        return pkg;
    }

    /**
     * Determines the file-system directory for a new package {@code qualifiedName} under
     * {@code sourceFolder} — see {@link #createPackage}'s javadoc for the rationale.
     *
     * <p>Finds the longest package already known to {@code sourceFolder} (i.e. one of
     * {@link SourceFolder#getPackages()}) whose qualified name is a dotted prefix of
     * {@code qualifiedName}, resolves that package's own directory <em>within this folder</em>
     * (from one of its existing java files), and nests only the remaining dotted segments under
     * it. Falls back to {@code sourceFolder.getDirectory() + qualifiedName-as-path} when no
     * ancestor package is found (a genuinely new, unrelated top-level hierarchy).</p>
     */
    private File resolvePackageDirectory(String qualifiedName, SourceFolder sourceFolder) {
        String bestAncestor = "";
        File bestAncestorDir = null;
        for (SourcePackage candidate : sourceFolder.getPackages()) {
            String candidateName = candidate.getQualifiedName();
            if (!isPackagePrefixOf(candidateName, qualifiedName) || candidateName.length() <= bestAncestor.length()) {
                continue;
            }
            File dir = directoryOfPackageInFolder(candidate, sourceFolder);
            if (dir != null) {
                bestAncestor = candidateName;
                bestAncestorDir = dir;
            }
        }
        File baseDir = bestAncestorDir != null ? bestAncestorDir : sourceFolder.getDirectory();
        String remaining;
        if (qualifiedName.equals(bestAncestor)) {
            remaining = ""; // exact match: qualifiedName IS the ancestor package itself
        } else if (bestAncestor.isEmpty()) {
            remaining = qualifiedName;
        } else {
            remaining = qualifiedName.substring(bestAncestor.length() + 1); // skip the separating dot
        }
        return remaining.isEmpty() ? baseDir : new File(baseDir, remaining.replace('.', File.separatorChar));
    }

    /** {@code true} if {@code prefix} is a non-empty package that {@code full} equals or nests under. */
    private static boolean isPackagePrefixOf(String prefix, String full) {
        return !prefix.isEmpty() && (full.equals(prefix) || full.startsWith(prefix + "."));
    }

    /** The directory of {@code pkg}'s own files within {@code folder}, or {@code null} if none found. */
    private static File directoryOfPackageInFolder(SourcePackage pkg, SourceFolder folder) {
        for (SourceJavaFile jf : pkg.getJavaFiles()) {
            if (jf.getSourceDirectory().equals(folder.getDirectory())) {
                File parent = jf.getFile().getParentFile();
                if (parent != null) {
                    return parent;
                }
            }
        }
        return null;
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

    /**
     * Like {@link #resolveTargetDirectory(SourcePackage)}, but anchored to an
     * explicitly chosen {@link SourceFolder} (e.g. when a package spans several
     * source directories and the user picked a specific one). Falls back to the
     * folder-agnostic heuristic when {@code sourceFolder} is {@code null}.
     *
     * <p>Delegates to {@link #resolvePackageDirectory}, which resolves
     * {@code sourcePackage}'s own physical directory within the folder from any of
     * its known files (entity or not — including a still-empty package's own
     * {@code package-info.java}), not just its entities. An earlier version only
     * looked at existing entities and fell back to a naive
     * {@code folder + qualifiedName-as-path} otherwise, which reintroduced the
     * same ambient-prefix doubling bug fixed for {@link #createPackage} (§ its
     * javadoc) whenever the target package had zero entities yet — e.g. right
     * after creating a brand-new package and then immediately adding its first
     * entity.</p>
     */
    private File resolveTargetDirectory(SourcePackage sourcePackage, SourceFolder sourceFolder) {
        if (sourceFolder == null) {
            return resolveTargetDirectory(sourcePackage);
        }
        return resolvePackageDirectory(sourcePackage.getQualifiedName(), sourceFolder);
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

    /**
     * Editable-inspector setter: renames the display name (a free-form string, persisted in
     * the {@code .pamela} project file — not a source-code mutation, so it does not touch
     * any compilation unit). Fires {@code "name"} so any live label binding
     * (e.g. {@code MetaModelSummaryView}'s header) refreshes.
     */
    public void setName(String name) {
        String old = this.name;
        this.name = name;
        pcSupport.firePropertyChange("name", old, name);
    }

    /**
     * Fires a UI-intent signal that the user asked to rename this meta-model (the inspector's
     * "Rename…" button, next to the read-only name field). The model opens no dialog; the
     * application observes the inspected element and prompts for a new name in response
     * (model-editing-design.md §6) — a plain project-metadata edit, not a source rebuild.
     */
    public void requestRename() {
        pcSupport.firePropertyChange("renameRequested", null, this);
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
     * The registered source directories, each wrapped as a browsable
     * {@link SourceFolder} (ui-design.md §4.1), in registration order.
     *
     * @return an unmodifiable list
     */
    public List<SourceFolder> getSourceFolders() {
        List<SourceFolder> result = new ArrayList<>(sourceDirectories.size());
        for (File dir : sourceDirectories) {
            SourceFolder folder = sourceFolders.get(dir);
            if (folder != null) {
                result.add(folder);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Packages containing at least one {@code .java} file scanned from
     * {@code directory}, in the same order as {@link #getAllPackages()}.
     *
     * @param directory one of the registered {@link #getSourceDirectories()}
     * @return an unmodifiable list (possibly empty)
     */
    public List<SourcePackage> getPackagesForSourceDirectory(File directory) {
        List<SourcePackage> result = new ArrayList<>();
        for (SourcePackage pkg : packages.values()) {
            for (SourceJavaFile file : pkg.getJavaFiles()) {
                if (file.getSourceDirectory().equals(directory)) {
                    result.add(pkg);
                    break;
                }
            }
        }
        return Collections.unmodifiableList(result);
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

    /** Returns the number of {@link Error} issues in this metamodel. */
    public int getErrorsCount() {
        return (int) issues.stream().filter(i -> i instanceof Error).count();
    }

    /** Returns the number of {@link Warning} issues in this metamodel. */
    public int getWarningsCount() {
        return (int) issues.stream().filter(i -> i instanceof Warning).count();
    }

    /** Returns the number of {@link Information} issues in this metamodel. */
    public int getInformationCount() {
        return (int) issues.stream().filter(i -> i instanceof Information).count();
    }

    /**
     * Returns {@code true} if at least one {@link Error} is raised against {@code element}
     * itself, <em>or against any element nested underneath it</em> in the model's
     * containment hierarchy (see {@link #childrenOf(SourceElement)}) — e.g. a property error
     * is also reported by its owning entity, package and source folder, and by the metamodel
     * itself. Used to decorate a container's icon with an error marker so a problem is
     * visible without expanding — see {@code context-menu-design.md} for the
     * base-icon-plus-marker composition mechanism.
     */
    public boolean hasErrors(SourceElement element) {
        return hasIssue(element, Error.class);
    }

    /**
     * Returns {@code true} if at least one {@link Warning} is raised against {@code element}
     * itself, or against any element nested underneath it — see {@link #hasErrors(SourceElement)}.
     */
    public boolean hasWarnings(SourceElement element) {
        return hasIssue(element, Warning.class);
    }

    /**
     * Shared recursive lookup for {@link #hasErrors(SourceElement)} / {@link #hasWarnings(SourceElement)}:
     * true if {@code element} itself carries an issue of the given severity (identity comparison
     * on {@link Issue#getSource()}), or if any of its {@linkplain #childrenOf(SourceElement)
     * containment children} does, recursively.
     */
    private boolean hasIssue(SourceElement element, Class<? extends Issue> severityClass) {
        if (element == null) {
            return false;
        }
        for (Issue issue : issues) {
            if (severityClass.isInstance(issue) && issue.getSource() == element) {
                return true;
            }
        }
        for (SourceElement child : childrenOf(element)) {
            if (hasIssue(child, severityClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The direct containment children of {@code element}, used only to roll up issue
     * severity markers (see {@link #hasIssue(SourceElement, Class)}). Mirrors the
     * containment hierarchy of {@code source-metamodel-design.md §2}:
     * metamodel → entities; source folder → packages; package → entities;
     * entity → properties, initializers, custom methods, implementation class.
     * Returns an empty list for element kinds that have no containment children
     * (properties, initializers, custom methods, implementation classes).
     */
    private List<SourceElement> childrenOf(SourceElement element) {
        if (element instanceof SourceMetaModel) {
            return new ArrayList<>(((SourceMetaModel) element).getEntities().values());
        }
        if (element instanceof SourceFolder) {
            return new ArrayList<>(((SourceFolder) element).getPackages());
        }
        if (element instanceof SourcePackage) {
            return new ArrayList<>(((SourcePackage) element).getEntities());
        }
        if (element instanceof SourceModelEntity) {
            SourceModelEntity entity = (SourceModelEntity) element;
            List<SourceElement> children = new ArrayList<>();
            children.addAll(entity.getDeclaredProperties().values());
            children.addAll(entity.getInitializers());
            children.addAll(entity.getDeclaredCustomMethods());
            SourceImplementationClass impl = entity.getImplementationClass();
            if (impl != null) {
                children.add(impl);
            }
            return children;
        }
        return Collections.emptyList();
    }

    /** {@code this} — a {@link SourceMetaModel} is its own owning meta-model. */
    @Override
    public SourceMetaModel getMetaModel() {
        return this;
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
