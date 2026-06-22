package org.openflexo.pamela.editor.ui.preferences;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;

/**
 * Container theme for class-diagram visual design ({@code /classDiagramDesign}). Holds the
 * {@link EntityStylePreferences} and {@link ConnectorStylePreferences} sub-themes, plus a few
 * diagram-wide toggles.
 */
@ModelEntity
@ImplementationClass(ClassDiagramDesignPreferences.ClassDiagramDesignPreferencesImpl.class)
public interface ClassDiagramDesignPreferences extends PreferencesNode {

    String SHOW_GRID = "showGrid";

    @Getter(value = SHOW_GRID, defaultValue = "false")
    boolean getShowGrid();

    @Setter(SHOW_GRID)
    void setShowGrid(boolean showGrid);

    abstract class ClassDiagramDesignPreferencesImpl extends PreferencesNodeImpl
            implements ClassDiagramDesignPreferences {
    }
}
