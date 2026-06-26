package org.openflexo.pamela.editor.model;

import java.beans.PropertyChangeSupport;
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
 * <p>This class owns the <em>in-memory source buffer</em> for its file. All
 * model mutations (annotation edits, structural AST changes) write to this
 * buffer via {@link #setText(String)} / {@link #regenerateFromAST()} and mark
 * the unit <em>dirty</em>; nothing reaches disk until the buffer is flushed
 * ({@link #flush()} / {@link SourceMetaModel#flushAll()}), which happens on an
 * explicit <em>Save project</em>. This decouples "the current source text of a
 * compilation unit" from "the file on disk" so edits can be held dirty and the
 * meta-model can be rebuilt from the buffer (parse-from-buffer) without writing
 * the file — see {@code editable-source-dirty-buffer-design.md}.</p>
 *
 * <p>The dirty buffer is mirrored into the owning {@link SourceMetaModel} (keyed
 * by primary type qualified name) so it survives a {@code rebuildMetaModel()}
 * that replaces every {@code SourceCompilationUnit} instance.</p>
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
    // Non-final: the rename operation updates the primary type name (the pending-buffer key)
    private String primaryTypeName;

    // In-memory source buffer (see class doc). null until first accessed via getText().
    private String currentSource;
    private boolean dirty;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

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

        // Derive the file path from the Spoon compilation unit. For a unit parsed
        // from an in-memory buffer (VirtualFile, used by parse-from-buffer) this is
        // null; the file is then restored from the pending entry — see
        // SourceMetaModel.phase1Discovery / seedFromPending.
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
    // Package-private mutators (used by rename / parse-from-buffer)
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
     * Updates the primary type qualified name (the pending-buffer key). Called by
     * the rename operation, which changes both the file and the type name.
     */
    void setPrimaryTypeName(String qualifiedName) {
        this.primaryTypeName = qualifiedName;
    }

    /**
     * Restores the buffer (and, if needed, the file) of a compilation unit that
     * was re-parsed from an in-memory buffer. A {@code VirtualFile}-parsed unit
     * has no file path (see {@link spoon.support.compiler.VirtualFile}), so the
     * real file is recovered from the pending entry and the dirty text re-adopted,
     * so a model edit that was not flushed survives the rebuild it triggered.
     *
     * @param recoveredFile the real {@code .java} file the buffer targets
     * @param pendingText   the unsaved source text
     */
    void seedFromPending(File recoveredFile, String pendingText) {
        // Always override the file: a VirtualFile-parsed unit reports a bogus relative
        // path (its virtual name, e.g. "Edge.java") rather than null, so guarding on
        // "file == null" would keep that non-existent path. The pending entry holds the
        // real target file recorded when the edit was made.
        this.file = recoveredFile;
        this.currentSource = pendingText;
        this.dirty = true;
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
     * from the renamed type, so the next print picks up the new name.</p>
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

    /** Clears the dirty flag (called after the buffer has been flushed to disk). */
    void markClean() {
        this.dirty = false;
    }

    // -------------------------------------------------------------------------
    // Public API — source buffer
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
     * The current source text of this compilation unit — the in-memory buffer,
     * which may differ from the file on disk if the unit is {@linkplain #isDirty()
     * dirty}. Seeded lazily: from a pending (carried-over) edit, else from disk,
     * else (for a brand-new entity with no file yet) from the AST pretty-print.
     *
     * @return the current source text; never {@code null}
     */
    public String getText() {
        if (currentSource == null) {
            // 1. A pending edit carried across a rebuild (parse-from-buffer) wins.
            String pending = metaModel.getPendingSourceText(primaryTypeName);
            if (pending != null) {
                currentSource = pending;
                dirty = true;
            } else if (file != null && file.exists()) {
                // 2. Existing file: read disk verbatim (preserves formatting/comments).
                try {
                    currentSource = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    currentSource = printFromAst();
                }
            } else {
                // 3. Brand-new entity not yet on disk: pretty-print the AST.
                currentSource = printFromAst();
            }
        }
        return currentSource;
    }

    /**
     * Replaces the in-memory source buffer, marks this unit dirty, registers the
     * edit with the meta-model (so it survives a rebuild), and fires a
     * {@code "source"} {@link java.beans.PropertyChangeEvent}. Does <b>not</b>
     * write to disk — call {@link #flush()} (or {@link SourceMetaModel#flushAll()})
     * to persist.
     *
     * @param newSource the new source text (must not be {@code null})
     */
    public void setText(String newSource) {
        String old = this.currentSource;
        this.currentSource = newSource;
        this.dirty = true;
        metaModel.registerPendingSource(primaryTypeName, file, newSource);
        pcs.firePropertyChange("source", old, newSource);
    }

    /**
     * Re-renders the buffer from the current state of the Spoon AST (used after a
     * structural AST mutation: rename, add/remove property, super-interface…).
     * Marks the unit dirty; does <b>not</b> write to disk.
     *
     * <p>Two print strategies, as before: the compilation-unit printer for a
     * loaded file, {@link DefaultJavaPrettyPrinter} for a brand-new entity.</p>
     */
    public void regenerateFromAST() {
        setText(printFromAst());
    }

    /**
     * Pretty-prints the current Spoon AST to source text (no disk I/O, no dirty
     * marking). Internal helper for {@link #regenerateFromAST()} and the new-entity
     * seed in {@link #getText()}.
     */
    private String printFromAst() {
        if (ctCompilationUnit != null) {
            // Normal path: use Spoon's compilation-unit pretty-printer
            return ctCompilationUnit.getFactory().getEnvironment()
                    .createPrettyPrinter()
                    .printCompilationUnit(ctCompilationUnit);
        }
        // New-entity path: use DefaultJavaPrettyPrinter directly on the interface
        DefaultJavaPrettyPrinter printer = new DefaultJavaPrettyPrinter(
                ctInterface.getFactory().getEnvironment());
        printer.calculate(null, Collections.singletonList(ctInterface));
        return printer.getResult();
    }

    /**
     * Writes the current buffer to disk and clears the dirty flag (the only disk
     * write). Creates parent directories as needed. Normally called via
     * {@link SourceMetaModel#flushAll()} on an explicit <em>Save project</em>.
     *
     * @throws IOException           if the file cannot be written
     * @throws IllegalStateException if there is no associated file on disk
     */
    public void flush() throws IOException {
        if (file == null) {
            throw new IllegalStateException("Cannot flush a compilation unit with no associated file");
        }
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        Files.write(file.toPath(), getText().getBytes(StandardCharsets.UTF_8));
        dirty = false;
        metaModel.clearPendingSource(primaryTypeName);
    }

    /**
     * {@code true} if the in-memory buffer has unsaved changes relative to disk.
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Immediate write-back: re-render from the AST and flush to disk in one step.
     *
     * @deprecated Prefer the deferred {@link #regenerateFromAST()} (buffer only) and
     *             an explicit {@link #flush()} / {@link SourceMetaModel#flushAll()}.
     *             Retained for callers that still want a synchronous write.
     * @throws IOException if the file cannot be written
     */
    @Deprecated
    public void save() throws IOException {
        regenerateFromAST();
        flush();
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

    /** PropertyChangeSupport firing {@code "source"} when the buffer changes. */
    public PropertyChangeSupport getPropertyChangeSupport() {
        return pcs;
    }

    @Override
    public String toString() {
        return "SourceCompilationUnit(" + primaryTypeName + (dirty ? ", dirty" : "") + ")";
    }
}
