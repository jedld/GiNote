package com.dayosoft.quicknotes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.dayosoft.utils.DialogUtils;
import com.dayosoft.utils.FTQueryCompleteListener;
import com.dayosoft.utils.FusionTableService;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

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

public class Options extends Activity implements OnClickListener,
		FTQueryCompleteListener {

	EditText suffixField, tableNameField;
	GoogleAccountManager accountManager;
	SharedPreferences settings;
	private static final String TAG = "Ginote";
	String[] items = {};
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
		tableNameField = (EditText)findViewById(R.id.editTextTableName);
		tableNameField.setText(getTableName());
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
		FusionTableService service = new FusionTableService(getAuthToken());
		service.query("SHOW TABLES", true, this);
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

	String getAuthToken() {
		return settings.getString(PREF_AUTH_TOKEN, null);
	}

	private void promptTableName() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Create New Fustion Table");
		alert.setMessage("Enter the name of your table");

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setText("GINOTE");
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				final String value = input.getText().toString();
				FusionTableService service = new FusionTableService(
						getAuthToken());
				service.create(
						"CREATE TABLE "
								+ value
								+ " (UID: STRING, TITLE: STRING, CONTENT: STRING, DATE_CREATED: DATETIME, DATE_UPDATED: NUMBER)",
						true, new FTQueryCompleteListener() {
							@Override
							public void onQueryComplete(
									ArrayList<HashMap<String, String>> result) {
								Toast.makeText(Options.this, "Table Created.",
										Toast.LENGTH_SHORT).show();
								setTableName(value);
							}
						});
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

		alert.show();
	}

	@Override
	public void onQueryComplete(ArrayList<HashMap<String, String>> result) {

		ArrayList<String> tablenames = new ArrayList<String>();
		tablenames.add("Create New Table");
		for (HashMap<String, String> map : result) {
			tablenames.add(map.get("name"));
		}

		items = tablenames.toArray(items);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Pick a table to sync to");
		builder.setItems(items, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int item) {
				if (item == 0) {
					promptTableName();
				} else {
					setTableName(items[item]);
				}
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void setTableName(String name) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("sync_table", name);
		editor.commit();
		tableNameField.setTag(name);
	}
	
	private String getTableName() {
		return settings.getString("sync_table", "");
	}
}
