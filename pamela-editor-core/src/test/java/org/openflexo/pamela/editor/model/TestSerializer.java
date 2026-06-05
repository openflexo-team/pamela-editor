package org.openflexo.pamela.editor.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;

/**
 * Tests for {@link SourceMetaModelSerializer} — round-trip save/load of
 * {@code .pamela} project files.
 */
public class TestSerializer {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    // =========================================================================
    // Round-trip: model1
    // =========================================================================

    /**
     * Build a SourceMetaModel from test/model1, save it to a temp .pamela file,
     * reload it, and verify that the same entities are discovered.
     */
    @Test
    public void testRoundTripModel1() throws IOException {
        File sourceDir = new File(System.getProperty("user.dir") + "/src/test/java/test/model1");

        // Build original meta-model
        SourceMetaModel original = new SourceMetaModel();
        original.setName("model1-project");
        original.addSourceDirectory(sourceDir);
        original.addRootTypeName("test.model1.Foo1");
        original.buildMetaModel();

        assertEquals(2, original.getEntities().size());

        // Save to a temp .pamela file
        File pamelaFile = tmp.newFile("model1.pamela");
        SourceMetaModelSerializer.save(original, pamelaFile);

        // Verify the file was created and is non-empty
        assertTrue("Saved file should exist", pamelaFile.exists());
        String content = new String(Files.readAllBytes(pamelaFile.toPath()), StandardCharsets.UTF_8);
        assertTrue("File should contain the project name", content.contains("model1-project"));
        assertTrue("File should contain the root type", content.contains("test.model1.Foo1"));

        // Reload
        SourceMetaModel reloaded = SourceMetaModelSerializer.load(pamelaFile);

        // Verify same entities are discovered
        assertEquals("Reloaded model should have same number of entities",
                original.getEntities().size(), reloaded.getEntities().size());
        assertNotNull("Foo1 should be present after reload",
                reloaded.getEntity("test.model1.Foo1"));
        assertNotNull("Foo2 should be present after reload",
                reloaded.getEntity("test.model1.Foo2"));
        assertEquals("model1-project", reloaded.getName());
    }

    // =========================================================================
    // Round-trip: model2 (full)
    // =========================================================================

    /**
     * Build the full model2 meta-model (12 entities), save and reload.
     * Verifies that all 12 entities are re-discovered after reload.
     */
    @Test
    public void testRoundTripModel2Full() throws IOException {
        File sourceDir = new File(System.getProperty("user.dir") + "/src/test/java/test/model2");

        SourceMetaModel original = new SourceMetaModel();
        original.setName("model2-full");
        original.addSourceDirectory(sourceDir);
        original.addRootTypeName("test.model2.FlexoProcess");
        original.addRootTypeName("test.model2.ActivityNode");
        original.addRootTypeName("test.model2.StartNode");
        original.addRootTypeName("test.model2.EndNode");
        original.addRootTypeName("test.model2.MyNode");
        original.addRootTypeName("test.model2.TokenEdge");
        original.buildMetaModel();

        assertEquals(12, original.getEntities().size());

        File pamelaFile = tmp.newFile("model2.pamela");
        SourceMetaModelSerializer.save(original, pamelaFile);

        SourceMetaModel reloaded = SourceMetaModelSerializer.load(pamelaFile);

        assertEquals("All 12 entities should be re-discovered",
                12, reloaded.getEntities().size());
        assertEquals("model2-full", reloaded.getName());
    }

    // =========================================================================
    // File content verification
    // =========================================================================

    /**
     * Verifies that the saved file contains all expected JSON fields and that
     * paths are relative (not absolute).
     */
    @Test
    public void testSavedFileContent() throws IOException {
        File sourceDir = new File(System.getProperty("user.dir") + "/src/test/java/test/model1");

        SourceMetaModel mm = new SourceMetaModel();
        mm.setName("content-check");
        mm.addSourceDirectory(sourceDir);
        mm.addRootTypeName("test.model1.Foo1");
        mm.buildMetaModel();

        File pamelaFile = tmp.newFile("content-check.pamela");
        SourceMetaModelSerializer.save(mm, pamelaFile);

        String content = new String(Files.readAllBytes(pamelaFile.toPath()), StandardCharsets.UTF_8);

        // Required fields are present
        assertTrue("Should contain 'name' field", content.contains("\"name\""));
        assertTrue("Should contain 'sourceDirectories' field", content.contains("\"sourceDirectories\""));
        assertTrue("Should contain 'rootTypes' field", content.contains("\"rootTypes\""));

        // Paths must be relative: no entry in sourceDirectories should start with '/'
        // (which would indicate an absolute Unix path).  A relative path like
        // "../../../src/..." may still contain path segments that appear in the
        // absolute path, but it must NOT begin with '/'.
        assertFalse("sourceDirectories entries should not start with '/' (absolute path)",
                content.matches("(?s).*\"sourceDirectories\".*?\\[.*?\"/[^\"]*\".*?\\].*"));
    }

    // =========================================================================
    // Error handling
    // =========================================================================

    /**
     * Loading a non-existent file must throw IOException with a clear message.
     */
    @Test(expected = IOException.class)
    public void testLoadNonExistentFile() throws IOException {
        SourceMetaModelSerializer.load(new File(tmp.getRoot(), "does-not-exist.pamela"));
    }

    /**
     * If a .pamela file references a source directory that does not exist,
     * load() must throw IOException rather than silently ignoring it.
     */
    @Test(expected = IOException.class)
    public void testLoadMissingSourceDirectory() throws IOException {
        // Write a minimal .pamela file pointing to a non-existent directory
        File pamelaFile = tmp.newFile("broken.pamela");
        String json = "{\n"
                + "  \"name\": \"broken\",\n"
                + "  \"sourceDirectories\": [\n"
                + "    \"this/directory/does/not/exist\"\n"
                + "  ],\n"
                + "  \"rootTypes\": [\n"
                + "    \"some.Type\"\n"
                + "  ]\n"
                + "}\n";
        Files.write(pamelaFile.toPath(), json.getBytes(StandardCharsets.UTF_8));

        SourceMetaModelSerializer.load(pamelaFile);
    }

    // =========================================================================
    // Root types are preserved
    // =========================================================================

    /**
     * Verifies that all root type names are preserved exactly across a save/load
     * cycle, and in the same order.
     */
    @Test
    public void testRootTypesPreservedExactly() throws IOException {
        File sourceDir = new File(System.getProperty("user.dir") + "/src/test/java/test/model2");

        SourceMetaModel original = new SourceMetaModel();
        original.setName("root-type-order");
        original.addSourceDirectory(sourceDir);
        original.addRootTypeName("test.model2.FlexoProcess");
        original.addRootTypeName("test.model2.ActivityNode");
        original.addRootTypeName("test.model2.TokenEdge");
        original.buildMetaModel();

        File pamelaFile = tmp.newFile("root-types.pamela");
        SourceMetaModelSerializer.save(original, pamelaFile);

        SourceMetaModel reloaded = SourceMetaModelSerializer.load(pamelaFile);

        assertEquals("Root type count should be preserved",
                original.getRootTypeNames().size(), reloaded.getRootTypeNames().size());
        for (int i = 0; i < original.getRootTypeNames().size(); i++) {
            assertEquals("Root type at index " + i + " should match",
                    original.getRootTypeNames().get(i),
                    reloaded.getRootTypeNames().get(i));
        }
    }
}
