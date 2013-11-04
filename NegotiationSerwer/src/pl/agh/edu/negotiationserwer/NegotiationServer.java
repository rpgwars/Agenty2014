package pl.agh.edu.negotiationserwer;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

public class NegotiationServer extends Agent{
	
	private static final String manager = "manager";
	private static final long serialVersionUID = 7280447083550383907L;
	private Logger logger = jade.util.Logger.getMyLogger(this.getClass().getName());
	
	private double longtitude; 
	private double latitude; 
	private double minPrice; 
	private double maxDistance; 
	
	private Map<String, ConversationState> agentConversationStateMap; 
	
	@Override
	protected void setup() {
		super.setup();
		
		ACLMessage subscription = new ACLMessage(ACLMessage.SUBSCRIBE);
		Codec codec = new SLCodec();
		Ontology onto = BasicOntology.getInstance();
		subscription.setLanguage(codec.getName());
		subscription.setOntology(onto.getName());
		String convId = "C-" + getLocalName();
		subscription.setConversationId(convId);
		subscription.addReceiver(new AID(manager, AID.ISLOCALNAME));
		send(subscription);
		
		Object[] args = getArguments(); 
		
		longtitude = Double.parseDouble(args[0].toString());
		latitude = Double.parseDouble(args[1].toString());
		minPrice = Double.parseDouble(args[2].toString());
		maxDistance = Double.parseDouble(args[3].toString());
		
		agentConversationStateMap = new HashMap<>();
		
		
		addBehaviour(new CallForProposalListener(this));
		addBehaviour(new ProposalDecisionListener(this));
		addBehaviour(new NegotiationResponseSender(this));
		logger.log(Level.INFO, "agent setup, subscription sent, location: " + args[0].toString() + " " + args[1].toString() + " minPrice: " 
				+ args[2].toString() + " maxDistance: " + args[3].toString());
	}
	
	private class ConversationState{
		
		public ConversationState(AID aid){
			this.aid = aid; 
		}
		
		public AID aid; 
		public int negotiation = 0; 
		public double price = minPrice*1.5; 

	}
	
	
	private class CallForProposalListener extends CyclicBehaviour{

		private Agent agent;
		public CallForProposalListener(Agent agent){
			super(agent);
			this.agent = agent; 
		}
		
		@Override
		public void action() {
			
			
			MessageTemplate performativeTemplate = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage callForProposal = receive(performativeTemplate);
			
			if(callForProposal != null){
				logger.log(Level.INFO, "receiving cfp");
				if(!agentConversationStateMap.containsKey(callForProposal.getConversationId())){
					logger.log(Level.INFO, "received right cfp");
					agentConversationStateMap.put(callForProposal.getConversationId(), new ConversationState(callForProposal.getSender()));
					logger.log(Level.INFO, "received content " + callForProposal.getContent());
					String[] content = callForProposal.getContent().split(";");
					double clientLongtitude = Double.parseDouble(content[0]);
					double clientLatitude = Double.parseDouble(content[1]);
					
					double distance = (longtitude-clientLongtitude)*(longtitude-clientLongtitude);
					distance += (latitude-clientLatitude)*(latitude-clientLatitude);
					
					addBehaviour(new CallForProposolaResponseSender(agent, distance, callForProposal.getConversationId()));
				}
				else{
					logger.log(Level.INFO, "received cfp with wrong cid " + callForProposal.getConversationId());	
				}
				
			}
			else{
				block(); 
			}
			
		}
		
	}
	
	private class CallForProposolaResponseSender extends OneShotBehaviour{

		private double distance; 
		private String conversationId; 
		
		public CallForProposolaResponseSender(Agent agent, double distance, String conversationId){
			super(agent);
			this.distance = distance; 
			this.conversationId = conversationId; 
		}
		
		@Override
		public void action() {
			
			ConversationState conversationState = agentConversationStateMap.get(conversationId);
			ACLMessage aclMessage;
			if(distance <= maxDistance){
				logger.log(Level.INFO, "sending propose to " + conversationState.aid + " - distance ok " );
				aclMessage = new ACLMessage(ACLMessage.PROPOSE);
				aclMessage.setContent(Double.toString(minPrice * 1.5));
				
			}
			else{
				logger.log(Level.INFO, "sending refuse to " + conversationState.aid + " - distance too large " );
				agentConversationStateMap.remove(conversationId);
				aclMessage = new ACLMessage(ACLMessage.REFUSE);
			}
			aclMessage.setConversationId(conversationId);
			aclMessage.addReceiver(conversationState.aid);
			send(aclMessage);
		}
		
	}
	
	private class ProposalDecisionListener extends CyclicBehaviour{
		
		
		private static final long serialVersionUID = 853310112438869259L;
		private Agent agent;
		public ProposalDecisionListener(Agent agent){
			super(agent);
			this.agent = agent;
		}

		@Override
		public void action() {
			
			MessageTemplate decisionTemplate = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL),
					MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL));
			
			ACLMessage decision = receive(decisionTemplate);
			
			if(decision != null){
				logger.log(Level.INFO, "receiving decision");
				if(decision.getPerformative() == ACLMessage.REJECT_PROPOSAL){
					logger.log(Level.INFO, "agent " + decision.getSender().toString() + " rejected proposal");
					agentConversationStateMap.remove(decision.getConversationId());
				}
				
				else{
					logger.log(Level.INFO, "agent " + decision.getSender().toString() + " accepted proposal");
					addBehaviour(new InformSender(agent, decision.getConversationId()));
				}
				
			}
			else{
				block(); 
			}
		}
		
	}
	
	private class InformSender extends OneShotBehaviour{

		private String conversationId; 
		
		
		public InformSender(Agent agent, String conversationId){
			super(agent);
			this.conversationId = conversationId;
		}
		
		
		@Override
		public void action() {
			
			ConversationState conversationState = agentConversationStateMap.remove(conversationId);
			ACLMessage aclMessage = new ACLMessage(ACLMessage.INFORM);
			aclMessage.addReceiver(conversationState.aid);
			aclMessage.setContent(Double.toString(conversationState.price));
			aclMessage.setConversationId(conversationId);
			send(aclMessage);
			logger.log(Level.INFO, "inform sent to " + conversationState.aid);
			
			
		}
		
	}
	
	private class NegotiationResponseSender extends CyclicBehaviour{

		
		public NegotiationResponseSender(Agent agent){
			super(agent);
		 
		}
		
		@Override
		public void action() {
			
			
			MessageTemplate decisionTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM_IF);
			ACLMessage negotiation = receive(decisionTemplate);
			
			if(negotiation != null){
				ACLMessage aclMessage;
				
				aclMessage = new ACLMessage(ACLMessage.INFORM_IF);
				aclMessage.addReceiver(negotiation.getSender());
				Random random = new Random(); 
				if(random.nextBoolean()){
					logger.log(Level.INFO, "cena nie zostaje obnizona");
					aclMessage.setContent(Double.toString(minPrice * 1.5));
				}
				else{
					logger.log(Level.INFO, "cena zostaje obnizona");
					aclMessage.setContent(Double.toString(minPrice));
				}
					
				send(aclMessage);
			}
			else{
				block(); 
			}
		}
		
	}
	
	
	
	
}
