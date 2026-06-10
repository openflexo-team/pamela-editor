package org.openflexo.pamela.editor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import java.nio.file.Paths;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Serializes and deserializes a {@link SourceMetaModel} to/from a {@code .pamela}
 * project file (JSON, UTF-8).
 *
 * <h3>File format</h3>
 * <pre>
 * {
 *   "name" : "MyPamelaModel",
 *   "sourceDirectories" : [
 *     "src/main/java",
 *     "../other-module/src/main/java"
 *   ],
 *   "rootTypes" : [
 *     "test.model2.FlexoProcess",
 *     "test.model2.ActivityNode"
 *   ]
 * }
 * </pre>
 *
 * <p>Only the construction inputs are stored — entities, properties, packages
 * and issues are derived from source analysis on open and are never persisted.</p>
 *
 * <p>Source directory paths are always <em>relative to the directory that
 * contains the {@code .pamela} file</em>, making projects portable across
 * machines and safe to commit to a VCS.</p>
 */
public class SourceMetaModelSerializer {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private SourceMetaModelSerializer() {
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Loads a {@link SourceMetaModel} from a {@code .pamela} project file.
     *
     * <p>Each {@code sourceDirectory} path in the file is resolved relative to
     * the directory that contains {@code pamelaFile}. If a resolved directory
     * does not exist on disk, an {@link IOException} is thrown — missing
     * directories are never silently ignored.</p>
     *
     * <p>The meta-model is fully built (all three construction phases) before
     * being returned.</p>
     *
     * @param pamelaFile the {@code .pamela} file to load; must exist
     * @return a fully built {@link SourceMetaModel}
     * @throws IOException if the file cannot be read, cannot be parsed, or if a
     *                     source directory listed in the file does not exist on disk
     */
    public static SourceMetaModel load(File pamelaFile) throws IOException {
        return load(pamelaFile, null);
    }

    /**
     * Loads a {@link SourceMetaModel} from a {@code .pamela} project file, reporting
     * build progress to the given listener (parse stages + resolution phases).
     *
     * @param pamelaFile       the {@code .pamela} file to load; must exist
     * @param progressListener optional progress listener (may be {@code null})
     * @return a fully built {@link SourceMetaModel}
     * @throws IOException as {@link #load(File)}
     */
    public static SourceMetaModel load(File pamelaFile,
            org.openflexo.pamela.editor.model.BuildProgressListener progressListener)
            throws IOException {
        if (!pamelaFile.exists()) {
            throw new IOException("Project file not found: " + pamelaFile.getAbsolutePath());
        }

        JsonNode root = MAPPER.readTree(pamelaFile);

        Path baseDir = pamelaFile.toPath().getParent();
        if (baseDir == null) {
            baseDir = Paths.get(".");
        }

        SourceMetaModel mm = new SourceMetaModel();

        // name (optional)
        JsonNode nameNode = root.get("name");
        if (nameNode != null && !nameNode.isNull()) {
            String name = nameNode.asText();
            if (!name.isEmpty()) {
                mm.setName(name);
            }
        }

        // sourceDirectories
        JsonNode dirsNode = root.get("sourceDirectories");
        if (dirsNode != null && dirsNode.isArray()) {
            for (JsonNode dirNode : dirsNode) {
                String relPath = dirNode.asText();
                File resolved = baseDir.resolve(relPath).normalize().toFile();
                if (!resolved.exists()) {
                    throw new IOException(
                            "Source directory listed in project file does not exist: "
                            + resolved.getAbsolutePath()
                            + "  (entry: \"" + relPath + "\""
                            + ", project file: " + pamelaFile.getAbsolutePath() + ")");
                }
                mm.addSourceDirectory(resolved);
            }
        }

        // rootTypes
        JsonNode typesNode = root.get("rootTypes");
        if (typesNode != null && typesNode.isArray()) {
            for (JsonNode typeNode : typesNode) {
                String typeName = typeNode.asText().trim();
                if (!typeName.isEmpty()) {
                    mm.addRootTypeName(typeName);
                }
            }
        }

        // Enable the build cache (approach B, see source-metamodel-design.md §18): a
        // hidden sidecar next to the .pamela file lets buildMetaModel() restrict the
        // Spoon parse to the reachable entity files when the directory fingerprint is
        // unchanged. Both this initial load and later in-editor rebuilds benefit.
        mm.setBuildCacheFile(
                org.openflexo.pamela.editor.model.SourceBuildCache.cacheFileFor(pamelaFile));

        mm.setProgressListener(progressListener);
        mm.buildMetaModel();
        return mm;
    }

