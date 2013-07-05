package net.thebub.privacyproxy.activities;

import java.util.ArrayList;

import net.thebub.privacyproxy.PrivacyProxyAPI.APICommand;
import net.thebub.privacyproxy.PrivacyProxyAPI.APIResponse;
import net.thebub.privacyproxy.R;
import net.thebub.privacyproxy.util.ServerConnection;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MenuActivity extends Activity {
	
	private SharedPreferences mPreferences;
	
	private ListView mListView;
	
	private LogoutTask mLogoutTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);
		
		mPreferences = getSharedPreferences("PrivacyProxyPreferences", Context.MODE_PRIVATE);
		
		mListView = (ListView) findViewById(R.id.menu_list);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				Intent menuActionIntent = null;
				Boolean execute = true;
				
				switch (position) {
				case 0:
					menuActionIntent = new Intent(parent.getContext(), WeblogActivity.class);
					break;
				case 1:
					menuActionIntent = new Intent(parent.getContext(), SettingsActivity.class);
					break;
				case 2:
					mLogoutTask = new LogoutTask();
					mLogoutTask.execute();
					execute = false;
					break;
				}
				
				if(execute){
					startActivity(menuActionIntent);
				}
			}
			
		});
		
		ArrayList<String> menuEntries = new ArrayList<String>();
		
		menuEntries.add(getString(R.string.menu_weblog));
		menuEntries.add(getString(R.string.menu_settings));
		menuEntries.add(getString(R.string.menu_logout));
		
		mListView.setAdapter(new MenuListAdapter(this, R.layout.layout_weblog_entry, menuEntries));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
	
	protected void showLogin() {
		Intent menuActionIntent = new Intent(MenuActivity.this, LoginActivity.class);
		
		menuActionIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		
		startActivity(menuActionIntent);		
	}
	
	private class MenuListAdapter extends ArrayAdapter<String> {

		private ArrayList<String> menuItemList;

		public MenuListAdapter(Context context, int textViewResourceId, 
				ArrayList<String> items) {
			super(context, textViewResourceId, items);
			this.menuItemList = new ArrayList<String>();
			this.menuItemList.addAll(items);
		}

		private class ViewHolder {
			TextView url;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {

				LayoutInflater vi = (LayoutInflater) getSystemService(
						Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.layout_weblog_entry, null);

				holder = new ViewHolder();
				holder.url = (TextView) convertView.findViewById(R.id.webloglist_entry_title);

				convertView.setTag(holder);

			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			String menuEntry = menuItemList.get(position);
			holder.url.setText(menuEntry);

			return convertView;
		}

	}
	
private class LogoutTask extends AsyncTask<Void, Void, Boolean> {
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Boolean doInBackground(Void... none) {

			ServerConnection connection = ServerConnection.getInstance();

			String sessionID = mPreferences.getString(getString(R.string.pref_session_id), "");
			
			APIResponse response = connection.sendRequest(APICommand.logout,sessionID);

			if(response == null || !response.getSuccess()) {
				return false;
			}

			return true;
		}

		@Override
		protected void onPostExecute(final Boolean data) {
			mLogoutTask = null;
			
			if (data) {				
				showLogin();
			}
		}

		@Override
		protected void onCancelled() {
			mLogoutTask = null;
		}
	}
}
