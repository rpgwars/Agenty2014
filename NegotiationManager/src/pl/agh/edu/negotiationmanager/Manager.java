package pl.agh.edu.negotiationmanager;

import pl.agh.edu.mobileagentplatform.manager.ParticipantsManager;

public class Manager extends ParticipantsManager{

}

/*



import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.introspection.AMSSubscriber;
import jade.domain.introspection.DeadAgent;
import jade.domain.introspection.Event;
import jade.domain.introspection.IntrospectionOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SubscriptionResponder;
import jade.proto.SubscriptionResponder.Subscription;
import jade.proto.SubscriptionResponder.SubscriptionManager;
import jade.util.ExtendedProperties;
import jade.util.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.naming.PartialResultException;




public class Manager extends ParticipantsManager {
	private Map<AID, Subscription> participants = new HashMap<AID, Subscription>();
	private AMSSubscriber subscriber;
	private Logger logger = jade.util.Logger.getMyLogger(this.getClass().getName());
	
	
	protected void setup() {
		
		logger.log(Level.INFO, "creating manager");

		MessageTemplate sTemplate = MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE);	
		addBehaviour(new SubscriptionResponder(this, sTemplate, this));
		
		// Register to the AMS to detect when chat participants suddenly die
		subscriber = new AMSSubscriber() {

			private static final long serialVersionUID = 5032952459446276066L;

			protected void installHandlers(Map handlersTable) {
				// Fill the event handler table. We are only interested in the
				// DEADAGENT event
				handlersTable.put(IntrospectionOntology.DEADAGENT, new EventHandler() {
					public void handle(Event ev) {
						DeadAgent da = (DeadAgent)ev;
						AID id = da.getAgent();
						if (participants.containsKey(id)) {
							try {
								deregister((Subscription) participants.get(id));
							}
							catch (Exception e) {
								logger.log(Level.SEVERE, "Error during deregistration");
							}
						}
					}
				});
			}
		};
		addBehaviour(subscriber);
		addBehaviour(new ParticipantsRequestListener(this));
		
		logger.log(Level.INFO, "manager created");
	}



	@Override
	public boolean register(Subscription subscription) throws RefuseException, NotUnderstoodException { 
	
			
		AID newAid = subscription.getMessage().getSender();
		participants.put(newAid, subscription);
		logger.log(Level.INFO, "registered new member " + newAid.toString());
		return false;
		
	}

	public boolean deregister(Subscription subscription) throws FailureException {
		
		AID oldAid = subscription.getMessage().getSender();
		participants.remove(oldAid);
		logger.log(Level.INFO, "canceled subscription " + oldAid.toString());
		return false;
	}
	
	
	
	private class ParticipantsRequestListener extends CyclicBehaviour{

		
		private static final long serialVersionUID = 2229448248591933227L;
		public ParticipantsRequestListener(Agent agent){
			super(agent);
		}
		
		@Override
		public void action() {
			
			MessageTemplate performativeTemplate = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage request = receive(performativeTemplate);
			
			if(request != null){
				logger.log(Level.INFO, "received request for serwers ");
				ACLMessage response = new ACLMessage(ACLMessage.INFORM_REF);
				response.addReceiver(request.getSender());
				StringBuffer stringBuffer = new StringBuffer(); 
				for(AID aid : participants.keySet()){
					stringBuffer.append(aid.getLocalName());
					stringBuffer.append(";");
				}
				response.setContent(stringBuffer.toString());
				send(response);
				logger.log(Level.INFO, "server request response sent " + stringBuffer.toString() + " to " + request.getSender().toString());
			}
			else{
				block();
			}
			
		}

	}
	
}
*/