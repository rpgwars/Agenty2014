package pl.agh.edu.mobileagentplatform.negotiationprotocols;

import java.util.List;
import java.util.Map;

public abstract class ContractNetProposalEvaluator {
	
	public abstract int evaluate(Map<String,String> proposalContent);
	public List<Integer> finalEvaluation(List<Map<String,String>> proposalsContent, List<Integer> proposalEvaluation){
		return null; 
	}
	
	private Parameters parameters = new Parameters(5000);
	
	public void setParameters(Parameters parameters){
		this.parameters = parameters; 
	}
	
	Parameters getParameters(){
		return parameters;
	}
	
	class Parameters {
		
		private int maxAccepted = 1; 
		private int minAccepted = 1;
		private boolean waitTillTimeExceeds = true;
		private long proposalReceiveDuration; 
		
		public Parameters(long proposalReceiveDuration){
			this.proposalReceiveDuration = proposalReceiveDuration; 
		}
		
		public Parameters setMaxAccepted(int maxAccepted){
			this.maxAccepted = maxAccepted;
			return this;
		}
		
		int getMaxAccepted(){
			return maxAccepted;
		}
		
		public Parameters setMinAccepted(int minAccepted){
			this.minAccepted = minAccepted; 
			return this; 
		}
		
		int getMinAccepted(){
			return minAccepted;
		}
		
		public Parameters setWaitTillTimeExceeds(boolean waitTillTimeExceeds){
			this.waitTillTimeExceeds = waitTillTimeExceeds; 
			return this;
		}
		
		boolean isWaitTillTimeExceeds(){
			return waitTillTimeExceeds;
		}
		
		long getProposalReciveDuration(){
			return proposalReceiveDuration;
		}
		
	}
}
