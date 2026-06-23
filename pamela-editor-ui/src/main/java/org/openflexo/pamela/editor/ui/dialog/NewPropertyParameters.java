package org.openflexo.pamela.editor.ui.dialog;

import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.Set;

import org.openflexo.pamela.editor.ui.action.ModelEditingSupport;
import org.openflexo.toolbox.HasPropertyChangeSupport;

/**
 * Parameter bean for the New Property dialog: identifier, cardinality
 * ({@code list} = LIST vs SINGLE), and value type.
 *
 * <p>The type is selected from a fixed {@link #getTypeChoices()} list for now
 * (entities + common JDK types); a dedicated free-text TypeSelector widget is a
 * follow-up. {@link #isInputValid()} requires a legal, unused identifier and a
 * selected type.</p>
 */
public class NewPropertyParameters implements HasPropertyChangeSupport {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private String identifier;
    private boolean list;
    private String type;
    private final List<String> typeChoices;
    private final Set<String> existingIdentifiers;

    public NewPropertyParameters(List<String> typeChoices, Set<String> existingIdentifiers) {
        this.typeChoices = typeChoices;
        this.existingIdentifiers = existingIdentifiers;
        this.type = typeChoices.isEmpty() ? null : typeChoices.get(0);
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        String old = this.identifier;
        this.identifier = identifier;
        pcs.firePropertyChange("identifier", old, identifier);
        pcs.firePropertyChange("inputValid", null, isInputValid());
    }

    public boolean getList() {
        return list;
    }

    public void setList(boolean list) {
        boolean old = this.list;
        this.list = list;
        pcs.firePropertyChange("list", old, list);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        String old = this.type;
        this.type = type;
        pcs.firePropertyChange("type", old, type);
        pcs.firePropertyChange("inputValid", null, isInputValid());
    }

    public List<String> getTypeChoices() {
        return typeChoices;
    }

    /** Bound to the OK button {@code enable}. */
    public boolean isInputValid() {
        String id = identifier == null ? "" : identifier.trim();
        return ModelEditingSupport.isValidJavaIdentifier(id)
                && !existingIdentifiers.contains(id)
                && type != null && !type.isEmpty();
    }

    public String getTrimmedIdentifier() {
        return identifier == null ? "" : identifier.trim();
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
