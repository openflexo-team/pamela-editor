package org.openflexo.pamela.editor.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;

/**
 * Tests for all 8 mutation operations on the {@code Source*} model layer.
 *
 * <p>Each test:
 * <ol>
 *   <li>Copies the relevant test model directory into a {@code TemporaryFolder}.</li>
 *   <li>Writes a {@code .pamela} project file pointing to the copy.</li>
 *   <li>Loads the meta-model, performs a mutation, and asserts in-memory state.</li>
 *   <li>Reloads from the written-back source and asserts the persisted state.</li>
 * </ol>
 * </p>
 */
public class TestMutations {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Copies a flat source directory (one level deep — no sub-directories) into
     * {@code destParent}.
     *
     * @param src        source directory
     * @param destParent destination parent; the copy is placed as
     *                   {@code destParent/src.getName()}
     * @return the copied directory
     */
    private File copyDir(File src, File destParent) throws IOException {
        File dest = new File(destParent, src.getName());
        dest.mkdirs();
        File[] files = src.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    Files.copy(f.toPath(), new File(dest, f.getName()).toPath());
                }
            }
        }
        return dest;
    }

    /**
     * Writes a minimal {@code .pamela} project file.
     *
     * @param pamelaFile     destination file
     * @param sourceDir      source directory (relative path will be computed)
     * @param rootTypes      fully qualified root type names
     */
    private void writePamelaFile(File pamelaFile, File sourceDir, String... rootTypes) throws IOException {
        // Compute relative path from the pamela file's parent to the source dir
        File base = pamelaFile.getParentFile();
        String relPath = base.toPath().relativize(sourceDir.toPath()).toString()
                .replace(File.separatorChar, '/');

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": \"mutation-test\",\n");
        sb.append("  \"sourceDirectories\": [\"").append(relPath).append("\"],\n");
        sb.append("  \"rootTypes\": [");
        for (int i = 0; i < rootTypes.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"').append(rootTypes[i]).append('"');
        }
        sb.append("]\n");
        sb.append("}\n");

        FileWriter fw = new FileWriter(pamelaFile);
        fw.write(sb.toString());
        fw.close();
    }

    // =========================================================================
    // Base directories for the original (read-only) test models
    // =========================================================================

    private static final File MODEL1_SRC = new File(
            System.getProperty("user.dir") + "/src/test/java/test/model1");

    private static final File MODEL2_SRC = new File(
            System.getProperty("user.dir") + "/src/test/java/test/model2");

    // =========================================================================
    // Test 1 — rename
    // =========================================================================

    /**
     * Rename {@code Foo1} to {@code Bar1}, then verify:
     * - In-memory: simpleName, qualifiedName, entity map key updated.
     * - On disk: {@code Bar1.java} exists, {@code Foo1.java} is deleted.
     * - After reload: {@code Bar1} is discoverable, entity count still 2.
     */
    @Test
    public void testRename() throws IOException {
        // --- Setup ---
        File workDir = tmp.newFolder("testRename");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        assertEquals(2, mm.getEntities().size());

        SourceModelEntity foo1 = mm.getEntity("test.model1.Foo1");
        assertNotNull(foo1);

        // --- Mutate ---
        foo1.rename("Bar1");
        mm.flushAll();

        // --- In-memory assertions ---
        assertEquals("Bar1", foo1.getSimpleName());
        assertEquals("test.model1.Bar1", foo1.getQualifiedName());
        assertNull("Old key should be gone", mm.getEntity("test.model1.Foo1"));
        assertNotNull("New key should exist", mm.getEntity("test.model1.Bar1"));

        // --- File assertions ---
        File bar1File = new File(srcCopy, "Bar1.java");
        File foo1File = new File(srcCopy, "Foo1.java");
        assertTrue("Bar1.java must exist on disk", bar1File.exists());
        assertFalse("Foo1.java must be deleted", foo1File.exists());

        // --- Reload ---
        mm.flushAll(); // deferred-save: persist buffers before reloading from disk
        File pamelaFile2 = new File(workDir, "test2.pamela");
        writePamelaFile(pamelaFile2, srcCopy, "test.model1.Bar1");
        SourceMetaModel mm2 = SourceMetaModelSerializer.load(pamelaFile2);

        assertEquals("After reload, entity count must still be 2", 2, mm2.getEntities().size());
        assertNotNull("Bar1 must be discoverable after reload", mm2.getEntity("test.model1.Bar1"));
        assertNull("Foo1 must not exist after reload", mm2.getEntity("test.model1.Foo1"));
    }

    // =========================================================================
    // Test 2 — addSingleProperty
    // =========================================================================

    /**
     * Add a SINGLE property {@code "nickname"} (type {@code java.lang.String})
     * to {@code Foo2}.
     */
    @Test
    public void testAddSingleProperty() throws IOException {
        // --- Setup ---
        File workDir = tmp.newFolder("testAddSingle");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);

        SourceModelEntity foo2 = mm.getEntity("test.model1.Foo2");
        assertNotNull(foo2);
        // Foo2 initially has only "name"
        assertEquals(1, foo2.getDeclaredProperties().size());

        // --- Mutate ---
        SourceModelProperty prop = foo2.addSingleProperty("nickname", "java.lang.String");

        // --- In-memory assertions ---
        assertNotNull(prop);
        assertEquals("nickname", prop.getPropertyIdentifier());
        assertEquals(Cardinality.SINGLE, prop.getCardinality());
        assertEquals("java.lang.String", prop.getType().getQualifiedName());
        assertNotNull("Getter must be present", prop.getGetterMethodName());
        assertNotNull("Setter must be present", prop.getSetterMethodName());
        assertTrue("Property must be in declaredProperties",
                foo2.getDeclaredProperties().containsKey("nickname"));

        // --- Reload ---
        mm.flushAll(); // deferred-save: persist buffers before reloading from disk
        File pamelaFile2 = new File(workDir, "test2.pamela");
        writePamelaFile(pamelaFile2, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm2 = SourceMetaModelSerializer.load(pamelaFile2);

        SourceModelEntity foo2r = mm2.getEntity("test.model1.Foo2");
        assertNotNull(foo2r);
        assertTrue("'nickname' must be present after reload",
                foo2r.getDeclaredProperties().containsKey("nickname"));
        assertEquals(Cardinality.SINGLE,
                foo2r.getDeclaredProperties().get("nickname").getCardinality());
    }

    // =========================================================================
    // Test 3 — addListProperty
    // =========================================================================

    /**
     * Add a LIST property {@code "aliases"} (element type {@code java.lang.String})
     * to {@code Foo1}.
     */
    @Test
    public void testAddListProperty() throws IOException {
        // --- Setup ---
        File workDir = tmp.newFolder("testAddList");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);

        SourceModelEntity foo1 = mm.getEntity("test.model1.Foo1");
        assertNotNull(foo1);
        int beforeCount = foo1.getDeclaredProperties().size();

        // --- Mutate ---
        SourceModelProperty prop = foo1.addListProperty("aliases", "java.lang.String");

        // --- In-memory assertions ---
        assertNotNull(prop);
        assertEquals("aliases", prop.getPropertyIdentifier());
        assertEquals(Cardinality.LIST, prop.getCardinality());
        assertEquals("java.lang.String", prop.getType().getQualifiedName());
        assertNotNull("Adder must be present", prop.getAdderMethodName());
        assertNotNull("Remover must be present", prop.getRemoverMethodName());
        assertEquals("DeclaredProperties count should grow by 1",
                beforeCount + 1, foo1.getDeclaredProperties().size());

        // --- Reload ---
        mm.flushAll(); // deferred-save: persist buffers before reloading from disk
        File pamelaFile2 = new File(workDir, "test2.pamela");
        writePamelaFile(pamelaFile2, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm2 = SourceMetaModelSerializer.load(pamelaFile2);

        SourceModelEntity foo1r = mm2.getEntity("test.model1.Foo1");
        assertNotNull(foo1r);
        assertTrue("'aliases' must be present after reload",
                foo1r.getDeclaredProperties().containsKey("aliases"));
        SourceModelProperty aliasProp = foo1r.getDeclaredProperties().get("aliases");
        assertEquals(Cardinality.LIST, aliasProp.getCardinality());
        assertNotNull("Adder must survive reload", aliasProp.getAdderMethodName());
        assertNotNull("Remover must survive reload", aliasProp.getRemoverMethodName());
    }

    // =========================================================================
    // Test 4 — remove property
    // =========================================================================

    /**
     * Remove the {@code "foo2"} property from {@code Foo1}.
     */
    @Test
    public void testRemoveProperty() throws IOException {
        // --- Setup ---
        File workDir = tmp.newFolder("testRemove");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);

        SourceModelEntity foo1 = mm.getEntity("test.model1.Foo1");
        assertNotNull(foo1);
        SourceModelProperty foo2Prop = foo1.getDeclaredProperties().get("foo2");
        assertNotNull("'foo2' must exist before removal", foo2Prop);

        // --- Mutate ---
        foo2Prop.remove();

        // --- In-memory assertions ---
        assertFalse("'foo2' must be gone from declaredProperties",
                foo1.getDeclaredProperties().containsKey("foo2"));

        // --- Reload ---
        mm.flushAll(); // deferred-save: persist buffers before reloading from disk
        File pamelaFile2 = new File(workDir, "test2.pamela");
        writePamelaFile(pamelaFile2, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm2 = SourceMetaModelSerializer.load(pamelaFile2);

        SourceModelEntity foo1r = mm2.getEntity("test.model1.Foo1");
        assertNotNull(foo1r);
        assertFalse("'foo2' must be gone after reload",
                foo1r.getDeclaredProperties().containsKey("foo2"));
    }

    // =========================================================================
    // Test 5 — createEntity
    // =========================================================================

    /**
     * Create a new entity {@code Foo3} in the {@code test.model1} package.
     */
    @Test
    public void testCreateEntity() throws IOException {
        // --- Setup ---
        File workDir = tmp.newFolder("testCreate");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        assertEquals(2, mm.getEntities().size());

        SourcePackage pkg = mm.getPackage("test.model1");
        assertNotNull(pkg);

        // --- Mutate ---
        SourceModelEntity foo3 = mm.createEntity("Foo3", pkg);
        mm.flushAll();

        // --- In-memory assertions ---
        assertNotNull(foo3);
        assertEquals("Foo3", foo3.getSimpleName());
        assertEquals("test.model1.Foo3", foo3.getQualifiedName());
        assertEquals(3, mm.getEntities().size());
        assertNotNull(mm.getEntity("test.model1.Foo3"));

        // File must exist on disk
        File foo3File = new File(srcCopy, "Foo3.java");
        assertTrue("Foo3.java must exist on disk", foo3File.exists());

        // --- Reload ---
        mm.flushAll(); // deferred-save: persist buffers before reloading from disk
        File pamelaFile2 = new File(workDir, "test2.pamela");
        writePamelaFile(pamelaFile2, srcCopy, "test.model1.Foo1", "test.model1.Foo3");
        SourceMetaModel mm2 = SourceMetaModelSerializer.load(pamelaFile2);

        assertEquals("After reload, entity count must be 3", 3, mm2.getEntities().size());
        assertNotNull("Foo3 must be discoverable after reload", mm2.getEntity("test.model1.Foo3"));
    }

    /**
     * The 4-arg {@link SourceMetaModel#createEntity(String, SourcePackage, SourceFolder, boolean)}
     * overload (used by {@code NewEntityAction}'s richer dialog): the file lands under the chosen
     * {@link SourceFolder}, {@code isAbstract} is baked into the {@code @ModelEntity} annotation at
     * AST-creation time (no source-position dependency), and a super-entity can be added right after
     * creation in the same synchronous call — all three must round-trip through a reload.
     */
    @Test
    public void testCreateEntityWithFolderAbstractAndSuperEntity() throws IOException {
        // --- Setup ---
        File workDir = tmp.newFolder("testCreateAbstractFolder");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourcePackage pkg = mm.getPackage("test.model1");
        assertNotNull(pkg);
        SourceModelEntity foo1 = mm.getEntity("test.model1.Foo1");
        assertNotNull(foo1);
        assertEquals(1, mm.getSourceFolders().size());
        SourceFolder folder = mm.getSourceFolders().get(0);

        // --- Mutate: create an abstract entity in the explicit folder, then add a super-entity ---
        // Named "Foo9" (not "Foo3") — model1 already has a Foo3.java fixture (a plain, not-yet-entity
        // interface used by the declare-as-entity tests) that a colliding name would shadow.
        SourceModelEntity foo9 = mm.createEntity("Foo9", pkg, folder, true);
        foo9.addSuperEntity(foo1);
        mm.flushAll();

        // --- In-memory assertions ---
        assertTrue("Newly created entity must be abstract", foo9.isAbstract());
        assertTrue("Super-entity must be recorded", foo9.getDirectSuperEntities().contains(foo1));

        // File must exist directly under the chosen folder (not nested under a spurious
        // "test/model1" subdirectory — the folder-scoped placement heuristic must not
        // double-append the package path when it can reuse a sibling entity's directory).
        File foo9File = new File(srcCopy, "Foo9.java");
        assertTrue("Foo9.java must exist directly under the chosen source folder", foo9File.exists());

        // --- Reload: isAbstract and the super-interface must round-trip through the printed source ---
        File pamelaFile2 = new File(workDir, "test2.pamela");
        writePamelaFile(pamelaFile2, srcCopy, "test.model1.Foo1", "test.model1.Foo9");
        SourceMetaModel mm2 = SourceMetaModelSerializer.load(pamelaFile2);
        SourceModelEntity foo9Reloaded = mm2.getEntity("test.model1.Foo9");
        assertNotNull(foo9Reloaded);
        assertTrue("isAbstract must survive round-trip", foo9Reloaded.isAbstract());
        assertTrue("super-entity must survive round-trip",
                foo9Reloaded.getDirectSuperEntities().contains(mm2.getEntity("test.model1.Foo1")));
    }

    /**
     * {@link SourceMetaModel#createPackage(String, SourceFolder)} (used by {@code NewPackageAction}):
     * a brand-new, empty package must be immediately visible in memory, materialise on disk right
     * away (directory + a {@code package-info.java} stub — not deferred, since an empty directory
     * alone would vanish on the very next rebuild), and survive a full reload just like any other
     * package discovered by the ordinary filesystem scan.
     *
     * <p>Uses a single-segment, unrelated name ({@code "newpkg"}, no dotted prefix in common with
     * the folder's own ambient package "test.model1") so it lands directly under the folder root —
     * the ambient-prefix-aware placement itself is covered separately by
     * {@link #testCreatePackageDoesNotDoubleTheAmbientFolderPrefix()}.</p>
     */
    @Test
    public void testCreatePackage() throws IOException {
        // --- Setup ---
        File workDir = tmp.newFolder("testCreatePackage");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        assertEquals(1, mm.getSourceFolders().size());
        SourceFolder folder = mm.getSourceFolders().get(0);
        assertNull("Package must not exist yet", mm.getPackage("newpkg"));

        // --- Mutate: create a brand-new, empty package ---
        SourcePackage created = mm.createPackage("newpkg", folder);

        // --- In-memory assertions ---
        assertNotNull(created);
        assertEquals("newpkg", created.getQualifiedName());
        assertSame("Same instance registered in the meta-model", created, mm.getPackage("newpkg"));
        assertTrue("The new package must be listed among the chosen folder's packages",
                folder.getPackages().contains(created));

        // --- File assertions: package-info.java is written immediately (not deferred) ---
        File packageInfoFile = new File(srcCopy, "newpkg" + File.separator + "package-info.java");
        assertTrue("package-info.java must exist immediately on disk", packageInfoFile.exists());
        String content = new String(Files.readAllBytes(packageInfoFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue("package-info.java must declare the new package", content.contains("package newpkg;"));

        // --- Reload: the package must be re-discovered like any other package (no root type needed) ---
        File pamelaFile2 = new File(workDir, "test2.pamela");
        writePamelaFile(pamelaFile2, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm2 = SourceMetaModelSerializer.load(pamelaFile2);
        assertNotNull("The package must survive a reload (package-info.java keeps it alive)",
                mm2.getPackage("newpkg"));
    }

    /**
     * Regression for a real bug report: {@code model2}'s registered source folder is a
     * deliberately non-conventional layout — the folder is literally named {@code model2}, but the
     * files inside declare {@code package test.model2;} (a two-segment ambient package with no
     * matching directory nesting). Naively computing
     * {@code folder.getDirectory() + qualifiedName.replace('.', separator)} for a new package named
     * with the <em>full</em> qualified name (as a user typing a Java package name would naturally
     * do, e.g. {@code "test.model2.sub"}) doubled the ambient prefix, landing at
     * {@code model2/test/model2/sub} instead of {@code model2/sub}. {@code createPackage} must
     * recognise {@code test.model2} as the folder's own already-known ancestor package and nest
     * only the remaining segment under the folder itself.
     */
    @Test
    public void testCreatePackageDoesNotDoubleTheAmbientFolderPrefix() throws IOException {
        // --- Setup ---
        File workDir = tmp.newFolder("testCreatePackageAmbient");
        File srcCopy = copyDir(MODEL2_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model2.FlexoProcess");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        assertEquals(1, mm.getSourceFolders().size());
        SourceFolder folder = mm.getSourceFolders().get(0);
        assertTrue("The folder's own ambient package must already be test.model2",
                folder.getPackages().stream().anyMatch(p -> "test.model2".equals(p.getQualifiedName())));

        // --- Mutate: type the FULL qualified name, matching the folder's own ambient package ---
        SourcePackage created = mm.createPackage("test.model2.sub", folder);

        // --- Assertions: must land directly under the folder, not double-nested ---
        assertEquals("test.model2.sub", created.getQualifiedName());
        File expectedDir = new File(srcCopy, "sub");
        File wronglyNestedDir = new File(srcCopy, "test" + File.separator + "model2" + File.separator + "sub");
        assertTrue("package-info.java must land directly under the source folder",
                new File(expectedDir, "package-info.java").exists());
        assertFalse("must NOT double the ambient 'test/model2' prefix", wronglyNestedDir.exists());
    }

    /**
     * Regression for a real bug report: creating a package via {@link SourceMetaModel#createPackage}
     * and then immediately creating an entity inside that brand-new, still-empty package (the exact
     * "New Package then New Entity in it" flow) failed with a {@code NullPointerException} ("Operation
     * failed: null" in the UI). Root cause: a {@link SourcePackage} only gets its Spoon
     * {@code CtPackage} wired up once it has at least one {@link SourceModelEntity} (see the
     * package-wiring loop in {@code buildProperties}) — a freshly created, still-empty package
     * (holding only a {@code package-info.java}) has {@code getCtPackage() == null}, and
     * {@code createEntity} unconditionally called {@code sourcePackage.getCtPackage().addType(...)}.
     * Fixed by resolving/creating the {@code CtPackage} via the Spoon factory
     * ({@code Package().getOrCreate(...)}) instead of assuming it is already set.
     */
    @Test
    public void testCreateEntityInFreshlyCreatedEmptyPackage() throws IOException {
        // --- Setup ---
        File workDir = tmp.newFolder("testCreateEntityInFreshPackage");
        File srcCopy = copyDir(MODEL2_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model2.FlexoProcess");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourceFolder folder = mm.getSourceFolders().get(0);

        // --- Mutate: create a brand-new, empty package, then immediately create an entity in it ---
        SourcePackage pkg = mm.createPackage("test.model2.foo", folder);
        assertTrue("Sanity: the freshly created package must still be empty", pkg.getEntities().isEmpty());
        assertNull("Sanity: an empty package has no Spoon CtPackage yet — this is the actual trigger",
                pkg.getCtPackage());
        SourceModelEntity entity = mm.createEntity("Foo", pkg, folder, false);
        mm.addRootTypeName(entity.getQualifiedName());

        // --- Assertions ---
        assertNotNull(entity);
        assertEquals("test.model2.foo.Foo", entity.getQualifiedName());
        assertEquals(pkg, entity.getSourcePackage());
        File entityFile = new File(srcCopy, "foo" + File.separator + "Foo.java");
        // File content is buffered (deferred save); flush to verify it lands on disk correctly.
        mm.flushAll();
        assertTrue("Foo.java must land directly under the package's own directory",
                entityFile.exists());
    }

    // =========================================================================
    // Test 6 — deleteEntity
    // =========================================================================

    /**
     * Delete {@code Foo2} from the meta-model.
     */
    @Test
    public void testDeleteEntity() throws IOException {
        // --- Setup ---
        File workDir = tmp.newFolder("testDelete");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        assertEquals(2, mm.getEntities().size());

        SourceModelEntity foo2 = mm.getEntity("test.model1.Foo2");
        assertNotNull(foo2);
        File foo2File = foo2.getCompilationUnit().getFile();

        // --- Mutate ---
        foo2.delete();
        mm.flushAll();

        // --- In-memory assertions ---
        assertNull("Foo2 must be gone from entity map", mm.getEntity("test.model1.Foo2"));
        assertEquals(1, mm.getEntities().size());

        // --- File assertion ---
        assertFalse("Foo2.java must be deleted from disk", foo2File.exists());
    }

    // =========================================================================
    // Test 7 — addSuperEntity
    // =========================================================================

    /**
     * Make {@code Foo2} extend {@code Foo1} (model1 has no inheritance initially).
     */
    @Test
    public void testAddSuperEntity() throws IOException {
        // --- Setup ---
        File workDir = tmp.newFolder("testAddSuper");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);

        SourceModelEntity foo1 = mm.getEntity("test.model1.Foo1");
        SourceModelEntity foo2 = mm.getEntity("test.model1.Foo2");
        assertNotNull(foo1);
        assertNotNull(foo2);
        assertTrue("Foo2 should initially have no super-entities",
                foo2.getDirectSuperEntities().isEmpty());

        // --- Mutate ---
        foo2.addSuperEntity(foo1);

        // --- In-memory assertions ---
        List<SourceModelEntity> supers = foo2.getDirectSuperEntities();
        assertEquals("Foo2 should have exactly 1 direct super-entity", 1, supers.size());
        assertEquals("test.model1.Foo1", supers.get(0).getQualifiedName());

        // Inherited properties should now be visible via getAllProperties
        Map<String, SourceModelProperty> all = foo2.getAllProperties();
        assertTrue("'foo2' property from Foo1 should be visible via getAllProperties()",
                all.containsKey("foo2"));

        // --- Reload ---
        mm.flushAll(); // deferred-save: persist buffers before reloading from disk
        File pamelaFile2 = new File(workDir, "test2.pamela");
        writePamelaFile(pamelaFile2, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm2 = SourceMetaModelSerializer.load(pamelaFile2);

        SourceModelEntity foo2r = mm2.getEntity("test.model1.Foo2");
        assertNotNull(foo2r);
        List<SourceModelEntity> supersr = foo2r.getDirectSuperEntities();
        assertEquals("After reload, Foo2 should still have 1 super-entity", 1, supersr.size());
        assertEquals("test.model1.Foo1", supersr.get(0).getQualifiedName());
    }

    // =========================================================================
    // Test 8 — removeSuperEntity
    // =========================================================================

    /**
     * Remove the {@code AbstractNode} super-entity from {@code ActivityNode}
     * (model2 — ActivityNode extends AbstractNode initially).
     */
    @Test
    public void testRemoveSuperEntity() throws IOException {
        // --- Setup: copy model2 ---
        File workDir = tmp.newFolder("testRemoveSuper");
        File srcCopy = copyDir(MODEL2_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy,
                "test.model2.ActivityNode",
                "test.model2.AbstractNode",
                "test.model2.FlexoProcess");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);

        SourceModelEntity activityNode = mm.getEntity("test.model2.ActivityNode");
        SourceModelEntity abstractNode = mm.getEntity("test.model2.AbstractNode");
        assertNotNull(activityNode);
        assertNotNull(abstractNode);

        List<SourceModelEntity> supsBefore = activityNode.getDirectSuperEntities();
        assertEquals("ActivityNode should initially have 1 super-entity", 1, supsBefore.size());
        assertEquals("test.model2.AbstractNode", supsBefore.get(0).getQualifiedName());

        // --- Mutate ---
        activityNode.removeSuperEntity(abstractNode);

        // --- In-memory assertions ---
        assertTrue("ActivityNode should have no super-entities after removal",
                activityNode.getDirectSuperEntities().isEmpty());

        // --- Reload ---
        mm.flushAll(); // deferred-save: persist buffers before reloading from disk
        File pamelaFile2 = new File(workDir, "test2.pamela");
        writePamelaFile(pamelaFile2, srcCopy,
                "test.model2.ActivityNode",
                "test.model2.AbstractNode",
                "test.model2.FlexoProcess");
        SourceMetaModel mm2 = SourceMetaModelSerializer.load(pamelaFile2);

        SourceModelEntity activityNoder = mm2.getEntity("test.model2.ActivityNode");
        assertNotNull(activityNoder);
        assertTrue("After reload, ActivityNode should have no super-entities",
                activityNoder.getDirectSuperEntities().isEmpty());
    }

    // =========================================================================
    // Test 9 — declareAsEntity
    // =========================================================================

    /**
     * Promote the plain interface {@code Foo3} (which carries a comment and no
     * {@code @ModelEntity}) into a PAMELA entity. Verifies:
     * - In-memory: it becomes a root type.
     * - On disk: {@code @ModelEntity} is inserted AND the original comment is
     *   preserved (Sniper minimal-diff printing).
     * - After rebuild and reload: {@code Foo3} is a discoverable entity.
     */
    @Test
    public void testDeclareAsEntity() throws IOException {
        // --- Setup ---
        File workDir = tmp.newFolder("testDeclare");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        assertEquals("Foo3 is not a root, so only Foo1+Foo2 are entities",
                2, mm.getEntities().size());

        // Locate Foo3 as a non-entity Java file
        SourcePackage pkg = mm.getPackage("test.model1");
        assertNotNull(pkg);
        SourceJavaFile foo3File = pkg.getNonEntityJavaFiles().stream()
                .filter(f -> f.getSimpleName().equals("Foo3"))
                .findFirst().orElse(null);
        assertNotNull("Foo3 must be present as a non-entity java file", foo3File);
        assertFalse("Foo3 must not be a model entity yet", foo3File.isPotentialModelEntity());

        // --- Mutate ---
        mm.declareAsEntity(foo3File);
        mm.flushAll();

        // --- In-memory assertions ---
        assertTrue("Foo3 must be registered as a root type",
                mm.getRootTypeNames().contains("test.model1.Foo3"));

        // --- File assertions (Sniper diff-minimal write) ---
        File foo3OnDisk = new File(srcCopy, "Foo3.java");
        String content = new String(Files.readAllBytes(foo3OnDisk.toPath()));
        assertTrue("@ModelEntity must be inserted", content.contains("@ModelEntity"));
        assertTrue("Import must be inserted",
                content.contains("import org.openflexo.pamela.annotations.ModelEntity;"));
        assertTrue("Original comment must be preserved (minimal-diff text edit)",
                content.contains("This is not a PAMELA entity"));

        // --- Rebuild: Foo3 should now materialise as an entity ---
        mm.rebuildMetaModel();
        assertNotNull("Foo3 must now be an entity", mm.getEntity("test.model1.Foo3"));
        assertEquals("Entity count must now be 3", 3, mm.getEntities().size());

        // --- Reload from disk ---
        mm.flushAll(); // deferred-save: persist buffers before reloading from disk
        File pamelaFile2 = new File(workDir, "test2.pamela");
        writePamelaFile(pamelaFile2, srcCopy, "test.model1.Foo1", "test.model1.Foo3");
        SourceMetaModel mm2 = SourceMetaModelSerializer.load(pamelaFile2);
        assertNotNull("Foo3 must be discoverable after reload", mm2.getEntity("test.model1.Foo3"));
        assertEquals("After reload, entity count must be 3", 3, mm2.getEntities().size());
    }

    // =========================================================================
    // Test 10 — promoteMethodToProperty (co-construction: promote a plain getter)
    // =========================================================================

    /**
     * A developer wrote a plain {@code getName()}/{@code setName(...)} pair in the
     * IDE; promoting {@code getName} adds {@code @Getter}/{@code @Setter} and makes
     * it a PAMELA property on the next rebuild.
     */
    @Test
    public void testPromoteMethodToProperty() throws IOException {
        // --- Setup ---
        File workDir = tmp.newFolder("testPromote");
        File srcCopy = copyDir(MODEL1_SRC, workDir);

        // Inject a plain, un-annotated getter/setter pair into Foo1.
        File foo1File = new File(srcCopy, "Foo1.java");
        String foo1 = new String(Files.readAllBytes(foo1File.toPath()));
        int lastBrace = foo1.lastIndexOf('}');
        String injected = "\n\tpublic String getName();\n\n"
                + "\tpublic void setName(String aName);\n\n";
        foo1 = foo1.substring(0, lastBrace) + injected + foo1.substring(lastBrace);
        Files.write(foo1File.toPath(), foo1.getBytes());

        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourceModelEntity entity = mm.getEntity("test.model1.Foo1");
        assertNotNull(entity);
        // Only "foo2" is a property so far; getName is plain Java.
        assertEquals(1, entity.getDeclaredProperties().size());
        assertFalse("getName must not be a property yet",
                entity.getDeclaredProperties().containsKey("name"));
        assertTrue("getName must be promotable",
                entity.getPromotableGetterNames().contains("getName"));

        // --- Mutate ---
        entity.promoteMethodToProperty("getName", "name", true);

        // --- File assertions ---
        mm.flushAll(); // deferred-save: persist the buffer before reading from disk
        String content = new String(Files.readAllBytes(foo1File.toPath()));
        // Foo1 is CONSTANT-style → promote references a (newly declared) constant, not a literal.
        assertTrue("@Getter(value = NAME) must be inserted",
                content.contains("@Getter(value = NAME)"));
        assertTrue("@Setter(value = NAME) must be inserted",
                content.contains("@Setter(value = NAME)"));
        assertTrue("NAME constant must be declared",
                content.contains("static final String NAME = \"name\""));
        assertTrue("original getFoo2 property must be preserved",
                content.contains("getFoo2"));

        // --- Rebuild: name should now materialise as a property ---
        mm.rebuildMetaModel();
        SourceModelEntity rebuilt = mm.getEntity("test.model1.Foo1");
        assertNotNull(rebuilt);
        assertTrue("name must now be a property",
                rebuilt.getDeclaredProperties().containsKey("name"));
        assertEquals(2, rebuilt.getDeclaredProperties().size());
        SourceModelProperty nameProp = rebuilt.getDeclaredProperties().get("name");
        assertEquals(Cardinality.SINGLE, nameProp.getCardinality());

        // --- Reload from disk ---
        mm.flushAll(); // deferred-save: persist buffers before reloading from disk
        File pamelaFile2 = new File(workDir, "test2.pamela");
        writePamelaFile(pamelaFile2, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm2 = SourceMetaModelSerializer.load(pamelaFile2);
        SourceModelEntity reloaded = mm2.getEntity("test.model1.Foo1");
        assertNotNull(reloaded);
        assertTrue("name property must survive reload",
                reloaded.getDeclaredProperties().containsKey("name"));
    }

    // =========================================================================
    // Test 11 — changeType
    // =========================================================================

    /** Retype Foo1.foo2 from {@code Foo2} to {@code String}. */
    @Test
    public void testChangePropertyType() throws IOException {
        File workDir = tmp.newFolder("testChangeType");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourceModelEntity foo1 = mm.getEntity("test.model1.Foo1");
        SourceModelProperty foo2Prop = foo1.getDeclaredProperties().get("foo2");
        assertNotNull(foo2Prop);
        assertEquals("Foo2", foo2Prop.getType().getSimpleName());

        // --- Mutate ---
        foo2Prop.changeType("java.lang.String");
        mm.flushAll();

        String content = new String(Files.readAllBytes(new File(srcCopy, "Foo1.java").toPath()));
        assertTrue("getter must now return String", content.contains("String getFoo2"));

        // --- Rebuild ---
        mm.rebuildMetaModel();
        SourceModelEntity rebuilt = mm.getEntity("test.model1.Foo1");
        assertEquals("String",
                rebuilt.getDeclaredProperties().get("foo2").getType().getSimpleName());
    }

    // =========================================================================
    // Test 12 — renameProperty
    // =========================================================================

    /** Rename Foo1.foo2 → bar: methods and @Getter/@Setter value follow. */
    @Test
    public void testRenameProperty() throws IOException {
        File workDir = tmp.newFolder("testRenameProp");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourceModelEntity foo1 = mm.getEntity("test.model1.Foo1");
        SourceModelProperty foo2Prop = foo1.getDeclaredProperties().get("foo2");
        assertNotNull(foo2Prop);

        // --- Mutate ---
        foo2Prop.rename("bar");
        mm.flushAll();

        String content = new String(Files.readAllBytes(new File(srcCopy, "Foo1.java").toPath()));
        assertTrue("getter renamed to getBar", content.contains("getBar"));
        assertTrue("setter renamed to setBar", content.contains("setBar"));
        assertTrue("@Getter value updated to \"bar\"", content.contains("\"bar\""));

        // --- Rebuild ---
        mm.rebuildMetaModel();
        SourceModelEntity rebuilt = mm.getEntity("test.model1.Foo1");
        assertTrue("bar must now be a property",
                rebuilt.getDeclaredProperties().containsKey("bar"));
        assertFalse("foo2 must be gone",
                rebuilt.getDeclaredProperties().containsKey("foo2"));

        // --- Reload ---
        mm.flushAll(); // deferred-save: persist buffers before reloading from disk
        File pamelaFile2 = new File(workDir, "test2.pamela");
        writePamelaFile(pamelaFile2, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm2 = SourceMetaModelSerializer.load(pamelaFile2);
        assertTrue("bar must survive reload",
                mm2.getEntity("test.model1.Foo1").getDeclaredProperties().containsKey("bar"));
    }

    // =========================================================================
    // Test 13 — renameProperty updates the inverse side (model2)
    // =========================================================================

    /**
     * Renaming {@code AbstractNode.outgoingEdges} must update the inverse
     * {@code @Getter(inverse = …)} on {@code Edge.startNode}.
     */
    @Test
    public void testRenamePropertyUpdatesInverse() throws IOException {
        File workDir = tmp.newFolder("testRenameInverse");
        File srcCopy = copyDir(MODEL2_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model2.FlexoProcess");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourceModelEntity node = mm.getEntity("test.model2.AbstractNode");
        assertNotNull(node);
        SourceModelProperty outgoing = node.getDeclaredProperties().get("outgoingEdges");
        assertNotNull(outgoing);
        assertNotNull("outgoingEdges must have a resolved inverse",
                outgoing.getInverseProperty());

        // --- Mutate ---
        outgoing.rename("outEdges");
        mm.flushAll();

        // --- File assertions: this side renamed, inverse side updated ---
        String nodeSrc = new String(Files.readAllBytes(new File(srcCopy, "AbstractNode.java").toPath()));
        assertTrue("accessor renamed", nodeSrc.contains("getOutEdges"));
        assertTrue("adder renamed", nodeSrc.contains("addToOutEdges"));
        assertTrue("key updated", nodeSrc.contains("\"outEdges\""));

        String edgeSrc = new String(Files.readAllBytes(new File(srcCopy, "Edge.java").toPath()));
        assertTrue("inverse on Edge.startNode must point to the new key",
                edgeSrc.contains("\"outEdges\""));

        // --- Rebuild: property renamed and inverse still resolves both ways ---
        mm.rebuildMetaModel();
        SourceModelEntity node2 = mm.getEntity("test.model2.AbstractNode");
        assertTrue("outEdges must now be a property",
                node2.getDeclaredProperties().containsKey("outEdges"));
        assertFalse("outgoingEdges must be gone",
                node2.getDeclaredProperties().containsKey("outgoingEdges"));
        SourceModelProperty outEdges = node2.getDeclaredProperties().get("outEdges");
        assertNotNull("inverse must still resolve after rename",
                outEdges.getInverseProperty());
    }

    // =========================================================================
    // Test 13b — setInverse clears + re-establishes the link on both sides
    // =========================================================================

    /**
     * {@code Edge.startNode} ↔ {@code AbstractNode.outgoingEdges} are an inverse pair. Clearing the
     * inverse must drop {@code @Getter(inverse=)} on both sides; re-setting it must restore both.
     */
    @Test
    public void testSetInverse() throws IOException {
        File workDir = tmp.newFolder("testSetInverse");
        File srcCopy = copyDir(MODEL2_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model2.FlexoProcess");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourceModelProperty startNode =
                mm.getEntity("test.model2.Edge").getDeclaredProperties().get("startNode");
        SourceModelProperty outgoing =
                mm.getEntity("test.model2.AbstractNode").getDeclaredProperties().get("outgoingEdges");
        assertNotNull(startNode);
        assertNotNull(outgoing);
        assertSame("startNode↔outgoingEdges paired initially", outgoing, startNode.getInverseProperty());

        // --- Clear the inverse (both sides) ---
        startNode.setInverse(null);
        mm.flushAll();
        mm.rebuildMetaModel();
        SourceModelProperty sn = mm.getEntity("test.model2.Edge").getDeclaredProperties().get("startNode");
        SourceModelProperty og = mm.getEntity("test.model2.AbstractNode").getDeclaredProperties().get("outgoingEdges");
        assertNull("startNode inverse cleared", sn.getInverseProperty());
        assertNull("outgoingEdges inverse cleared", og.getInverseProperty());

        // --- Re-establish the inverse (both sides) ---
        sn.setInverse(og);
        mm.flushAll();
        String edgeSrc = new String(Files.readAllBytes(new File(srcCopy, "Edge.java").toPath()));
        assertTrue("startNode getter must carry inverse = \"outgoingEdges\"",
                edgeSrc.contains("\"outgoingEdges\""));
        mm.rebuildMetaModel();
        SourceModelProperty sn2 = mm.getEntity("test.model2.Edge").getDeclaredProperties().get("startNode");
        SourceModelProperty og2 = mm.getEntity("test.model2.AbstractNode").getDeclaredProperties().get("outgoingEdges");
        assertSame("startNode→outgoingEdges restored", og2, sn2.getInverseProperty());
        assertSame("outgoingEdges→startNode restored", sn2, og2.getInverseProperty());
    }

    // =========================================================================
    // Test 14 — toggle @Embedded (composition)
    // =========================================================================

    /** Add then remove {@code @Embedded} on Foo1.foo2. */
    @Test
    public void testToggleEmbedded() throws IOException {
        File workDir = tmp.newFolder("testEmbedded");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        File foo1File = new File(srcCopy, "Foo1.java");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourceModelProperty foo2 = mm.getEntity("test.model1.Foo1")
                .getDeclaredProperties().get("foo2");
        assertFalse("not embedded initially", foo2.isEmbedded());

        // --- Add @Embedded ---
        foo2.setEmbedded(true);
        mm.flushAll();
        assertTrue("@Embedded must be inserted",
                new String(Files.readAllBytes(foo1File.toPath())).contains("@Embedded"));
        mm.rebuildMetaModel();
        assertTrue("property must now be embedded",
                mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("foo2").isEmbedded());

        // --- Remove @Embedded ---
        mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("foo2").setEmbedded(false);
        mm.flushAll();
        assertFalse("@Embedded must be removed",
                new String(Files.readAllBytes(foo1File.toPath())).contains("@Embedded"));
        mm.rebuildMetaModel();
        assertFalse("property must no longer be embedded",
                mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("foo2").isEmbedded());
    }

    // =========================================================================
    // Test 14b — setAbstract (editable inspector flag; fires PropertyChange)
    // =========================================================================

    @Test
    public void testSetAbstract() throws IOException {
        File workDir = tmp.newFolder("testAbstract");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        File foo1File = new File(srcCopy, "Foo1.java");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourceModelEntity foo1 = mm.getEntity("test.model1.Foo1");
        assertFalse(foo1.isAbstract());

        final boolean[] fired = { false };
        foo1.getPropertyChangeSupport().addPropertyChangeListener("abstract",
                evt -> fired[0] = true);

        // --- Make abstract ---
        foo1.setAbstract(true);
        mm.flushAll();
        assertTrue("PropertyChange must fire", fired[0]);
        assertTrue(foo1.isAbstract());
        assertTrue("@ModelEntity(isAbstract = true) in source",
                new String(Files.readAllBytes(foo1File.toPath())).contains("@ModelEntity(isAbstract = true)"));
        mm.rebuildMetaModel();
        assertTrue("abstract survives rebuild", mm.getEntity("test.model1.Foo1").isAbstract());

        // --- Make concrete again (parameter removed) ---
        mm.getEntity("test.model1.Foo1").setAbstract(false);
        mm.flushAll();
        String src = new String(Files.readAllBytes(foo1File.toPath()));
        assertFalse("isAbstract parameter removed", src.contains("isAbstract"));
        mm.rebuildMetaModel();
        assertFalse(mm.getEntity("test.model1.Foo1").isAbstract());
    }

    // =========================================================================
    // Test 14c — editable @Getter parameters (isDerived / defaultValue)
    // =========================================================================

    @Test
    public void testEditableGetterParameters() throws IOException {
        File workDir = tmp.newFolder("testGetterParams");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        File foo1File = new File(srcCopy, "Foo1.java");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourceModelProperty foo2 = mm.getEntity("test.model1.Foo1")
                .getDeclaredProperties().get("foo2");

        final boolean[] fired = { false };
        foo2.getPropertyChangeSupport().addPropertyChangeListener("derived", e -> fired[0] = true);

        // --- isDerived ---
        foo2.setDerived(true);
        mm.flushAll();
        assertTrue(fired[0]);
        assertTrue(foo2.isDerived());
        assertTrue(new String(Files.readAllBytes(foo1File.toPath())).contains("isDerived = true"));

        // --- defaultValue ---
        foo2.setDefaultValue("hello");
        mm.flushAll();
        assertTrue(new String(Files.readAllBytes(foo1File.toPath())).contains("defaultValue = \"hello\""));

        // --- Rebuild reflects both ---
        mm.rebuildMetaModel();
        SourceModelProperty rebuilt = mm.getEntity("test.model1.Foo1")
                .getDeclaredProperties().get("foo2");
        assertTrue("derived survives rebuild", rebuilt.isDerived());
        assertEquals("hello", rebuilt.getDefaultValue());

        // --- Clearing defaultValue removes the parameter ---
        rebuilt.setDefaultValue("");
        mm.flushAll();
        assertFalse(new String(Files.readAllBytes(foo1File.toPath())).contains("defaultValue"));
    }

    // =========================================================================
    // Test 15 — add / remove setter (SINGLE accessor toggle)
    // =========================================================================

    @Test
    public void testAddRemoveSetter() throws IOException {
        File workDir = tmp.newFolder("testSetter");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        File foo1File = new File(srcCopy, "Foo1.java");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        // Start from a SINGLE property that has a setter.
        mm.getEntity("test.model1.Foo1").addSingleProperty("label", "java.lang.String");
        mm.flushAll();
        mm.rebuildMetaModel();
        assertNotNull("setter present after creation", mm.getEntity("test.model1.Foo1")
                .getDeclaredProperties().get("label").getSetterMethodName());

        // --- Remove the setter ---
        mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("label").removeSetter();
        mm.flushAll();
        assertFalse("setLabel removed from source",
                new String(Files.readAllBytes(foo1File.toPath())).contains("setLabel"));
        mm.rebuildMetaModel();
        assertNull("no setter after removal", mm.getEntity("test.model1.Foo1")
                .getDeclaredProperties().get("label").getSetterMethodName());

        // --- Add it back ---
        mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("label").addSetter();
        mm.flushAll();
        assertTrue("setLabel re-added to source",
                new String(Files.readAllBytes(foo1File.toPath())).contains("setLabel"));
        mm.rebuildMetaModel();
        assertNotNull("setter present again", mm.getEntity("test.model1.Foo1")
                .getDeclaredProperties().get("label").getSetterMethodName());
    }

    // =========================================================================
    // Test 16 — add / remove adder+remover (LIST accessor toggle)
    // =========================================================================

    @Test
    public void testAddRemoveAdderRemover() throws IOException {
        File workDir = tmp.newFolder("testAdder");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        File foo1File = new File(srcCopy, "Foo1.java");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        mm.getEntity("test.model1.Foo1").addListProperty("items", "java.lang.String");
        mm.flushAll();
        mm.rebuildMetaModel();
        assertNotNull("adder present after creation", mm.getEntity("test.model1.Foo1")
                .getDeclaredProperties().get("items").getAdderMethodName());

        // --- Remove adder + remover ---
        mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("items").removeAdderRemover();
        mm.flushAll();
        String afterRemove = new String(Files.readAllBytes(foo1File.toPath()));
        assertFalse("addToItems removed", afterRemove.contains("addToItems"));
        assertFalse("removeFromItems removed", afterRemove.contains("removeFromItems"));
        mm.rebuildMetaModel();
        assertNull("no adder after removal", mm.getEntity("test.model1.Foo1")
                .getDeclaredProperties().get("items").getAdderMethodName());

        // --- Add them back ---
        mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("items").addAdderRemover();
        mm.flushAll();
        String afterAdd = new String(Files.readAllBytes(foo1File.toPath()));
        assertTrue("addToItems re-added", afterAdd.contains("addToItems"));
        assertTrue("removeFromItems re-added", afterAdd.contains("removeFromItems"));
        mm.rebuildMetaModel();
        assertNotNull("adder present again", mm.getEntity("test.model1.Foo1")
                .getDeclaredProperties().get("items").getAdderMethodName());
        assertNotNull("remover present again", mm.getEntity("test.model1.Foo1")
                .getDeclaredProperties().get("items").getRemoverMethodName());
    }

    // =========================================================================
    // Test 17 — inspector checkbox: uncheck detaches the annotation only (keeps
    // the method), re-check smart-reattaches the conventional method
    // =========================================================================

    @Test
    public void testSetterPresentToggleDetachesAnnotationOnly() throws IOException {
        File workDir = tmp.newFolder("testSetterToggle");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        File foo1File = new File(srcCopy, "Foo1.java");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        mm.getEntity("test.model1.Foo1").addSingleProperty("label", "java.lang.String");
        mm.flushAll();
        mm.rebuildMetaModel();

        // --- Uncheck: the @Setter annotation goes, the setLabel method stays ---
        mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("label").setSetterPresent(false);
        mm.flushAll();
        String afterUncheck = new String(Files.readAllBytes(foo1File.toPath()));
        assertTrue("setLabel method kept (detach is annotation-only)", afterUncheck.contains("setLabel"));
        // The label setter lost its @Setter, but only it — the setLabel declaration is now plain.
        assertTrue("setLabel is now un-annotated",
                afterUncheck.matches("(?s).*\\n\\s*public void setLabel\\(.*"));
        mm.rebuildMetaModel();
        assertNull("no setter role after uncheck", mm.getEntity("test.model1.Foo1")
                .getDeclaredProperties().get("label").getSetterMethodName());

        // --- Re-check: smart-reattach the conventional setLabel(String) ---
        mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("label").setSetterPresent(true);
        mm.flushAll();
        assertTrue("@Setter re-attached", new String(Files.readAllBytes(foo1File.toPath())).contains("@Setter"));
        mm.rebuildMetaModel();
        assertEquals("setter is the conventional method again", "setLabel",
                mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("label").getSetterMethodName());
    }

    // =========================================================================
    // Test 18 — inspector selector: retarget moves the annotation to the chosen
    // (signature-compatible) method
    // =========================================================================

    @Test
    public void testRetargetSetterMovesAnnotation() throws IOException {
        File workDir = tmp.newFolder("testRetargetSetter");
        File srcCopy = copyDir(MODEL1_SRC, workDir);

        // Inject a second, plain, signature-compatible method into Foo1.
        File foo1File = new File(srcCopy, "Foo1.java");
        String foo1 = new String(Files.readAllBytes(foo1File.toPath()));
        int lastBrace = foo1.lastIndexOf('}');
        foo1 = foo1.substring(0, lastBrace)
                + "\n\tpublic void assignLabel(String value);\n\n"
                + foo1.substring(lastBrace);
        Files.write(foo1File.toPath(), foo1.getBytes());

        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        mm.getEntity("test.model1.Foo1").addSingleProperty("label", "java.lang.String");
        mm.flushAll();
        mm.rebuildMetaModel();

        SourceModelProperty label = mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("label");
        assertEquals("starts on the conventional setter", "setLabel", label.getSetterMethodName());

        // The candidate list offers both setLabel and assignLabel.
        spoon.reflect.declaration.CtMethod<?> assign = null;
        for (spoon.reflect.declaration.CtMethod<?> m : label.getSetterCandidates()) {
            if ("assignLabel".equals(m.getSimpleName())) {
                assign = m;
            }
        }
        assertNotNull("assignLabel is a signature-compatible candidate", assign);

        // --- Retarget: move @Setter from setLabel to assignLabel ---
        label.setSetterMethod(assign);
        mm.flushAll();
        mm.rebuildMetaModel();
        assertEquals("setter retargeted to assignLabel", "assignLabel",
                mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("label").getSetterMethodName());
    }

    // =========================================================================
    // Test 19 — inspector super-entity table: unlink (RemoveAction) + add-intent
    // =========================================================================

    @Test
    public void testInspectorUnlinkSuperEntity() throws IOException {
        File workDir = tmp.newFolder("testInspectorSuper");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);

        // Set up an inheritance link Foo2 -> Foo1.
        mm.getEntity("test.model1.Foo2").addSuperEntity(mm.getEntity("test.model1.Foo1"));
        mm.flushAll();
        mm.rebuildMetaModel();
        SourceModelEntity foo2 = mm.getEntity("test.model1.Foo2");
        assertEquals(1, foo2.getDirectSuperEntities().size());

        // --- Remove via the inspector path (table RemoveAction → unlinkSuperEntity) ---
        foo2.unlinkSuperEntity(foo2.getDirectSuperEntities().get(0));
        mm.flushAll();
        mm.rebuildMetaModel();
        assertTrue("super-entity removed",
                mm.getEntity("test.model1.Foo2").getDirectSuperEntities().isEmpty());

        // --- Add ("+" footer) is a UI intent: requestAddSuperEntity() fires the signal ---
        final boolean[] fired = { false };
        SourceModelEntity foo2b = mm.getEntity("test.model1.Foo2");
        foo2b.getPropertyChangeSupport().addPropertyChangeListener("addSuperEntityRequested",
                evt -> fired[0] = true);
        foo2b.requestAddSuperEntity();
        assertTrue("requestAddSuperEntity fires the UI-intent signal", fired[0]);
    }

    // =========================================================================
    // Test 20 — createProperty: generate the getter, attach an existing setter
    // =========================================================================

    @Test
    public void testCreatePropertyGenerateAndAttach() throws IOException {
        File workDir = tmp.newFolder("testCreateProperty");
        File srcCopy = copyDir(MODEL1_SRC, workDir);

        // Inject a plain, signature-compatible setter into Foo1 (no @Setter yet).
        File foo1File = new File(srcCopy, "Foo1.java");
        String foo1 = new String(Files.readAllBytes(foo1File.toPath()));
        int lastBrace = foo1.lastIndexOf('}');
        foo1 = foo1.substring(0, lastBrace)
                + "\n\tpublic void assignLabel(String value);\n\n"
                + foo1.substring(lastBrace);
        Files.write(foo1File.toPath(), foo1.getBytes());

        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);

        // Generate getLabel(), attach the existing assignLabel() as the @Setter.
        mm.getEntity("test.model1.Foo1").createProperty("label", "java.lang.String", false,
                java.util.Arrays.asList(
                        AccessorSpec.generate(AccessorSpec.Role.GETTER, "getLabel"),
                        AccessorSpec.attach(AccessorSpec.Role.SETTER, "assignLabel")));
        mm.flushAll();
        String src = new String(Files.readAllBytes(foo1File.toPath()));
        assertTrue("generated getter present", src.contains("getLabel"));
        assertTrue("existing setter kept", src.contains("assignLabel"));

        mm.rebuildMetaModel();
        SourceModelProperty label = mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("label");
        assertNotNull("property created", label);
        assertEquals("getter is the generated method", "getLabel", label.getGetterMethodName());
        assertEquals("setter is the attached existing method", "assignLabel", label.getSetterMethodName());
    }

    // =========================================================================
    // Method-level promote (signature-based) — model-editing-design.md §3.3
    // =========================================================================

    /** Promote a plain {@code List<T>} getter into a LIST property (cardinality from the return type). */
    @Test
    public void testPromoteGetterToListProperty() throws IOException {
        File workDir = tmp.newFolder("testPromoteList");
        File srcCopy = copyDir(MODEL1_SRC, workDir);

        // Inject a plain, un-annotated List getter into Foo1.
        File foo1File = new File(srcCopy, "Foo1.java");
        String foo1 = new String(Files.readAllBytes(foo1File.toPath()));
        int lastBrace = foo1.lastIndexOf('}');
        foo1 = foo1.substring(0, lastBrace)
                + "\n\tpublic java.util.List<String> getTags();\n\n"
                + foo1.substring(lastBrace);
        Files.write(foo1File.toPath(), foo1.getBytes());

        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourceModelEntity entity = mm.getEntity("test.model1.Foo1");
        assertNotNull(entity);

        // --- Mutate ---
        entity.promoteGetterToProperty("getTags", "tags", true);

        // --- File assertions ---
        mm.flushAll(); // deferred-save: persist the buffer before reading from disk
        String content = new String(Files.readAllBytes(foo1File.toPath()));
        // Foo1 is CONSTANT-style → the LIST getter references a newly declared constant.
        assertTrue("LIST @Getter must be inserted",
                content.contains("@Getter(value = TAGS, cardinality = Getter.Cardinality.LIST)"));
        assertTrue("TAGS constant must be declared",
                content.contains("static final String TAGS = \"tags\""));

        // --- Rebuild ---
        mm.rebuildMetaModel();
        SourceModelEntity rebuilt = mm.getEntity("test.model1.Foo1");
        SourceModelProperty tags = rebuilt.getDeclaredProperties().get("tags");
        assertNotNull("tags must materialise as a property", tags);
        assertEquals(Cardinality.LIST, tags.getCardinality());
        assertEquals("java.lang.String", tags.getType().getQualifiedName());
    }

    /** Attach an existing plain {@code setXxx}-shaped method as the {@code @Setter} of a property. */
    @Test
    public void testAttachSetter() throws IOException {
        File workDir = tmp.newFolder("testAttachSetter");
        File srcCopy = copyDir(MODEL1_SRC, workDir);

        // Inject a property getter (no setter) + a plain, differently-named one-arg method.
        File foo1File = new File(srcCopy, "Foo1.java");
        String foo1 = new String(Files.readAllBytes(foo1File.toPath()));
        int lastBrace = foo1.lastIndexOf('}');
        foo1 = foo1.substring(0, lastBrace)
                + "\n\t@org.openflexo.pamela.annotations.Getter(value = \"label\")\n"
                + "\tpublic String getLabel();\n\n"
                + "\tpublic void assignLabel(String aLabel);\n\n"
                + foo1.substring(lastBrace);
        Files.write(foo1File.toPath(), foo1.getBytes());

        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourceModelEntity entity = mm.getEntity("test.model1.Foo1");
        SourceModelProperty label = entity.getDeclaredProperties().get("label");
        assertNotNull("label property must exist", label);
        assertNull("label has no setter yet", label.getSetterMethodName());

        // --- Mutate: attach the existing method as the setter ---
        label.attachSetter("assignLabel");

        // --- File assertion ---
        mm.flushAll(); // deferred-save: persist the buffer before reading from disk
        String content = new String(Files.readAllBytes(foo1File.toPath()));
        assertTrue("@Setter(value = \"label\") must be inserted on assignLabel",
                content.contains("@Setter(value = \"label\")"));

        // --- Rebuild: assignLabel must now be the property's setter ---
        mm.rebuildMetaModel();
        SourceModelProperty rebuilt = mm.getEntity("test.model1.Foo1")
                .getDeclaredProperties().get("label");
        assertEquals("assignLabel", rebuilt.getSetterMethodName());
    }

    /** Attach an existing plain one-arg method as the {@code @Adder} of a LIST property. */
    @Test
    public void testAttachAdder() throws IOException {
        File workDir = tmp.newFolder("testAttachAdder");
        File srcCopy = copyDir(MODEL1_SRC, workDir);

        File foo1File = new File(srcCopy, "Foo1.java");
        String foo1 = new String(Files.readAllBytes(foo1File.toPath()));
        int lastBrace = foo1.lastIndexOf('}');
        foo1 = foo1.substring(0, lastBrace)
                + "\n\t@org.openflexo.pamela.annotations.Getter(value = \"tags\","
                + " cardinality = org.openflexo.pamela.annotations.Getter.Cardinality.LIST)\n"
                + "\tpublic java.util.List<String> getTags();\n\n"
                + "\tpublic void include(String aTag);\n\n"
                + foo1.substring(lastBrace);
        Files.write(foo1File.toPath(), foo1.getBytes());

        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourceModelProperty tags = mm.getEntity("test.model1.Foo1")
                .getDeclaredProperties().get("tags");
        assertNotNull("tags LIST property must exist", tags);
        assertEquals(Cardinality.LIST, tags.getCardinality());
        assertNull("tags has no adder yet", tags.getAdderMethodName());

        // --- Mutate ---
        tags.attachAdder("include");

        mm.flushAll(); // deferred-save: persist the buffer before reading from disk
        String content = new String(Files.readAllBytes(foo1File.toPath()));
        assertTrue("@Adder(value = \"tags\") must be inserted",
                content.contains("@Adder(value = \"tags\")"));

        mm.rebuildMetaModel();
        SourceModelProperty rebuilt = mm.getEntity("test.model1.Foo1")
                .getDeclaredProperties().get("tags");
        assertEquals("include", rebuilt.getAdderMethodName());
    }

    /**
     * Promote a getter AND pull in an existing sibling setter in one text edit
     * (model-editing-design.md §3.3a — the {@code extraAccessors} overload).
     */
    @Test
    public void testPromoteGetterWithSetter() throws IOException {
        File workDir = tmp.newFolder("testPromoteWithSetter");
        File srcCopy = copyDir(MODEL1_SRC, workDir);

        // Plain getter + matching plain setter, both un-annotated.
        File foo1File = new File(srcCopy, "Foo1.java");
        String foo1 = new String(Files.readAllBytes(foo1File.toPath()));
        int lastBrace = foo1.lastIndexOf('}');
        foo1 = foo1.substring(0, lastBrace)
                + "\n\tpublic String getDescription();\n\n"
                + "\tpublic void setDescription(String aDescription);\n\n"
                + foo1.substring(lastBrace);
        Files.write(foo1File.toPath(), foo1.getBytes());

        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourceModelEntity entity = mm.getEntity("test.model1.Foo1");

        // --- Mutate: promote getter + attach the sibling setter in one pass ---
        java.util.Map<Class<? extends java.lang.annotation.Annotation>, String> extras =
                new java.util.LinkedHashMap<>();
        extras.put(org.openflexo.pamela.annotations.Setter.class, "setDescription");
        entity.promoteGetterToProperty("getDescription", "description", false, extras);

        mm.flushAll(); // deferred-save: persist the buffer before reading from disk
        String content = new String(Files.readAllBytes(foo1File.toPath()));
        // Foo1 is CONSTANT-style → getter + pulled-in setter share a newly declared constant.
        assertTrue("@Getter inserted", content.contains("@Getter(value = DESCRIPTION)"));
        assertTrue("@Setter inserted on the sibling", content.contains("@Setter(value = DESCRIPTION)"));
        assertTrue("DESCRIPTION constant must be declared once",
                content.contains("static final String DESCRIPTION = \"description\""));
        assertEquals("exactly one DESCRIPTION constant", content.indexOf("String DESCRIPTION"),
                content.lastIndexOf("String DESCRIPTION"));

        mm.rebuildMetaModel();
        SourceModelProperty desc = mm.getEntity("test.model1.Foo1")
                .getDeclaredProperties().get("description");
        assertNotNull("description property must materialise", desc);
        assertEquals(Cardinality.SINGLE, desc.getCardinality());
        assertEquals("getDescription", desc.getGetterMethodName());
        assertEquals("setDescription", desc.getSetterMethodName());
    }

    // =========================================================================
    // Lot 6b — declare operation / initializer / finder
    // =========================================================================

    /** Declare a plain method as an {@code @Operation} marker. */
    @Test
    public void testDeclareOperation() throws IOException {
        File workDir = tmp.newFolder("testDeclareOperation");
        File srcCopy = copyDir(MODEL1_SRC, workDir);

        File foo1File = new File(srcCopy, "Foo1.java");
        String foo1 = new String(Files.readAllBytes(foo1File.toPath()));
        int lastBrace = foo1.lastIndexOf('}');
        foo1 = foo1.substring(0, lastBrace)
                + "\n\tpublic void doSomething();\n\n"
                + foo1.substring(lastBrace);
        Files.write(foo1File.toPath(), foo1.getBytes());

        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);

        mm.getEntity("test.model1.Foo1").declareOperation("doSomething", 0);

        mm.flushAll(); // deferred-save: persist the buffer before reading from disk
        String content = new String(Files.readAllBytes(foo1File.toPath()));
        assertTrue("@Operation inserted", content.contains("@Operation"));
        assertTrue("import added",
                content.contains("import org.openflexo.pamela.annotations.Operation;"));
        mm.rebuildMetaModel();
        assertNotNull(mm.getEntity("test.model1.Foo1"));
    }

    /** Declare a method as an {@code @Initializer} with {@code @Parameter} on each argument. */
    @Test
    public void testDeclareInitializer() throws IOException {
        File workDir = tmp.newFolder("testDeclareInitializer");
        File srcCopy = copyDir(MODEL1_SRC, workDir);

        // An existing property + a plain factory method taking a value of that property's type.
        File foo1File = new File(srcCopy, "Foo1.java");
        String foo1 = new String(Files.readAllBytes(foo1File.toPath()));
        int lastBrace = foo1.lastIndexOf('}');
        foo1 = foo1.substring(0, lastBrace)
                + "\n\t@org.openflexo.pamela.annotations.Getter(value = \"label\")\n"
                + "\tpublic String getLabel();\n\n"
                + "\tpublic Foo1 build(String label);\n\n"
                + foo1.substring(lastBrace);
        Files.write(foo1File.toPath(), foo1.getBytes());

        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);

        mm.getEntity("test.model1.Foo1")
                .declareInitializer("build", java.util.Arrays.asList("label"));

        mm.flushAll(); // deferred-save: persist the buffer before reading from disk
        String content = new String(Files.readAllBytes(foo1File.toPath()));
        assertTrue("@Initializer inserted", content.contains("@Initializer"));
        assertTrue("@Parameter inserted inline on the argument",
                content.contains("@Parameter(\"label\") String label"));

        mm.rebuildMetaModel();
        SourceModelEntity rebuilt = mm.getEntity("test.model1.Foo1");
        boolean found = rebuilt.getInitializers().stream()
                .anyMatch(i -> "build".equals(i.getMethodName()));
        assertTrue("build must be recognised as an initializer after rebuild", found);
    }

    /** Declare a plain one-arg method as an {@code @Finder} over a LIST property. */
    @Test
    public void testDeclareFinder() throws IOException {
        File workDir = tmp.newFolder("testDeclareFinder");
        File srcCopy = copyDir(MODEL2_SRC, workDir);

        // Inject a plain finder-shaped method into FlexoProcess (nodes : List<AbstractNode>).
        File procFile = new File(srcCopy, "FlexoProcess.java");
        String proc = new String(Files.readAllBytes(procFile.toPath()));
        int lastBrace = proc.lastIndexOf('}');
        proc = proc.substring(0, lastBrace)
                + "\n\tAbstractNode findNode(String key);\n\n"
                + proc.substring(lastBrace);
        Files.write(procFile.toPath(), proc.getBytes());

        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy,
                "test.model2.FlexoProcess", "test.model2.AbstractNode");
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);

        mm.getEntity("test.model2.FlexoProcess")
                .declareFinder("findNode", "nodes", "name", false);
        mm.flushAll();

        String content = new String(Files.readAllBytes(procFile.toPath()));
        assertTrue("@Finder inserted",
                content.contains("@Finder(collection = \"nodes\", attribute = \"name\")"));
        assertTrue("import added",
                content.contains("import org.openflexo.pamela.annotations.Finder;"));
        mm.rebuildMetaModel();
        assertNotNull(mm.getEntity("test.model2.FlexoProcess"));
    }

    // =========================================================================
    // Deferred-save contract — editable-source-dirty-buffer-design.md
    // =========================================================================

    /**
     * A mutation marks the model dirty and is reflected in-memory immediately, but the
     * {@code .java} file on disk is NOT written until {@link SourceMetaModel#flushAll()}.
     */
    @Test
    public void testDeferredMutationIsDirtyWithoutDiskWrite() throws IOException {
        File workDir = tmp.newFolder("testDeferredDirty");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        assertFalse("model must be clean after load", mm.isDirty());

        File foo2File = new File(srcCopy, "Foo2.java");
        byte[] before = Files.readAllBytes(foo2File.toPath());

        // --- Deferred mutation: no disk write ---
        SourceModelEntity foo2 = mm.getEntity("test.model1.Foo2");
        foo2.addSingleProperty("nickname", "java.lang.String");

        assertTrue("model must be dirty after a mutation", mm.isDirty());
        assertTrue("the new property is visible in-memory immediately",
                foo2.getDeclaredProperties().containsKey("nickname"));
        assertTrue("the .java file on disk must be untouched until flush",
                java.util.Arrays.equals(before, Files.readAllBytes(foo2File.toPath())));

        // --- Flush: now the disk is written ---
        mm.flushAll();
        assertFalse("model must be clean after flush", mm.isDirty());
        String after = new String(Files.readAllBytes(foo2File.toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue("flushed source must contain the new getter", after.contains("getNickname"));
    }

    /**
     * A rebuild after a deferred mutation parses the in-memory buffer (Spoon
     * {@code VirtualFile}, parse-from-buffer), so the change materialises in the model
     * without ever writing the file.
     */
    @Test
    public void testRebuildFromBufferMaterializesWithoutFlush() throws IOException {
        File workDir = tmp.newFolder("testRebuildFromBuffer");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        File foo1File = new File(srcCopy, "Foo1.java");
        byte[] before = Files.readAllBytes(foo1File.toPath());

        // --- Deferred mutation, then rebuild WITHOUT flushing ---
        mm.getEntity("test.model1.Foo1").addListProperty("aliases", "java.lang.String");
        assertTrue(mm.isDirty());
        mm.rebuildMetaModel(); // must parse the in-memory buffer, not the disk file

        SourceModelEntity foo1 = mm.getEntity("test.model1.Foo1");
        assertNotNull(foo1);
        assertTrue("the deferred property must materialise via parse-from-buffer",
                foo1.getDeclaredProperties().containsKey("aliases"));
        assertEquals(Cardinality.LIST,
                foo1.getDeclaredProperties().get("aliases").getCardinality());
        assertTrue("the model is still dirty (never flushed)", mm.isDirty());
        assertTrue("disk must still be unchanged after a buffer-only rebuild",
                java.util.Arrays.equals(before, Files.readAllBytes(foo1File.toPath())));
    }

    /**
     * Renaming an entity is deferred: the file move (write new + delete old) happens
     * only on flush; the in-memory model is renamed immediately.
     */
    @Test
    public void testDeferredRenameFileMove() throws IOException {
        File workDir = tmp.newFolder("testDeferredRename");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        File foo1File = new File(srcCopy, "Foo1.java");
        File bar1File = new File(srcCopy, "Bar1.java");

        // --- Deferred rename: file move not applied yet ---
        mm.getEntity("test.model1.Foo1").rename("Bar1");
        assertTrue("model must be dirty after rename", mm.isDirty());
        assertEquals("Bar1", mm.getEntity("test.model1.Bar1").getSimpleName());
        assertTrue("old file still on disk until flush", foo1File.exists());
        assertFalse("new file not on disk until flush", bar1File.exists());

        // --- Flush: applies the file move ---
        mm.flushAll();
        assertTrue("Bar1.java written on flush", bar1File.exists());
        assertFalse("Foo1.java removed on flush", foo1File.exists());
        assertFalse("clean after flush", mm.isDirty());
    }

    /**
     * Repro of the interactive scenario: open Edge.java, type {@code @Getter("prout")} before
     * the plain {@code int getNewProperty()} method, then rebuild. The model must keep Edge and
     * surface the new property (no exception, entity not dropped).
     */
    @Test
    public void testEdgeTextEditAddsProperty() throws IOException {
        File workDir = tmp.newFolder("testEdgeTextEdit");
        File srcCopy = copyDir(MODEL2_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model2.FlexoProcess");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourceModelEntity edge = mm.getEntity("test.model2.Edge");
        assertNotNull("Edge must be reachable", edge);
        SourceCompilationUnit cu = edge.getCompilationUnit();

        // Simulate the editable-view keystroke: buffer edit, no disk write.
        String edited = cu.getText().replace("public int getNewProperty();",
                "@Getter(\"prout\")\n\tpublic int getNewProperty();");
        assertTrue("the marker method must exist in Edge", edited.contains("@Getter(\"prout\")"));
        cu.setText(edited);

        // Focus-loss path: rebuild from the buffer (no flush).
        mm.rebuildMetaModel();
        SourceModelEntity edge2 = mm.getEntity("test.model2.Edge");
        assertNotNull("Edge must survive the buffer rebuild", edge2);
        assertTrue("the new property must appear",
                edge2.getDeclaredProperties().containsKey("prout"));

        // Save path: flush then rebuild from disk.
        mm.flushAll();
        mm.rebuildMetaModel();
        SourceModelEntity edge3 = mm.getEntity("test.model2.Edge");
        assertNotNull("Edge must survive the save+rebuild", edge3);
        assertTrue("the new property must persist",
                edge3.getDeclaredProperties().containsKey("prout"));
    }

    /**
     * Repro of NewEntityAction's UI flow — createEntity + addRootTypeName, then rebuild
     * WITHOUT flushing (exactly what {@code SourceEditingAction.doPerform} does). The new
     * entity must materialise in the model (already covered by {@link #testCreateEntity}
     * via flush) AND the meta-model must fire {@code "packages"} on every stable
     * {@link SourceFolder}, since a browser node already expanded on {@code folder.packages}
     * listens on the folder itself (not on the meta-model) and would otherwise keep
     * displaying the stale, pre-rebuild {@link SourcePackage} — see
     * {@code gina-analysis.md §18.5} for the analogous bug this mirrors (previously fixed
     * for {@code PamelaProject.diagrams}).
     */
    @Test
    public void testCreateEntityThenRebuildWithoutFlushNotifiesSourceFolder() throws IOException {
        File workDir = tmp.newFolder("testCreateEntityNoFlush");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourcePackage pkg = mm.getPackage("test.model1");
        assertNotNull(pkg);
        SourceFolder folder = mm.getSourceFolders().get(0);
        assertNotNull(folder);

        final java.util.concurrent.atomic.AtomicBoolean folderNotified =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        folder.getPropertyChangeSupport().addPropertyChangeListener("packages",
                evt -> folderNotified.set(true));

        SourceModelEntity foo3 = mm.createEntity("Foo3", pkg);
        mm.addRootTypeName("test.model1.Foo3");
        assertNotNull("immediately after createEntity, in-memory entity exists", foo3);
        assertTrue("model must be dirty (no flush yet)", mm.isDirty());

        // Rebuild WITHOUT flushing, exactly like SourceEditingAction.doPerform -> rebuildProject
        mm.rebuildMetaModel();

        assertNotNull("Foo3 must survive the buffer-only rebuild triggered right after creation",
                mm.getEntity("test.model1.Foo3"));
        assertTrue("SourceFolder must fire \"packages\" so an already-expanded browser node "
                + "re-fetches the fresh (post-rebuild) SourcePackage instances", folderNotified.get());

        // The (new, post-rebuild) SourcePackage instance itself must expose the new entity.
        SourcePackage pkgAfter = mm.getPackage("test.model1");
        assertNotNull(pkgAfter);
        assertTrue("the rebuilt package must list the new entity",
                pkgAfter.getEntities().contains(mm.getEntity("test.model1.Foo3")));
    }

    /**
     * {@link SourcePackage#addEntity} must notify listeners directly (not only via a full
     * meta-model rebuild), so a live view bound to {@code pkg.members}/{@code pkg.entities}
     * on an existing, still-referenced {@code SourcePackage} instance refreshes too.
     */
    @Test
    public void testSourcePackageAddEntityFiresMembersChange() throws IOException {
        File workDir = tmp.newFolder("testPackageAddEntityNotifies");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourcePackage pkg = mm.getPackage("test.model1");
        assertNotNull(pkg);

        final java.util.concurrent.atomic.AtomicBoolean membersNotified =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        pkg.getPropertyChangeSupport().addPropertyChangeListener("members",
                evt -> membersNotified.set(true));

        mm.createEntity("Foo3", pkg);

        assertTrue("SourcePackage.addEntity must fire \"members\"", membersNotified.get());
    }

    /**
     * After a buffer-only rebuild, a dirty unit's compilation unit must still report its
     * REAL, existing {@code .java} file (a {@code VirtualFile}-parsed unit reports a bogus
     * virtual path otherwise) — so the source view shows it and a later flush writes the
     * right file. Regression for "// File not found" after promoting a method.
     */
    @Test
    public void testCompilationUnitFileSurvivesBufferRebuild() throws IOException {
        File workDir = tmp.newFolder("testCuFileBuffer");
        File srcCopy = copyDir(MODEL2_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model2.FlexoProcess");

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        SourceModelEntity edge = mm.getEntity("test.model2.Edge");
        assertNotNull("Edge must be reachable in model2", edge);
        File edgeFileBefore = edge.getCompilationUnit().getFile();
        assertNotNull(edgeFileBefore);
        assertTrue("Edge.java must exist before the edit", edgeFileBefore.exists());

        // Deferred mutation on Edge, then rebuild-from-buffer (no flush).
        edge.addSingleProperty("note", "java.lang.String");
        assertTrue(mm.isDirty());
        mm.rebuildMetaModel();

        SourceCompilationUnit cu = mm.getEntity("test.model2.Edge").getCompilationUnit();
        assertNotNull("Edge CU must exist after the buffer rebuild", cu);
        File f = cu.getFile();
        assertNotNull("CU file must be recovered, not the virtual path", f);
        assertTrue("CU file must be the real, existing .java file", f.exists());
        assertEquals("CU file must be the original Edge.java",
                edgeFileBefore.getCanonicalPath(), f.getCanonicalPath());
        assertTrue("getText() must return the dirty buffer (what the editor shows)",
                cu.getText().contains("getNote"));
    }

    // =========================================================================
    // Convention-aware generation (property-identifier-constant-design.md, Lot 1b)
    // =========================================================================

    private static final File MODEL3_SRC = new File(
            System.getProperty("user.dir") + "/src/test/java/test/model3");

    /**
     * Generating a property on a CONSTANT-style entity (Foo1 uses {@code FOO2}) declares a
     * {@code static final String} constant and references it (unqualified) in every accessor,
     * instead of a string literal. The getter creates the constant; the setter reuses it by value.
     */
    @Test
    public void testGeneratePropertyEmitsConstantOnConstantEntity() throws IOException {
        File workDir = tmp.newFolder("genConstant");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);

        SourceModelEntity foo1 = mm.getEntity("test.model1.Foo1");
        assertEquals(PropertyIdentifierStyle.CONSTANT, foo1.getIdentifierStyle());

        foo1.addSingleProperty("label", "java.lang.String");
        mm.flushAll();

        String content = new String(Files.readAllBytes(new File(srcCopy, "Foo1.java").toPath()));
        assertTrue("constant field must be declared",
                content.contains("static final String LABEL = \"label\""));
        // The printer collapses single-element annotations to the shorthand form, like the model.
        assertTrue("getter must reference the constant", content.contains("@Getter(LABEL)"));
        assertTrue("setter must reference the constant", content.contains("@Setter(LABEL)"));
        assertFalse("getter must not use a literal identifier", content.contains("@Getter(\"label\")"));
        assertFalse("constant must be referenced unqualified", content.contains("Foo1.LABEL"));
        // Reuse-by-value: a single LABEL declaration shared by getter + setter.
        assertEquals("exactly one LABEL constant declared", content.indexOf("String LABEL"),
                content.lastIndexOf("String LABEL"));

        mm.rebuildMetaModel();
        SourceModelProperty label = mm.getEntity("test.model1.Foo1")
                .getDeclaredProperties().get("label");
        assertEquals(PropertyIdentifierStyle.CONSTANT, label.getIdentifierStyle());
        assertEquals("LABEL", label.getReferencedConstantName());
    }

    /**
     * Generating a property on a LITERAL-style entity (model3 {@code LiteralEntity}) keeps the
     * string-literal idiom — no constant is declared.
     */
    @Test
    public void testGeneratePropertyEmitsLiteralOnLiteralEntity() throws IOException {
        File workDir = tmp.newFolder("genLiteral");
        File srcCopy = copyDir(MODEL3_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model3.LiteralEntity");
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);

        SourceModelEntity literal = mm.getEntity("test.model3.LiteralEntity");
        assertEquals(PropertyIdentifierStyle.LITERAL, literal.getIdentifierStyle());

        literal.addSingleProperty("title", "java.lang.String");
        mm.flushAll();

        String content = new String(Files.readAllBytes(new File(srcCopy, "LiteralEntity.java").toPath()));
        assertTrue("getter must use a string literal", content.contains("@Getter(\"title\")"));
        assertFalse("no constant must be declared", content.contains("TITLE"));
    }

    /**
     * Adding a setter to an existing CONSTANT property reuses the property's own constant
     * (cloning the getter's reference), not a fresh literal.
     */
    @Test
    public void testAddSetterReusesPropertyConstant() throws IOException {
        File workDir = tmp.newFolder("addSetterConstant");
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);

        // Create a getter-only constant property, then add the setter.
        SourceModelEntity foo1 = mm.getEntity("test.model1.Foo1");
        foo1.createProperty("label", "java.lang.String", false, java.util.Arrays.asList(
                AccessorSpec.generate(AccessorSpec.Role.GETTER, "getLabel")));
        mm.rebuildMetaModel();
        mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("label").addSetter();
        mm.flushAll();

        String content = new String(Files.readAllBytes(new File(srcCopy, "Foo1.java").toPath()));
        assertTrue("setter must reuse the constant", content.contains("@Setter(LABEL)"));
        assertFalse("setter must not use a literal", content.contains("@Setter(\"label\")"));
    }

    /**
     * Promoting on a LITERAL-style entity (model3 {@code LiteralEntity}) keeps the string-literal
     * idiom in the text-edit path — no constant declared.
     */
    @Test
    public void testPromoteOnLiteralEntityStaysLiteral() throws IOException {
        File workDir = tmp.newFolder("promoteLiteral");
        File srcCopy = copyDir(MODEL3_SRC, workDir);

        File file = new File(srcCopy, "LiteralEntity.java");
        String src = new String(Files.readAllBytes(file.toPath()));
        int lastBrace = src.lastIndexOf('}');
        src = src.substring(0, lastBrace) + "\n\tpublic String getCount();\n\n" + src.substring(lastBrace);
        Files.write(file.toPath(), src.getBytes());

        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model3.LiteralEntity");
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);

        mm.getEntity("test.model3.LiteralEntity").promoteMethodToProperty("getCount", "count", false);
        mm.flushAll();

        String content = new String(Files.readAllBytes(file.toPath()));
        assertTrue("getter must use a string literal", content.contains("@Getter(value = \"count\")"));
        assertFalse("no constant must be declared", content.contains("COUNT"));
    }

    /**
     * Attaching a setter to an existing CONSTANT property (the text-edit path) reuses the property's
     * own constant token, cloned from the getter's reference — not a literal.
     */
    @Test
    public void testAttachSetterReusesConstantTextPath() throws IOException {
        File workDir = tmp.newFolder("attachReuse");
        File srcCopy = copyDir(MODEL1_SRC, workDir);

        // A plain getter + sibling setter; promote only the getter → declares NOTE + @Getter(NOTE).
        File foo1File = new File(srcCopy, "Foo1.java");
        String foo1 = new String(Files.readAllBytes(foo1File.toPath()));
        int lastBrace = foo1.lastIndexOf('}');
        foo1 = foo1.substring(0, lastBrace)
                + "\n\tpublic String getNote();\n\n\tpublic void setNote(String value);\n\n"
                + foo1.substring(lastBrace);
        Files.write(foo1File.toPath(), foo1.getBytes());

        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);
        mm.getEntity("test.model1.Foo1").promoteGetterToProperty("getNote", "note", false);
        mm.rebuildMetaModel();

        // Attach the sibling setter to the now-constant property.
        mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("note").attachSetter("setNote");
        mm.flushAll();

        String content = new String(Files.readAllBytes(foo1File.toPath()));
        assertTrue("setter must reference the property constant", content.contains("@Setter(value = NOTE)"));
        assertFalse("setter must not use a literal", content.contains("@Setter(value = \"note\")"));
    }
}
