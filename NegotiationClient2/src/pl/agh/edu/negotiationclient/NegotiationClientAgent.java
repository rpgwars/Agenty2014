package pl.agh.edu.negotiationclient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

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
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.ConversationList;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;



public class NegotiationClientAgent extends ContractNetInitiatorAgent implements NegotiationClientInterface{
	
	

	private Logger logger = Logger.getJADELogger(this.getClass().getName());
	
    //private static final String mainAgentId = "manager";	
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
	}
	
	
	private void showBestPrice(String price) {
		Intent broadcast = new Intent();
		broadcast.setAction("jade.demo.REFRESH");
		broadcast.putExtra("price", price);
		context.sendBroadcast(broadcast);
	}
	
	

	public void getBestPrice() {
	
		Map<String,String> message = new HashMap<String,String>(); 
		message.put("longtitude", "7");
		message.put("latitude", "8");
		ContractNetProposalEvaluator evaluator = new ContractNetProposalEvaluator() {
			
			@Override
			public int evaluate(Map<String, String> proposalContent) {
				return Integer.MAX_VALUE - (int)Double.parseDouble(proposalContent.get("price"))*100;
			}
			
			@Override
			public List<Integer> finalEvaluation(
					List<Map<String, String>> proposalsContent,
					List<Integer> proposalEvaluation) {
					/*
					List<Integer> retList = new ArrayList<Integer>(3);
					retList.add(0);
					retList.add(1);
					retList.add(2);
					return retList;
					*/
					return null; 
			}
		};
		
		evaluator.setParameters(3, 3, true, 3000);
		ContractNetInitiatorConversationState conversationState = startConversation(message, evaluator);
		new ContractNetResultsReceiver().execute(conversationState);

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
        	final Collection<Map<String, String>> values = conversationState.getAcceptedProposals().values();
        	if(values.size() > 0){
        		
        		
        		NegotiationClientAgent.this.addBehaviour(new SimpleBehaviour() {
					private int receivedCnt = 0; 
        			
					@Override
					public boolean done() {
						return receivedCnt >= values.size()*3;
					}
					
					@Override
					public void action() {
						MessageTemplate unknownTemplate = MessageTemplate.MatchPerformative(ACLMessage.UNKNOWN);
						
						ACLMessage message = receive(unknownTemplate);
						if(message != null){
							logger.log(Level.INFO, message.getContent());
							receivedCnt++; 
						}
						else {
							block();
						}
					}
				});
            	return "receiveing offerts from " + values.size() + " agents";
        	}

        	else
        		return "not enough offerts";
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
