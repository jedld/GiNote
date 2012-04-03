package com.dayosoft.quicknotes;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

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
	SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
	public void process_new_records(Note notes[]) {
		StringBuffer queryBuffer = new StringBuffer();
		for (Note note : notes) {

			Date date_updated = note.getDate_created();
			if (note.getDate_updated() != null) {
				date_updated = note.getDate_updated();
			}
			String queryStr = "INSERT INTO " + getTableId()
					+ " (UID,TITLE,CONTENT,DATE_CREATED,DATE_UPDATED) VALUES "
					+ "('" + note.getUid() + "', '"
					+ StringUtils.replace(note.getTitle(), "'", "''") + "', '"
					+ StringUtils.replace(note.getContent(), "'", "''")
					+ "', '" + dateformat.format(note.getDate_created()) + "',"
					+ "'" + dateformat.format(date_updated) + "');";
			queryBuffer.append(queryStr);

		}
		ArrayList<HashMap<String, String>> result = service.create_sync(
				queryBuffer.toString(), true);
		if (result.size() == notes.length) {
			for (Note note : notes) {
				helper.touch(note.id);
			}
		}
	}

	public static String sqlLize(String value) {
		return "'" + StringUtils.replace(value, "'", "''") + "'";
	}

	@Override
	public void process_updated_records(Note[] updated_notes_array) {
		for (Note note : updated_notes_array) {
			ArrayList<HashMap<String, String>> result = service.query_sync(
					"SELECT ROWID FROM " + getTableId() + " WHERE UID = '"
							+ note.getUid() + "'", true);
			if (result.size() > 0) {
				String rowid = result.get(0).get("rowid");
				String queryStr = "UPDATE " + getTableId() + " SET TITLE = "
						+ sqlLize(note.getTitle()) + ", CONTENT = "
						+ sqlLize(note.getContent()) + " , DATE_UPDATED = '"
						+ dateformat.format(note.getDate_updated()) + "' WHERE ROWID = '"+rowid+"';";
				ArrayList<HashMap<String, String>> update_result = service
						.create_sync(queryStr, true);
				if (update_result!=null) {
					helper.touch(note.getId());
				}
				
			} else {
				String queryStr = "INSERT INTO " + getTableId()
				+ " (UID,TITLE,CONTENT,DATE_CREATED,DATE_UPDATED) VALUES "
				+ "('" + note.getUid() + "', '"
				+ StringUtils.replace(note.getTitle(), "'", "''") + "', '"
				+ StringUtils.replace(note.getContent(), "'", "''")
				+ "', '" + dateformat.format(note.getDate_created()) + "',"
				+ "'" + dateformat.format(note.getDate_updated()) + "');";
				ArrayList<HashMap<String, String>> insert_result = service.create_sync(
						queryStr, true);
				if (insert_result.size() > 0) {
					helper.touch(note.getId());
				}
			}
		}

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
