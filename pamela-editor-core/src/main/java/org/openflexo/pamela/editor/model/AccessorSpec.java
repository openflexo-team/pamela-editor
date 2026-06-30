package org.openflexo.pamela.editor.model;

/**
 * Describes one PAMELA accessor to materialise when {@link SourceModelEntity#createProperty
 * creating a property}: a role (getter / setter / updater / adder / remover / reindexer) plus
 * <em>how</em> it is realised — either <b>generate</b> a fresh conventional method, or
 * <b>attach</b> the PAMELA annotation to an existing developer-written method.
 *
 * <p>This is the explicit, per-role unit the UI presents ("Generate {@code setName(String)}" vs
 * "Use {@code assignName(String)}") so the user controls and sees exactly which methods will be
 * used — both when instantiating a new property and when promoting an existing one
 * (model-editing-design.md §3.3 / §3.3a).</p>
 */
public final class AccessorSpec {

    /** The accessor role. {@code GETTER} is mandatory in any property. */
    public enum Role { GETTER, SETTER, UPDATER, ADDER, REMOVER, REINDEXER }

    private final Role role;
    private final boolean generate;
    private final String methodName;

    private AccessorSpec(Role role, boolean generate, String methodName) {
        this.role = role;
        this.generate = generate;
        this.methodName = methodName;
    }

    /** Generate a new method named {@code name} carrying the role's annotation. */
    public static AccessorSpec generate(Role role, String name) {
        return new AccessorSpec(role, true, name);
    }

    /** Attach the role's annotation to the already-declared method {@code existingMethodName}. */
    public static AccessorSpec attach(Role role, String existingMethodName) {
        return new AccessorSpec(role, false, existingMethodName);
    }

    public Role getRole() {
        return role;
    }

    /** {@code true} to create a new method, {@code false} to annotate an existing one. */
    public boolean isGenerate() {
        return generate;
    }

    /** The method name to create (generate) or the existing method to annotate (attach). */
    public String getMethodName() {
        return methodName;
    }
}
