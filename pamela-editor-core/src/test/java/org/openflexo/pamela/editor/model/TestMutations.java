package org.openflexo.pamela.editor.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
        File pamelaFile2 = new File(workDir, "test2.pamela");
        writePamelaFile(pamelaFile2, srcCopy, "test.model1.Foo1", "test.model1.Foo3");
        SourceMetaModel mm2 = SourceMetaModelSerializer.load(pamelaFile2);

        assertEquals("After reload, entity count must be 3", 3, mm2.getEntities().size());
        assertNotNull("Foo3 must be discoverable after reload", mm2.getEntity("test.model1.Foo3"));
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
        String content = new String(Files.readAllBytes(foo1File.toPath()));
        assertTrue("@Getter(value = \"name\") must be inserted",
                content.contains("@Getter(value = \"name\")"));
        assertTrue("@Setter(value = \"name\") must be inserted",
                content.contains("@Setter(value = \"name\")"));
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
}
