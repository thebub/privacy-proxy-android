package net.thebub.privacyproxy.activities;

import net.thebub.privacyproxy.PrivacyProxyAPI.APICommand;
import net.thebub.privacyproxy.PrivacyProxyAPI.APIResponse;
import net.thebub.privacyproxy.PrivacyProxyAPI.PersonalDataEntry;
import net.thebub.privacyproxy.PrivacyProxyAPI.SettingAction;
import net.thebub.privacyproxy.PrivacyProxyAPI.UpdateSettingRequest;
import net.thebub.privacyproxy.R;
import net.thebub.privacyproxy.util.ServerConnection;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * This class wraps the details activity of a setting. It enables setting deletion.
 * @author dbub
 *
 */
public class SettingDetailActivity extends Activity {
	
	private PersonalDataEntry mData = null;
	private SharedPreferences mPreferences;
	
	private DeleteSettingTask mDeleteTask = null;

	/**
	 * This implementation of the AsyncTask, which will delete a setting.
	 * @author dbub
	 *
	 */
	private class DeleteSettingTask extends AsyncTask<Void, Void, Boolean> {
	
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
	
		@Override
		protected Boolean doInBackground(Void... data) {
			
			// Get the server connection instance
			ServerConnection connection = ServerConnection.getInstance();
			
			// Create a request builder and construct the request
			UpdateSettingRequest.Builder requestBuilder = UpdateSettingRequest.newBuilder();
			
			requestBuilder.setAction(SettingAction.delete);
			requestBuilder.setData(mData);
	
			String sessionID = mPreferences.getString(getString(R.string.pref_session_id), "");
	
			APIResponse response = connection.sendRequest(APICommand.updateSetting,sessionID,requestBuilder.build().toByteString());
	
			if(response != null) {
				return response.getSuccess();
			}
			
			return null;
		}
	
		@Override
		protected void onPostExecute(final Boolean success) {
			mDeleteTask = null;
			
			// Close activity on success, show a toats otheriwse
			if(success) {			
				SettingDetailActivity.this.setResult(RESULT_OK);
				SettingDetailActivity.this.finish();
			} else {
				Toast failureToast = new Toast(SettingDetailActivity.this);			
				failureToast.setText("Delete Failed");
				failureToast.setDuration(Toast.LENGTH_LONG);
				failureToast.show();
			}
		}
	
		@Override
		protected void onCancelled() {
			mDeleteTask = null;
			
			Toast failureToast = new Toast(SettingDetailActivity.this);			
			failureToast.setText("Delete Failed");
			failureToast.setDuration(Toast.LENGTH_LONG);
			failureToast.show();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting_detail);
		// Show the Up button in the action bar.
		setupActionBar();
		
		Intent intent = getIntent();
		mPreferences = getSharedPreferences("PrivacyProxyPreferences", Context.MODE_PRIVATE);
		
		mData = (PersonalDataEntry) intent.getSerializableExtra("settingData");
		
		EditText descView = (EditText) findViewById(R.id.setting_detail_desc);		
		descView.setText(intent.getStringExtra("settingDesc"));
		
		EditText typeView = (EditText) findViewById(R.id.setting_detail_type);
		typeView.setText(intent.getStringExtra("settingType"));
		
		Button deleteButton = (Button) findViewById(R.id.setting_detail_delete);		
		deleteButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Delete the setting on click of this button
				delete();				
			}
		});
		
		Button cancelButton = (Button) findViewById(R.id.setting_detail_cancel);		
		cancelButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Return to the settings menu, since user canceled the operation
				cancel();
			}
		});
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}
	
	private void delete() {
		// Start the delete activity
		mDeleteTask = new DeleteSettingTask();
		mDeleteTask.execute();
	}
	
	private void cancel() {
		// Return to settings menu
		setResult(RESULT_CANCELED);
		finish();
	}

}
