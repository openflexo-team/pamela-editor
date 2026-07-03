package org.openflexo.pamela.editor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A diagnostic attached to a {@link SourceMetaModel} or one of its elements.
 * Subclasses {@link Error}, {@link Warning} and {@link Information} distinguish severity.
 * Each issue may carry a list of {@link FixProposal}s that can apply a
 * source-level correction via Spoon.
 */
public abstract class Issue {

    private final String message;
    private final List<FixProposal> fixProposals;
    private final SourceElement source;

    public Issue(String message) {
        this(message, null);
    }

    /**
     * @param message the diagnostic message
     * @param source  the element this issue is raised against, or {@code null} when it is
     *                not attached to a specific element (e.g. a metamodel-level configuration
     *                issue such as an unresolved root type)
     */
    public Issue(String message, SourceElement source) {
        this.message = message;
        this.source = source;
        this.fixProposals = new ArrayList<>();
    }

    public String getMessage() {
        return message;
    }

    /** The element this issue is raised against, or {@code null} if none. */
    public SourceElement getSource() {
        return source;
    }

    public List<FixProposal> getFixProposals() {
        return fixProposals;
    }

    public void addFixProposal(FixProposal proposal) {
        fixProposals.add(proposal);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + message;
    }

    /**
     * A proposed automatic correction for an {@link Issue}.
     * Implementations apply their fix by mutating the Spoon AST
     * and saving the affected {@link SourceCompilationUnit}.
     */
    public abstract static class FixProposal {

        private final String proposalMessage;

        public FixProposal(String proposalMessage) {
            this.proposalMessage = proposalMessage;
        }

        public String getProposalMessage() {
            return proposalMessage;
        }

        /** Applies the source-level correction. */
        public abstract void fix();
    }
}
