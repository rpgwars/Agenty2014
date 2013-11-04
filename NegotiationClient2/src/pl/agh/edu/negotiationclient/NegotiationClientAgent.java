package pl.agh.edu.negotiationclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;


import android.content.Context;
import android.content.Intent;
import android.util.Log;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.ConversationList;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;



public class NegotiationClientAgent extends Agent implements NegotiationClientInterface{
	
	private NegotiationState negotiationState = NegotiationState.NOT_OPENED_STATE; 
	
	private enum NegotiationState{
		NOT_OPENED_STATE, GETTING_SERVERS_STATE, GETTING_PROPOSALS_STATE, NEGOTIATING_PRICE_STATE, ANSWERING_PROPOSALS_STATE
	}

	private Logger logger = Logger.getJADELogger(this.getClass().getName());
	
    private static final String mainAgentId = "manager";	
	private Context context;
	private String nickname; 
	private Timer timer; 
	
	protected void setup() {
		
		Object[] args = getArguments();
		
		if (args != null && args.length > 0) {
			if (args[0] instanceof Context) {
				context = (Context) args[0];
			}
			if(args[1] instanceof String){
				nickname = (String)args[1];
				//nickname = "kkkk";
			}
		}
	
		timer = new Timer(); 
		
		
		registerO2AInterface(NegotiationClientInterface.class, this);
		addBehaviour(new ManagerResponseListener(this));
		addBehaviour(new ProposeRefuseListener(this));
		addBehaviour(new InformReceiver(this));
		addBehaviour(new NegotiationReceiver(this));
	
	}
	
	
	private void showBestPrice(String price) {
		Intent broadcast = new Intent();
		broadcast.setAction("jade.demo.REFRESH");
		broadcast.putExtra("price", price);
		context.sendBroadcast(broadcast);
	}
	
	

	public void getBestPrice() {
		if(negotiationState == NegotiationState.NOT_OPENED_STATE){
			negotiationState = NegotiationState.GETTING_SERVERS_STATE;
			ACLMessage simpleMessage = new ACLMessage(ACLMessage.REQUEST);		
			AID receiverAid = new AID(mainAgentId,AID.ISLOCALNAME);
			simpleMessage.addReceiver(receiverAid);
			send(simpleMessage);
			
			logger.log(Level.INFO, "servers request sent");
		}
		else{
			logger.log(Level.INFO, "could not begin " + negotiationState);
		}
	}
	
	private List<AID> serverAgentList; 
	
	private class ManagerResponseListener extends CyclicBehaviour{
		
		private Agent agent;  
		public ManagerResponseListener(Agent agent){
			super(agent);
			this.agent = agent; 
		}
		
		@Override
		public void action() {
			
			MessageTemplate informRefTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF);
			
			ACLMessage informRef = receive(informRefTemplate);
			if(informRef != null && negotiationState == NegotiationState.GETTING_SERVERS_STATE){
				String[] aids = informRef.getContent().split(";");
				serverAgentList = new ArrayList<AID>(aids.length);
				for(String aid : aids){
					serverAgentList.add(new AID(aid, AID.ISLOCALNAME));
					logger.log(Level.INFO, "received aid " + aid);
				}
				agent.addBehaviour(new CFPSender(agent));
			}
			
