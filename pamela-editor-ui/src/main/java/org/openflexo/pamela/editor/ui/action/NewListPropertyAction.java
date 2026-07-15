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
 * Adds a new LIST-cardinality property (getter + optional adder/remover/reindexer) to a
 * {@link SourceModelEntity} (C5). The element type is chosen with the Gina
 * {@code TypeSelector} widget (JDK types + PAMELA source entities). See
 * {@link AbstractNewPropertyAction} for the shared mechanics with
 * {@link NewSinglePropertyAction}.
 */
public class NewListPropertyAction extends AbstractNewPropertyAction {

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/NewListPropertyForm.fib");

    private final AccessorOption adderOption = new AccessorOption(Role.ADDER, "Adder");
    private final AccessorOption removerOption = new AccessorOption(Role.REMOVER, "Remover");
    private final AccessorOption reindexerOption = new AccessorOption(Role.REINDEXER, "Reindexer");

    @Override
    public String getLabel() {
        return loc("new_list_property_action");
    }

    @Override
    protected String getDialogTitle() {
        return loc("new_list_property_title");
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected boolean isList() {
        return true;
    }

    @Override
    protected List<Role> siblingRoles() {
        return Arrays.asList(Role.ADDER, Role.REMOVER, Role.REINDEXER);
    }

    @Override
    protected AccessorOption optionFor(Role role) {
        switch (role) {
            case ADDER:     return adderOption;
            case REMOVER:   return removerOption;
            case REINDEXER: return reindexerOption;
            default: throw new IllegalArgumentException(
                    "Not a sibling role of NewListPropertyAction: " + role);
        }
    }

    @Override
    protected void fireOptionsChanged() {
        getPropertyChangeSupport().firePropertyChange("getterOption", null, getGetterOption());
        getPropertyChangeSupport().firePropertyChange("adderOption", null, adderOption);
        getPropertyChangeSupport().firePropertyChange("removerOption", null, removerOption);
        getPropertyChangeSupport().firePropertyChange("reindexerOption", null, reindexerOption);
    }

    // --- bound by NewListPropertyForm.fib -----------------------------------

    public AccessorOption getAdderOption() {
        return adderOption;
    }

    public AccessorOption getRemoverOption() {
        return removerOption;
    }

    public AccessorOption getReindexerOption() {
        return reindexerOption;
    }
}
