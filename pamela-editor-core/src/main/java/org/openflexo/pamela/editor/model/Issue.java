package org.openflexo.pamela.editor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A diagnostic attached to a {@link SourceMetaModel} or one of its elements.
 * Subclasses {@link Error} and {@link Warning} distinguish severity.
 * Each issue may carry a list of {@link FixProposal}s that can apply a
 * source-level correction via Spoon.
 */
public abstract class Issue {

    private final String message;
    private final List<FixProposal> fixProposals;

    public Issue(String message) {
        this.message = message;
        this.fixProposals = new ArrayList<>();
    }

    public String getMessage() {
        return message;
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
