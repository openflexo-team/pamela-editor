package org.openflexo.pamela.editor.ui.widget;

import java.awt.CardLayout;

import javax.swing.JPanel;

import org.openflexo.pamela.editor.diagram.EntityView;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.model.SourceJavaFile;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Context-sensitive complementary panel shown in the bottom-right column
 * (ui-design.md §19).
 *
 * <p>Three visible modes (plus an empty fallback):</p>
 * <ul>
 *   <li><strong>Statistics</strong> — when the active central view is
 *       {@link SourceMetaModel} or {@link SourcePackage}</li>
 *   <li><strong>Spoon Outline</strong> — when the active central view is
 *       {@link SourceModelEntity} or {@link SourceJavaFile}</li>
 *   <li><strong>Diagram inspector</strong> — when the active central view is
 *       a {@link PamelaClassDiagram}</li>
 * </ul>
 *
 * <p>In diagram mode the panel updates again (without switching cards) when
 * the user selects a shape on the canvas ({@link #setDiagramSelection}).</p>
 */
@SuppressWarnings("serial")
public class ContextPanel extends JPanel {

    private static final String CARD_EMPTY     = "empty";
    private static final String CARD_STATISTICS = "statistics";
    private static final String CARD_OUTLINE    = "outline";
    private static final String CARD_DIAGRAM    = "diagram";

    private final CardLayout cards = new CardLayout();

    private final StatisticsContextView       statisticsView;
    private final SpoonOutlineView            outlineView;
    private final DiagramElementInspectorView diagramView;

    private final PamelaEditorApplication application;

    /** The element currently driving the active mode (for soft-selection updates). */
    private Object activeElement;

    public ContextPanel(PamelaEditorApplication app) {
        this.application = app;
        setLayout(cards);

        add(new JPanel(), CARD_EMPTY);

        statisticsView = new StatisticsContextView();
        add(statisticsView, CARD_STATISTICS);

        outlineView = new SpoonOutlineView(app);
        add(outlineView, CARD_OUTLINE);

        diagramView = new DiagramElementInspectorView();
        add(diagramView, CARD_DIAGRAM);

        cards.show(this, CARD_EMPTY);
    }

    // -------------------------------------------------------------------------
    // Primary routing — called by showViewForElement() when central view changes
    // -------------------------------------------------------------------------

    /**
     * Routes to the appropriate panel for the given element, matching the central
     * view switch (ui-design.md §19, routing table).
     */
    public void showFor(Object element) {
        activeElement = element;

        if (element instanceof PamelaProject) {
            statisticsView.showFor(((PamelaProject) element).getMetaModel());
            cards.show(this, CARD_STATISTICS);

        } else if (element instanceof SourceMetaModel) {
            statisticsView.showFor((SourceMetaModel) element);
            cards.show(this, CARD_STATISTICS);

        } else if (element instanceof SourcePackage) {
            statisticsView.showFor((SourcePackage) element);
            cards.show(this, CARD_STATISTICS);

        } else if (element instanceof SourceModelEntity) {
            outlineView.showEntity((SourceModelEntity) element);
            cards.show(this, CARD_OUTLINE);

        } else if (element instanceof SourceJavaFile) {
            SourceMetaModel mm = application.getActiveMetaModel();
            outlineView.showJavaFile((SourceJavaFile) element, mm != null ? mm : findMetaModelFor((SourceJavaFile) element));
            cards.show(this, CARD_OUTLINE);

        } else if (element instanceof PamelaClassDiagram) {
            diagramView.showDiagram((PamelaClassDiagram) element);
            cards.show(this, CARD_DIAGRAM);

        } else {
            outlineView.clear();
            statisticsView.clear();
            diagramView.clear();
            cards.show(this, CARD_EMPTY);
        }
    }

    // -------------------------------------------------------------------------
    // Property selection → outline highlight
    // -------------------------------------------------------------------------

    /**
     * Selects the methods of {@code prop} in the SpoonOutlineView, if the outline
     * card is currently active (i.e. a {@link SourceModelEntity} source-code view is
     * shown in the centre).
     *
     * <p>Called from
     * {@link org.openflexo.pamela.editor.ui.PamelaEditorApplication#openOrSwitchCentralView}
     * after the property highlight has been applied to the source-code view.</p>
     */
    public void selectPropertyMethods(SourceModelProperty prop) {
        // The outline is active when the central view shows a SourceModelEntity
        if (!(activeElement instanceof SourceModelEntity)) return;
        outlineView.selectMethodsForProperty(prop);
    }

    // -------------------------------------------------------------------------
    // Soft-selection update (diagram canvas click, no central-view switch)
    // -------------------------------------------------------------------------

    /**
     * Called when the user clicks a shape on the active class diagram
     * ({@link PamelaEditorApplication#setCurrentSelectedElement(Object, boolean)} with
     * {@code navigateCentralView = false}).
     *
     * <p>Only has effect while the diagram card is active.  The diagram selection
     * mapper ({@code PamelaClassDiagramDrawing.modelElementFor}) delivers:
     * <ul>
     *   <li>{@link SourceModelEntity} — resolved entity box</li>
     *   <li>{@link EntityView}         — unresolved entity box</li>
     *   <li>{@link SourceModelProperty} / initializer / method — compartment row</li>
     *   <li>{@link PamelaClassDiagram} — background / empty selection</li>
     * </ul>
     * For a {@code SourceModelEntity} or its sub-elements the corresponding
     * {@code EntityView} is looked up in the active diagram so the geometry editor
     * can be populated.</p>
     */
    public void setDiagramSelection(Object element) {
        // Only act when the diagram card is currently visible
        if (!(activeElement instanceof PamelaClassDiagram)) return;
        PamelaClassDiagram diagram = (PamelaClassDiagram) activeElement;

        if (element instanceof EntityView) {
            // Unresolved entity box — pass directly
            diagramView.showEntityView((EntityView) element);

        } else if (element instanceof SourceModelEntity) {
            // Resolved entity box: find the EntityView whose entity is this one
            EntityView ev = findEntityViewFor((SourceModelEntity) element, diagram);
            if (ev != null) {
                diagramView.showEntityView(ev);
            } else {
                diagramView.showDiagram(diagram);
            }

        } else if (element instanceof SourceModelProperty) {
            // Compartment row for a property: show the parent entity's EntityView
            SourceModelEntity entity = ((SourceModelProperty) element).getModelEntity();
            EntityView ev = (entity != null) ? findEntityViewFor(entity, diagram) : null;
            if (ev != null) {
                diagramView.showEntityView(ev);
            } else {
                diagramView.showDiagram(diagram);
            }

        } else {
            // Background click, empty selection, or any other element → diagram info
            diagramView.showDiagram(diagram);
        }
    }

    /** Finds the {@link EntityView} in {@code diagram} whose resolved entity is {@code entity}. */
    private EntityView findEntityViewFor(SourceModelEntity entity, PamelaClassDiagram diagram) {
        if (entity == null) return null;
        for (EntityView ev : diagram.getEntityViews()) {
            if (ev.getEntity() == entity) {
                return ev;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private SourceMetaModel findMetaModelFor(SourceJavaFile file) {
        for (PamelaProject project : application.getProjects()) {
            SourceMetaModel mm = project.getMetaModel();
            if (mm != null) {
                for (SourcePackage pkg : mm.getAllPackages()) {
                    if (pkg.getJavaFiles().contains(file)) {
                        return mm;
                    }
                }
            }
        }
        return null;
    }
}
