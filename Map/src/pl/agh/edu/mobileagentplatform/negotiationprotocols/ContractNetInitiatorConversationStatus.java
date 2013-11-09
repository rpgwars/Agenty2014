package pl.agh.edu.mobileagentplatform.negotiationprotocols;

public enum ContractNetInitiatorConversationStatus {
	
	SENDING_CALLS_FOR_PROPOSAL_STATUS(0),GETTING_PROPOSALS_STATUS(1),ANSWERING_PROPOSALS_STATUS(2),RECEIVING_INFORM_STATUS(3),FINISHED_STATUS(4);
	
	private final int order;
	
	ContractNetInitiatorConversationStatus(int order){
		this.order = order; 
	}
	
	private int getOrder(){
		return this.order;
	}
	
	public boolean isBefore(ContractNetInitiatorConversationStatus conversationStatus){
		return getOrder() < conversationStatus.getOrder();
	}
	
}


