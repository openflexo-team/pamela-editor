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
}
