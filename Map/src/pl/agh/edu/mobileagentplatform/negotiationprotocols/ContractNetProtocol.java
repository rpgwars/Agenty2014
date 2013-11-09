package pl.agh.edu.mobileagentplatform.negotiationprotocols;

import jade.android.RuntimeCallback;
import jade.core.AID;

import java.util.List;
import java.util.Map;

public interface ContractNetProtocol {
	
	ContractNetInitiatorConversationState startConversation(Map<String,String> message, List<AID> participants, 
			ContractNetProposalEvaluator proposalEvaluator);
												   
}
