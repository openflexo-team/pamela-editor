package org.openflexo.pamela.editor.ui.widget;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;

/**
 * Shared click / right-click behaviour for the "entities table" block reused across
 * {@link PackageSummaryView}, {@link MetaModelSummaryView} and {@link SourceFolderSummaryView}:
 * clicking a row soft-selects the entity (updates the inspector without leaving the current
 * summary view, ui-design.md §18.2) and right-clicking it opens the same shared contextual menu
 * as the browsers and the diagram (ui-design.md §18.4).
 */
final class EntityTableSupport {

    private EntityTableSupport() {
    }

    static void selectEntity(PamelaEditorApplication application, Object selected) {
        if (application == null || !(selected instanceof SourceModelEntity)) {
            return;
        }
        application.setCurrentSelectedElement(selected, false);
    }

    static void rightClick(PamelaEditorApplication application, Object selected, Object event) {
        if (application == null || !(selected instanceof SourceModelEntity)) {
            return;
        }
        List<Object> facets = Collections.singletonList(selected);
        if (event instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) event;
            application.showContextualMenuFor(facets, me.getComponent(), me.getX(), me.getY());
        } else {
            Point p = MouseInfo.getPointerInfo().getLocation();
            application.showContextualMenuFor(facets, null, p.x, p.y);
        }
    }
}
