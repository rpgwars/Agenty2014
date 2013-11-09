package pl.agh.edu.negotiationclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import pl.agh.edu.mobileagentplatform.negotiationprotocols.ContractNetInitiatorAgent;
import pl.agh.edu.mobileagentplatform.negotiationprotocols.ContractNetInitiatorConversationState;
import pl.agh.edu.mobileagentplatform.negotiationprotocols.ContractNetInitiatorConversationStatus;
import pl.agh.edu.mobileagentplatform.negotiationprotocols.ContractNetProposalEvaluator;


import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
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



public class NegotiationClientAgent extends ContractNetInitiatorAgent implements NegotiationClientInterface{
	
	

	private Logger logger = Logger.getJADELogger(this.getClass().getName());
	
    private static final String mainAgentId = "manager";	
	private Context context;

	
	protected void setup() {
		super.setup();
		Object[] args = getArguments();
		
		if (args != null && args.length > 0) {
			if (args[0] instanceof Context) {
				context = (Context) args[0];
			}
		}	
		
		registerO2AInterface(NegotiationClientInterface.class, this);
		addBehaviour(new ManagerResponseListener(this));
	}
	
	
	private void showBestPrice(String price) {
		Intent broadcast = new Intent();
		broadcast.setAction("jade.demo.REFRESH");
		broadcast.putExtra("price", price);
		context.sendBroadcast(broadcast);
	}
	
	

	public void getBestPrice() {
		
		ACLMessage simpleMessage = new ACLMessage(ACLMessage.REQUEST);		
		AID receiverAid = new AID(mainAgentId,AID.ISLOCALNAME);
		simpleMessage.addReceiver(receiverAid);
		send(simpleMessage);
		
		logger.log(Level.INFO, "servers request sent");

	}
	
	private List<AID> serverAgentList; 
	
	private class ManagerResponseListener extends CyclicBehaviour{
		
		public ManagerResponseListener(Agent agent){
			super(agent);

		}
		
		@Override
		public void action() {
			
			MessageTemplate informRefTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF);
			
			ACLMessage informRef = receive(informRefTemplate);
			if(informRef != null){
				String[] aids = informRef.getContent().split(";");
				serverAgentList = new ArrayList<AID>(aids.length);
				for(String aid : aids){
					serverAgentList.add(new AID(aid, AID.ISLOCALNAME));
					logger.log(Level.INFO, "received aid " + aid);
				}
				Map<String,String> message = new HashMap<String,String>(); 
				message.put("longtitude", "7");
				message.put("latitude", "8");
				ContractNetProposalEvaluator evaluator = new ContractNetProposalEvaluator() {
					
					@Override
					public int evaluate(Map<String, String> proposalContent) {
						return 1;
					}
					
					@Override
					public List<Integer> finalEvaluation(
							List<Map<String, String>> proposalsContent,
							List<Integer> proposalEvaluation) {
							
							double minPrice = Double.MAX_VALUE;
							int position = -1; 
							for(int i = 0; i<proposalsContent.size(); i++){
								double price = Double.parseDouble(proposalsContent.get(i).get("price"));
								if(minPrice > price){
									minPrice = price; 
									position = i; 
								}
							}
							List<Integer> retList = new ArrayList<Integer>(1); 
							retList.add(position);
							return retList;
					}
				};
				
				ContractNetInitiatorConversationState conversationState = startConversation(message, serverAgentList, evaluator);
				new ContractNetResultsReceiver().execute(conversationState);
				
			}
			
			else{
				block(); 
			}
			
		}
		
	}
	
    private class ContractNetResultsReceiver extends AsyncTask<ContractNetInitiatorConversationState, Void, String> {

        @Override
        protected String doInBackground(ContractNetInitiatorConversationState... params) {
        	ContractNetInitiatorConversationState conversationState = params[0];
        	
        	while(conversationState.getStatus() != ContractNetInitiatorConversationStatus.FINISHED_STATUS){
	            try {
	                Thread.sleep(1000);
	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }
        	}
        	Collection<Map<String, String>> values = conversationState.getAcceptedProposals().values();
        	
        	for(Map<String,String> value : values){
        		return value.get("price"); 
        	}
        	return "no offerts";
        }

        @Override
        protected void onPostExecute(String bestPrice) {
        	showBestPrice(bestPrice);
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

}
