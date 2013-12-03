package pl.agh.edu.mobileagentplatform.negotiationprotocols;

import jade.core.AID;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.agh.edu.mobileagentplatform.negotiationprotocols.ContractNetProposalEvaluator.Parameters;

public class ContractNetInitiatorConversationState {

	private List<AID> callForProposalReceiversList;
	private ContractNetInitiatorConversationStatus conversationStatus; 
	private String message; 
	private String conversationId; 
	private Map<AID,Map<String,String>> proposalsContentMap; 
	private Map<AID,Integer> proposalsValueMap; 
	private ContractNetProposalEvaluator proposalEvaluator; 
	private int nrOfValuableProposals; 
	private Map<AID,Map<String,String>> acceptedProposals;
	private Map<AID,Map<String,String>> acceptedProposalsResults;
	
	public ContractNetInitiatorConversationState(String message, String conversationId, ContractNetProposalEvaluator proposalEvaluator) {
		this.message = message;
		this.conversationStatus = ContractNetInitiatorConversationStatus.GETTING_PARTICIPANTS_STATUS;
		this.conversationId = conversationId;
		this.proposalsContentMap = new HashMap<AID,Map<String,String>>();
		this.proposalsValueMap = new HashMap<AID,Integer>();
		this.proposalEvaluator = proposalEvaluator;
		this.nrOfValuableProposals = 0; 
		acceptedProposalsResults = new HashMap<AID,Map<String,String>>();
	}
	
	public boolean addProposal(AID senderAid, Map<String,String> proposalContent){
		int evaluation = proposalEvaluator.evaluate(proposalContent);
		Parameters parameters = proposalEvaluator.getParameters();
		proposalsContentMap.put(senderAid, proposalContent);
		proposalsValueMap.put(senderAid, evaluation);
		if(evaluation > 0)
			nrOfValuableProposals++; 
		if(parameters.isWaitTillTimeExceeds())
			return true; 
		if(nrOfValuableProposals >= parameters.getMinAccepted())
			return false; 
		return true; 	
	}

	public List<AID> getCallForProposalReceiversList() {
		return callForProposalReceiversList;
	}

	public void setCallForProposalReceiversList(List<AID> callForProposalReceiversList) {
		this.callForProposalReceiversList = callForProposalReceiversList;
	}

	public synchronized ContractNetInitiatorConversationStatus getStatus() {
		return conversationStatus;
	}

	public synchronized boolean setStatus(ContractNetInitiatorConversationStatus conversationStatus) {
		if(this.conversationStatus.isBefore(conversationStatus)){
			this.conversationStatus = conversationStatus;
			return true; 
		}
		return false; 
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}

	public Map<AID, Map<String, String>> getProposalsContentMap() {
		return proposalsContentMap;
	}

	public void setProposalsContentMap(Map<AID, Map<String, String>> proposalsMap) {
		this.proposalsContentMap = proposalsMap;
	}

	public Map<AID, Integer> getProposalsValueMap() {
		return proposalsValueMap;
	}

	public void setProposalsValueMap(Map<AID, Integer> proposalsValueMap) {
		this.proposalsValueMap = proposalsValueMap;
	}

	public ContractNetProposalEvaluator getProposalEvaluator() {
		return proposalEvaluator;
	}

	public void setProposalEvaluator(ContractNetProposalEvaluator proposalEvaluator) {
		this.proposalEvaluator = proposalEvaluator;
	}

	public Map<AID, Map<String, String>> getAcceptedProposals() {
		return acceptedProposals;
	}

	public void setAcceptedProposals(
			Map<AID, Map<String, String>> conversationResult) {
		this.acceptedProposals = conversationResult;
	}
	
	public void addResult(AID sender,Map<String,String> result){
		acceptedProposalsResults.put(sender, result);
	}

	public Map<AID, Map<String, String>> getAcceptedProposalsResults() {
		return acceptedProposalsResults;
	}

	
	
}
