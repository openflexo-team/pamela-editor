package org.openflexo.pamela.editor.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

/**
 * Tests for the incremental / dynamic construction of {@link SourceMetaModel}.
 *
 * <p>Each test builds a model programmatically (no .pamela file) by:
 * <ol>
 *   <li>Creating an empty {@link SourceMetaModel}</li>
 *   <li>Calling {@link SourceMetaModel#addSourceDirectory} and
 *       {@link SourceMetaModel#addRootTypeName}</li>
 *   <li>Calling {@link SourceMetaModel#buildMetaModel()} or
 *       {@link SourceMetaModel#rebuildMetaModel()} after each change</li>
 * </ol>
 *
 * <p>The scenarios cover:
 * <ul>
 *   <li>Scenario 1: create from scratch with a single root type</li>
 *   <li>Scenario 2: add a second root type, rebuild → more entities discovered</li>
 *   <li>Scenario 3: remove a root type, rebuild → fewer entities</li>
 *   <li>Scenario 4: add a second source directory, rebuild → entities from both dirs</li>
 *   <li>Scenario 5: remove all root types, rebuild → empty model</li>
 * </ul>
 */
public class TestIncrementalBuild {

    // Both test source directories live under the module's src/test/java tree
    private static final File MODEL1_DIR =
            new File(System.getProperty("user.dir") + "/src/test/java/test/model1");
    private static final File MODEL2_DIR =
            new File(System.getProperty("user.dir") + "/src/test/java/test/model2");

    // Convenience: the test/java root (covers both model1 and model2 in one pass)
    private static final File TEST_JAVA_ROOT =
            new File(System.getProperty("user.dir") + "/src/test/java");

    // =========================================================================
    // Scenario 1 — create from scratch with a single root type (Foo2)
    // =========================================================================

    /**
     * Build a model pointing at model1 sources with Foo2 as the only root type.
     * Only Foo2 should be discovered (Foo1 is not reachable from Foo2 via
     * super-interfaces or @Getter property types).
     */
    @Test
    public void scenario1_buildFromScratchSingleRoot() {
        SourceMetaModel model = new SourceMetaModel();
        model.setName("Scenario1");
        model.addSourceDirectory(MODEL1_DIR);
        model.addRootTypeName("test.model1.Foo2");

        model.buildMetaModel();

        assertEquals("Scenario 1: exactly 1 entity expected", 1, model.getEntitiesCount());
        assertNotNull(model.getEntity("test.model1.Foo2"));
        assertNull("Foo1 must not be discovered when root is Foo2 only",
                model.getEntity("test.model1.Foo1"));
        assertEquals("One package expected", 1, model.getPackagesCount());
    }

    // =========================================================================
    // Scenario 2 — add a second root type, rebuild → more entities
    // =========================================================================

    /**
     * Start from Foo2 only (1 entity), then add Foo1 as a second root type and
     * rebuild. The model must now contain 2 entities.
     *
     * <p>This validates that {@link SourceMetaModel#rebuildMetaModel()} properly
     * resets the previously built state before re-running the analysis.</p>
     */
    @Test
    public void scenario2_addRootTypeAndRebuild() {
        SourceMetaModel model = new SourceMetaModel();
        model.setName("Scenario2");
        model.addSourceDirectory(MODEL1_DIR);
        model.addRootTypeName("test.model1.Foo2");
        model.buildMetaModel();

        assertEquals("After first build: 1 entity", 1, model.getEntitiesCount());

        // Add Foo1 as a second root and rebuild
        model.addRootTypeName("test.model1.Foo1");
        model.rebuildMetaModel();

        assertEquals("After adding Foo1 and rebuilding: 2 entities", 2, model.getEntitiesCount());
        assertNotNull(model.getEntity("test.model1.Foo1"));
        assertNotNull(model.getEntity("test.model1.Foo2"));
    }

    // =========================================================================
    // Scenario 3 — remove a root type, rebuild → fewer entities
    // =========================================================================

    /**
     * Start from Foo1 (which transitively discovers Foo2 via the "foo2" property).
     * Then remove Foo1 from root types, keeping only Foo2, and rebuild.
     * Only Foo2 must remain.
     */
    @Test
    public void scenario3_removeRootTypeAndRebuild() {
        SourceMetaModel model = new SourceMetaModel();
        model.setName("Scenario3");
        model.addSourceDirectory(MODEL1_DIR);
        model.addRootTypeName("test.model1.Foo1");
        model.addRootTypeName("test.model1.Foo2");
        model.buildMetaModel();

        assertEquals("Initial build: 2 entities", 2, model.getEntitiesCount());

        // Remove Foo1 and rebuild; Foo2 should remain alone
        model.removeRootTypeName("test.model1.Foo1");
        model.rebuildMetaModel();

        assertEquals("After removing Foo1: 1 entity", 1, model.getEntitiesCount());
        assertNull("Foo1 must be gone after its root type is removed",
                model.getEntity("test.model1.Foo1"));
        assertNotNull("Foo2 must still be present", model.getEntity("test.model1.Foo2"));
    }

    // =========================================================================
    // Scenario 4 — add a second source directory, rebuild → entities from both dirs
    // =========================================================================

