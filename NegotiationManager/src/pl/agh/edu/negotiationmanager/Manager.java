package pl.agh.edu.negotiationmanager;

/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
 *****************************************************************/

//#J2ME_EXCLUDE_FILE

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
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
import jade.util.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;



/**
   This agent maintains knowledge of agents currently attending the 
   chat and inform them when someone joins/leaves the chat.
   @author Giovanni Caire - TILAB
 */
public class Manager extends Agent implements SubscriptionManager {
	private Map<AID, Subscription> participants = new HashMap<AID, Subscription>();
	private Ontology onto = BasicOntology.getInstance();
	private Codec codec = new SLCodec();
	private AMSSubscriber myAMSSubscriber;
	private Logger logger = jade.util.Logger.getMyLogger(this.getClass().getName());
	
	
	protected void setup() {
		
		logger.log(Level.INFO, "creating manager");
		// Prepare to accept subscriptions from chat participants
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(onto);
		
		
		
		MessageTemplate sTemplate = MessageTemplate.and(
				MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE),
				MessageTemplate.and(
						MessageTemplate.MatchLanguage(codec.getName()),
						MessageTemplate.MatchOntology(onto.getName()) ) );
		addBehaviour(new SubscriptionResponder(this, sTemplate, this));
		
		// Register to the AMS to detect when chat participants suddenly die
		myAMSSubscriber = new AMSSubscriber() {

			private static final long serialVersionUID = 5032952459446276066L;

			protected void installHandlers(Map handlersTable) {
				// Fill the event handler table. We are only interested in the
				// DEADAGENT event
				handlersTable.put(IntrospectionOntology.DEADAGENT, new EventHandler() {
					public void handle(Event ev) {
						DeadAgent da = (DeadAgent)ev;
						AID id = da.getAgent();
						// If the agent was attending the chat --> notify all
						// other participants that it has just left.
						if (participants.containsKey(id)) {
							try {
								deregister((Subscription) participants.get(id));
							}
							catch (Exception e) {
								//Should never happen
								e.printStackTrace();
							}
						}
					}
				});
			}
		};
		addBehaviour(myAMSSubscriber);
		addBehaviour(new RequestListener(this));
		
		logger.log(Level.INFO, "manager created");
	}

	protected void takeDown() {
		// Unsubscribe from the AMS
		send(myAMSSubscriber.getCancel());
		//FIXME: should inform current participants if any
	}

	///////////////////////////////////////////////
	// SubscriptionManager interface implementation
	///////////////////////////////////////////////
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
	
	
	
	private class RequestListener extends CyclicBehaviour{

		
		private static final long serialVersionUID = 2229448248591933227L;
		public RequestListener(Agent agent){
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
