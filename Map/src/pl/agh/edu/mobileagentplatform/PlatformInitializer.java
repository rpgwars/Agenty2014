package pl.agh.edu.mobileagentplatform;

import jade.android.AndroidHelper;
import jade.android.MicroRuntimeService;
import jade.android.MicroRuntimeServiceBinder;
import jade.android.RuntimeCallback;
import jade.core.MicroRuntime;
import jade.core.Profile;
import jade.util.Logger;
import jade.util.leap.Properties;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;

import java.util.logging.Level;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;


public class PlatformInitializer {
	
	private Logger logger = Logger.getJADELogger(this.getClass().getName());

	private MicroRuntimeServiceBinder microRuntimeServiceBinder = null;
	private ServiceConnection serviceConnection = null;
	
	private static PlatformInitializer instance = null; 

	private PlatformInitializer(){
		
	}
	
	public static PlatformInitializer getInstance(){
		if(instance == null){
			instance = new PlatformInitializer(); 
		}
		return instance; 
	}
	
	public void init(String host, String port, Activity activity, RuntimeCallback<Void> initCallback){
		initMicroRutime(host, port, activity, initCallback);
	}
	
	public void stop(){
		logger.log(Level.INFO, "Stopping Jade...");
		microRuntimeServiceBinder.stopAgentContainer(new RuntimeCallback<Void>() {
			@Override
			public void onSuccess(Void thisIsNull) {
				logger.log(Level.INFO, "Sucessfully stoped the container...");
			}
			
			@Override
			public void onFailure(Throwable throwable) {
				logger.log(Level.SEVERE, "Failed to stop the container...");
				}
			});

	}

	private void initMicroRutime(final String host, final String port, Activity activity, final RuntimeCallback<Void> initCallback) {

		final Properties profile = new Properties();
		profile.setProperty(Profile.MAIN_HOST, host);
		profile.setProperty(Profile.MAIN_PORT, port);
		profile.setProperty(Profile.MAIN, Boolean.FALSE.toString());
		profile.setProperty(Profile.JVM, Profile.ANDROID);
		logger.log(Level.INFO, "info " + host);

		if (AndroidHelper.isEmulator()) {
			// Emulator: this is needed to work with emulated devices
			profile.setProperty(Profile.LOCAL_HOST, AndroidHelper.LOOPBACK);
		} else {
			profile.setProperty(Profile.LOCAL_HOST,
					AndroidHelper.getLocalIPAddress());
		}
		// Emulator: this is not really needed on a real device
		profile.setProperty(Profile.LOCAL_PORT, "2000");

		if (microRuntimeServiceBinder == null) {
			serviceConnection = new ServiceConnection() {
				public void onServiceConnected(ComponentName className,
						IBinder service) {
					microRuntimeServiceBinder = (MicroRuntimeServiceBinder) service;
					logger.log(Level.INFO, "Gateway successfully bound to MicroRuntimeService");
					startContainer(profile,initCallback);
					
				};

				public void onServiceDisconnected(ComponentName className) {
					microRuntimeServiceBinder = null;
					logger.log(Level.INFO, "Gateway unbound from MicroRuntimeService");
				}
			};
			logger.log(Level.INFO, "Binding Gateway to MicroRuntimeService...");
			activity.bindService(new Intent(activity.getApplicationContext(),
					MicroRuntimeService.class), serviceConnection,
					Context.BIND_AUTO_CREATE);
		} else {
			logger.log(Level.INFO, "MicroRumtimeGateway already binded to service");
			startContainer(profile,initCallback);
		}
	}

	private void startContainer(Properties profile, final RuntimeCallback<Void> initCallback) {
		if (!MicroRuntime.isRunning()) {
			microRuntimeServiceBinder.startAgentContainer(profile,
					new RuntimeCallback<Void>() {
						@Override
						public void onSuccess(Void thisIsNull) {
							logger.log(Level.INFO, "Successfully start of the container...");
							initCallback.onSuccess(null);
						}

						@Override
						public void onFailure(Throwable throwable) {
							logger.log(Level.SEVERE, "Failed to start the container...");
						}
					});
		} 
	}

	public void startAgent(final String nickname, final String className, 
			final RuntimeCallback<AgentController> agentStartupCallback, Object[] parameters) {
		microRuntimeServiceBinder.startAgent(nickname,
				className, parameters,
				new RuntimeCallback<Void>() {
					@Override
					public void onSuccess(Void thisIsNull) {
						logger.log(Level.INFO, "Successfully start of the " + className);
						try {
							agentStartupCallback.onSuccess(MicroRuntime
									.getAgent(nickname));
						} catch (ControllerException e) {
							agentStartupCallback.onFailure(e);
						}
					}

					@Override
					public void onFailure(Throwable throwable) {
						logger.log(Level.SEVERE, "Failed to start the agent " + className);
						agentStartupCallback.onFailure(throwable);
					}
				});
	}


}

