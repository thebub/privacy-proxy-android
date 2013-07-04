package net.thebub.privacyproxy.activities;

import java.util.ArrayList;
import java.util.List;

import net.thebub.privacyproxy.PrivacyProxyAPI.APICommand;
import net.thebub.privacyproxy.PrivacyProxyAPI.APIResponse;
import net.thebub.privacyproxy.PrivacyProxyAPI.WebLogWebsitesResponse;
import net.thebub.privacyproxy.PrivacyProxyAPI.WebLogWebsitesResponse.WebLogWebsite;
import net.thebub.privacyproxy.R;
import net.thebub.privacyproxy.util.ServerConnection;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;

public class WeblogActivity extends Activity {

	public ListView mListView;
	private WebLogListAdapter listAdapter;
	private GetWeblogTask mWebLogTask;
	
	private SharedPreferences mPreferences;
	private View mLoadingScreen;

	private class GetWeblogTask extends AsyncTask<Void, Void, List<WebLogWebsite>> {
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			mListView.setVisibility(View.GONE);
			mLoadingScreen.setVisibility(View.VISIBLE);
		}

		@Override
		protected List<WebLogWebsite> doInBackground(Void... none) {

			ServerConnection connection = ServerConnection.getInstance();

			String sessionID = mPreferences.getString(getString(R.string.pref_session_id), "");
			
			APIResponse response = connection.sendRequest(APICommand.getWebpages,sessionID);

			if(response == null || !response.getSuccess()) {
				return null;
			}

			WebLogWebsitesResponse websitesResponse;
			try {
				websitesResponse = WebLogWebsitesResponse.parseFrom(response.getData());
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
				return null;
			}

			return websitesResponse.getPagesList();
		}

		@Override
		protected void onPostExecute(final List<WebLogWebsite> data) {
			mWebLogTask = null;

			ArrayList<WebLogWebsite> dataArray = new ArrayList<WebLogWebsite>(data);
			
			if (data != null) {				
				displayWebLogList(dataArray);
				mLoadingScreen.setVisibility(View.GONE);
				mListView.setVisibility(View.VISIBLE);
			} else {
				mLoadingScreen.setVisibility(View.GONE);
				mListView.setVisibility(View.VISIBLE);
			}
		}

		@Override
		protected void onCancelled() {
			mWebLogTask = null;
			mLoadingScreen.setVisibility(View.GONE);
			mListView.setVisibility(View.VISIBLE);
		}
	}

	private class WebLogListAdapter extends ArrayAdapter<WebLogWebsite> {

		private ArrayList<WebLogWebsite> weblogItemList;

		public WebLogListAdapter(Context context, int textViewResourceId, 
				ArrayList<WebLogWebsite> countryList) {
			super(context, textViewResourceId, countryList);
			this.weblogItemList = new ArrayList<WebLogWebsite>();
			this.weblogItemList.addAll(countryList);
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

			WebLogWebsite website = weblogItemList.get(position);
			holder.url.setText(website.getWebsite());

			return convertView;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_weblog);

		mListView = (ListView) findViewById(R.id.weblog_list_view);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				WebLogWebsite item = (WebLogWebsite) parent.getAdapter().getItem(position);
				
				Intent openWeblogWebsiteIntent = new Intent(parent.getContext(), WebsiteDetailsActivity.class);
				
				openWeblogWebsiteIntent.putExtra("websiteID", item.getId());
				openWeblogWebsiteIntent.putExtra("websiteName", item.getWebsite());
				
				startActivity(openWeblogWebsiteIntent);
			}
			
		});
		
		mPreferences = getSharedPreferences("PrivacyProxyPreferences", Context.MODE_PRIVATE);
		
		mLoadingScreen = findViewById(R.id.weblog_status);

		mWebLogTask = new GetWeblogTask();
		mWebLogTask.execute();
	}

	private void displayWebLogList(ArrayList<WebLogWebsite> entries) {
		listAdapter = new WebLogListAdapter(this, R.layout.layout_weblog_entry, entries);

		mListView.setAdapter(listAdapter);
	}

}
