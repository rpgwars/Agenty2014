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

package pl.agh.edu.negotiationclient;


import jade.android.RuntimeCallback;
import jade.core.MicroRuntime;
import jade.util.Logger;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import java.util.Map;
import java.util.logging.Level;

import pl.agh.edu.mobileagentplatform.PlatformInitializer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * This activity implement the main interface.
 * 
 * @author Michele Izzo - Telecomitalia
 */

public class MainActivity extends Activity{
	private Logger logger = Logger.getJADELogger(this.getClass().getName());

	static final int CHAT_REQUEST = 0;
	static final int SETTINGS_REQUEST = 1;

	private MyReceiver myReceiver;
	private MyHandler myHandler;

	private TextView infoTextView;

	private String nickname;
	
	private NegotiationClientInterface negotiationClientInterface; 
	private NegotiationServerInterface negotiationServerInterface; 
	private PlatformInitializer platformInitializer; 
	
	private class MyReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			logger.log(Level.INFO, "Received price update intent " + action);
			if (action.equalsIgnoreCase("jade.demo.chat.KILL")) {
				finish();
			}
			if (action.equalsIgnoreCase("jade.demo.REFRESH")) {
				
				TextView priceTextView = (TextView)findViewById(R.id.priceTextView);
				priceTextView.setText(intent.getExtras().get("price").toString());
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		platformInitializer = PlatformInitializer.getInstance();
		
		myReceiver = new MyReceiver();

				
		IntentFilter priceFilter = new IntentFilter();
		priceFilter.addAction("jade.demo.REFRESH");
		registerReceiver(myReceiver, priceFilter);


		myHandler = new MyHandler();
		setContentView(R.layout.main);
		Button button = (Button) findViewById(R.id.button_chat7);
		button.setOnClickListener(buttonChatListener);

		Button getBestPrice = (Button)findViewById(R.id.button_getBestPrice);
		getBestPrice.setOnClickListener(requestSender);

		infoTextView = (TextView) findViewById(R.id.infoTextView);
		infoTextView.setText("");
		
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unregisterReceiver(myReceiver);

		logger.log(Level.INFO, "Destroy activity!");
	}
	
	private OnClickListener requestSender = new OnClickListener(){
		
		public void onClick(View view){
			
			try {
				Map<String, String> agentClassNameNickNameMap = PlatformInitializer.getInstance().getAgentClassNameNickNameMap();
				negotiationClientInterface = MicroRuntime.getAgent(agentClassNameNickNameMap.get(NegotiationClientAgent.class.getName())).getO2AInterface(NegotiationClientInterface.class);
				negotiationServerInterface = MicroRuntime.getAgent(agentClassNameNickNameMap.get(NegotiationParticipantAgent.class.getName())).getO2AInterface(NegotiationServerInterface.class);
				negotiationClientInterface.getBestPrice();
			} catch (StaleProxyException e) {
				logger.log(Level.SEVERE, "could not get best price: stale proxy exception");
			} catch (ControllerException e) {
				logger.log(Level.SEVERE, "could not get best price: controller exception");
			}
			
		}
		
	};
	
	private RuntimeCallback<Void> platformInitCallback = new RuntimeCallback<Void>(){

		@Override
		public void onFailure(Throwable arg) {
			
		}

		@Override
		public void onSuccess(Void arg) {
			platformInitializer.startAgent(NegotiationClientAgent.class.getName(),
					agentStartupCallback, new Object[] { getApplicationContext()});
			platformInitializer.startAgent(NegotiationParticipantAgent.class.getName(), agentStartupCallback, 
					new Object[]{getApplicationContext(),"100","50","20","30"});
		}
		
	};
	
	private RuntimeCallback<AgentController> agentStartupCallback = new RuntimeCallback<AgentController>() {
		@Override
		public void onSuccess(AgentController agent) {
			logger.log(Level.INFO, "Agent " + agent.getClass().getName() + "  successfully started!");
		}

		@Override
		public void onFailure(Throwable throwable) {
			logger.log(Level.INFO, "Nickname already in use!");
			myHandler.postError(getString(R.string.msg_nickname_in_use));
		}
	};


	private OnClickListener buttonChatListener = new OnClickListener() {
		public void onClick(View v) {
				
				try {
					EditText address = (EditText)findViewById(R.id.edit_address);
					String host = address.getText().toString();
					if(host == null || host.equals(""))
						host = "10.0.2.2";
					String port = "1099";
					infoTextView.setText(getString(R.string.msg_connecting_to)
							+ " " + host + ":" + port + "...");
					
					nickname = ((EditText)findViewById(R.id.edit_nickname)).getText().toString();
					platformInitializer.init(nickname, host, port, MainActivity.this, platformInitCallback);

					
				} catch (Exception ex) {
					logger.log(Level.SEVERE, "Unexpected exception creating chat agent!");
					infoTextView.setText(getString(R.string.msg_unexpected));
				}
			
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CHAT_REQUEST) {
			if (resultCode == RESULT_CANCELED) {
				// The chat activity was closed.
				infoTextView.setText("");
				platformInitializer.stop();
				
			}
		}
	}
	
	public void ShowDialog(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setMessage(message).setCancelable(false)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}


	private class MyHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			if (bundle.containsKey("error")) {
				infoTextView.setText("");
				String message = bundle.getString("error");
				ShowDialog(message);
			}
		}

		public void postError(String error) {
			Message msg = obtainMessage();
			Bundle b = new Bundle();
			b.putString("error", error);
			msg.setData(b);
			sendMessage(msg);
		}
	}
}

