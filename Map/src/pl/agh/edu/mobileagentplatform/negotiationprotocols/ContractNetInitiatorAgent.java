package pl.agh.edu.mobileagentplatform.negotiationprotocols;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import pl.agh.edu.mobileagentplatform.PlatformInitializer;




public class ContractNetInitiatorAgent extends Agent implements ContractNetProtocol{
	
	private static final long serialVersionUID = -4320801954635002915L;
	
	private static final String mainAgentId = "manager";
	
	private Logger logger = Logger.getJADELogger(this.getClass().getName());
	
	private Map<String,ContractNetInitiatorConversationState> conversationIdStateMap; 
	
	private ObjectMapper objectMapper; 
	
	private Timer timer;
	
	protected void setup() {
		super.setup();
		Object[] args = getArguments();
		timer = new Timer(); 
		objectMapper = new ObjectMapper();
		conversationIdStateMap = new HashMap<String,ContractNetInitiatorConversationState>(); 
		addBehaviour(new CallForProposalReplyListener(this));
		addBehaviour(new InformListener(this));
		addBehaviour(new ManagerResponseListener(this));
		registerO2AInterface(ContractNetInitiatorAgent.class, this);
	}

	
	public ContractNetInitiatorConversationState startConversation(Map<String,String> message, ContractNetProposalEvaluator proposalEvaluator) {
		
		String conversationId = PlatformInitializer.getInstance().getAnotherConversationId();
		

		
		ContractNetInitiatorConversationState conversationState;
		try {
			objectMapper.writeValueAsString(message);
			conversationState = new ContractNetInitiatorConversationState(objectMapper.writeValueAsString(message), conversationId, proposalEvaluator);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Could not convert message to json string - conversation aborted");
			return null; 
		}
		conversationIdStateMap.put(conversationId, conversationState);
		ACLMessage participantsRequest = new ACLMessage(ACLMessage.REQUEST);		
		AID manager = new AID(mainAgentId,AID.ISLOCALNAME);
		participantsRequest.addReceiver(manager);
		participantsRequest.setConversationId(conversationId);
		send(participantsRequest);
		logger.log(Level.INFO, "participants request sent");
		return conversationState;
	}
	
	private class ManagerResponseListener extends CyclicBehaviour{
		

		private static final long serialVersionUID = 8848174081556922211L;

		public ManagerResponseListener(Agent agent){
			super(agent);

		}
		
		@Override
		public void action() {
			 
			MessageTemplate informRefTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF);
			ACLMessage informRef = receive(informRefTemplate);
			
