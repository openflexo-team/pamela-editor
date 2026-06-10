package org.openflexo.pamela.editor.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;

/**
 * Tests for the Spoon parse-scope build cache (approach B, see
 * {@code source-metamodel-design.md §18}).
 *
 * <p>Each test copies {@code test/model2} into a {@link TemporaryFolder} so the
 * directory can be mutated (mtime bumps, file additions) without touching the
 * repository, then drives {@link SourceMetaModelSerializer#load} which wires the cache.</p>
 */
public class TestBuildCache {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final File MODEL2_SRC =
            new File(System.getProperty("user.dir") + "/src/test/java/test/model2");

    /** Copies every {@code .java} of model2 (flat) into a fresh temp source dir. */
    private File copyModel2() throws IOException {
        File dest = tmp.newFolder("src");
        for (File f : MODEL2_SRC.listFiles()) {
            if (f.getName().endsWith(".java")) {
                Files.copy(f.toPath(), new File(dest, f.getName()).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return dest;
    }

    private int countJavaFiles(File dir) {
        int n = 0;
        for (File f : dir.listFiles()) {
            if (f.getName().endsWith(".java")) {
                n++;
            }
        }
        return n;
    }

    /** Writes a .pamela next to the temp root, pointing at {@code srcDir}, 6 roots → 12 entities. */
    private File writePamela(File srcDir) throws IOException {
        File pamela = new File(tmp.getRoot(), "proj.pamela");
        String rel = tmp.getRoot().toPath().relativize(srcDir.toPath()).toString().replace('\\', '/');
        String json = "{\n"
                + "  \"name\": \"cache-test\",\n"
                + "  \"sourceDirectories\": [ \"" + rel + "\" ],\n"
                + "  \"rootTypes\": [\n"
                + "    \"test.model2.FlexoProcess\", \"test.model2.ActivityNode\",\n"
                + "    \"test.model2.StartNode\", \"test.model2.EndNode\",\n"
                + "    \"test.model2.MyNode\", \"test.model2.TokenEdge\"\n"
                + "  ]\n}\n";
        Files.write(pamela.toPath(), json.getBytes(StandardCharsets.UTF_8));
        return pamela;
    }

    // =========================================================================
    // Cache creation + fast path correctness
    // =========================================================================

    /**
     * First load writes the cache sidecar; a second load takes the fast path and
     * reproduces the exact same entity set.
     */
    @Test
    public void testCacheWrittenAndFastPathReproducesModel() throws IOException {
        File src = copyModel2();
        File pamela = writePamela(src);

        SourceMetaModel full = SourceMetaModelSerializer.load(pamela);
        assertEquals(12, full.getEntities().size());

        File cacheFile = SourceBuildCache.cacheFileFor(pamela);
        assertEquals(".proj.pamela.cache", cacheFile.getName());
        assertTrue("cache sidecar should be written on first (full) load", cacheFile.isFile());

        // Second load → cache hit → fast path. Same entities.
        SourceMetaModel fast = SourceMetaModelSerializer.load(pamela);
        assertEquals("fast path must reproduce all entities",
                full.getEntities().keySet(), fast.getEntities().keySet());
        assertEquals(full.getTotalPropertiesCount(), fast.getTotalPropertiesCount());
        assertEquals(full.getIssues().size(), fast.getIssues().size());
    }

    /**
     * The cached restricted set excludes a non-entity, unreferenced file — proving the
     * parse scope is actually pruned to the files the analysis resolved.
     */
    @Test
    public void testRestrictedSetExcludesUnrelatedFile() throws IOException {
        File src = copyModel2();
        // A plain class, not @ModelEntity and referenced by nobody.
        File unrelated = new File(src, "Unrelated.java");
        Files.write(unrelated.toPath(),
                "package test.model2;\npublic class Unrelated {}\n".getBytes(StandardCharsets.UTF_8));

        File pamela = writePamela(src);
        SourceMetaModelSerializer.load(pamela);

        File cacheFile = SourceBuildCache.cacheFileFor(pamela);
        SourceBuildCache cache = SourceBuildCache.load(cacheFile);
        assertNotNull(cache);
        Set<File> restricted = cache.resolveFiles(pamela.getParentFile());
        assertNotNull("all cached files should resolve on disk", restricted);

        int total = countJavaFiles(src);
        assertTrue("restricted set (" + restricted.size() + ") should be < total files (" + total + ")",
                restricted.size() < total);

        // The unrelated, non-entity file must not be in the restricted set
        // (restricted files carry ".." segments, so compare canonical paths).
        File unrelatedCanon = unrelated.getCanonicalFile();
        boolean containsUnrelated = false;
        for (File f : restricted) {
            if (f.getCanonicalFile().equals(unrelatedCanon)) {
                containsUnrelated = true;
                break;
            }
        }
        assertFalse("the unrelated non-entity file must not be in the restricted set",
                containsUnrelated);

        // The 12 entities must be in the restricted set (impl files may add a few more).
        assertTrue(restricted.size() >= 12);
    }

    // =========================================================================
    // Invalidation
    // =========================================================================

    /**
     * The cache matches its own fingerprint, and bumping a file's mtime changes the
     * fingerprint so the next load rebuilds (and refreshes the cache).
     */
    @Test
    public void testMtimeChangeInvalidates() throws IOException {
        File src = copyModel2();
        File pamela = writePamela(src);
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamela);

        File cacheFile = SourceBuildCache.cacheFileFor(pamela);
        String fp1 = SourceBuildCache.fingerprint(mm.getSourceDirectories(), mm.getRootTypeNames());
        assertTrue("cache should match the fingerprint just used to build it",
                SourceBuildCache.load(cacheFile).matches(fp1));

        // Simulate an edit: bump the mtime of one source file.
        File touched = new File(src, "ActivityNode.java");
        assertTrue(touched.setLastModified(touched.lastModified() + 10_000L));

        String fp2 = SourceBuildCache.fingerprint(mm.getSourceDirectories(), mm.getRootTypeNames());
        assertNotEquals("fingerprint must change when a file's mtime changes", fp1, fp2);

        // Reload → full rebuild → cache rewritten to the new fingerprint.
        SourceMetaModel rebuilt = SourceMetaModelSerializer.load(pamela);
        assertEquals(12, rebuilt.getEntities().size());
        assertTrue("cache should be refreshed to the new fingerprint",
                SourceBuildCache.load(cacheFile).matches(fp2));
    }

    /**
     * Adding a new {@code .java} file changes the fingerprint (additions must be caught).
     */
    @Test
    public void testFileAdditionInvalidates() throws IOException {
        File src = copyModel2();
        File pamela = writePamela(src);
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamela);

        String fp1 = SourceBuildCache.fingerprint(mm.getSourceDirectories(), mm.getRootTypeNames());

        File extra = new File(src, "Extra.java");
        Files.write(extra.toPath(),
                "package test.model2;\npublic class Extra {}\n".getBytes(StandardCharsets.UTF_8));

        String fp2 = SourceBuildCache.fingerprint(mm.getSourceDirectories(), mm.getRootTypeNames());
        assertNotEquals("fingerprint must change when a file is added", fp1, fp2);
    }

    // =========================================================================
    // Robustness
    // =========================================================================

    /**
     * A corrupt cache file is treated as a miss: the load still succeeds (full build)
     * and the cache is rewritten as valid.
     */
    @Test
    public void testCorruptCacheFallsBackToFullBuild() throws IOException {
        File src = copyModel2();
        File pamela = writePamela(src);
        SourceMetaModelSerializer.load(pamela); // writes a valid cache

        File cacheFile = SourceBuildCache.cacheFileFor(pamela);
        Files.write(cacheFile.toPath(), "{ this is not valid json".getBytes(StandardCharsets.UTF_8));
        assertNull("corrupt cache must parse as a miss", SourceBuildCache.load(cacheFile));

        SourceMetaModel mm = SourceMetaModelSerializer.load(pamela);
        assertEquals("full build must succeed despite a corrupt cache", 12, mm.getEntities().size());
        assertNotNull("cache should be rewritten as valid", SourceBuildCache.load(cacheFile));
    }
}
