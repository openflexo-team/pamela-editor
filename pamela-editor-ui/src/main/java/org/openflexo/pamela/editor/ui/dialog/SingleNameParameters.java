package org.openflexo.pamela.editor.ui.dialog;

import java.beans.PropertyChangeSupport;
import java.util.Set;

import org.openflexo.pamela.editor.ui.action.ModelEditingSupport;
import org.openflexo.toolbox.HasPropertyChangeSupport;

/**
 * Parameter bean for a single-name dialog (New Entity, Rename Entity).
 *
 * <p>{@link #isInputValid()} drives the OK button's {@code enable} binding: the
 * name must be a legal Java identifier and not collide with a forbidden name
 * (existing sibling type names; for a rename the current name is excluded from
 * the forbidden set so "unchanged" is still valid input).</p>
 */
public class SingleNameParameters implements HasPropertyChangeSupport {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private String name;
    private final String promptLabel;
    private final Set<String> forbidden;

    public SingleNameParameters(String promptLabel, String initialName, Set<String> forbidden) {
        this.promptLabel = promptLabel;
        this.name = initialName;
        this.forbidden = forbidden;
    }

    public String getPromptLabel() {
        return promptLabel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        String old = this.name;
        this.name = name;
        pcs.firePropertyChange("name", old, name);
        pcs.firePropertyChange("inputValid", null, isInputValid());
    }

    /** Bound to the OK button {@code enable}. */
    public boolean isInputValid() {
        String n = name == null ? "" : name.trim();
        return ModelEditingSupport.isValidJavaIdentifier(n) && !forbidden.contains(n);
    }

    /** Trimmed name, for the caller after validation. */
    public String getTrimmedName() {
        return name == null ? "" : name.trim();
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