			if(informRef != null){
				ContractNetInitiatorConversationState conversationState = conversationIdStateMap.get(informRef.getConversationId());

				List<String> addressesList = new ArrayList<String>();
				try {
					addressesList = objectMapper.readValue(informRef.getContent(), 
						    new TypeReference<List<String>>(){});
				} catch (Exception e) {
					logger.log(Level.SEVERE, "couldnt parse participants aids");
				}
			
				List<AID> participantsList = new ArrayList<AID>(addressesList.size()); 
				for(String address : addressesList)
					participantsList.add(new AID(address,AID.ISLOCALNAME));
				conversationState.setCallForProposalReceiversList(participantsList);
				logger.log(Level.SEVERE, "setting conversation participants");
				conversationState.setStatus(ContractNetInitiatorConversationStatus.SENDING_CALLS_FOR_PROPOSAL_STATUS);
				addBehaviour(new CallForProposalSender(ManagerResponseListener.this.getAgent(),conversationState));
			}	
			else{
				block(); 
			}
			
		}
		
	}
	
	private class CallForProposalSender extends OneShotBehaviour{

		private static final long serialVersionUID = 5379306473221462941L;
		private ContractNetInitiatorConversationState conversationState; 
		public CallForProposalSender(Agent agent, ContractNetInitiatorConversationState conversationState){
			super(agent);
			this.conversationState = conversationState; 
		}
		
		@Override
		public void action() {
			
			ACLMessage aclMessage = new ACLMessage(ACLMessage.CFP);
			for(AID aid : conversationState.getCallForProposalReceiversList()){
				aclMessage.addReceiver(aid);
			}
			aclMessage.setConversationId(conversationState.getConversationId());
			aclMessage.setContent(conversationState.getMessage());
			send(aclMessage);
			logger.log(Level.INFO, "ContractNet call for proposal sent with conversation id  " + conversationState.getConversationId());
			timer.schedule(new TimerTask() {
				
				@Override
				public void run() {
					logger.log(Level.INFO, "Entering answering proposal state after timeout " + conversationState.getConversationId());
					boolean settingStatusSucceded = conversationState.setStatus(ContractNetInitiatorConversationStatus.ANSWERING_PROPOSALS_STATUS);
					if(settingStatusSucceded)
						CallForProposalSender.this.getAgent().addBehaviour(new ProposalAnswerSender(getAgent(), conversationState));
				}
			}, conversationState.getProposalEvaluator().getParameters().getProposalReciveDuration());
			conversationState.setStatus(ContractNetInitiatorConversationStatus.GETTING_PROPOSALS_STATUS);
			
		}
		
	}
	
	private class CallForProposalReplyListener extends CyclicBehaviour{
		
		private static final long serialVersionUID = -2418737849837000899L;

		public CallForProposalReplyListener(Agent agent){
			super(agent);
		}
		
		
		@Override
		public void action() {
			MessageTemplate proposalTemplate = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
					MessageTemplate.MatchPerformative(ACLMessage.REFUSE));
			
			ACLMessage proposalMessage = receive(proposalTemplate);
			if(proposalMessage != null){
				
				String conversationId = proposalMessage.getConversationId(); 
				ContractNetInitiatorConversationState conversationState = conversationIdStateMap.get(conversationId);
				if(conversationState == null){
					logger.log(Level.INFO, "Rejected proposal because of wrong " +
							"conversation id " + conversationId + " ))");
				}
				else{
					ContractNetInitiatorConversationStatus conversationStatus = conversationState.getStatus();
					if(conversationStatus != ContractNetInitiatorConversationStatus.GETTING_PROPOSALS_STATUS && 
							conversationStatus != ContractNetInitiatorConversationStatus.SENDING_CALLS_FOR_PROPOSAL_STATUS)
					{
						logger.log(Level.INFO, "proposal from" + proposalMessage.getSender().toString() +  " received too late");
						sendRejectMessage(proposalMessage.getSender(),conversationId);
					}
					else if(proposalMessage.getPerformative() == ACLMessage.REFUSE)
						logger.log(Level.INFO, "cfp rejected");
					else {
						logger.log(Level.INFO, "cfp accepted by " + proposalMessage.getSender().toString());
						Map<String,String> proposalContent;
						try {
							proposalContent = objectMapper.readValue(proposalMessage.getContent(), 
							    new TypeReference<HashMap<String,String>>(){});
						} catch (Exception e) {
							logger.log(Level.INFO, "could not parse proposal from " + proposalMessage.getSender().toString());
							sendRejectMessage(proposalMessage.getSender(),conversationId);
							return; 
						}
						
						boolean continueAcceptingProposals = conversationState.addProposal(proposalMessage.getSender(), proposalContent);
						logger.log(Level.INFO, "proposal accepted, continue accepting proposals =  " + continueAcceptingProposals);
						if(!continueAcceptingProposals){
							boolean settingStatusSucceded = conversationState.setStatus(ContractNetInitiatorConversationStatus.ANSWERING_PROPOSALS_STATUS);
							if(settingStatusSucceded)
								CallForProposalReplyListener.this.getAgent().addBehaviour(new ProposalAnswerSender(getAgent(), conversationState));
						}
						
					}
				}
	
			}
			else{
				block(); 
			}
		}
		
		private void sendRejectMessage(AID proposalSenderAid, String conversationId){
			ACLMessage rejectMessage = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
			rejectMessage.addReceiver(proposalSenderAid);
			rejectMessage.setConversationId(conversationId);
			send(rejectMessage);
		}
		
	}
	

	
	
	private class ProposalAnswerSender extends OneShotBehaviour{

		private static final long serialVersionUID = -1170575814975847827L;
		private ContractNetInitiatorConversationState conversationState; 
		
		public ProposalAnswerSender(Agent agent, ContractNetInitiatorConversationState conversationState){
			super(agent);
			this.conversationState = conversationState;
		}
		
		@Override
		public void action() {
			
			if(conversationState.getProposalsContentMap().size() == 0) {
				conversationIdStateMap.remove(conversationState.getConversationId());
				conversationState.setAcceptedProposals(new HashMap<AID, Map<String,String>>());
				conversationState.setStatus(ContractNetInitiatorConversationStatus.FINISHED_STATUS);
				logger.log(Level.INFO, "no oferts available for conversation " + conversationState.getConversationId());
				return; 
			}
			
			Map<AID,Map<String,String>> proposalsContenMap = conversationState.getProposalsContentMap();
			Map<AID,Integer> proposalsValueMap = conversationState.getProposalsValueMap();
			
			List<Entry<AID,Integer>> sortedByValueProposalSendersList = new ArrayList<Entry<AID,Integer>>(proposalsValueMap.entrySet());
			Collections.sort(sortedByValueProposalSendersList, new Comparator<Entry<AID,Integer>>(){

				public int compare(Entry<AID,Integer> entry1, Entry<AID,Integer> entry2) {
					return entry1.getValue() - entry2.getValue(); 
				}
				
			});
			
			List<Map<String, String>> proposalsContentList = new ArrayList<Map<String,String>>(sortedByValueProposalSendersList.size());
			List<Integer> proposalsValueList = new ArrayList<Integer>(sortedByValueProposalSendersList.size());
			
			for(Entry<AID,Integer> entry : sortedByValueProposalSendersList) {
				proposalsContentList.add(proposalsContenMap.get(entry.getKey()));
				proposalsValueList.add(proposalsValueMap.get(entry.getKey()));
			}
			
			List<Integer> selectedProposals = conversationState.getProposalEvaluator().finalEvaluation(proposalsContentList, proposalsValueList);
			if(selectedProposals == null){
				int minAcceptedProposals = conversationState.getProposalEvaluator().getParameters().getMinAccepted();
				int i; 
				for(i = 0; (i < proposalsValueList.size()) && (proposalsValueList.get(i) > 0); i++);
				if(i < minAcceptedProposals){
					ACLMessage rejectProposalMessage = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
					for(AID sender : proposalsValueMap.keySet()){
						rejectProposalMessage.addReceiver(sender);
					}
					rejectProposalMessage.setConversationId(conversationState.getConversationId());
					send(rejectProposalMessage);
					logger.log(Level.INFO, "Rejecting all proposals for conversation " + conversationState.getConversationId() +  " because not enough of them were qualified ");
					conversationIdStateMap.remove(conversationState.getConversationId());
					conversationState.setAcceptedProposals(new HashMap<AID, Map<String,String>>());
					conversationState.setStatus(ContractNetInitiatorConversationStatus.FINISHED_STATUS);
					return; 
				}
				else{
					int maxAcceptedProposals = conversationState.getProposalEvaluator().getParameters().getMaxAccepted();
					for(; i < (proposalsValueList.size()) && (proposalsValueList.get(i) > 0) && (i < maxAcceptedProposals); i++);
					selectedProposals = new ArrayList<Integer>(i);
					for(int j = 0; j < i; j++)
						selectedProposals.add(j);
				}
			}
			
			int j;
			Map<AID,Map<String,String>> selectedProposalsMap = new HashMap<AID,Map<String,String>>();
			
			
			for(j = 0; j<selectedProposals.size(); j++) {
				AID proposalSender = sortedByValueProposalSendersList.get(selectedProposals.get(j)).getKey();
				selectedProposalsMap.put(proposalSender, proposalsContenMap.get(proposalSender));
				proposalsContenMap.remove(proposalSender);
			}
			
			ACLMessage rejectProposalMessage = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
			for(AID aid : proposalsContenMap.keySet()){
				rejectProposalMessage.addReceiver(aid);
			}
			logger.log(Level.INFO, "Rejecting " + proposalsContenMap.size() + " proposals for conversation "
					+ conversationState.getConversationId() +  " because othere were better ");
			rejectProposalMessage.setConversationId(conversationState.getConversationId());
			send(rejectProposalMessage);
			

			
			ACLMessage acceptProposalMessage = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
			for(AID aid : selectedProposalsMap.keySet()){
				acceptProposalMessage.addReceiver(aid);
			}
			conversationState.setAcceptedProposals(selectedProposalsMap);
			conversationState.setStatus(ContractNetInitiatorConversationStatus.RECEIVING_INFORM_STATUS);
			logger.log(Level.INFO, "Accepting " + selectedProposals.size() + " proposals for conversation " + conversationState.getConversationId());
			acceptProposalMessage.setConversationId(conversationState.getConversationId());
			send(acceptProposalMessage);
						
		}		
	}
	
	private class InformListener extends CyclicBehaviour{	

		private static final long serialVersionUID = -2839397043029743773L;
		 
		public InformListener(Agent agent){
			super(agent); 
		}
		
		@Override
		public void action() {
			
			MessageTemplate informOrFailureTemplate = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchPerformative(ACLMessage.FAILURE));
			ACLMessage message = receive(informOrFailureTemplate);
			
			if(message != null){
				String conversationId = message.getConversationId();
				ContractNetInitiatorConversationState conversationState = conversationIdStateMap.get(conversationId);
				if(conversationState == null){
					logger.log(Level.INFO, "rejected inform or failure because of wrong conversationId " + message.getConversationId());
				}
				else if(conversationState.getStatus() != ContractNetInitiatorConversationStatus.RECEIVING_INFORM_STATUS)
					logger.log(Level.INFO, "inform or failure received in wrong state for conversationId " + message.getConversationId());
				else{
					logger.log(Level.INFO, "inform or failure received for conversationId " + message.getConversationId());
					
					Map<String,String> messageContent = new HashMap<String,String>();
					try {
						messageContent = objectMapper.readValue(message.getContent(), 
						    new TypeReference<HashMap<String,String>>(){});
					} catch (Exception e) {
						logger.log(Level.INFO, "could not parse inform or failure from " + message.getSender().toString());
						return; 
					}
					
					conversationState.addResult(message.getSender(), messageContent);
					if(conversationState.getAcceptedProposalsResults().size() == conversationState.getAcceptedProposals().size()){
						conversationState.setStatus(ContractNetInitiatorConversationStatus.FINISHED_STATUS);
						conversationIdStateMap.remove(conversationState.getConversationId());
						logger.log(Level.INFO, "all replys received for conversationId " + conversationId);
					}

				}
			}
			else{
				block(); 
			}
		}	
		
	}

}
