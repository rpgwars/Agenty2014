package pl.agh.edu.mobileagentplatform.testenvironment;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;

public class AgentCreator extends Agent {
	
	private static final long serialVersionUID = 9188767811557572382L;
	private Logger logger = Logger.getJADELogger(this.getClass().getName());
	
	@Override
	protected void setup() {
		super.setup();
		addBehaviour(new AgentCreation());
		Object[] arguments = getArguments();
		
		//create agents from file
		if(arguments != null && arguments.length == 1){
			createAgents(arguments[0].toString());
		}
			
		
	}
	
	
	private class AgentCreation extends CyclicBehaviour {

		private static final long serialVersionUID = 9184562214970161837L;
		
		@Override
		public void action() {
						
			MessageTemplate requestTemplate = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage request = receive(requestTemplate);
			
			if(request != null){
				createAgents(request.getContent());
			}	
			else{
				block(); 
			}
			
		}
		
	}
	
	private void createAgents(String filePath){
		try{
			BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));
			for(String line; (line = bufferedReader.readLine()) != null; ){
				createAgent(line);
			}	
			bufferedReader.close();
		}
		catch(IOException ioe){
			logger.log(Level.SEVERE, "Could not read data from file " + filePath);
		}
	
	}
	
	private void createAgent(String agentDescription) {
		
		String[] agent = agentDescription.split("\\s");
		Object[] agentArguments = new Object[agent.length - 2];
		for(int i = 0; i<agent.length - 2; i++)
			agentArguments[i] = agent[i+2];
		String agentName = agent[0]; 
		String agentClassName = agent[1]; 
		try {
			((AgentController)getContainerController().createNewAgent(agentName,agentClassName,agentArguments)).start();
			logger.log(Level.INFO, "Agent " + agentClassName + " created");
		} catch (StaleProxyException e) {
			logger.log(Level.SEVERE, "Can not create agent because of stale proxy");
		}
		
	}

}
