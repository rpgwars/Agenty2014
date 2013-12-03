package pl.agh.edu.mobileagentplatform.negotiationprotocols;

import java.util.Map;

public interface ContractNetProtocol {
	
	ContractNetInitiatorConversationState startConversation(Map<String,String> message, ContractNetProposalEvaluator proposalEvaluator);
												   
}
