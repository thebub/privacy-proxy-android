package net.thebub.privacyproxy.activities;

import java.util.ArrayList;
import java.util.List;

import net.thebub.privacyproxy.PrivacyProxyAPI.APICommand;
import net.thebub.privacyproxy.PrivacyProxyAPI.APIResponse;
import net.thebub.privacyproxy.PrivacyProxyAPI.GetSettingsResponse;
import net.thebub.privacyproxy.PrivacyProxyAPI.PersonalDataEntry;
import net.thebub.privacyproxy.R;
import net.thebub.privacyproxy.util.DataTypeIDs;
import net.thebub.privacyproxy.util.ServerConnection;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;

public class SettingsActivity extends Activity {

	public ListView mListView;
	private SettingsListAdapter listAdapter;
	private GetSettingsTask mSettingsTask;
	
	private SharedPreferences mPreferences;
	private View mLoadingScreen;


	private class GetSettingsTask extends AsyncTask<Void, Void, List<PersonalDataEntry>> {
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			mListView.setVisibility(View.GONE);
			mLoadingScreen.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected List<PersonalDataEntry> doInBackground(Void... none) {

			ServerConnection connection = ServerConnection.getInstance();

			String sessionID = mPreferences.getString(getString(R.string.pref_session_id), "");
			
			APIResponse response = connection.sendRequest(APICommand.getSettings,sessionID);

			if(response == null || !response.getSuccess()) {
				return null;
			}

			GetSettingsResponse settingsResponse;
			try {
				settingsResponse = GetSettingsResponse.parseFrom(response.getData());
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
				return null;
			}

			return settingsResponse.getEntryList();
		}

		@Override
		protected void onPostExecute(final List<PersonalDataEntry> data) {
			mSettingsTask = null;

			ArrayList<PersonalDataEntry> dataArray = new ArrayList<PersonalDataEntry>(data);
			
			if (data != null) {				
				displaySettings(dataArray);
				mLoadingScreen.setVisibility(View.GONE);
				mListView.setVisibility(View.VISIBLE);
			} else {
				mLoadingScreen.setVisibility(View.GONE);
				mListView.setVisibility(View.VISIBLE);				
			}
		}

		@Override
		protected void onCancelled() {
			mSettingsTask = null;
			mLoadingScreen.setVisibility(View.GONE);
			mListView.setVisibility(View.VISIBLE);
		}
	}

	private class SettingsListAdapter extends ArrayAdapter<PersonalDataEntry> {

		private ArrayList<PersonalDataEntry> settingsItemList;

		public SettingsListAdapter(Context context, int textViewResourceId, 
				ArrayList<PersonalDataEntry> settingsList) {
			super(context, textViewResourceId, settingsList);
			this.settingsItemList = new ArrayList<PersonalDataEntry>();
			this.settingsItemList.addAll(settingsList);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {

				LayoutInflater vi = (LayoutInflater) getSystemService(
						Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.layout_settings_entry, null);
			}
			
			PersonalDataEntry setting = settingsItemList.get(position);
			
			TextView title = (TextView) convertView.findViewById(R.id.settingslist_entry_title);
			title.setText(setting.getDescription());
			TextView type = (TextView) convertView.findViewById(R.id.settingslist_entry_type);
			type.setText(getString(DataTypeIDs.getID(setting.getType())));

			return convertView;

		}

	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		this.mListView = (ListView) findViewById(R.id.settings_list_view);
		this.mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				PersonalDataEntry item = (PersonalDataEntry) parent.getAdapter().getItem(position);
				
				Intent openSettingDeatilsIntent = new Intent(parent.getContext(), SettingDetailActivity.class);
				
				openSettingDeatilsIntent.putExtra("settingID", item.getId());
				openSettingDeatilsIntent.putExtra("settingDesc", item.getDescription());
				openSettingDeatilsIntent.putExtra("settingType", getString(DataTypeIDs.getID(item.getType())));
				openSettingDeatilsIntent.putExtra("settingData", item);
				
				startActivity(openSettingDeatilsIntent);
			}
			
		});
		
		mPreferences = getSharedPreferences("PrivacyProxyPreferences", Context.MODE_PRIVATE); 

		mLoadingScreen = findViewById(R.id.settings_status);
		
		mSettingsTask = new GetSettingsTask();
		mSettingsTask.execute();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu_settingslist, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.settings_menu_add:
	            newSetting();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	private void displaySettings(ArrayList<PersonalDataEntry> entries) {
		listAdapter = new SettingsListAdapter(this, R.layout.layout_weblog_entry, entries);

		mListView.setAdapter(listAdapter);
	}
	
	
	private void newSetting() {
		Intent openCreateSettingIntent = new Intent(this, SettingCreateActivity.class);
				
		startActivity(openCreateSettingIntent);
	}
}
