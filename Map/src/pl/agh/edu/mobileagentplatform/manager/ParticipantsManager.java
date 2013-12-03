package pl.agh.edu.mobileagentplatform.manager;

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
import jade.util.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;



/**
   This agent maintains knowledge of agents currently attending the 
   chat and inform them when someone joins/leaves the chat.
   @author Giovanni Caire - TILAB
 */
public class ParticipantsManager extends Agent implements SubscriptionManager {
	protected Map<AID, Subscription> participants = new HashMap<AID, Subscription>();
	protected ObjectMapper objectMapper;
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
		objectMapper = new ObjectMapper(); 
		logger.log(Level.INFO, "manager created");
	}


	///////////////////////////////////////////////
	// SubscriptionManager interface implementation
	///////////////////////////////////////////////
	
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
				List<String> addresses = new ArrayList<String>(participants.size());
				for(AID aid : participants.keySet())
					addresses.add(aid.getLocalName());
				try {
					response.setContent(objectMapper.writeValueAsString(addresses));
					logger.log(Level.INFO, "server participants information sent (" + participants.size() + ")");
				} catch (Exception e) {
					response.setContent(null);
					logger.log(Level.SEVERE, "server could send participants information");
				}
				send(response);
			}
			else{
				block();
			}
			
		}

	}
	
}
