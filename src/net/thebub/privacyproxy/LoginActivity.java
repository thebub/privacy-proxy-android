package net.thebub.privacyproxy;

import net.thebub.privacyproxy.PrivacyProxyAPI.APICommand;
import net.thebub.privacyproxy.PrivacyProxyAPI.APIResponse;
import net.thebub.privacyproxy.PrivacyProxyAPI.LoginResponse;
import net.thebub.privacyproxy.util.ServerConnection;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity {
	
	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private AuthTask mAuthTask = null;

	// Values for email and password at the time of the login attempt.
	private String mUsername;
	private String mPassword;

	// UI references.
	private EditText mUsernameView;
	private EditText mPasswordView;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;
	
	private Button mRecoverPasswordButton;
	
	private static final int DIALOG_LOGIN_SUCCESS = 0;
	private static final int DIALOG_REGISTRATION_SUCCESS = 1;	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_login);

		// Set up the login form.
		mUsernameView = (EditText) findViewById(R.id.username);
		mUsernameView.setText(mUsername);

		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int id,
							KeyEvent keyEvent) {
						if (id == R.id.login || id == EditorInfo.IME_NULL) {
							attemptLogin();
							return true;
						}
						return false;
					}
				});

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);
		
		mRecoverPasswordButton = (Button) findViewById(R.id.recover_password_button);

		findViewById(R.id.sign_in_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptLogin();
					}
				});
		
		findViewById(R.id.register_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						// TODO: Open registration view
					}
				});
	}
	
	/**
	 * This function checks the requirements for logging in or registering an account.
	 * If there are form errors (missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	private boolean checkFormData() {
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
//			mPasswordView.setError(getString(R.string.error_password_short));
//			focusView = mPasswordView;
//			cancel = true;
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

	/**
	 * Attempts to sign in the account specified by the login form.
	 */
	public void attemptLogin() {
		if (mAuthTask != null) {
			return;
		}

		if (checkFormData()) {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
			showProgress(true);
			mAuthTask = new UserLoginTask();
			mAuthTask.execute();
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginStatusView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}
	
	public abstract class AuthTask extends AsyncTask<Void, Void, Boolean> {}

	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class UserLoginTask extends AuthTask {
		
		private String sessionID;
		
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
			
			this.sessionID = loginResponse.getSessionID();
		
			return true;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			mAuthTask = null;

			if (success) {
				Intent openMenuIntent = new Intent(LoginActivity.this, MenuActivity.class);
				startActivity(openMenuIntent);
				showProgress(false);
			} else {
				showProgress(false);
				mPasswordView
						.setError(getString(R.string.error_login));
				mPasswordView.requestFocus();
				
				mRecoverPasswordButton.setVisibility(View.VISIBLE);
			}
		}

		@Override
		protected void onCancelled() {
			mAuthTask = null;
			showProgress(false);
		}
	}
}
