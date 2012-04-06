package com.dayosoft.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dayosoft.ginotefusion.GoogleFTUpdater;
import com.dayosoft.ginotefusion.ListNotes;
import com.dayosoft.ginotefusion.Note;
import com.dayosoft.ginotefusion.NoteMeta;

public class GoogleFTSyncer extends AsyncTask {
	SharedPreferences settings;

	Context context;
	ProgressDialog progress_dialog;
	static final String PREF_AUTH_TOKEN = "authToken";
	DictionaryOpenHelper helper;
	FusionTableService service;
	SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	View button;

	public GoogleFTSyncer(Context c, View button) {
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
	protected void onPreExecute() {
		// TODO Auto-generated method stub
		super.onPreExecute();
		progress_dialog = ProgressDialog.show(context, "",
				"Syncing. Please wait...", true);

		if (button != null)
			button.setEnabled(false);
	}

	@Override
	protected void onPostExecute(Object result) {
		// TODO Auto-generated method stub
		super.onPostExecute(result);
		if (button != null)
			button.setEnabled(true);
		if (progress_dialog != null) {
			progress_dialog.dismiss();
		}
		ListNotes.listAdapter.notifyChange();
	}

	@Override
	protected Object doInBackground(Object... params) {
		if (!this.getTableId().equalsIgnoreCase("")) {
			
			GoogleFTUpdater updater = new GoogleFTUpdater(context);
			updater.startSync();
			setRequestRefresh(false);
			
			ArrayList<HashMap<String, String>> result = service
					.query_sync(
							"SELECT ROWID,UID,TITLE,CONTENT,POSITION,DATE_CREATED,DATE_UPDATED,SYNC_TS,IS_DELETED FROM "
									+ getTableId()
									+ " WHERE DATE_UPDATED >= "
									+ helper.getLastFtSync(), true);
			for (HashMap<String, String> item : result) {
				Note note_ft = new Note();
				String rowid = item.get("ROWID");
				note_ft.setUid(item.get("UID"));
				note_ft.setTitle(item.get("TITLE"));
				note_ft.setContent(item.get("CONTENT"));
				note_ft.setSync_ts(Long.parseLong(item.get("SYNC_TS")));
				note_ft.setDelete_pending(Integer.parseInt(item
						.get("IS_DELETED")));
				Document document = DialogUtils.getDomElement(item
						.get("POSITION"));
				String longlat[] = StringUtils.split(document.getChildNodes()
						.item(0).getTextContent(), ',');
				double longitude = Double.parseDouble(longlat[0]);
				double latitude = Double.parseDouble(longlat[1]);

				note_ft.setLongitude(longitude);
				note_ft.setLatitude(latitude);

				if (note_ft.getLatitude() != 0 && note_ft.getLongitude() != 0) {
					NoteMeta meta = new NoteMeta();
					meta.setType(NoteMeta.GOOGLEMAPSURL);
					meta.setResource_url(GoogleMapsLocation.generateMapsUrl(
							latitude, longitude));
					note_ft.addMeta(meta);
				}

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
					if ((note.getDate_updated() < note_ft.getDate_updated())
							&& (note_ft.getFt_dirty() == 0)) {
						if (note_ft.getDelete_pending() == 1) {
							helper.delete(note.getId());
						} else {
							note.setTitle(note_ft.getTitle());
							note.setContent(note_ft.getContent());
							note.setDate_updated(note_ft.getDate_updated());
							note.setLatitude(note_ft.getLatitude());
							note.setLongitude(note_ft.getLongitude());
							helper.persist(note);
							helper.touch(note.getId(), note_ft.getSync_ts());
						}
					}
				} else {
					if (note_ft.getDelete_pending() == 0) {
						helper.persist(note_ft);
						helper.touch(note_ft.getId(), note_ft.getSync_ts());
					}
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

	private void setRequestRefresh(boolean value) {
		Editor editor = settings.edit();
		editor.putBoolean("request_sync", value);
		editor.apply();
	}
}