    /**
     * Start with model1 sources and Foo1 as root (discovers Foo1+Foo2).
     * Then add the model2 source directory and FlexoProcess as an additional root.
     * The rebuilt model must contain entities from both directories.
     *
     * <p>Starting from FlexoProcess alone the upward BFS reaches 6 entities from
     * model2 (FlexoProcess, WKFObject, TestModelObject, AbstractNode, Edge,
     * WKFAnnotation), plus the 2 entities from model1 = 8 total.</p>
     */
    @Test
    public void scenario4_addSourceDirectoryAndRebuild() {
        SourceMetaModel model = new SourceMetaModel();
        model.setName("Scenario4");
        model.addSourceDirectory(MODEL1_DIR);
        model.addRootTypeName("test.model1.Foo1");
        model.buildMetaModel();

        assertEquals("Initial build (model1): 2 entities", 2, model.getEntitiesCount());

        // Add model2 source directory and a model2 root type
        model.addSourceDirectory(MODEL2_DIR);
        model.addRootTypeName("test.model2.FlexoProcess");
        model.rebuildMetaModel();

        // model1 entities still present
        assertNotNull(model.getEntity("test.model1.Foo1"));
        assertNotNull(model.getEntity("test.model1.Foo2"));

        // model2 entities reachable from FlexoProcess
        assertNotNull(model.getEntity("test.model2.FlexoProcess"));
        assertNotNull(model.getEntity("test.model2.WKFObject"));
        assertNotNull(model.getEntity("test.model2.TestModelObject"));
        assertNotNull(model.getEntity("test.model2.AbstractNode"));
        assertNotNull(model.getEntity("test.model2.Edge"));
        assertNotNull(model.getEntity("test.model2.WKFAnnotation"));

        // Total: 2 (model1) + 6 (model2 from FlexoProcess) = 8
        assertEquals("After adding model2: 8 entities total", 8, model.getEntitiesCount());
    }

    // =========================================================================
    // Scenario 5 — remove all root types, rebuild → empty model
    // =========================================================================

    /**
     * Build a model from Foo1, then remove all root types and rebuild.
     * The resulting model must be empty (no entities, no packages).
     */
    @Test
    public void scenario5_removeAllRootTypesAndRebuild() {
        SourceMetaModel model = new SourceMetaModel();
        model.setName("Scenario5");
        model.addSourceDirectory(MODEL1_DIR);
        model.addRootTypeName("test.model1.Foo1");
        model.buildMetaModel();

        assertEquals("Initial build: 2 entities", 2, model.getEntitiesCount());

        model.removeRootTypeName("test.model1.Foo1");
        model.rebuildMetaModel();

        assertEquals("After removing all roots: 0 entities", 0, model.getEntitiesCount());
        assertEquals("After removing all roots: 0 packages", 0, model.getPackagesCount());
        assertTrue("Issues list should be empty after removing all roots (no warnings for missing roots)",
                model.getIssues().isEmpty());
    }

    // =========================================================================
    // Scenario 6 — single combined source root covers both models
    // =========================================================================

    /**
     * Use the parent {@code src/test/java} directory as a single source root.
     * Both model1 and model2 types should be resolvable from it.
     * Roots: Foo1 + FlexoProcess.
     */
    @Test
    public void scenario6_singleSourceRootCoveringBothModels() {
        SourceMetaModel model = new SourceMetaModel();
        model.setName("Scenario6");
        model.addSourceDirectory(TEST_JAVA_ROOT);
        model.addRootTypeName("test.model1.Foo1");
        model.addRootTypeName("test.model2.FlexoProcess");
        model.buildMetaModel();

        assertNotNull(model.getEntity("test.model1.Foo1"));
        assertNotNull(model.getEntity("test.model1.Foo2"));
        assertNotNull(model.getEntity("test.model2.FlexoProcess"));
        assertNotNull(model.getEntity("test.model2.WKFObject"));

        // 2 packages: test.model1 and test.model2
        assertTrue("At least 2 packages expected", model.getPackagesCount() >= 2);
    }

    // =========================================================================
    // Scenario 7 — full model2 with all leaf roots, then remove source dir
    // =========================================================================

    /**
     * Build a full model2 by providing all leaf entities as roots (12 entities
     * total). Then remove the model2 source directory and rebuild — the model
     * must become empty because the source is no longer available.
     */
    @Test
    public void scenario7_removeSourceDirectoryAndRebuild() {
        SourceMetaModel model = new SourceMetaModel();
        model.setName("Scenario7");
        model.addSourceDirectory(MODEL2_DIR);
        model.addRootTypeName("test.model2.ActivityNode");
        model.addRootTypeName("test.model2.StartNode");
        model.addRootTypeName("test.model2.EndNode");
        model.addRootTypeName("test.model2.MyNode");
        model.addRootTypeName("test.model2.TokenEdge");
        model.addRootTypeName("test.model2.WKFAnnotation");
        model.buildMetaModel();

        int fullCount = model.getEntitiesCount();
        assertTrue("Full model2 must have at least 6 entities", fullCount >= 6);

        // Remove the source directory — Spoon can no longer parse the types
        model.removeSourceDirectory(MODEL2_DIR);
        model.rebuildMetaModel();

        assertEquals("After removing source dir: 0 entities", 0, model.getEntitiesCount());
    }
}
