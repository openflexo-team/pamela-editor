package org.openflexo.pamela.editor.model;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Build cache for the Spoon parse-scope pruning (approach B, see
 * {@code source-metamodel-design.md §18}).
 *
 * <p>A first full {@link SourceMetaModel#buildMetaModel()} records the set of source
 * files actually resolved into model elements (entity + implementation-class
 * compilation units) together with a <em>directory fingerprint</em>. On a subsequent
 * open, if the fingerprint is unchanged, the parse is restricted to exactly those
 * files — reproducing the meta-model while parsing a fraction of the directory.</p>
 *
 * <p>The cache is a regenerable, hidden JSON sidecar next to the {@code .pamela} file.
 * File paths are stored relative to that directory (VCS-portable); the file is meant to
 * be git-ignored. Any read/parse problem yields a cache miss (a full parse), never an
 * error: the cache is a pure optimization.</p>
 */
public final class SourceBuildCache {

    /** Bumped when the on-disk format changes; an older version is treated as a miss. */
    private static final int VERSION = 1;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String fingerprint;
    private final List<String> restrictedRelPaths;

    private SourceBuildCache(String fingerprint, List<String> restrictedRelPaths) {
        this.fingerprint = fingerprint;
        this.restrictedRelPaths = restrictedRelPaths;
    }

    // -------------------------------------------------------------------------
    // Freshness
    // -------------------------------------------------------------------------

    /** {@code true} if this cache was built against the given fingerprint. */
    public boolean matches(String currentFingerprint) {
        return fingerprint != null && fingerprint.equals(currentFingerprint);
    }

    /**
     * Resolves the cached relative paths against {@code baseDir} (the {@code .pamela}
     * directory). Returns {@code null} — forcing a full parse — if any recorded file is
     * missing on disk (a defensive check; the fingerprint would normally already differ).
     */
    public Set<File> resolveFiles(File baseDir) {
        LinkedHashSet<File> out = new LinkedHashSet<>();
        for (String rel : restrictedRelPaths) {
            File f = new File(rel);
            if (!f.isAbsolute()) {
                f = new File(baseDir, rel);
            }
            if (!f.isFile()) {
                return null;
            }
            out.add(f);
        }
        return out;
    }

    // -------------------------------------------------------------------------
    // Fingerprint
    // -------------------------------------------------------------------------

    /**
     * Computes a fingerprint of the build inputs: the set of {@code (path, lastModified)}
     * of every {@code .java} file under the source directories, plus the root type names
     * and the source-directory paths. Captures edits (mtime), additions (new path) and
     * deletions (missing path). Cost is a few {@code stat}s, no content read.
     */
    public static String fingerprint(List<File> sourceDirectories, List<String> rootTypeNames) {
        StringBuilder sb = new StringBuilder();

        List<String> roots = new ArrayList<>(rootTypeNames);
        Collections.sort(roots);
        for (String r : roots) {
            sb.append("R:").append(r).append('\n');
        }

        List<String> entries = new ArrayList<>();
        List<File> dirs = new ArrayList<>(sourceDirectories);
        dirs.sort((a, b) -> a.getAbsolutePath().compareTo(b.getAbsolutePath()));
        for (File dir : dirs) {
            if (dir == null || !dir.isDirectory()) {
                continue;
            }
            sb.append("D:").append(dir.getAbsolutePath()).append('\n');
            collectJavaFiles(dir, entries);
        }
        Collections.sort(entries);
        for (String e : entries) {
            sb.append(e).append('\n');
        }
        return sha256(sb.toString());
    }

    private static void collectJavaFiles(File dir, List<String> entries) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File c : children) {
            if (c.isDirectory()) {
                collectJavaFiles(c, entries);
            } else if (c.getName().endsWith(".java")) {
                entries.add(c.getAbsolutePath() + ':' + c.lastModified());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Load / save
    // -------------------------------------------------------------------------

    /**
     * Loads a cache from the given sidecar file. Returns {@code null} on any problem
     * (missing file, wrong version, parse error) — i.e. a cache miss.
     */
    public static SourceBuildCache load(File cacheFile) {
        if (cacheFile == null || !cacheFile.isFile()) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(cacheFile);
            if (root.path("version").asInt(-1) != VERSION) {
                return null;
            }
            String fp = root.path("fingerprint").asText(null);
            if (fp == null || fp.isEmpty()) {
                return null;
            }
            List<String> rel = new ArrayList<>();
            JsonNode arr = root.get("restrictedFiles");
            if (arr != null && arr.isArray()) {
                for (JsonNode n : arr) {
                    String s = n.asText();
                    if (s != null && !s.isEmpty()) {
                        rel.add(s);
                    }
                }
            }
            return new SourceBuildCache(fp, rel);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Writes the cache sidecar. Paths in {@code files} are stored relative to
     * {@code baseDir} when possible (absolute as a fallback). Best-effort: never throws.
     */
    public static void save(File cacheFile, String fingerprint, Set<File> files, File baseDir) {
        if (cacheFile == null || fingerprint == null) {
            return;
        }
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("version", VERSION);
            root.put("fingerprint", fingerprint);

            // Canonicalize the base so relativization against the (also canonicalized)
            // file paths is clean. Spoon reports canonical file paths (e.g. /private/var
            // on macOS), while the .pamela directory may be a symlink form (/var); without
            // canonicalizing both sides relativize() yields a spurious "../../private/…".
            Path base = baseDir != null ? canonical(baseDir) : null;
            List<String> rels = new ArrayList<>();
            for (File f : files) {
                Path p = canonical(f);
                String rel;
                if (base != null) {
                    try {
                        rel = base.relativize(p).toString().replace(File.separatorChar, '/');
                    } catch (IllegalArgumentException ex) {
                        // different root (e.g. another Windows drive): keep absolute
                        rel = p.toString().replace(File.separatorChar, '/');
                    }
                } else {
                    rel = p.toString().replace(File.separatorChar, '/');
                }
                rels.add(rel);
            }
            Collections.sort(rels);

            ArrayNode arr = MAPPER.createArrayNode();
            for (String r : rels) {
                arr.add(r);
            }
            root.set("restrictedFiles", arr);

            File parent = cacheFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(cacheFile, root);
        } catch (Exception e) {
            // Cache write is a pure optimization — swallow any I/O problem.
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Derives the cache sidecar file for a {@code .pamela} project file:
     * a hidden sibling {@code .<pamelaFileName>.cache} (e.g.
     * {@code FMLModel.pamela} → {@code .FMLModel.pamela.cache}).
     */
    public static File cacheFileFor(File pamelaFile) {
        File parent = pamelaFile.getParentFile();
        String name = "." + pamelaFile.getName() + ".cache";
        return parent != null ? new File(parent, name) : new File(name);
    }

    /** Canonical path, falling back to the absolute path if canonicalization fails. */
    private static Path canonical(File f) {
        try {
            return f.getCanonicalFile().toPath();
        } catch (Exception e) {
            return f.toPath().toAbsolutePath();
        }
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
