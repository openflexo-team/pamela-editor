package org.openflexo.pamela.editor.ui.preferences;

import java.awt.Rectangle;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;

/**
 * Window-geometry preferences ({@code /general/window}). Absorbs the frame / inspector /
 * palette bounds formerly stored through {@code java.util.prefs} (see
 * {@code preferences-design.md §1.2, §5.1}).
 *
 * <p>Bounds are {@link Rectangle} SINGLE properties; the {@link PreferencesFactory}
 * registers {@code AWTRectangleConverter} so they are string-convertible for JSON
 * serialization. No PAMELA {@code defaultValue} is declared (the {@code java.awt.Rectangle}
 * converter is not in the global default library); callers supply a default at read time.</p>
 */
@ModelEntity
@ImplementationClass(WindowPreferences.WindowPreferencesImpl.class)
public interface WindowPreferences extends PreferencesNode {

    String FRAME_BOUNDS = "frameBounds";
    String INSPECTOR_BOUNDS = "inspectorBounds";
    String PALETTE_BOUNDS = "paletteBounds";

    @Getter(value = FRAME_BOUNDS, ignoreType = true)
    Rectangle getFrameBounds();

    @Setter(FRAME_BOUNDS)
    void setFrameBounds(Rectangle bounds);

    @Getter(value = INSPECTOR_BOUNDS, ignoreType = true)
    Rectangle getInspectorBounds();

    @Setter(INSPECTOR_BOUNDS)
    void setInspectorBounds(Rectangle bounds);

    @Getter(value = PALETTE_BOUNDS, ignoreType = true)
    Rectangle getPaletteBounds();

    @Setter(PALETTE_BOUNDS)
    void setPaletteBounds(Rectangle bounds);

    abstract class WindowPreferencesImpl extends PreferencesNodeImpl implements WindowPreferences {
    }
}
