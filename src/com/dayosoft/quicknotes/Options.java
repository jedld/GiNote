package com.dayosoft.quicknotes;

import java.io.IOException;

import com.dayosoft.utils.DialogUtils;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

class ClientCredentials {

	/** Value of the "API key" shown under "Simple API Access". */
	public static final String KEY = "AIzaSyD_nOpru1d78QH9fZKG8Fue-93NSthKfek";

	public static void errorIfNotSpecified() {
		if (KEY == null) {
			System.err.println("Please enter your API key in "
					+ ClientCredentials.class);
			System.exit(1);
		}
	}
}

public class Options extends Activity implements OnClickListener {

	EditText suffixField;
	GoogleAccountManager accountManager;
	SharedPreferences settings;
	private static final String TAG = "Ginote";
	GoogleCredential credential = new GoogleCredential();
	static final String PREF_ACCOUNT_NAME = "accountName";

	static final String PREF_AUTH_TOKEN = "authToken";

	String accountName;
	// This must be the exact string, and is a special for alias OAuth 2 scope
	// "https://www.googleapis.com/auth/tasks"
	private static final String AUTH_TOKEN_TYPE = "fusiontables";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.options);
		settings = getSharedPreferences("ginote_settings", MODE_PRIVATE);
		CheckBox useGPS = (CheckBox) findViewById(R.id.UseGPS);
		CheckBox autoAddNote = (CheckBox) findViewById(R.id.autoAddNote);
		DialogUtils.linkBoxToPrefs(useGPS, settings, "use_gps");
		DialogUtils.linkBoxToPrefs(autoAddNote, settings, "auto_add_note");

		Button setupGoogleAccount = (Button) findViewById(R.id.buttonFusionTableSync);
		setupGoogleAccount.setOnClickListener(this);
		accountManager = new GoogleAccountManager(this);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.buttonFusionTableSync:
			accountManager.manager.getAuthTokenByFeatures(
					GoogleAccountManager.ACCOUNT_TYPE, AUTH_TOKEN_TYPE, null,
					Options.this, null, null,
					new AccountManagerCallback<Bundle>() {

						public void run(AccountManagerFuture<Bundle> future) {
							Bundle bundle;
							try {
								bundle = future.getResult();
								setAccountName(bundle
										.getString(AccountManager.KEY_ACCOUNT_NAME));
								setAuthToken(bundle
										.getString(AccountManager.KEY_AUTHTOKEN));
								onAuthToken();
							} catch (OperationCanceledException e) {
								// user canceled
							} catch (AuthenticatorException e) {
								Log.e(TAG, e.getMessage(), e);
							} catch (IOException e) {
								Log.e(TAG, e.getMessage(), e);
							}
						}

					}, null);

		}
	}

	void onAuthToken() {
	}

	void setAccountName(String accountName) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREF_ACCOUNT_NAME, accountName);
		editor.commit();
		this.accountName = accountName;
	}

	void setAuthToken(String authToken) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREF_AUTH_TOKEN, authToken);
		editor.commit();
		credential.setAccessToken(authToken);
		Log.d(this.getClass().toString(), "Auth Token = " + authToken);
	}

}
