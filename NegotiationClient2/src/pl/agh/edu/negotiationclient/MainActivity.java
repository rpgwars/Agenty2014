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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import pl.agh.edu.mobileagentplatform.PlatformInitializer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
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

public class MainActivity extends Activity {
	private Logger logger = Logger.getJADELogger(this.getClass().getName());

	static final int CHAT_REQUEST = 0;
	static final int SETTINGS_REQUEST = 1;
	private int firstParameter;
	private int secondParameter;
	public static String[] offertsArray;
	public static Map<String, String> destinationArray = new HashMap<String, String>();
	public static int MODE;

	private MyReceiver myReceiver;
	private MyHandler myHandler;

	private LocationManager mLocationManager = null;
	public Location agentLocation = null;

	private TextView infoTextView;

	private String nickname;
	private Button listViewButton;
	public static boolean listViewIsRunning = false;

	private NegotiationClientInterface negotiationClientInterface;
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

				TextView priceTextView = (TextView) findViewById(R.id.priceTextView);
				String result = intent.getExtras().get("price").toString();
				String[] firstSplit = result.split("@");
				if (firstSplit.length > 1) {
					String[] splitValues = firstSplit[1].split("#");
					offertsArray = new String[splitValues.length];
					for (int i = 0; i < splitValues.length; i++) {
						offertsArray[i] = splitValues[i];
					}
					priceTextView.setText(firstSplit[0]);
					listViewButton.performClick();
				} else {
					priceTextView.setText(result);
				}

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

		Button getBestPrice = (Button) findViewById(R.id.button_getBestPrice);
		getBestPrice.setOnClickListener(requestSender);

		infoTextView = (TextView) findViewById(R.id.infoTextView);
		infoTextView.setText("");

		listViewButton = (Button) findViewById(R.id.lsitViewButton);
		listViewButton.setOnClickListener(listViewButtonListener);

		initializeLocationManager();
		agentLocation = mLocationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (agentLocation == null) {
			agentLocation = mLocationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}

		if (agentLocation != null) {
			logger.log(Level.INFO, "lat=" + agentLocation.getLatitude()
					+ " lon=" + agentLocation.getLongitude());
		}
	}

	@Override
	protected void onDestroy() {
		MainActivity.offertsArray = null;
		MainActivity.destinationArray.clear();
		platformInitializer.stop();
		super.onDestroy();
		// finish();

		unregisterReceiver(myReceiver);

		logger.log(Level.INFO, "Destroy activity!");
	}

	private OnClickListener requestSender = new OnClickListener() {

		public void onClick(View view) {

			try {
				Map<String, String> agentClassNameNickNameMap = PlatformInitializer
						.getInstance().getAgentClassNameNickNameMap();
				if (MODE == 1 || MODE == 2) {

					negotiationClientInterface = MicroRuntime
							.getAgent(
									agentClassNameNickNameMap
											.get(NegotiationClientAgent.class
													.getName()))
							.getO2AInterface(NegotiationClientInterface.class);
				}
				if (MODE == 1 || MODE == 3) {
					MicroRuntime.getAgent(
							agentClassNameNickNameMap
									.get(NegotiationParticipantAgent.class
											.getName())).getO2AInterface(
							NegotiationServerInterface.class);
				}
				if (MODE == 1 || MODE == 2) {
					negotiationClientInterface.loadParameter(firstParameter,
							secondParameter,
							String.valueOf(agentLocation.getLatitude()),
							String.valueOf(agentLocation.getLongitude()));
					negotiationClientInterface.getBestPrice();
				}
			} catch (StaleProxyException e) {
				logger.log(Level.SEVERE,
						"could not get best price: stale proxy exception");
			} catch (ControllerException e) {
				logger.log(Level.SEVERE,
						"could not get best price: controller exception");
			}

		}

	};

	private OnClickListener listViewButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (offertsArray == null) {
				ShowDialog("Nie otrzymano ofert");
			} else {
				Intent intent = new Intent(MainActivity.this,
						OffertsActivity.class);
				startActivity(intent);
			}
		}
	};

	private RuntimeCallback<Void> platformInitCallback = new RuntimeCallback<Void>() {

		@Override
		public void onFailure(Throwable arg) {

		}

		@Override
		public void onSuccess(Void arg) {
			if (MODE == 1 || MODE == 2) {
				platformInitializer.startAgent(
						NegotiationClientAgent.class.getName(),
						agentStartupCallback,
						new Object[] { getApplicationContext() });
			}
			if (MODE == 1 || MODE == 3) {
				platformInitializer.startAgent(
						NegotiationParticipantAgent.class.getName(),
						agentStartupCallback,
						new Object[] { getApplicationContext(),
								String.valueOf(agentLocation.getLatitude()),
								String.valueOf(agentLocation.getLongitude()),
								"5", "2" });
			}
		}

	};

	private RuntimeCallback<AgentController> agentStartupCallback = new RuntimeCallback<AgentController>() {
		@Override
		public void onSuccess(AgentController agent) {
			logger.log(Level.INFO, "Agent " + agent.getClass().getName()
					+ "  successfully started!");
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
				EditText modeTextfield = (EditText) findViewById(R.id.edit_mode);
				String mode = modeTextfield.getText().toString();
				if (!mode.matches("")) {
					MODE = Integer.parseInt(mode);
				} else {
					MODE = 1;
				}

				EditText address = (EditText) findViewById(R.id.edit_address);
				String host = address.getText().toString();
				if (host == null || host.equals(""))
					host = "192.168.1.100";
				// host = "10.0.2.2";
				String port = "1099";
				infoTextView.setText(getString(R.string.msg_connecting_to)
						+ " " + host + ":" + port + "...");

				nickname = ((EditText) findViewById(R.id.edit_nickname))
						.getText().toString();

				String tmpParam1 = ((EditText) findViewById(R.id.edit_param1))
						.getText().toString();
				String tmpParam2 = ((EditText) findViewById(R.id.edit_param2))
						.getText().toString();
				if (!tmpParam1.matches("") && !tmpParam2.matches("")) {
					firstParameter = Integer.parseInt(tmpParam1);
					secondParameter = Integer.parseInt(tmpParam2);
				} else {
					if (MODE == 1) {
						firstParameter = 1;
						secondParameter = 1;
					} else {
						firstParameter = 3;
						secondParameter = 3;
					}

				}

				platformInitializer.init(nickname, host, port,
						MainActivity.this, platformInitCallback);

			} catch (Exception ex) {
				logger.log(Level.SEVERE,
						"Unexpected exception creating chat agent!");
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

	@SuppressLint("HandlerLeak")
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

	private void initializeLocationManager() {
		Log.d("service_loc", "initializeLocationManager");
		if (mLocationManager == null) {
			mLocationManager = (LocationManager) getApplicationContext()
					.getSystemService(Context.LOCATION_SERVICE);
		}
	}

}
