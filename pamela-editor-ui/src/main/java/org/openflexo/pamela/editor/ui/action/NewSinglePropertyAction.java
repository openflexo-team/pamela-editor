/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor.
 *
 */

package org.openflexo.pamela.editor.ui.action;

import java.util.Arrays;
import java.util.List;

import org.openflexo.pamela.editor.model.AccessorSpec.Role;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Adds a new SINGLE-cardinality property (getter + optional setter/updater) to a
 * {@link SourceModelEntity} (C4). The value type is chosen with the Gina
 * {@code TypeSelector} widget (JDK types + PAMELA source entities). See
 * {@link AbstractNewPropertyAction} for the shared mechanics with
 * {@link NewListPropertyAction}.
 */
public class NewSinglePropertyAction extends AbstractNewPropertyAction {

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/NewSinglePropertyForm.fib");

    private final AccessorOption setterOption = new AccessorOption(Role.SETTER, "Setter");
    private final AccessorOption updaterOption = new AccessorOption(Role.UPDATER, "Updater");

    @Override
    public String getLabel() {
        return loc("new_single_property_action");
    }

    @Override
    protected String getDialogTitle() {
        return loc("new_single_property_title");
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected boolean isList() {
        return false;
    }

    @Override
    protected List<Role> siblingRoles() {
        return Arrays.asList(Role.SETTER, Role.UPDATER);
    }

    @Override
    protected AccessorOption optionFor(Role role) {
        switch (role) {
            case SETTER:  return setterOption;
            case UPDATER: return updaterOption;
            default: throw new IllegalArgumentException(
                    "Not a sibling role of NewSinglePropertyAction: " + role);
        }
    }

    @Override
    protected void fireOptionsChanged() {
        getPropertyChangeSupport().firePropertyChange("getterOption", null, getGetterOption());
        getPropertyChangeSupport().firePropertyChange("setterOption", null, setterOption);
        getPropertyChangeSupport().firePropertyChange("updaterOption", null, updaterOption);
    }

    // --- bound by NewSinglePropertyForm.fib ---------------------------------

    public AccessorOption getSetterOption() {
        return setterOption;
    }

    public AccessorOption getUpdaterOption() {
        return updaterOption;
    }
}
