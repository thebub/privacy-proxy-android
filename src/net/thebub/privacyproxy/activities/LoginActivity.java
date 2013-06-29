package net.thebub.privacyproxy.activities;

import net.thebub.privacyproxy.PrivacyProxyAPI;
import net.thebub.privacyproxy.PrivacyProxyAPI.APICommand;
import net.thebub.privacyproxy.PrivacyProxyAPI.APIResponse;
import net.thebub.privacyproxy.PrivacyProxyAPI.LoginResponse;
import net.thebub.privacyproxy.R;
import net.thebub.privacyproxy.util.ServerConnection;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity {
	
	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */

	
	public abstract class AuthTask extends AsyncTask<Void, Void, Boolean> {}
	
	public class LoginForm {

		public View mLoginView;
		
		public EditText mUsernameView;
		public String mUsername;
		public EditText mPasswordView;
		public String mPassword;
		
		public Button mLoginButton;
		public Button mRecoverPasswordButton;
		public Button mRegisterButton;
		
		public LoginForm() {		
			this.mLoginView = findViewById(R.id.login_form);
			
			this.mUsernameView = (EditText) findViewById(R.id.username);
			mUsernameView.setText(mPreferences.getString(getString(R.string.pref_username), ""), TextView.BufferType.EDITABLE);
			this.mPasswordView = (EditText) findViewById(R.id.password);
			this.mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
					if (id == R.id.login || id == EditorInfo.IME_NULL) {
						attemptLogin();
						return true;
					}
					return false;
				}
			});
			
			this.mLoginButton = (Button) findViewById(R.id.sign_in_button);
			this.mRecoverPasswordButton = (Button) findViewById(R.id.recover_password_button);
			this.mRegisterButton = (Button) findViewById(R.id.register_button);
			
			mLoginButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					attemptLogin();
				}
			});
			
			mRecoverPasswordButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
				}
			});
			
			mRegisterButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					fadeViews(mLoginView, mRegistrationForm.mRegisterView);
					mRegistrationForm.mUsernameView.requestFocus();
				}
			});
			
			if(mPreferences.getString(getString(R.string.pref_username), "").length() == 0) {
				this.mUsernameView.requestFocus();
			} else {
				this.mPasswordView.requestFocus();
			}
		}
		
		private void attemptLogin() {			
			if (mAuthTask != null) {
				return;
			}

			if (this.checkForm()) {
				// Show a progress spinner, and kick off a background task to
				// perform the user login attempt.
				mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
				fadeViews(mLoginView, mAuthStatusView);
				
				mAuthTask = new UserLoginTask();
				mAuthTask.execute();
			}
		}
		
		private boolean checkForm() {
			// Reset errors.
			mUsernameView.setError(null);
			mPasswordView.setError(null);

			// Store values at the time of the login attempt.
			mUsername = mUsernameView.getText().toString();
			mPassword = mPasswordView.getText().toString();

			boolean cancel = false;
			View focusView = null;

			// Check for a valid password.
			if (TextUtils.isEmpty(mPassword)) {
				mPasswordView.setError(getString(R.string.error_field_required));
				focusView = mPasswordView;
				cancel = true;
			} else if (mPassword.length() < 8) {
//				mPasswordView.setError(getString(R.string.error_password_short));
//				focusView = mPasswordView;
//				cancel = true;
			}

			// Check for a valid email address.
			if (TextUtils.isEmpty(mUsername)) {
				mUsernameView.setError(getString(R.string.error_field_required));
				focusView = mUsernameView;
				cancel = true;
			}
			
			if (cancel) {
				focusView.requestFocus();
			}
			
			return !cancel;
		}
		
		public class UserLoginTask extends AuthTask {
			
			@Override
			protected Boolean doInBackground(Void... none) {
							
				net.thebub.privacyproxy.PrivacyProxyAPI.LoginData.Builder requestBuilder = PrivacyProxyAPI.LoginData.newBuilder();
				
				requestBuilder.setUsername(mUsername);
				requestBuilder.setPassword(mPassword);
				
				ServerConnection connection = ServerConnection.getInstance();
				
				APIResponse response = connection.sendRequest(APICommand.login, requestBuilder.build().toByteString());;
				
				if(response == null || !response.getSuccess()) {
					return false;
				}
				
				LoginResponse loginResponse;
				try {
					loginResponse = LoginResponse.parseFrom(response.getData());
				} catch (InvalidProtocolBufferException e) {
					e.printStackTrace();
					return false;
				}
				
				SharedPreferences.Editor editor = mPreferences.edit();
				
				editor.putString(getString(R.string.pref_username), mUsername);
				editor.putString(getString(R.string.pref_session_id), loginResponse.getSessionID());
				
				editor.commit();			
			
				return true;
			}

			@Override
			protected void onPostExecute(final Boolean success) {
				mAuthTask = null;

				if (success) {
					Intent openMenuIntent = new Intent(LoginActivity.this, MenuActivity.class);
					openMenuIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(openMenuIntent);
					mRecoverPasswordButton.setVisibility(View.GONE);
					
					fadeViews(mAuthStatusView, mLoginView);
				} else {
					fadeViews(mAuthStatusView, mLoginView);
					
					mRecoverPasswordButton.setVisibility(View.VISIBLE);
					
					mPasswordView.requestFocus();
					mPasswordView.setError(getString(R.string.error_login));				
				}
			}

			@Override
			protected void onCancelled() {
				mAuthTask = null;
				fadeViews(mAuthStatusView, mLoginView);
			}
		}
	}
	
	public class RegistrationForm {
		
		public View mRegisterView;
		
		public EditText mUsernameView;
		public String mUsername;
		public EditText mEmailView;
		public String mEmail;
		public EditText mPasswordView;
		public String mPassword;
		public CheckBox mTOS;
		
		public Button mRegisterButton;
		public Button mCancelButton;
		
		public RegistrationForm() {
			this.mRegisterView = findViewById(R.id.registration_form);
			
			this.mUsernameView = (EditText) findViewById(R.id.registration_username);
			this.mEmailView = (EditText) findViewById(R.id.registration_email);
			this.mPasswordView = (EditText) findViewById(R.id.registration_password);
			this.mTOS = (CheckBox) findViewById(R.id.registration_tos);
			
			this.mRegisterButton = (Button) findViewById(R.id.registration_register);
			this.mCancelButton = (Button) findViewById(R.id.registration_cancel);
						
			this.mRegisterButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					attemptRegistration();
				}
			});
			
			this.mCancelButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					fadeViews(mRegisterView, mLoginForm.mLoginView);
					mLoginForm.mUsernameView.requestFocus();
				}
			});
		}
		
		private void attemptRegistration() {		
			if (mAuthTask != null) {
				return;
			}

			if (this.checkForm()) {
				// Show a progress spinner, and kick off a background task to
				// perform the user login attempt.
				mLoginStatusMessageView.setText(R.string.login_progress_registering);
				fadeViews(mRegisterView, mAuthStatusView);
				
				mAuthTask = new UserRegisterTask();
				mAuthTask.execute();
			}
		}
		
		private boolean checkForm() {
			// Reset errors.
			mUsernameView.setError(null);
			mEmailView.setError(null);
			mPasswordView.setError(null);

			// Store values at the time of the login attempt.
			mUsername = mUsernameView.getText().toString();
			mEmail = mEmailView.getText().toString();
			mPassword = mPasswordView.getText().toString();

			boolean cancel = false;
			View focusView = null;
			
			if(!this.mTOS.isChecked()) {
				mTOS.setError(getString(R.string.error_field_required));
				focusView = this.mTOS;
				cancel = true;
			}

			// Check for a valid password.
			if (TextUtils.isEmpty(mPassword)) {
				mPasswordView.setError(getString(R.string.error_field_required));
				focusView = mPasswordView;
				cancel = true;
			} else if (mPassword.length() < 8) {
				mPasswordView.setError(getString(R.string.error_password_short));
				focusView = mPasswordView;
				cancel = true;
			}
			
			// Check for a valid email address.
			if (TextUtils.isEmpty(mEmail)) {
				mEmailView.setError(getString(R.string.error_field_required));
				focusView = mEmailView;
				cancel = true;
			}

			// Check for a valid username
			if (TextUtils.isEmpty(mUsername)) {
				mUsernameView.setError(getString(R.string.error_field_required));
				focusView = mUsernameView;
				cancel = true;
			}
			
			if (cancel) {
				focusView.requestFocus();
			}
			
			return !cancel;
		}
		
		public class UserRegisterTask extends AuthTask {
			
			@Override
			protected Boolean doInBackground(Void... none) {
							
				net.thebub.privacyproxy.PrivacyProxyAPI.CreateUserRequest.Builder requestBuilder = PrivacyProxyAPI.CreateUserRequest.newBuilder();
				
				requestBuilder.setUsername(mUsername);
				requestBuilder.setEmail(mEmail);
				requestBuilder.setPassword(mPassword);
				
				ServerConnection connection = ServerConnection.getInstance();
				
				APIResponse response = connection.sendRequest(APICommand.createUser, requestBuilder.build().toByteString());;
				
				if(response == null || !response.getSuccess()) {
					return false;
				}
				
				LoginResponse loginResponse;
				try {
					loginResponse = LoginResponse.parseFrom(response.getData());
				} catch (InvalidProtocolBufferException e) {
					e.printStackTrace();
					return false;
				}
				
				SharedPreferences.Editor editor = mPreferences.edit();
				
				editor.putString(getString(R.string.pref_username), mUsername);
				editor.putString(getString(R.string.pref_session_id), loginResponse.getSessionID());
				
				editor.commit();			
			
				return true;
			}

			@Override
			protected void onPostExecute(final Boolean success) {
				mAuthTask = null;

				if (success) {
					Intent openMenuIntent = new Intent(LoginActivity.this, MenuActivity.class);
					openMenuIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(openMenuIntent);
					
					mUsernameView.setText("",BufferType.EDITABLE);
					mPasswordView.setText("",BufferType.EDITABLE);
					mEmailView.setText("",BufferType.EDITABLE);
					mTOS.setChecked(false);
					
					fadeViews(mAuthStatusView, mLoginForm.mLoginView);
				} else {
					fadeViews(mAuthStatusView, mRegisterView);
										
					mPasswordView.requestFocus();
					mPasswordView.setError(getString(R.string.error_login));				
				}
			}

			@Override
			protected void onCancelled() {
				mAuthTask = null;
				fadeViews(mAuthStatusView, mRegisterView);
			}
		}
		
	}
	
	public LoginForm mLoginForm;
	public RegistrationForm mRegistrationForm;
	
	// UI references.
	private View mAuthStatusView;
	private TextView mLoginStatusMessageView;
	
	private SharedPreferences mPreferences;
	
	private AuthTask mAuthTask = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_login);
		
		mPreferences = getSharedPreferences("PrivacyProxyPreferences", Context.MODE_PRIVATE);
		
		mAuthStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		mLoginForm = new LoginForm();
		mRegistrationForm = new RegistrationForm();		
	}	

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void fadeViews(final View fadeOutView, final View fadeInView) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			fadeInView.setVisibility(View.VISIBLE);
			fadeInView.animate().setDuration(shortAnimTime)
					.alpha(1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							fadeInView.setVisibility(View.VISIBLE);
						}
					});

			fadeOutView.setVisibility(View.VISIBLE);
			fadeOutView.animate().setDuration(shortAnimTime)
					.alpha(0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							fadeOutView.setVisibility(View.GONE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			fadeInView.setVisibility(View.VISIBLE);
			fadeOutView.setVisibility(View.GONE);
		}
	}
}
