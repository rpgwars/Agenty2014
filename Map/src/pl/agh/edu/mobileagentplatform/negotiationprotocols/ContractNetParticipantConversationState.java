package pl.agh.edu.mobileagentplatform.negotiationprotocols;

import java.util.Map;

import jade.core.AID;

public class ContractNetParticipantConversationState {
	
	
	private AID callForProposalSender;
	private Map<String,String> proposalContent; 
	
	public ContractNetParticipantConversationState(AID callForProposalSender, Map<String,String> proposalContent) {
		this.callForProposalSender = callForProposalSender; 
		this.proposalContent = proposalContent;
	}

	public AID getCallForProposalSender() {
		return callForProposalSender;
	}

	public void setCallForProposalSender(AID callForProposalSender) {
		this.callForProposalSender = callForProposalSender;
	}

	public Map<String, String> getProposalContent() {
		return proposalContent;
	}

	public void setProposalContent(Map<String, String> callForProposalContent) {
		this.proposalContent = callForProposalContent;
	}
	
	

}
