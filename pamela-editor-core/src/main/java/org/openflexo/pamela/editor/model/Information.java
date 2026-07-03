package org.openflexo.pamela.editor.model;

/**
 * A purely informational diagnostic — neither a questionable pattern ({@link Warning}) nor
 * a blocking problem ({@link Error}), just a notice worth surfacing to the user.
 */
public class Information extends Issue {

    public Information(String message) {
        super(message);
    }

    public Information(String message, SourceElement source) {
        super(message, source);
    }
}
