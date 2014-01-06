package pl.agh.edu.negotiationclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class OffertsActivity extends Activity {
	private static StableArrayAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		List<String> list = generateOtputData();
		MainActivity.listViewIsRunning = true;
		setContentView(R.layout.activity_listview);
		final ListView listview = (ListView) findViewById(R.id.listview);
		adapter = new StableArrayAdapter(this,
				android.R.layout.simple_list_item_1, list);
		listview.setAdapter(adapter);

	}
	
    @Override
    protected void onDestroy() {
		MainActivity.listViewIsRunning = false;
        super.onDestroy();
    }

	public static List<String> generateOtputData() {
		List<String> result = new ArrayList<String>();
		int i = 0;
		for (String key : MainActivity.destinationArray.keySet()) {
			result.add(key + ": "+ MainActivity.destinationArray.get(key) + " "
					+ MainActivity.offertsArray[i]);
			i++;
		}
		return result;
	}
	
	public static void notifyAboutChanges() {
		List<String> newList = generateOtputData();
		adapter.clear();
		adapter.addAll(newList);
		adapter.notifyDataSetChanged();
	}

	private class StableArrayAdapter extends ArrayAdapter<String> {

		HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

		public StableArrayAdapter(Context context, int textViewResourceId,
				List<String> objects) {
			super(context, textViewResourceId, objects);
			for (int i = 0; i < objects.size(); ++i) {
				mIdMap.put(objects.get(i), i);
			}
		}


		@Override
		public boolean hasStableIds() {
			return true;
		}

	}

}
