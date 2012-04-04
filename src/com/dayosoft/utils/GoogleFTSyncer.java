package com.dayosoft.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.Button;
import android.widget.Toast;

import com.dayosoft.quicknotes.ListNotes;
import com.dayosoft.quicknotes.Note;

public class GoogleFTSyncer extends AsyncTask {
	SharedPreferences settings;

	Context context;
	static final String PREF_AUTH_TOKEN = "authToken";
	DictionaryOpenHelper helper;
	FusionTableService service;
	SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	Button button;

	public GoogleFTSyncer(Context c, Button button) {
		this.context = c;
		this.settings = c.getSharedPreferences("ginote_settings",
				c.MODE_PRIVATE);
		this.helper = new DictionaryOpenHelper(c);
		this.button = button;
		this.service = new FusionTableService(getAuthToken());
	}

	public static String sqlLize(String value) {
		return "'" + StringUtils.replace(value, "'", "''") + "'";
	}

	@Override
	protected void onPostExecute(Object result) {
		// TODO Auto-generated method stub
		super.onPostExecute(result);
		if (button != null)
			button.setEnabled(true);
		Toast.makeText(context, "Sync in progress", Toast.LENGTH_SHORT);
	}

	@Override
	protected void onPreExecute() {
		// TODO Auto-generated method stub
		super.onPreExecute();
		if (button != null)
			button.setEnabled(false);
		
		ListNotes.listAdapter.notifyInvalidate();
	}

	@Override
	protected Object doInBackground(Object... params) {
		if (!this.getTableId().equalsIgnoreCase("")) {
			ArrayList<HashMap<String, String>> result = service.query_sync(
					"SELECT ROWID,UID,TITLE,CONTENT,DATE_CREATED,DATE_UPDATED FROM "
							+ getTableId() + " WHERE DATE_UPDATED >= "
							+ helper.getLastFtSync(), true);
			for (HashMap<String, String> item : result) {
				Note note_ft = new Note();
				String rowid = item.get("rowid");
				note_ft.setUid(item.get("UID"));
				note_ft.setTitle(item.get("TITLE"));
				note_ft.setContent(item.get("CONTENT"));
				
				note_ft.setSync_ts(System.currentTimeMillis());
				try {
					note_ft.setDate_created(dateformat.parse(item
							.get("DATE_CREATED")));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				note_ft.setDate_updated(Long.parseLong(item.get("DATE_UPDATED")));
				Note note = helper.load(item.get("UID"));
				if (note != null) {
					if (note.getDate_updated() > note_ft.getDate_updated()) {
						String queryStr = "UPDATE " + getTableId()
								+ " SET TITLE = " + sqlLize(note.getTitle())
								+ ", CONTENT = " + sqlLize(note.getContent())
								+ " , DATE_UPDATED = " + note.getDate_updated()
								+ " WHERE ROWID = '" + rowid + "';";
						ArrayList<HashMap<String, String>> update_result = service
								.create_sync(queryStr, true);
						if (update_result != null) {
							helper.touch(note.getId());
						}
					} else if (note.getDate_updated() < note_ft
							.getDate_updated()) {
						note.setTitle(note_ft.getTitle());
						note.setContent(note_ft.getContent());
						note.setDate_updated(note_ft.getDate_updated());
						helper.persist(note);
						helper.touch(note.getId());
					}
				} else {
					helper.persist(note_ft);
					helper.touch(note_ft.getId());
				}
			}
			helper.touchLastFTSync();
		}
		return null;
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
