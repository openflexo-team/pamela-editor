/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor.
 *
 */

package org.openflexo.pamela.editor.ui.action;

import java.beans.PropertyChangeSupport;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openflexo.pamela.annotations.Adder;
import org.openflexo.pamela.annotations.Reindexer;
import org.openflexo.pamela.annotations.Remover;
import org.openflexo.pamela.annotations.Setter;
import org.openflexo.pamela.annotations.Updater;
import org.openflexo.pamela.editor.model.AccessorSpec;
import org.openflexo.pamela.editor.ui.widget.SpoonOutlineController;
import org.openflexo.toolbox.HasPropertyChangeSupport;

import spoon.reflect.declaration.CtMethod;

/**
 * One accessor role in a property-creation/promotion dialog. The row offers, explicitly:
 * <ol>
 *   <li>a <b>checkbox</b> — whether this accessor is handled at all;</li>
 *   <li>a <b>mode dropdown</b> — {@link Mode#GENERATE generate a new method} or
 *       {@link Mode#ATTACH attach an existing one};</li>
 *   <li>in {@code GENERATE} mode, an editable <b>method-name</b> field (defaulted, modifiable);
 *       in {@code ATTACH} mode, a <b>method selector</b> restricted to signature-compatible
 *       methods ({@link #candidates});</li>
 *   <li>a one-line <b>summary</b> recapping what the row will do.</li>
 * </ol>
 * Shared by the New-property / Promote-getter / Promote-method dialogs (model-editing-design.md §3.3).
 */
public final class AccessorOption implements HasPropertyChangeSupport {

    /** How the accessor is realised. */
    public enum Mode {
        GENERATE("Generate new method"),
        ATTACH("Attach existing method");

        private final String label;
        Mode(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final AccessorSpec.Role role;
    private final String label;

    // Configuration (set by the owning action when preparing the dialog).
    private boolean show;
    private boolean mandatory;        // getter: always included, checkbox disabled
    private boolean allowGenerate = true;
    private List<CtMethod<?>> candidates = Collections.emptyList();

    // State (edited in the dialog).
    private boolean include;
    private Mode mode = Mode.GENERATE;
    private String generatedName = "";
    private CtMethod<?> method;       // selected existing method (ATTACH mode)

    private Runnable changeCallback;  // optional: notified on any state change (OK-button refresh)

    public AccessorOption(AccessorSpec.Role role, String label) {
        this.role = role;
        this.label = label;
    }

    /**
     * Configures this option for a dialog row.
     *
     * @param show           whether the row is visible (cardinality-dependent)
     * @param mandatory      always included, not toggleable (the getter)
     * @param allowGenerate  whether GENERATE is offered (normally {@code true})
     * @param candidates     existing signature-matching methods offered in ATTACH mode
     * @param generatedName  default (editable) method name for GENERATE mode
     * @param defaultMethod  pre-selected existing method → starts in ATTACH mode; {@code null} → GENERATE
     * @param defaultInclude initial checkbox state
     */
    public void configure(boolean show, boolean mandatory, boolean allowGenerate,
            List<CtMethod<?>> candidates, String generatedName,
            CtMethod<?> defaultMethod, boolean defaultInclude) {
        this.show = show;
        this.mandatory = mandatory;
        this.allowGenerate = allowGenerate;
        this.candidates = candidates != null ? candidates : Collections.emptyList();
        this.generatedName = generatedName != null ? generatedName : "";
        this.method = defaultMethod;
        this.mode = (defaultMethod != null) ? Mode.ATTACH : Mode.GENERATE;
        this.include = mandatory || defaultInclude;
        fireAll();
    }

    /** The effective accessor to materialise, or {@code null} when not included / incomplete. */
    public AccessorSpec toSpec() {
        if (!include) {
            return null;
        }
        if (mode == Mode.ATTACH) {
            return method != null ? AccessorSpec.attach(role, method.getSimpleName()) : null;
        }
        return generatedName.trim().isEmpty() ? null
                : AccessorSpec.generate(role, generatedName.trim());
    }

    /** {@code true} when the row is either not handled, or fully specified for its mode. */
    public boolean isComplete() {
        if (!include) {
            return true;
        }
        return mode == Mode.ATTACH ? method != null : !generatedName.trim().isEmpty();
    }

    // --- bound by the dialog FIB --------------------------------------------

    public AccessorSpec.Role getRole() { return role; }
    public String getLabel()      { return label; }
    public boolean isShow()       { return show; }
    public boolean isMandatory()  { return mandatory; }
    /** Checkbox enabled only when the role is not mandatory. */
    public boolean isToggleable() { return !mandatory; }
    public List<CtMethod<?>> getCandidates() { return candidates; }

    /** The modes offered by the dropdown: GENERATE (if allowed) and ATTACH (if candidates exist). */
    public List<Mode> getAvailableModes() {
        List<Mode> modes = new ArrayList<>();
        if (allowGenerate) {
            modes.add(Mode.GENERATE);
        }
        if (!candidates.isEmpty()) {
            modes.add(Mode.ATTACH);
        }
        if (modes.isEmpty()) {
            modes.add(Mode.GENERATE);
        }
        return modes;
    }

    public boolean getInclude() { return include; }
    public void setInclude(boolean include) {
        if (mandatory) {
            return; // always included
        }
        this.include = include;
        pcs.firePropertyChange("include", null, include);
        changed();
    }

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) {
        this.mode = mode != null ? mode : Mode.GENERATE;
        pcs.firePropertyChange("mode", null, this.mode);
        pcs.firePropertyChange("generateMode", null, isGenerateMode());
        pcs.firePropertyChange("attachMode", null, isAttachMode());
        changed();
    }

