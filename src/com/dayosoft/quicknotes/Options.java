package com.dayosoft.quicknotes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.dayosoft.utils.DialogUtils;
import com.dayosoft.utils.FTQueryCompleteListener;
import com.dayosoft.utils.FusionTableService;
import com.dayosoft.utils.GoogleFTSyncer;
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
	Button syncFT;
	private static final String TAG = "Ginote";
	String[] items = {}, table_ids = {};
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
		syncFT = (Button) findViewById(R.id.buttonSync);
		if (getAuthToken() != null) {
			syncFT.setEnabled(true);
		}

		DialogUtils.linkBoxToPrefs(useGPS, settings, "use_gps");
		DialogUtils.linkBoxToPrefs(autoAddNote, settings, "auto_add_note");

		Button setupGoogleAccount = (Button) findViewById(R.id.buttonFusionTableSync);
		tableNameField = (EditText) findViewById(R.id.editTextTableName);
		tableNameField.setText(getTableName());
		setupGoogleAccount.setOnClickListener(this);
		syncFT.setOnClickListener(this);
		accountManager = new GoogleAccountManager(this);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.buttonSync:
			GoogleFTSyncer syncer = new GoogleFTSyncer(this, syncFT);
			syncer.execute();
			break;
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
				final GoogleFTUpdater updater = new GoogleFTUpdater(
						Options.this);
				updater.createTable(value, new FTQueryCompleteListener() {
					@Override
					public void onQueryComplete(
							ArrayList<HashMap<String, String>> result) {
						Toast.makeText(Options.this, "Table Created.",
								Toast.LENGTH_SHORT).show();
						final String table_id = result.get(0).get("TABLEID");
						Log.d(this.getClass().toString(), "storing table "
								+ table_id);

						setTableName(value);
						setTableId(table_id);
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
		ArrayList<String> tableids = new ArrayList<String>();

		tablenames.add("Create New Table");
		tableids.add("");
		for (HashMap<String, String> map : result) {
			tablenames.add(map.get("NAME"));
			tableids.add(map.get("TABLE ID"));
		}

		table_ids = tableids.toArray(table_ids);
		items = tablenames.toArray(items);
		Log.d(this.getClass().toString(), "onQueryComplete()");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Pick a table to sync to");
		builder.setItems(items, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int item) {
				final int item_index = item;
				if (item_index == 0) {
					promptTableName();
				} else {
					final GoogleFTUpdater updater = new GoogleFTUpdater(
							Options.this);
					updater.describeTable(table_ids[item_index],
							new FTQueryCompleteListener() {
								@Override
								public void onQueryComplete(
										ArrayList<HashMap<String, String>> result) {
									// Check for table structure compatibility
									HashMap<String, String> targetTableSchema = new HashMap<String, String>();
									for (HashMap<String, String> types : result) {
										targetTableSchema.put(
												types.get("NAME"),
												types.get("TYPE"));
									}
									
									//make sure target table is structurally correct
									if (updater
											.isValidSchema(targetTableSchema)) {
										setTableName(items[item_index]);
										setTableId(table_ids[item_index]);
									} else {
										AlertDialog.Builder errorAlert = new AlertDialog.Builder(Options.this);
										errorAlert.setMessage("Sorry the table " + items[item_index] + " does not have the correct table structure for GiNote FT. " +
												"Choose \"Create New Table\" to create a new one or choose another table.");
										AlertDialog alert = errorAlert.create();
										alert.show();
									}

								}
							});
				}
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void setTableId(String table_id) {
		Log.d(this.getClass().toString(), "table id = " + table_id);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("sync_table_id", table_id);
		editor.commit();
	}

	private void setTableName(String name) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("sync_table", name);
		editor.commit();
		tableNameField.setText(name);
		syncFT.setEnabled(true);
	}

	private String getTableName() {
		return settings.getString("sync_table", "");
	}
}
