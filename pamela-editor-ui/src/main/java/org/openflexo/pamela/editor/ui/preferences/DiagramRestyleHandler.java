package org.openflexo.pamela.editor.ui.preferences;

/**
 * Hook the preferences dialog uses to offer restyling open diagrams after the user applies new
 * style defaults (see {@code preferences-design.md §6bis}, step C). Implemented by the application;
 * keeps {@link PreferencesDialog} decoupled from the diagram/UI layer.
 */
public interface DiagramRestyleHandler {

    /** Whether at least one class diagram is currently open (so a restyle prompt is relevant). */
    boolean hasOpenDiagrams();

    /**
     * Copies the current preference style defaults into every open diagram's embedded styles,
     * refreshes their drawings, and marks the affected projects dirty (persisted on next save).
     */
    void restyleOpenDiagrams();
}
