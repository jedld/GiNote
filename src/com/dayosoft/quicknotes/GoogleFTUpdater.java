package com.dayosoft.quicknotes;

import java.util.ArrayList;
import java.util.HashMap;

import com.dayosoft.utils.DictionaryOpenHelper;
import com.dayosoft.utils.FTQueryCompleteListener;
import com.dayosoft.utils.FusionTableService;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

public class GoogleFTUpdater extends AsyncTask implements NoteSyncer {
	SharedPreferences settings;

	Context context;
	static final String PREF_AUTH_TOKEN = "authToken";
	DictionaryOpenHelper helper;
	FusionTableService service;

	public GoogleFTUpdater(Context c) {
		this.context = c;
		this.settings = c.getSharedPreferences("ginote_settings",
				c.MODE_PRIVATE);
		this.helper = new DictionaryOpenHelper(c);
		this.service = new FusionTableService(getAuthToken());
	}

	@Override
	protected Object doInBackground(Object... params) {
		if (!this.getTableId().equalsIgnoreCase("")) {
			helper.listUnsyncedNotes(this);
		}
		return null;
	}

	@Override
	public void process(Note note) {
		final int id = note.getId();
		service.create_sync("INSERT INTO " + getTableId()
				+ " (UID,TITLE,CONTENT) VALUES " + "('" + note.getUid()
				+ "', '" + note.getTitle() + "', '" + note.getContent() + "')",
				true, new FTQueryCompleteListener() {
					@Override
					public void onQueryComplete(
							ArrayList<HashMap<String, String>> result) {
						helper.touch(id);
					}

				});
	}

	String getAuthToken() {
		return settings.getString(PREF_AUTH_TOKEN, null);
	}

	private String getTableName() {
		return settings.getString("sync_table", "");
	}

	private String getTableId() {
		return settings.getString("sync_table_id", "");
	}

}
