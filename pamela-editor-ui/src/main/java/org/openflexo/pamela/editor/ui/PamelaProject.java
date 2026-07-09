package org.openflexo.pamela.editor.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import java.beans.PropertyChangeSupport;

import org.openflexo.diana.swing.control.SwingToolFactory;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagramFactory;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagramSerializer;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.exceptions.ModelDefinitionException;
import org.openflexo.pamela.factory.EditingContextImpl;
import org.openflexo.toolbox.HasPropertyChangeSupport;

/**
 * UI-level wrapper for an open PAMELA project.
 *
 * <p>This is not a {@code @ModelEntity}; it is a plain Java object managed by
 * {@link PamelaEditorApplication}.</p>
 *
 * <p>Structure:
 * <ul>
 *   <li>{@link #pamelaFile} — the {@code .pamela} file on disk</li>
 *   <li>{@link #metaModel} — built by Spoon analysis (from pamela-editor-core)</li>
 *   <li>{@link #diagrams} — loaded from sidecar {@code .diagram} files</li>
 *   <li>{@link #diagramFactory} — PAMELA factory for diagram model objects (one per project,
 *       owns the project's {@link EditingContextImpl})</li>
 * </ul>
 * </p>
 */
public class PamelaProject implements HasPropertyChangeSupport {

    private static final Logger logger =
            Logger.getLogger(PamelaProject.class.getPackage().getName());

    private final PropertyChangeSupport pcSupport = new PropertyChangeSupport(this);

    @Override
    public PropertyChangeSupport getPropertyChangeSupport() {
        return pcSupport;
    }

    @Override
    public String getDeletedProperty() {
        return null;
    }

    // Non-final: the "Rename Meta-Model…" action can also rename the .pamela file on disk
    // (implementation-class-support-design.md sibling — see RenameMetaModelAction).
    private File pamelaFile;
    private final SourceMetaModel metaModel;
    private final List<PamelaClassDiagram> diagrams;

    /** PAMELA model factory for this project's diagrams. */
    private final PamelaClassDiagramFactory diagramFactory;

    /** True when the project has unsaved diagram changes (positions, create/delete/rename…). */
    private boolean dirty = false;

    /**
     * Sidecar file names ({@code <id>.diagram}) believed to be on disk for this
     * project: seeded from the {@code .pamela} {@code "diagrams"} list at load,
     * refreshed after each save. Used to purge files of deleted diagrams.
     */
    private final Set<String> knownSidecarFiles = new LinkedHashSet<>();

    /**
     * Diana tool factory shared with the application.
     * Injected via {@link #setToolFactory(SwingToolFactory)} once available.
     */
    private SwingToolFactory toolFactory;

    public PamelaProject(File pamelaFile, SourceMetaModel metaModel) {
        this.pamelaFile = pamelaFile;
        this.metaModel = metaModel;
        this.diagrams = new ArrayList<>();

        // Create a dedicated editing context and diagram factory for this project
        EditingContextImpl editingContext = new EditingContextImpl();
        editingContext.createUndoManager();
        PamelaClassDiagramFactory factory = null;
        try {
            factory = new PamelaClassDiagramFactory(editingContext);
        } catch (ModelDefinitionException e) {
            logger.severe("Failed to create PamelaClassDiagramFactory: " + e.getMessage());
        }
        this.diagramFactory = factory;
    }

    // -------------------------------------------------------------------------
    // Basic accessors
    // -------------------------------------------------------------------------

    /** The {@code .pamela} file that defines this project. */
    public File getPamelaFile() {
        return pamelaFile;
    }

    /**
     * Repoints this project at a different {@code .pamela} file — used when the user renames
     * the project file (the physical rename on disk is done by the caller,
     * {@link PamelaEditorApplication#applyMetaModelRename}). Fires {@code "pamelaFile"} so any
     * live binding (e.g. the {@code PamelaProject.inspector} location field) refreshes.
     */
    public void setPamelaFile(File pamelaFile) {
        File old = this.pamelaFile;
        this.pamelaFile = pamelaFile;
        pcSupport.firePropertyChange("pamelaFile", old, pamelaFile);
        // The inspector's location field binds data.pamelaFilePath, so fire that name too.
        pcSupport.firePropertyChange("pamelaFilePath", null, getPamelaFilePath());
    }

    /** Absolute path of the {@code .pamela} file, as a String for inspector bindings. */
    public String getPamelaFilePath() {
        return pamelaFile != null ? pamelaFile.getAbsolutePath() : "";
    }

    /** The fully built {@link SourceMetaModel} for this project. */
    public SourceMetaModel getMetaModel() {
        return metaModel;
    }

