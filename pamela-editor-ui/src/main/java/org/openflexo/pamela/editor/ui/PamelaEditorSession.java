package org.openflexo.pamela.editor.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.openflexo.diana.swing.control.SwingToolFactory;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagramFactory;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.exceptions.ModelDefinitionException;
import org.openflexo.pamela.factory.EditingContextImpl;

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
 *   <li>{@link #diagrams} — loaded from sidecar {@code .diagram.json} files</li>
 *   <li>{@link #diagramFactory} — PAMELA factory for diagram model objects (one per session,
 *       owns the session's {@link EditingContextImpl})</li>
 * </ul>
 * </p>
 */
public class PamelaEditorSession {

    private static final Logger logger =
            Logger.getLogger(PamelaEditorSession.class.getPackage().getName());

    private final File pamelaFile;
    private final SourceMetaModel metaModel;
    private final List<PamelaClassDiagram> diagrams;

    /** PAMELA model factory for this session's diagrams. */
    private final PamelaClassDiagramFactory diagramFactory;

    /**
     * Diana tool factory shared with the application.
     * Injected via {@link #setToolFactory(SwingToolFactory)} once available.
     */
    private SwingToolFactory toolFactory;

    public PamelaEditorSession(File pamelaFile, SourceMetaModel metaModel) {
        this.pamelaFile = pamelaFile;
        this.metaModel = metaModel;
        this.diagrams = new ArrayList<>();

        // Create a dedicated editing context and diagram factory for this session
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

    /** The fully built {@link SourceMetaModel} for this project. */
    public SourceMetaModel getMetaModel() {
        return metaModel;
    }

    /**
     * The class diagrams belonging to this session (loaded from sidecar files).
     *
     * @return an unmodifiable view
     */
    public List<PamelaClassDiagram> getDiagrams() {
        return Collections.unmodifiableList(diagrams);
    }

    public void addDiagram(PamelaClassDiagram diagram) {
        diagrams.add(diagram);
    }

    public void removeDiagram(PamelaClassDiagram diagram) {
        diagrams.remove(diagram);
    }

    // -------------------------------------------------------------------------
    // Factory / tool factory
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link PamelaClassDiagramFactory} for this session.
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
        return "PamelaEditorSession(" + name + ")";
    }
}
