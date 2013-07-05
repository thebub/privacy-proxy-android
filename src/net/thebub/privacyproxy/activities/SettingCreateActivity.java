package net.thebub.privacyproxy.activities;

import net.thebub.privacyproxy.R;
import net.thebub.privacyproxy.PrivacyProxyAPI.APICommand;
import net.thebub.privacyproxy.PrivacyProxyAPI.APIResponse;
import net.thebub.privacyproxy.PrivacyProxyAPI.PersonalDataEntry;
import net.thebub.privacyproxy.PrivacyProxyAPI.PersonalDataTypes;
import net.thebub.privacyproxy.PrivacyProxyAPI.SettingAction;
import net.thebub.privacyproxy.PrivacyProxyAPI.UpdateSettingRequest;
import net.thebub.privacyproxy.util.ServerConnection;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.Toast;

public class SettingCreateActivity extends Activity implements OnItemSelectedListener {
		
	private SharedPreferences mPreferences;
	private CreateSettingTask mCreateTask;
	
	private EditText mDescription = null;
	private Spinner mType = null;
	
	private TableRow mCreditcardRow = null;
	private EditText mCreditcardField = null;
	
	private TableRow mEmailRow = null;
	private EditText mEmailField = null;
	
	private TableRow mDateRow = null;
	private EditText mDateField = null;
	
	
	private class CreateSettingTask extends AsyncTask<PersonalDataEntry, Void, Boolean> {
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
	
		@Override
		protected Boolean doInBackground(PersonalDataEntry... data) {
			ServerConnection connection = ServerConnection.getInstance();
			
			UpdateSettingRequest.Builder requestBuilder = UpdateSettingRequest.newBuilder();
			
			requestBuilder.setAction(SettingAction.create);
			requestBuilder.setData(data[0]);
	
			String sessionID = mPreferences.getString(getString(R.string.pref_session_id), "");
	
			APIResponse response = connection.sendRequest(APICommand.updateSetting,sessionID,requestBuilder.build().toByteString());
	
			if(response != null) {
				return response.getSuccess();
			}
			
			return null;
		}
	
		@Override
		protected void onPostExecute(final Boolean success) {
			mCreateTask = null;
			
			if(success) {			
				SettingCreateActivity.this.setResult(RESULT_OK);
				SettingCreateActivity.this.finish();
			} else {
				Toast failureToast = new Toast(SettingCreateActivity.this);			
				failureToast.setText("Creation Failed");
				failureToast.setDuration(Toast.LENGTH_LONG);
				failureToast.show();
			}
		}
	
		@Override
		protected void onCancelled() {
			mCreateTask = null;
			
			Toast failureToast = new Toast(SettingCreateActivity.this);			
			failureToast.setText("Creation Failed");
			failureToast.setDuration(Toast.LENGTH_LONG);
			failureToast.show();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting_create);
		// Show the Up button in the action bar.
		setupActionBar();
		
		mPreferences = getSharedPreferences("PrivacyProxyPreferences", Context.MODE_PRIVATE);
		
		mDescription = (EditText) findViewById(R.id.setting_create_desc);
				
		mCreditcardRow = (TableRow) findViewById(R.id.setting_create_creditcard);
		mCreditcardField = (EditText) findViewById(R.id.setting_create_creditcard_creditcard);
		
		mEmailRow = (TableRow) findViewById(R.id.setting_create_email);
		mEmailField = (EditText) findViewById(R.id.setting_create_email_email);
		
		mDateRow = (TableRow) findViewById(R.id.setting_create_date);
		mDateField = (EditText) findViewById(R.id.setting_create_date_date);
		
		mType = (Spinner) findViewById(R.id.setting_create_type);
		// Create an ArrayAdapter using the string array and a default spinner layout
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
		        R.array.setting_create_types, android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		mType.setAdapter(adapter);
		mType.setOnItemSelectedListener(this);
		
		Button createButton = (Button) findViewById(R.id.setting_create_create);
		createButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				create();
			}
		});
		
		Button cancelButton = (Button) findViewById(R.id.setting_create_cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				cancel();
			}
		});
	}
	
	private void create() {
		net.thebub.privacyproxy.PrivacyProxyAPI.PersonalDataEntry.Builder dataBuilder = PersonalDataEntry.newBuilder();
		
		dataBuilder.setDescription(mDescription.getText().toString());
		
		if(((String) mType.getSelectedItem()).equalsIgnoreCase("credit card")) {
			dataBuilder.setType(PersonalDataTypes.creditcard);
			dataBuilder.setHash(mCreditcardField.getText().toString());
		} else if(((String) mType.getSelectedItem()).equalsIgnoreCase("email")) {
			dataBuilder.setType(PersonalDataTypes.email);
			dataBuilder.setHash(mEmailField.getText().toString());
		} else if(((String) mType.getSelectedItem()).equalsIgnoreCase("date")) {
			dataBuilder.setType(PersonalDataTypes.date);
			dataBuilder.setHash(mDateField.getText().toString());
		}
						
		mCreateTask = new CreateSettingTask();
		mCreateTask.execute(dataBuilder.build());
	}
	
	private void cancel() {
		setResult(RESULT_CANCELED);
		finish();
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

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		String selectedItem = (String) parent.getItemAtPosition(pos);
		
		if(selectedItem.equalsIgnoreCase("Credit card")) {
			mCreditcardRow.setVisibility(View.VISIBLE);
			mEmailRow.setVisibility(View.GONE);
			mDateRow.setVisibility(View.GONE);
		} else if (selectedItem.equalsIgnoreCase("Email")) {
			mCreditcardRow.setVisibility(View.GONE);
			mEmailRow.setVisibility(View.VISIBLE);
			mDateRow.setVisibility(View.GONE);
		} else if (selectedItem.equalsIgnoreCase("Date")) {
			mCreditcardRow.setVisibility(View.GONE);
			mEmailRow.setVisibility(View.GONE);
			mDateRow.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
	}

}
