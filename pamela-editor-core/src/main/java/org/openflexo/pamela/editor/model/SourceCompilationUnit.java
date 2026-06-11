package org.openflexo.pamela.editor.model;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;

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
 *
 * <p>Two construction paths exist:
 * <ul>
 *   <li>Normal path (loading): via {@link #SourceCompilationUnit(CompilationUnit, SourceMetaModel)}
 *       — backed by a Spoon {@link CompilationUnit}.</li>
 *   <li>New-entity path (creation): via {@link #forNewEntity(CtInterface, File, SourceMetaModel)}
 *       — backed directly by a {@link CtInterface}, no Spoon compilation unit yet.</li>
 * </ul>
 * </p>
 */
public class SourceCompilationUnit implements SourceElement {

    // Internal Spoon reference — never exposed in the public API.
    // Exactly one of ctCompilationUnit or ctInterface is non-null.
    private final CompilationUnit ctCompilationUnit;
    private final CtInterface<?> ctInterface; // non-null only for new entities created via forNewEntity

    // Non-final: the rename operation updates the file path
    private File file;
    private final SourceMetaModel metaModel;
    private final String primaryTypeName;

    /**
     * Constructs a {@code SourceCompilationUnit} from an existing Spoon
     * compilation unit (normal load path).
     *
     * @param ctCompilationUnit the Spoon compilation unit (must not be {@code null})
     * @param metaModel         the owning meta-model
     */
    public SourceCompilationUnit(CompilationUnit ctCompilationUnit, SourceMetaModel metaModel) {
        this.ctCompilationUnit = ctCompilationUnit;
        this.ctInterface = null;
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

    /**
     * Private constructor for the new-entity path.
     * Use {@link #forNewEntity(CtInterface, File, SourceMetaModel)} to construct.
     */
    private SourceCompilationUnit(CtInterface<?> ctInterface, File file, SourceMetaModel metaModel) {
        this.ctCompilationUnit = null;
        this.ctInterface = ctInterface;
        this.file = file;
        this.metaModel = metaModel;
        this.primaryTypeName = ctInterface.getQualifiedName();
    }

    /**
     * Factory method for a freshly created entity that has no Spoon compilation
     * unit yet.  Used by {@link SourceMetaModel#createEntity}.
     *
     * @param ctInterface the newly created Spoon interface (must not be {@code null})
     * @param file        the target {@code .java} file on disk (may not exist yet)
     * @param metaModel   the owning meta-model
     * @return a new {@code SourceCompilationUnit}
     */
    static SourceCompilationUnit forNewEntity(CtInterface<?> ctInterface, File file, SourceMetaModel metaModel) {
        return new SourceCompilationUnit(ctInterface, file, metaModel);
    }

    // -------------------------------------------------------------------------
    // Package-private mutators (used by rename)
    // -------------------------------------------------------------------------

    /**
     * Updates the backing file reference.  Called by the rename operation after
     * the file has been moved on disk.
     *
     * @param f the new file (must not be {@code null})
     */
    void setFile(File f) {
        this.file = f;
    }

    /**
     * Updates the primary declared type reference in the Spoon compilation unit
     * to match the renamed type.
     *
     * <p>After {@link spoon.reflect.declaration.CtType#setSimpleName} is called,
     * the CU's internal {@code declaredTypeReferences} still contains the OLD
     * qualified name.  If a compiled {@code .class} file for the old name exists
     * on the classpath (e.g. from a previous {@code compileTestJava}), Spoon's
     * pretty-printer would resolve it to that shadow class and print the OLD name.
     * Calling this method replaces the stale reference with a fresh one derived
     * from the renamed type, so the next {@link #save()} prints the new name.</p>
     *
     * @param renamedType the type after {@code setSimpleName()} has been called
     */
    void updateDeclaredType(CtType<?> renamedType) {
        if (ctCompilationUnit != null) {
            // CompilationUnit extends CtCompilationUnit; setDeclaredTypes is accessible directly.
            List<CtType<?>> typeList = new ArrayList<>();
            typeList.add(renamedType);
            ctCompilationUnit.setDeclaredTypes(typeList);
        }
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
     * <p>Two save strategies are used:
     * <ul>
     *   <li>If backed by a {@link CompilationUnit}: use Spoon's compilation-unit
     *       printer.</li>
     *   <li>If backed directly by a {@link CtInterface} (new entity): use
     *       {@link DefaultJavaPrettyPrinter} to generate the source text.</li>
     * </ul>
     * </p>
     *
     * @throws IOException           if the file cannot be written
     * @throws IllegalStateException if there is no associated file on disk
     */
    public void save() throws IOException {
        if (file == null) {
            throw new IllegalStateException("Cannot save a compilation unit with no associated file");
        }
        // Ensure parent directories exist
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        String source;
        if (ctCompilationUnit != null) {
            // Normal path: use Spoon's compilation-unit pretty-printer
            source = ctCompilationUnit.getFactory().getEnvironment()
                    .createPrettyPrinter()
                    .printCompilationUnit(ctCompilationUnit);
        } else {
            // New-entity path: use DefaultJavaPrettyPrinter directly on the interface
            DefaultJavaPrettyPrinter printer = new DefaultJavaPrettyPrinter(
                    ctInterface.getFactory().getEnvironment());
            printer.calculate(null, Collections.singletonList(ctInterface));
            source = printer.getResult();
        }
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns the top-level types declared in this compilation unit, in source order.
     *
     * <p>This method is intentionally exposed in the public API despite the general
     * rule of not leaking Spoon types — the Spoon outline view is inherently a Spoon
     * view and needs direct access to {@link CtType} instances (see
     * {@code ui-design.md §19.3}, decision D2).</p>
     *
     * @return an unmodifiable list of declared types; never {@code null}
     */
    public List<CtType<?>> getRootTypes() {
        if (ctCompilationUnit != null) {
            return Collections.unmodifiableList(ctCompilationUnit.getDeclaredTypes());
        }
        if (ctInterface != null) {
            return Collections.singletonList(ctInterface);
        }
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "SourceCompilationUnit(" + primaryTypeName + ")";
    }
}
