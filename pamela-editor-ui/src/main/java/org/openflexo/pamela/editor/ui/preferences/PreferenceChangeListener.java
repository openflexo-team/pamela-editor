package org.openflexo.pamela.editor.ui.preferences;

/**
 * Notified when a preference value changes (immediate-apply model, decision D4).
 * Lets the application react live — apply look-and-feel/language, refresh the
 * <i>Open Recent</i> menu, etc.
 */
@FunctionalInterface
public interface PreferenceChangeListener {

    /**
     * @param node     the theme node whose property changed
     * @param key      the property identifier
     * @param oldValue previous value (may be {@code null})
     * @param newValue new value (may be {@code null})
     */
    void preferenceChanged(PreferencesNode node, String key, Object oldValue, Object newValue);
}
