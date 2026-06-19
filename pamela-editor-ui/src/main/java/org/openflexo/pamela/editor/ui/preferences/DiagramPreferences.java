package org.openflexo.pamela.editor.ui.preferences;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;

/** Class-diagram preferences theme ({@code /diagram}). */
@ModelEntity
@ImplementationClass(DiagramPreferences.DiagramPreferencesImpl.class)
public interface DiagramPreferences extends PreferencesNode {

    String DEFAULT_RELATIONSHIP_STYLE = "defaultRelationshipStyleId";
    String DEFAULT_INHERITANCE_STYLE = "defaultInheritanceStyleId";
    String SHOW_GRID = "showGrid";

    @Getter(value = DEFAULT_RELATIONSHIP_STYLE, defaultValue = "relationship-rect-polylin")
    String getDefaultRelationshipStyleId();

    @Setter(DEFAULT_RELATIONSHIP_STYLE)
    void setDefaultRelationshipStyleId(String id);

    @Getter(value = DEFAULT_INHERITANCE_STYLE, defaultValue = "inheritance-line")
    String getDefaultInheritanceStyleId();

    @Setter(DEFAULT_INHERITANCE_STYLE)
    void setDefaultInheritanceStyleId(String id);

    @Getter(value = SHOW_GRID, defaultValue = "false")
    boolean getShowGrid();

    @Setter(SHOW_GRID)
    void setShowGrid(boolean showGrid);

    abstract class DiagramPreferencesImpl extends PreferencesNodeImpl implements DiagramPreferences {
    }
}
