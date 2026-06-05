package org.openflexo.pamela.editor.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import spoon.reflect.cu.CompilationUnit;

/**
 * Represents a single {@code .java} file in the source tree.
 *
 * <p>This is the <em>write-back point</em>: after any AST mutation performed
 * via a {@code Source*} class, the affected {@code SourceCompilationUnit}
 * rewrites the {@code .java} file to disk via Spoon's pretty-printer.</p>
 *
 * <p>One {@code SourceCompilationUnit} may be referenced by at most one
 * {@link SourceModelEntity} (as the file that declares its interface or class)
 * and at most one {@link SourceImplementationClass} (as the impl class's file).</p>
 */
public class SourceCompilationUnit implements SourceElement {

    // Internal Spoon reference — never exposed in the public API
    private final CompilationUnit ctCompilationUnit;

    private final File file;
    private final SourceMetaModel metaModel;
    private final String primaryTypeName;

    /**
     * Constructs a {@code SourceCompilationUnit}.
     *
     * @param ctCompilationUnit the Spoon compilation unit (must not be {@code null})
     * @param metaModel         the owning meta-model
     */
    public SourceCompilationUnit(CompilationUnit ctCompilationUnit, SourceMetaModel metaModel) {
        this.ctCompilationUnit = ctCompilationUnit;
        this.metaModel = metaModel;

        // Derive the file path from the Spoon compilation unit
        String path = ctCompilationUnit.getFile() != null ? ctCompilationUnit.getFile().getPath() : null;
        this.file = path != null ? new File(path) : null;

        // Determine the primary type qualified name
        String mainType = "";
        if (!ctCompilationUnit.getDeclaredTypes().isEmpty()) {
            mainType = ctCompilationUnit.getDeclaredTypes().get(0).getQualifiedName();
        }
        this.primaryTypeName = mainType;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * The {@code .java} file on disk, or {@code null} if the compilation unit
     * has no associated file (e.g., synthetic or in-memory sources).
     */
    public File getFile() {
        return file;
    }

    /** The owning meta-model. */
    public SourceMetaModel getMetaModel() {
        return metaModel;
    }

    /**
     * The qualified name of the main (first declared) type in this file,
     * e.g. {@code "org.example.Person"}.  Empty string if the file declares
     * no types.
     */
    public String getPrimaryTypeName() {
        return primaryTypeName;
    }

    /**
     * Rewrites the {@code .java} file on disk by pretty-printing the current
     * state of the Spoon AST.
     *
     * <p>Call this method after any AST mutation performed on this compilation
     * unit (renaming, adding/removing properties, etc.).</p>
     *
     * @throws IOException if the file cannot be written
     * @throws IllegalStateException if there is no associated file on disk
     */
    public void save() throws IOException {
        if (file == null) {
            throw new IllegalStateException("Cannot save a compilation unit with no associated file");
        }
        // Use Spoon's built-in pretty printer to produce the source text,
        // then overwrite the file.
        String source = ctCompilationUnit.getFactory().getEnvironment()
                .createPrettyPrinter()
                .printCompilationUnit(ctCompilationUnit);
        Files.write(file.toPath(), source.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public String toString() {
        return "SourceCompilationUnit(" + primaryTypeName + ")";
    }
}
