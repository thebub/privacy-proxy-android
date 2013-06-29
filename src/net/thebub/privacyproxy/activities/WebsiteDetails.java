package net.thebub.privacyproxy.activities;

import java.util.ArrayList;
import java.util.List;

import net.thebub.privacyproxy.PrivacyProxyAPI.APICommand;
import net.thebub.privacyproxy.PrivacyProxyAPI.APIResponse;
import net.thebub.privacyproxy.PrivacyProxyAPI.PersonalDataEntry;
import net.thebub.privacyproxy.PrivacyProxyAPI.WebLogWebsiteDataRequest;
import net.thebub.privacyproxy.PrivacyProxyAPI.WebLogWebsiteDataResponse;
import net.thebub.privacyproxy.PrivacyProxyAPI.WebLogWebsiteDataResponse.WebLogWebsiteData;
import net.thebub.privacyproxy.R;
import net.thebub.privacyproxy.util.ServerConnection;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;

public class WebsiteDetails extends Activity {

	private View mContentView;
	private View mLoadingScreen;
	
	private ListView mDetailsList;
	private WebsiteDetailsListAdapter listAdapter;
	private GetWebsiteDetailsTask mWebDetailsTask;
	
	private SharedPreferences mPreferences;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_website_details);
		// Show the Up button in the action bar.
		setupActionBar();

		Intent intent = getIntent();
		
		mPreferences = getSharedPreferences("PrivacyProxyPreferences", Context.MODE_PRIVATE);
		
		mContentView = findViewById(R.id.webdetails_data_view);
		mLoadingScreen = findViewById(R.id.webdetails_status);
		
		mDetailsList = (ListView) findViewById(R.id.website_details_list);
		
		TextView website = (TextView) findViewById(R.id.website_details_website);
		website.setText(intent.getStringExtra("websiteName"));		
		
		Integer websiteID = intent.getIntExtra("websiteID",-1);
		if(websiteID != -1) {
			mWebDetailsTask = new GetWebsiteDetailsTask();
			mWebDetailsTask.execute(websiteID);
		}
	}

	private class GetWebsiteDetailsTask extends AsyncTask<Integer, Void, List<WebLogWebsiteData>> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			mContentView.setVisibility(View.GONE);
			mLoadingScreen.setVisibility(View.VISIBLE);
		}

		@Override
		protected List<WebLogWebsiteData> doInBackground(Integer... websiteIDs) {

			Integer websiteID = websiteIDs[0];
			
			ServerConnection connection = ServerConnection.getInstance();
			
			WebLogWebsiteDataRequest.Builder requestBuilder = WebLogWebsiteDataRequest.newBuilder();
			
			requestBuilder.setId(websiteID);

			String sessionID = mPreferences.getString(getString(R.string.pref_session_id), "");

			APIResponse response = connection.sendRequest(APICommand.getWebpageData,sessionID,requestBuilder.build().toByteString());

			if(response == null || !response.getSuccess()) {
				return null;
			}

			WebLogWebsiteDataResponse websiteDataResponse;
			try {
				websiteDataResponse = WebLogWebsiteDataResponse.parseFrom(response.getData());
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
				return null;
			}

			return websiteDataResponse.getDataList();
		}

		@Override
		protected void onPostExecute(final List<WebLogWebsiteData> data) {
			mWebDetailsTask = null;

			if (data != null) {				
				displayWebsiteData(data);
				mLoadingScreen.setVisibility(View.GONE);
				mContentView.setVisibility(View.VISIBLE);
			} else {
				mLoadingScreen.setVisibility(View.GONE);
				mContentView.setVisibility(View.VISIBLE);
			}
		}

		@Override
		protected void onCancelled() {
			mWebDetailsTask = null;
			mLoadingScreen.setVisibility(View.GONE);
			mContentView.setVisibility(View.VISIBLE);
		}
	}
	
	private class WebsiteDetailsListAdapter extends ArrayAdapter<WeblogEntry> {

		private ArrayList<WeblogEntry> websiteDetailsList;

		public WebsiteDetailsListAdapter(Context context, int textViewResourceId, 
				ArrayList<WeblogEntry> list) {
			super(context, textViewResourceId, list);
			this.websiteDetailsList = new ArrayList<WeblogEntry>();
			this.websiteDetailsList.addAll(list);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(
						Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.layout_webdetails_entry, null);
			}

			WeblogEntry entry = this.websiteDetailsList.get(position);
			
			TextView desc = (TextView) convertView.findViewById(R.id.webdetails_desc);
			desc.setText(entry.entry.getDescription());
			TextView type = (TextView) convertView.findViewById(R.id.webdetails_type);
			type.setText(entry.entry.getType().name());
			TextView date = (TextView) convertView.findViewById(R.id.webdetails_date);
			date.setText(entry.date);
			
			return convertView;
		}

	}
	
	private class WeblogEntry {
		public String date;
		public PersonalDataEntry entry;
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void displayWebsiteData(List<WebLogWebsiteData> data) {
		ArrayList<WeblogEntry> list = new ArrayList<WeblogEntry>();
		
		for (WebLogWebsiteData dateEntry : data) {
			String date = dateEntry.getDate();
			
			for (PersonalDataEntry dataEntry : dateEntry.getEntryList()) {
				WeblogEntry tmp = new WeblogEntry();
				tmp.date = date;
				tmp.entry = dataEntry;
				
				list.add(tmp);
			}
		}
		
		listAdapter = new WebsiteDetailsListAdapter(this, R.layout.layout_webdetails_entry, list);

		mDetailsList.setAdapter(listAdapter);
	}

}
