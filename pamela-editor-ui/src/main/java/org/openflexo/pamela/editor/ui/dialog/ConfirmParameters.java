package org.openflexo.pamela.editor.ui.dialog;

import java.beans.PropertyChangeSupport;

import org.openflexo.toolbox.HasPropertyChangeSupport;

/**
 * Parameter bean for a yes/no confirmation dialog. Carries the (already
 * composed, possibly multi-line) message shown to the user. The dialog's
 * buttons call {@code controller.chooseYesAndDispose()} /
 * {@code chooseNoAndDispose()}.
 */
public class ConfirmParameters implements HasPropertyChangeSupport {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final String message;
    private final String confirmLabel;

    public ConfirmParameters(String message, String confirmLabel) {
        this.message = message;
        this.confirmLabel = confirmLabel;
    }

    /**
     * The message as HTML so a Swing {@code JLabel} renders multi-line text
     * (newlines become {@code <br>}). Basic {@code <}/{@code >}/{@code &}
     * escaping is applied.
     */
    public String getMessage() {
        String escaped = message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        return "<html>" + escaped + "</html>";
    }

    /** Label of the affirmative button (e.g. "Delete"). */
    public String getConfirmLabel() {
        return confirmLabel;
    }

    @Override
    public PropertyChangeSupport getPropertyChangeSupport() {
        return pcs;
    }

    @Override
    public String getDeletedProperty() {
        return null;
    }
}
