package org.openflexo.pamela.editor.model;

/**
 * A non-critical diagnostic that signals a questionable pattern
 * (e.g., a root type that is not annotated {@code @ModelEntity},
 * an {@code isDerived} property without {@code @ReturnedValue}).
 */
public class Warning extends Issue {

    public Warning(String message) {
        super(message);
    }
}