    /** Row-visibility helpers for the name field vs the selector. */
    public boolean isGenerateMode() { return mode == Mode.GENERATE; }
    public boolean isAttachMode()   { return mode == Mode.ATTACH; }

    public String getGeneratedName() { return generatedName; }
    public void setGeneratedName(String generatedName) {
        this.generatedName = generatedName != null ? generatedName : "";
        pcs.firePropertyChange("generatedName", null, this.generatedName);
        changed();
    }

    public CtMethod<?> getMethod() { return method; }
    public void setMethod(CtMethod<?> method) {
        this.method = method;
        pcs.firePropertyChange("method", null, method);
        changed();
    }

    /** Updates the attach-candidate set (preserving include/mode/name state). */
    public void setCandidates(List<CtMethod<?>> candidates) {
        this.candidates = candidates != null ? candidates : Collections.emptyList();
        pcs.firePropertyChange("candidates", null, this.candidates);
        pcs.firePropertyChange("availableModes", null, getAvailableModes());
        changed();
    }

    /** Optional callback fired on any state change (used to refresh the dialog's OK button). */
    public void setChangeCallback(Runnable changeCallback) {
        this.changeCallback = changeCallback;
    }

    /** One-line recap of what this row will do (shown small + italic under the row). */
    public String getSummary() {
        if (!include) {
            return "Not handled.";
        }
        if (mode == Mode.ATTACH) {
            if (method == null) {
                return "Choose an existing method to attach.";
            }
            String l = SpoonOutlineController.labelFor(method);
            return "Annotate existing method " + (l != null ? l : method.getSimpleName()) + ".";
        }
        if (generatedName.trim().isEmpty()) {
            return "Enter a method name to generate.";
        }
        return "Generate method " + generatedName.trim()
                + (role == AccessorSpec.Role.GETTER ? "()." : "(…).");
    }

    private void changed() {
        pcs.firePropertyChange("summary", null, getSummary());
        if (changeCallback != null) {
            changeCallback.run();
        }
    }

    private void fireAll() {
        pcs.firePropertyChange("show", null, show);
        pcs.firePropertyChange("include", null, include);
        pcs.firePropertyChange("mode", null, mode);
        pcs.firePropertyChange("generateMode", null, isGenerateMode());
        pcs.firePropertyChange("attachMode", null, isAttachMode());
        pcs.firePropertyChange("generatedName", null, generatedName);
        pcs.firePropertyChange("method", null, method);
        pcs.firePropertyChange("candidates", null, candidates);
        pcs.firePropertyChange("availableModes", null, getAvailableModes());
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

    // --- static helpers shared by the promote/new-property actions -----------

    /** The PAMELA annotation class backing a secondary accessor role. */
    public static Class<? extends Annotation> annotationFor(AccessorSpec.Role role) {
        switch (role) {
            case SETTER:    return Setter.class;
            case UPDATER:   return Updater.class;
            case ADDER:     return Adder.class;
            case REMOVER:   return Remover.class;
            case REINDEXER: return Reindexer.class;
            default:        throw new IllegalArgumentException("No accessor annotation for " + role);
        }
    }

    /**
     * Configures a sibling option for a <em>promote</em> (refining an existing entity). Both modes
     * are offered (generate or attach); shown only when candidates exist or generation is desired.
     * Defaults to ATTACH on a name match among {@code preferredNames}, else GENERATE.
     */
    public static void configureSibling(AccessorOption opt, boolean show,
            List<CtMethod<?>> candidates, String... preferredNames) {
        CtMethod<?> nameMatch = null;
        for (String preferred : preferredNames) {
            for (CtMethod<?> m : candidates) {
                if (m.getSimpleName().equals(preferred)) {
                    nameMatch = m;
                    break;
                }
            }
            if (nameMatch != null) {
                break;
            }
        }
        String defaultName = preferredNames.length > 0 ? preferredNames[0] : "";
        // Default-include + default-attach on a confident name match; otherwise opt-in.
        opt.configure(show, false, true, candidates, defaultName, nameMatch, nameMatch != null);
    }
}
