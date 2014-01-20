package pl.agh.edu.negotiationserwer;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import pl.agh.edu.mobileagentplatform.negotiationprotocols.ContractNetParticipantAgent;

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

public class NegotiationServer extends ContractNetParticipantAgent{
	
	private static final String manager = "manager";
	private static final long serialVersionUID = 7280447083550383907L;
	private Logger logger = jade.util.Logger.getMyLogger(this.getClass().getName());
	
	private double longtitude; 
	private double latitude; 
	private double minPrice; 
	private double maxDistance; 
	
	
	@Override
	protected void setup() {
		super.setup();
		
		ACLMessage subscription = new ACLMessage(ACLMessage.SUBSCRIBE);
		String convId = "C-" + getLocalName();
		subscription.setConversationId(convId);
		subscription.addReceiver(new AID(manager, AID.ISLOCALNAME));
		send(subscription);
		
		Object[] args = getArguments(); 
		
		longtitude = Double.parseDouble(args[0].toString());
		latitude = Double.parseDouble(args[1].toString());
		minPrice = Double.parseDouble(args[2].toString());
		maxDistance = Double.parseDouble(args[3].toString());
		
		logger.log(Level.INFO, "agent setup, subscription sent, location: " + args[0].toString() + " " + args[1].toString() + " minPrice: " 
				+ args[2].toString() + " maxDistance: " + args[3].toString());
	}
	

	
	@Override
	protected Map<String, String> createConversationResult(final AID sender, Map<String,String> proposal) {
		
		Runnable senderTask = new Runnable(){

			@Override
			public void run() {
				Random random = new Random(); 
				for(int i = 0; i<3; i++){
					
					ACLMessage message = new ACLMessage(ACLMessage.UNKNOWN);
					message.setContent("Polozenie " + getName() + " dlugosc: " + Math.abs(random.nextInt()) % 10 + " " + " szerokosc " + Math.abs(random.nextInt()) % 10);
					message.addReceiver(sender);
					send(message);
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						
					}
				}
				
			}
			
		};
		Thread senderThread = new Thread(senderTask);
		senderThread.start();
		
		return new HashMap<String,String>();
	}

	@Override
	protected Map<String, String> createProposal(AID sender, Map<String,String> callForProposalContent) {
		double clientLongtitude = Double.parseDouble(callForProposalContent.get("longtitude"));
		double clientLatitude = Double.parseDouble(callForProposalContent.get("latitude"));
		
		double distance = (longtitude-clientLongtitude)*(longtitude-clientLongtitude);
		distance += (latitude-clientLatitude)*(latitude-clientLatitude);
		
		if(distance <= maxDistance){
			logger.log(Level.INFO, "proposed - distance ok " );
			Map<String,String> proposal = new HashMap<String,String>();
			proposal.put("price", Double.toString(minPrice*1.5));
			return proposal;
		}
		else{
			logger.log(Level.INFO, " refused - distance too large " );
			return null;
		}
	}
	
}
