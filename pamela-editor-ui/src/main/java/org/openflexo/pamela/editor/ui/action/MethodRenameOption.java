/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor.
 *
 */

package org.openflexo.pamela.editor.ui.action;

import java.beans.PropertyChangeSupport;

import org.openflexo.pamela.editor.model.AccessorSpec.Role;
import org.openflexo.toolbox.HasPropertyChangeSupport;

/**
 * One accessor row of the "Rename property" dialog. Each present accessor of the
 * renamed property (getter/setter/adder/remover/reindexer/updater) offers:
 * <ol>
 *   <li>a <b>checkbox</b> — whether the accessor <em>method</em> is also renamed;</li>
 *   <li>an editable <b>method-name</b> field — the new method name (defaulted to the
 *       convention derived from the new property identifier, modifiable).</li>
 * </ol>
 * The PAMELA key itself is always updated on every accessor regardless of these
 * choices; this row only governs whether/how the <em>method name</em> changes.
 * See {@link RenamePropertyAction} and model-editing-design.md §3.2.
 */
public final class MethodRenameOption implements HasPropertyChangeSupport {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final Role role;
    private final String label;

    private boolean present;       // whether this accessor exists on the property (row visible)
    private String currentName;    // the accessor's current method name (for the summary)
    private boolean rename;        // checkbox: also rename the method?
    private String newName = "";   // the new method name (conventional default, editable)

    private Runnable changeCallback;  // optional: notified on any state change (OK-button refresh)

    public MethodRenameOption(Role role, String label) {
        this.role = role;
        this.label = label;
    }

    /**
     * Configures this row from a present accessor.
     *
     * @param currentName  the accessor's current method name
     * @param defaultName  the conventional new method name (from the new identifier)
     * @param renameByDefault whether the checkbox starts checked
     */
    public void configurePresent(String currentName, String defaultName, boolean renameByDefault) {
        this.present = true;
        this.currentName = currentName;
        this.rename = renameByDefault;
        this.newName = defaultName;
        fireAll();
    }

    /** Marks this row as not applicable (accessor absent) — the row is hidden. */
    public void configureAbsent() {
        this.present = false;
        this.currentName = null;
        this.rename = false;
        this.newName = "";
        fireAll();
    }

    /** Updates the default new name (on identifier change), preserving the checkbox choice. */
    public void setDefaultName(String defaultName) {
        setNewName(defaultName);
    }

    public Role getRole() {
        return role;
    }

    /** {@code true} once the row is complete enough to validate the dialog. */
    public boolean isComplete() {
        if (!present || !rename) {
            return true; // nothing to rename, or accessor absent → not blocking
        }
        return ModelEditingSupport.isValidJavaIdentifier(getTrimmedNewName());
    }

    /** The new method name if the user opted to rename, else {@code null}. */
    public String toRenameName() {
        if (present && rename) {
            return getTrimmedNewName();
        }
        return null;
    }

    void setChangeCallback(Runnable changeCallback) {
        this.changeCallback = changeCallback;
    }

    private void notifyChange() {
        if (changeCallback != null) {
            changeCallback.run();
        }
    }

    private String getTrimmedNewName() {
        return newName == null ? "" : newName.trim();
    }

    // --- bound by RenamePropertyForm.fib ------------------------------------

    public String getLabel() {
        return label;
    }

    public boolean isPresent() {
        return present;
    }

    public boolean isRename() {
        return rename;
    }

    public void setRename(boolean rename) {
        boolean old = this.rename;
        this.rename = rename;
        pcs.firePropertyChange("rename", old, rename);
        pcs.firePropertyChange("summary", null, getSummary());
        notifyChange();
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        String old = this.newName;
        this.newName = newName;
        pcs.firePropertyChange("newName", old, newName);
        pcs.firePropertyChange("summary", null, getSummary());
        notifyChange();
    }

    /** One-line italic recap of what the row will do. */
    public String getSummary() {
        if (!present) {
            return "";
        }
        if (!rename) {
            return "Keep method ‘" + currentName + "’ (only the PAMELA key changes).";
        }
        return "Rename method ‘" + currentName + "’ → ‘" + getTrimmedNewName() + "’.";
    }

    private void fireAll() {
        pcs.firePropertyChange("present", null, present);
        pcs.firePropertyChange("rename", null, rename);
        pcs.firePropertyChange("newName", null, newName);
        pcs.firePropertyChange("summary", null, getSummary());
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