    /**
     * Fires a UI-intent signal that the user asked to rename this project's meta-model (the
     * {@code PamelaProject.inspector}'s "Rename…" button, next to the read-only name field).
     *
     * <p>Deliberately fired on <b>this</b> object's own {@code PropertyChangeSupport} rather
     * than on {@link #metaModel}'s: when a project is selected in the browser, {@code element}
     * passed to {@code installEditListener} (model-editing-design.md §6) is the
     * {@code PamelaProject} itself — {@code PamelaProject.inspector} is what actually gets
     * shown (not {@code SourceMetaModel.inspector}; {@code PamelaProject} is not a
     * {@code SourceElement}) — so the signal must originate here to be observed at all.</p>
     */
    public void requestRename() {
        pcSupport.firePropertyChange("renameRequested", null, this);
    }

    /**
     * The class diagrams belonging to this project (loaded from sidecar files).
     *
     * @return an unmodifiable view
     */
    public List<PamelaClassDiagram> getDiagrams() {
        return Collections.unmodifiableList(diagrams);
    }

    public void addDiagram(PamelaClassDiagram diagram) {
        diagrams.add(diagram);
        // Fire on this object (not just "projects" on the application) so the browser's
        // per-cell children-binding listener — attached to this PamelaProject instance via
        // the "project.diagrams" binding path — is notified even when the project node was
        // already expanded/loaded (see gina-analysis.md §18.5 and FIBBrowserModel.updateSync:
        // an already-loaded cell relies on its own binding listener, not on a force-recurse
        // from an ancestor). Old value passed as null to bypass the equals() guard.
        pcSupport.firePropertyChange("diagrams", null, getDiagrams());
    }

    public void removeDiagram(PamelaClassDiagram diagram) {
        diagrams.remove(diagram);
        pcSupport.firePropertyChange("diagrams", null, getDiagrams());
    }

    /**
     * Generates a stable, project-unique id for a new diagram: {@code slugify(name)}
     * plus a {@code -N} suffix if that slug is already used by another diagram.
     */
    public String generateDiagramId(String name) {
        String base = PamelaClassDiagramSerializer.slugify(name);
        Set<String> existing = new HashSet<>();
        for (PamelaClassDiagram d : diagrams) {
            if (d.getId() != null) {
                existing.add(d.getId());
            }
        }
        if (!existing.contains(base)) {
            return base;
        }
        int n = 2;
        while (existing.contains(base + "-" + n)) {
            n++;
        }
        return base + "-" + n;
    }

    // -------------------------------------------------------------------------
    // Dirty state and sidecar tracking (diagram lifecycle)
    // -------------------------------------------------------------------------

    /**
     * True when the project has unsaved changes — either diagram changes (the
     * {@link #dirty} flag) or unsaved source-model edits held in the meta-model's
     * dirty buffers (deferred-save; see {@code editable-source-dirty-buffer-design.md}).
     */
    public boolean isDirty() {
        return dirty || (metaModel != null && metaModel.isDirty());
    }

    /** Sets the dirty flag (cleared on save, set on any diagram mutation). */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /** Marks the project as having unsaved changes. */
    public void markDirty() {
        this.dirty = true;
    }

    /** Sidecar file names this project is known to have on disk (for purge-on-save). */
    public Set<String> getKnownSidecarFiles() {
        return knownSidecarFiles;
    }

    /** Replaces the known-sidecar set (called at load and after each save). */
    public void setKnownSidecarFiles(java.util.Collection<String> fileNames) {
        knownSidecarFiles.clear();
        if (fileNames != null) {
            knownSidecarFiles.addAll(fileNames);
        }
    }

    // -------------------------------------------------------------------------
    // Factory / tool factory
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link PamelaClassDiagramFactory} for this project.
     * May be {@code null} if factory creation failed (see constructor log).
     */
    public PamelaClassDiagramFactory getDiagramFactory() {
        return diagramFactory;
    }

    /**
     * Returns the Diana {@link SwingToolFactory} for diagram editors.
     * Injected by {@link PamelaEditorApplication} after the main window is ready.
     */
    public SwingToolFactory getToolFactory() {
        return toolFactory;
    }

    /** Called by {@link PamelaEditorApplication} once the main frame is available. */
    public void setToolFactory(SwingToolFactory toolFactory) {
        this.toolFactory = toolFactory;
    }

    // -------------------------------------------------------------------------
    // Object
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        String name = (metaModel != null && metaModel.getName() != null)
                ? metaModel.getName()
                : (pamelaFile != null ? pamelaFile.getName() : "?");
        return "PamelaProject(" + name + ")";
    }
}
