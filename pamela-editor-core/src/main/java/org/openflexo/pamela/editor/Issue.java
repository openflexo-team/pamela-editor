package org.openflexo.pamela.editor;

import java.util.ArrayList;
import java.util.List;

public abstract class Issue {

	private String message;
	private List<FixProposal> fixProposals;

	public Issue(String message) {
		this.message = message;
		fixProposals = new ArrayList<>();
	}

	public void addToFixProposals(FixProposal fixProposal) {
		fixProposals.add(fixProposal);
	}

	public String getMessage() {
		return message;
	}

	public abstract class FixProposal {

		private String proposalMessage;

		public FixProposal(String proposalMessage) {
			this.proposalMessage = proposalMessage;
		}

		public abstract void fix();
	}
}
