package org.openflexo.pamela.editor.ui.dialog;

import java.beans.PropertyChangeSupport;
import java.util.List;

import org.openflexo.toolbox.HasPropertyChangeSupport;

/**
 * Parameter bean for a "pick one value from a list" dialog (Add / Remove
 * super-entity). The choices populate a {@code <DropDown list="data.choices">};
 * {@link #isInputValid()} requires a selection.
 */
public class PickFromListParameters implements HasPropertyChangeSupport {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final String promptLabel;
    private final List<String> choices;
    private String selected;

    public PickFromListParameters(String promptLabel, List<String> choices) {
        this.promptLabel = promptLabel;
        this.choices = choices;
        this.selected = choices.isEmpty() ? null : choices.get(0);
    }

    public String getPromptLabel() {
        return promptLabel;
    }

    public List<String> getChoices() {
        return choices;
    }

    public String getSelected() {
        return selected;
    }

    public void setSelected(String selected) {
        String old = this.selected;
        this.selected = selected;
        pcs.firePropertyChange("selected", old, selected);
        pcs.firePropertyChange("inputValid", null, isInputValid());
    }

    /** Bound to the OK button {@code enable}. */
    public boolean isInputValid() {
        return selected != null && !selected.isEmpty();
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
