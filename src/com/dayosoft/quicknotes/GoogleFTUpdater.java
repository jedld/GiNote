package com.dayosoft.quicknotes;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.dayosoft.utils.DictionaryOpenHelper;
import com.dayosoft.utils.FTQueryCompleteListener;
import com.dayosoft.utils.FusionTableService;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class GoogleFTUpdater extends AsyncTask implements NoteSyncer {
	SharedPreferences settings;

	Context context;
	static final String PREF_AUTH_TOKEN = "authToken";
	DictionaryOpenHelper helper;
	FusionTableService service;
	SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	TreeMap<String, String> validTableSchema = new TreeMap<String, String>();

	
	public GoogleFTUpdater(Context c) {
		this.context = c;
		this.settings = c.getSharedPreferences("ginote_settings",
				c.MODE_PRIVATE);
		this.helper = new DictionaryOpenHelper(c);
		this.service = new FusionTableService(getAuthToken());
		
		
		validTableSchema.put("UID", FusionTableService.STRING);
		validTableSchema.put("TITLE", FusionTableService.STRING);
		validTableSchema.put("CONTENT", FusionTableService.STRING);
		validTableSchema.put("POSITION", FusionTableService.LOCATION);
		validTableSchema.put("DATE_CREATED", FusionTableService.DATETIME);
		validTableSchema.put("DATE_UPDATED", FusionTableService.NUMBER);
		validTableSchema.put("SYNC_TS", FusionTableService.NUMBER);
		validTableSchema.put("IS_DELETED", FusionTableService.NUMBER);
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
		long sync_ts = System.currentTimeMillis();
		for (Note note : notes) {

			long date_updated = note.getDate_updated();
			if (note.getDate_updated() != null) {
				date_updated = note.getDate_updated();
			}
			String queryStr = "INSERT INTO "
					+ getTableId()
					+ " (UID,TITLE,CONTENT,POSITION,DATE_CREATED,DATE_UPDATED,SYNC_TS,IS_DELETED) VALUES "
					+ "('" + note.getUid() + "', " + sqlLize(note.getTitle())
					+ "," + sqlLize(note.getContent()) + ","
					+ "'<Point><coordinates>" + note.getLongitude() + ","
					+ note.getLatitude() + "</coordinates></Point>'" + ", '"
					+ dateformat.format(note.getDate_created()) + "'," + "'"
					+ date_updated + "' , " +sync_ts+" , 0);";
			queryBuffer.append(queryStr);

		}
		ArrayList<HashMap<String, String>> result = service.create_sync(
				queryBuffer.toString(), true);
		if (result.size() == notes.length) {
			for (Note note : notes) {
				helper.touch(note.id, sync_ts );
			}
		}
	}

	public static String sqlLize(String value) {
		return "'" + StringUtils.replace(value, "'", "''") + "'";
	}

	public void persistToFT(Note note) {
		long sync_ts = System.currentTimeMillis();
		String queryStr = "INSERT INTO "
				+ getTableId()
				+ " (UID, TITLE, CONTENT, POSITION, DATE_CREATED, DATE_UPDATED, SYNC_TS) VALUES "
				+ "('" + note.getUid() + "', '" + sqlLize(note.getTitle())
				+ "," + sqlLize(note.getContent()) + ", '<Point><coordinates>"
				+ note.getLongitude() + "," + note.getLatitude()
				+ "</coordinates></Point>'" + ", '"
				+ dateformat.format(note.getDate_created()) + "',"
				+ note.getDate_updated() + ", " + sync_ts + " );";
		ArrayList<HashMap<String, String>> insert_result = service.create_sync(
				queryStr, true);
		if (insert_result.size() > 0) {
			helper.touch(note.getId(), sync_ts);
		}
	}

	@Override
	public void process_updated_records(Note[] updated_notes_array) {
		for (Note note : updated_notes_array) {
			ArrayList<HashMap<String, String>> result = service.query_sync(
					"SELECT ROWID,SYNC_TS FROM " + getTableId()
							+ " WHERE UID = '" + note.getUid() + "'", true);
			if (result.size() > 0) {
				String rowid = result.get(0).get("ROWID");
				long ft_sync_ts = Long.parseLong(result.get(0).get("SYNC_TS"));

				if (ft_sync_ts == note.getSync_ts()) {
					long sync_ts = System.currentTimeMillis();
					String queryStr = "UPDATE " + getTableId()
							+ " SET TITLE = " + sqlLize(note.getTitle())
							+ ", POSITION = '<Point><coordinates>"
							+ note.getLongitude() + "," + note.getLatitude()
							+ "</coordinates></Point>'" + ", CONTENT = "
							+ sqlLize(note.getContent()) + " , DATE_UPDATED = "
							+ note.getDate_updated() + ", SYNC_TS = " + sync_ts
							+ " WHERE ROWID = '" + rowid + "';";
					ArrayList<HashMap<String, String>> update_result = service
							.create_sync(queryStr, true);
					if (update_result != null) {
						helper.touch(note.getId(), sync_ts);
					}
				} else {
					// only goes here if someone else changed it before your
					// copy got synced
					// to prevent loss of data we just create a separate copy in
					// Google Tables
					note.setUid(UUID.randomUUID().toString());
					helper.persist(note);
					persistToFT(note);
				}

			} else {
				persistToFT(note);
			}
		}

	}

	public boolean isValidSchema(HashMap<String, String> targetTableSchema) {
		for (String column_name : validTableSchema.keySet()) {
			if (targetTableSchema.containsKey(column_name)) {
				if (!targetTableSchema.get(column_name).equalsIgnoreCase(
						validTableSchema.get(column_name))) {
					return false;
				}
			} else {
				return false;
			}
		}

		return true;
	}

	public void describeTable(String tableId,
			FTQueryCompleteListener onCompleteListener) {
		service.query("DESCRIBE " + tableId, true, onCompleteListener);
	}

	public void createTable(String tableName,
			FTQueryCompleteListener onCompleteListener) {
		ArrayList<String> column_and_types = new ArrayList<String>();
		for (String column_name : validTableSchema.keySet()) {
			column_and_types.add(column_name + ": "
					+ validTableSchema.get(column_name));
		}
		String column_and_types_string = StringUtils
				.join(column_and_types, ',');

		service.create("CREATE TABLE '" + tableName + "' ("
				+ column_and_types_string + ")", true, onCompleteListener);
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