    /**
     * Returns the list of diagram sidecar file names declared in a
     * {@code .pamela} project file.
     *
     * <p>This is the list of file names (not paths) from the {@code "diagrams"}
     * JSON array. Returns an empty list if the field is absent or empty.</p>
     *
     * @param pamelaFile the {@code .pamela} file to read; must exist
     * @return list of diagram file names (e.g. {@code ["Overview.diagram.json"]})
     * @throws IOException if the file cannot be read or is not valid JSON
     */
    public static List<String> loadDiagramFileNames(File pamelaFile) throws IOException {
        List<String> names = new java.util.ArrayList<>();
        if (!pamelaFile.exists()) {
            return names;
        }
        JsonNode root = MAPPER.readTree(pamelaFile);
        JsonNode diagramsNode = root.get("diagrams");
        if (diagramsNode != null && diagramsNode.isArray()) {
            for (JsonNode n : diagramsNode) {
                String name = n.asText().trim();
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    /**
     * Saves a {@link SourceMetaModel} to a {@code .pamela} project file.
     *
     * <p>Source directory paths are written as paths <em>relative to the
     * directory containing {@code pamelaFile}</em>. The file is overwritten if
     * it already exists. Parent directories are created if necessary.</p>
     *
     * <p>No diagram sidecar references are written. Use
     * {@link #save(SourceMetaModel, File, List)} to persist diagram names.</p>
     *
     * @param metaModel  the meta-model whose construction inputs to persist
     * @param pamelaFile the destination {@code .pamela} file
     * @throws IOException if the file cannot be written
     */
    public static void save(SourceMetaModel metaModel, File pamelaFile) throws IOException {
        save(metaModel, pamelaFile, java.util.Collections.emptyList());
    }

    /**
     * Saves a {@link SourceMetaModel} to a {@code .pamela} project file,
     * including the list of diagram sidecar file names.
     *
     * <p>Source directory paths are written as paths <em>relative to the
     * directory containing {@code pamelaFile}</em>. The file is overwritten if
     * it already exists. Parent directories are created if necessary.</p>
     *
     * @param metaModel         the meta-model whose construction inputs to persist
     * @param pamelaFile        the destination {@code .pamela} file
     * @param diagramFileNames  file names (not paths) of the diagram sidecar files
     *                          to list in the {@code "diagrams"} JSON array
     * @throws IOException if the file cannot be written
     */
    public static void save(SourceMetaModel metaModel, File pamelaFile,
                            List<String> diagramFileNames) throws IOException {
        // Create parent directories if they don't exist
        File parentDir = pamelaFile.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }

        Path baseDir = pamelaFile.toPath().getParent();
        if (baseDir == null) {
            baseDir = Paths.get(".");
        }

        ObjectNode root = MAPPER.createObjectNode();

        // name
        root.put("name", metaModel.getName() != null ? metaModel.getName() : "");

        // sourceDirectories — relative paths, forward slashes
        ArrayNode dirsArray = MAPPER.createArrayNode();
        List<File> sourceDirs = metaModel.getSourceDirectories();
        for (File dir : sourceDirs) {
            Path relative = baseDir.relativize(dir.toPath().toAbsolutePath());
            dirsArray.add(relative.toString().replace(File.separatorChar, '/'));
        }
        root.set("sourceDirectories", dirsArray);

        // rootTypes
        ArrayNode typesArray = MAPPER.createArrayNode();
        for (String rootType : metaModel.getRootTypeNames()) {
            typesArray.add(rootType);
        }
        root.set("rootTypes", typesArray);

        // diagrams — sidecar file names (omit array if empty)
        if (diagramFileNames != null && !diagramFileNames.isEmpty()) {
            ArrayNode diagramsArray = MAPPER.createArrayNode();
            for (String name : diagramFileNames) {
                diagramsArray.add(name);
            }
            root.set("diagrams", diagramsArray);
        }

        MAPPER.writeValue(pamelaFile, root);
    }
}
