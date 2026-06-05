package org.openflexo.pamela.editor.model;

/**
 * A critical diagnostic that blocks editor features (e.g., {@code @ModelEntity} on a class,
 * a {@link SourceModelProperty} with a setter on a LIST property).
 */
public class Error extends Issue {

    public Error(String message) {
        super(message);
    }
}
