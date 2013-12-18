package pl.agh.edu.mobileagentplatform.negotiationprotocols;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public abstract class ContractNetParticipantAgent extends Agent {

	private static final long serialVersionUID = -7135489675993761192L;
	private Map<String,ContractNetParticipantConversationState> conversationIdStateMap; 
	private Logger logger = jade.util.Logger.getMyLogger(this.getClass().getName());
	private ObjectMapper objectMapper; 
	
	@Override
	protected void setup() {
		super.setup();
		conversationIdStateMap = new HashMap<String,ContractNetParticipantConversationState>();
		objectMapper = new ObjectMapper(); 
		addBehaviour(new CallForProposalListener(this));
		addBehaviour(new ProposalAnswerListener(this));
	}
	
	protected Map<String,String> createProposal(AID sender, Map<String,String> callForProposalContent){
		return null; 
	}
	protected abstract Map<String,String> createConversationResult(AID sender, Map<String,String> proposal);
	
	
	private class CallForProposalListener extends CyclicBehaviour{
		
		private static final long serialVersionUID = -6868943568641689435L;

		public CallForProposalListener(Agent agent){
			super(agent); 
		}
		
		@Override
		public void action() {
			
			MessageTemplate performativeTemplate = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage callForProposal = receive(performativeTemplate);
			
			if(callForProposal != null){
				logger.log(Level.INFO, "receiving cfp with conversationId " + callForProposal.getConversationId());
				if(conversationIdStateMap.containsKey(callForProposal.getConversationId())){
					logger.log(Level.INFO, "received cfp with allready registered conversationId " + callForProposal.getConversationId());	
					return;
				}
				
				Map<String,String> callForProposalContent;
				try {
					callForProposalContent = objectMapper.readValue(callForProposal.getContent(), 
					    new TypeReference<HashMap<String,String>>(){});
				} catch (Exception e) {
					logger.log(Level.INFO, "could not parse call for proposal from " + callForProposal.getSender().toString());
					sendRefuseMessage(callForProposal.getSender(), callForProposal.getConversationId());
					return; 
				}
				
				Map<String,String> proposalContent = createProposal(callForProposal.getSender(), callForProposalContent);
				if(proposalContent == null){
					logger.log(Level.INFO, "refused to send propose to " + callForProposal.getSender().toString());
					sendRefuseMessage(callForProposal.getSender(), callForProposal.getConversationId());
					return; 
				}
				ContractNetParticipantConversationState conversationState = new ContractNetParticipantConversationState(callForProposal.getSender(), proposalContent);
				conversationIdStateMap.put(callForProposal.getConversationId(), conversationState);
				addBehaviour(new ProposalSender(getAgent(),callForProposal.getConversationId()));

			}
			else{
				block(); 
			}
			
		}
		
		private void sendRefuseMessage(AID callForProposalSenderAid, String conversationId){
			ACLMessage rejectMessage = new ACLMessage(ACLMessage.REFUSE);
			rejectMessage.addReceiver(callForProposalSenderAid);
			rejectMessage.setConversationId(conversationId);
			send(rejectMessage);
		}
		
	}
	
	private class ProposalSender extends OneShotBehaviour{ 
		
		private static final long serialVersionUID = -1456319135912548473L;
		
		private String conversationId; 
		public ProposalSender(Agent agent, String conversationId){
			super(agent);
			this.conversationId = conversationId;
		}
		
		@Override
		public void action() {
			
			ContractNetParticipantConversationState conversationState = conversationIdStateMap.get(conversationId); 
			logger.log(Level.INFO, "sending proposal to " + conversationState.getCallForProposalSender().toString());
			ACLMessage proposalMessage = new ACLMessage(ACLMessage.PROPOSE);
			try {
				proposalMessage.setContent(objectMapper.writeValueAsString(conversationState.getProposalContent()));
			} catch (Exception e) {
				logger.log(Level.INFO, "could not serialize proposal for " + conversationState.getCallForProposalSender().toString());	
			}

			proposalMessage.setConversationId(conversationId);
			proposalMessage.addReceiver(conversationState.getCallForProposalSender());
			send(proposalMessage);
		}
		
	}
	
	private class ProposalAnswerListener extends CyclicBehaviour{

		private static final long serialVersionUID = 853310112438869259L;
		
		public ProposalAnswerListener(Agent agent){
			super(agent);
		}

		@Override
		public void action() {
			
			MessageTemplate decisionTemplate = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL),
					MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL));
			
			ACLMessage decision = receive(decisionTemplate);
			
			if(decision != null){
				if(!conversationIdStateMap.containsKey(decision.getConversationId())){
					logger.log(Level.INFO, "recived decison with wrong conversationId from agent " + decision.getSender().toString());
					return; 
				}
				
				if(decision.getPerformative() == ACLMessage.REJECT_PROPOSAL){
					logger.log(Level.INFO, "agent " + decision.getSender().toString() + " rejected proposal");
					conversationIdStateMap.remove(decision.getConversationId());
				}
				
				else{
					logger.log(Level.INFO, "agent " + decision.getSender().toString() + " accepted proposal");
					addBehaviour(new ConversationResultSender(getAgent(), decision.getConversationId()));
				}
				
			}
			else{
				block(); 
			}
		}
		
	}
	
	private class ConversationResultSender extends OneShotBehaviour{

		private static final long serialVersionUID = -6129337145427610895L;
		private String conversationId; 
		
		
		public ConversationResultSender(Agent agent, String conversationId){
			super(agent);
			this.conversationId = conversationId;
		}
		
		
		@Override
		public void action() {
			
			ContractNetParticipantConversationState conversationState = conversationIdStateMap.remove(conversationId);
			if(conversationState != null) {
				Map<String, String> conversationResult = createConversationResult(conversationState.getCallForProposalSender(), conversationState.getProposalContent());
				if(conversationResult == null){
					ACLMessage failure = new ACLMessage(ACLMessage.FAILURE);
					failure.setConversationId(conversationId);
					failure.addReceiver(conversationState.getCallForProposalSender());
					send(failure);
					return;
				}
				ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
				inform.addReceiver(conversationState.getCallForProposalSender());
				try {
					inform.setContent(objectMapper.writeValueAsString(conversationResult));
				} catch (Exception e) {
					logger.log(Level.INFO, "could not send conversation result because of content serialization error");
					return; 
				}
				inform.setConversationId(conversationId);
				send(inform);
				logger.log(Level.INFO, "inform sent to " + conversationState.getCallForProposalSender().toString());	
			}
			else {
				logger.log(Level.INFO, "could not send conversation result because of wrong conversation id");
			}
	
		}
		
	}
}
