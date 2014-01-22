package pl.agh.edu.negotiationclient;

import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import pl.agh.edu.mobileagentplatform.negotiationprotocols.ContractNetInitiatorAgent;
import pl.agh.edu.mobileagentplatform.negotiationprotocols.ContractNetInitiatorConversationState;
import pl.agh.edu.mobileagentplatform.negotiationprotocols.ContractNetInitiatorConversationStatus;
import pl.agh.edu.mobileagentplatform.negotiationprotocols.ContractNetProposalEvaluator;


public class SimpleNegotiationClient extends ContractNetInitiatorAgent{
	
	private static final String manager = "manager";
	private Logger logger = jade.util.Logger.getMyLogger(this.getClass().getName());
	private int maxAccepted; 
	private int minAccepted; 
	private String longtitude;
	private String latitude;
	
	@Override
	protected void setup() {
		super.setup();
		
		ACLMessage subscription = new ACLMessage(ACLMessage.SUBSCRIBE);
		String convId = "C-" + getLocalName();
		subscription.setConversationId(convId);
		subscription.addReceiver(new AID(manager, AID.ISLOCALNAME));
		send(subscription);
		
		Object[] args = getArguments(); 
		maxAccepted = Integer.parseInt((String)args[0]);
		minAccepted = Integer.parseInt((String)args[1]);
		longtitude = (String)args[2];
		latitude = (String)args[3];
		
		logger.log(Level.INFO, "initiator agent started lon:" + longtitude + " lat:" + latitude);
		Thread t = new Thread(){
			public void run(){
				getBestPrice();
			}
		};
		t.start();
		
	}
	
	
	public void getBestPrice() {
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Map<String, String> message = new HashMap<String, String>();
		message.put("longtitude", longtitude);
		message.put("latitude", latitude);
		ContractNetProposalEvaluator evaluator = new ContractNetProposalEvaluator() {

			@Override
			public int evaluate(Map<String, String> proposalContent) {
				return Integer.MAX_VALUE
						- (int) Double
								.parseDouble(proposalContent.get("price"))
						* 100;
			}

			@Override
			public List<Integer> finalEvaluation(
					List<Map<String, String>> proposalsContent,
					List<Integer> proposalEvaluation) {
	
				return null;
			}
		};

		evaluator.setParameters(maxAccepted, minAccepted, true, 3000);
		ContractNetInitiatorConversationState conversationState = startConversation(
				message, evaluator);
		new ContractNetResultsReceiver(conversationState).start();

	}

	private class ContractNetResultsReceiver extends Thread {
		
		private ContractNetInitiatorConversationState conversationState = null;
		
		public ContractNetResultsReceiver(ContractNetInitiatorConversationState conversationState){
			this.conversationState = conversationState;
		}
		
		@Override
		public void run(){
			

			while (conversationState.getStatus() != ContractNetInitiatorConversationStatus.FINISHED_STATUS) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			final Collection<Map<String, String>> values = conversationState
					.getAcceptedProposals().values();
			if (values.size() > 0) {

				String result = "received offerts from " + values.size()
						+ " agents@";
				for (Map<String, String> value : values) {
					for (String key : value.keySet()) {
						result = result + key + " " + value.get(key) + "#";
					}
				}

				SimpleNegotiationClient.this.addBehaviour(new SimpleBehaviour() {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;
					private int receivedCnt = 0;

					@Override
					public boolean done() {
						return receivedCnt >= values.size() * 3;
					}

					@Override
					public void action() {
						MessageTemplate unknownTemplate = MessageTemplate
								.MatchPerformative(ACLMessage.UNKNOWN);

						ACLMessage message = receive(unknownTemplate);
						if (message != null) {
								notifyPositions(
									message.getSender().getLocalName(),
									message.getContent()
											.replace(
													message.getSender()
															.getName(), "")
											.replace("  ", " "));

							logger.log(Level.INFO, message.getContent());
							receivedCnt++;
						} else {
							block();
						}
					}

				});

				showResult(result);
			}

			else
				showResult("not enough offerts");
		}
		
		private void showResult(String result){
			logger.log(Level.INFO, result);
		}
		
		private void notifyPositions(String agent, String position){
			logger.log(Level.INFO, agent + " || " + position);
		}

	}

}