			else{
				block(); 
			}
			
		}
		
	}
	
	private class CFPSender extends OneShotBehaviour{

		private Agent agent;
		public CFPSender(Agent agent){
			super(agent);
			this.agent = agent;
		}
		
		@Override
		public void action() {
			
			ACLMessage aclMessage = new ACLMessage(ACLMessage.CFP);
			for(AID aid : serverAgentList){
				aclMessage.addReceiver(aid);
			}
			aclMessage.setConversationId(nickname);
			aclMessage.setContent("7;8");
			send(aclMessage);
			logger.log(Level.INFO, "cfp sent with convid " + nickname);
			proposals = new HashMap<AID, Double>();
			
			timer.schedule(new TimerTask() {
				
				@Override
				public void run() {
					negotiationState = NegotiationState.ANSWERING_PROPOSALS_STATE;
					CFPSender.this.agent.addBehaviour(new ProposalAnswerSender(CFPSender.this.agent));
				}
			}, 2500);
			negotiationState = NegotiationState.GETTING_PROPOSALS_STATE;
			
			
		}
		
	}
	
	private Map<AID,Double> proposals; 
	
	private class ProposeRefuseListener extends CyclicBehaviour{
		
		public ProposeRefuseListener(Agent agent){
			super(agent);
		}
		
		
		@Override
		public void action() {
			MessageTemplate proposalTemplate = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
					MessageTemplate.MatchPerformative(ACLMessage.REFUSE));
			
			ACLMessage proposal = receive(proposalTemplate);
			if(proposal != null){
				if(!proposal.getConversationId().equals(nickname)){
					
					logger.log(Level.INFO, "rejected proposal because of wrong convId " + proposal.getConversationId() + " ))");
				}
				else if(negotiationState != NegotiationState.GETTING_PROPOSALS_STATE)
					logger.log(Level.INFO, "proposal received in wrong state");
				else if(proposal.getPerformative() == ACLMessage.REFUSE)
					logger.log(Level.INFO, "cfp rejected");
				else
					proposals.put(proposal.getSender(), Double.parseDouble(proposal.getContent()));
					
			}
			else{
				block(); 
			}
		}
		
	}
	
	private AID bestAid; 
	private double bestPrice; 
	
	private class ProposalAnswerSender extends OneShotBehaviour{

		private Agent agent; 
		
		public ProposalAnswerSender(Agent agent){
			super(agent);
			this.agent = agent; 
		}
		
		@Override
		public void action() {
			if(proposals.size() == 0){
				showBestPrice("no oferts available");
				logger.log(Level.INFO, "no oferts available");
				negotiationState = NegotiationState.NOT_OPENED_STATE;
				return;
			}
			AID bestAid = null;
			double bestPrice = Double.MAX_VALUE; 
			for(AID aid : proposals.keySet()){
				double price = proposals.get(aid);
				if(price < bestPrice){
					bestAid = aid; 
					bestPrice = price; 
				}
				
			}
			
			//agent.addBehaviour(new InformReceiver(bestAid, bestPrice,agent));
			NegotiationClientAgent.this.bestAid = bestAid; 
			NegotiationClientAgent.this.bestPrice = bestPrice; 
			
			ACLMessage aclMessage = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
			aclMessage.setConversationId(nickname);
			aclMessage.addReceiver(bestAid);
			send(aclMessage);
			
			logger.log(Level.INFO, "accepting " + bestAid.toString() + " proposal");
			
			aclMessage = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
			aclMessage.setConversationId(nickname);
			
			proposals.remove(bestAid);
			for(AID aid : proposals.keySet())
				aclMessage.addReceiver(aid);
			
			send(aclMessage);
			proposals.clear();
			
		}
		
	}
	
	private class InformReceiver extends CyclicBehaviour{	
		/**
		 * 
		 */
		private static final long serialVersionUID = -2839397043029743773L;
		private Agent agent; 
		public InformReceiver(Agent agent){
			this.agent = agent; 
		}
		
		@Override
		public void action() {
			
			MessageTemplate informTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage inform = receive(informTemplate);
			
			if(inform != null){
				if(!inform.getConversationId().equals(nickname)){
					logger.log(Level.INFO, "rejected inform because of wrong convId " +inform.getConversationId() + " ))" );
					
				}
				else if(negotiationState != NegotiationState.ANSWERING_PROPOSALS_STATE)
					logger.log(Level.INFO, "inform received in wrong state");
				else{
					logger.log(Level.INFO, "inform received " + bestPrice);
					showBestPrice(bestPrice + " " + bestAid.toString());
					
					
					negotiationState = NegotiationState.NEGOTIATING_PRICE_STATE;
					ACLMessage message = new ACLMessage(ACLMessage.INFORM_IF);
					message.addReceiver(bestAid);
					send(message);
					
					timer.schedule(new TimerTask() {
						
						@Override
						public void run() {
							negotiationState = NegotiationState.NOT_OPENED_STATE;
							showBestPrice("after negotiations " + bestPrice + " " + bestAid.toString());
						}
					}, 4000);
				}
			}
			else{
				block(); 
			}
			
			
		}	
		
	}
	
	private class NegotiationReceiver extends CyclicBehaviour{	
		/**
		 * 
		 */
		private static final long serialVersionUID = -2839397043029743773L;
		private Agent agent; 
		public NegotiationReceiver(Agent agent){
			super(agent);
			this.agent = agent;
			
		}
		
		@Override
		public void action() {
			
			MessageTemplate informIfTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM_IF);
			ACLMessage informIf = receive(informIfTemplate);
			
			if(informIf != null){
				
				if(negotiationState != NegotiationState.NEGOTIATING_PRICE_STATE)
					logger.log(Level.INFO, "negotiation received in wrong state");
				else{
					logger.log(Level.INFO, "price after negotiations " + bestPrice);
					bestPrice = Double.parseDouble(informIf.getContent());
				}
			}
			else{
				block(); 
			}
			
			
		}	
		
	}

}
