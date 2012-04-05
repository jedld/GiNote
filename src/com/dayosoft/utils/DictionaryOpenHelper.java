package com.dayosoft.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;

import com.dayosoft.quicknotes.GoogleFTUpdater;
import com.dayosoft.quicknotes.Note;
import com.dayosoft.quicknotes.NoteMeta;
import com.dayosoft.quicknotes.NoteSyncer;

import android.app.backup.BackupManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DictionaryOpenHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 8;
	private static final String DATABASE_NAME = "QUICKNOTES";
	private static final String DICTIONARY_TABLE_NAME = "notes";
	private static final String DICTIONARY_TABLE_CREATE = "CREATE TABLE "
			+ DICTIONARY_TABLE_NAME
			+ " ( ID INTEGER PRIMARY KEY AUTOINCREMENT, TITLE TEXT, "
			+ " CONTENT TEXT, DATE_CREATED DATE DEFAULT CURRENT_TIMESTAMP, DATE_UPDATED INTEGER);";
	SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	BackupManager manager;
	Context context;

	public DictionaryOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
			manager = new BackupManager(context);
		}
	}

	public void onDataChanged() {
		onDataChanged(true);
	}

	public void onDataChanged(boolean ft_sync) {
		if (DialogUtils.hasINet() && ft_sync) {
			GoogleFTUpdater updater = new GoogleFTUpdater(context);
			updater.execute();
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		onUpgrade(db, 0, DATABASE_VERSION);
	}

	public void clearall() {
		SQLiteDatabase db = getWritableDatabase();
		db.delete("NOTES", null, null);
		db.delete("NOTE_META", null, null);
		db.execSQL("UPDATE NOTE_VERSION SET LAST_MODIFIED=date('now')");
		onDataChanged();
	}

	public List<Note> listNotes() {
		return listNotes(null);
	}

	public Vector<Integer> getNoteIds(String query) {
		Vector<Integer> results = new Vector<Integer>();
		

		String wheres[] = {"title LIKE '%" + query + "%'"};
		String whereClause = query!=null ? getWhereClause(wheres) : getWhereClause(null);
		
		String columns[] = { "id" };
		Log.d(this.getClass().toString(), "Reading note ids");
		Cursor notelist = getReadableDatabase().query("notes", columns,
				whereClause, null, null, null, "date_created DESC LIMIT 100");
		if (notelist.moveToFirst()) {
			do {
				results.add(notelist.getInt(0));
			} while (notelist.moveToNext());
		}
		notelist.close();
		return results;
	}

	public int countNotes(String query) {
		String wheres[] = {"title LIKE '%" + query + "%'"};
		String whereClause = query!=null ? getWhereClause(wheres) : getWhereClause(null);
		String columns[] = { "count(id) as total" };
		Cursor notelist = getReadableDatabase().query("notes", columns,
				whereClause, null, null, null, "date_created DESC LIMIT 100");
		int total = 0;
		if (notelist.moveToFirst()) {
			total = notelist.getInt(0);
		}
		notelist.close();
		return total;
	}

	public int count(String table, String whereClause) {
		Cursor count_raw = getReadableDatabase().rawQuery(
				"SELECT COUNT(*) AS COUNT FROM " + table + " WHERE "
						+ whereClause, null);
		count_raw.moveToFirst();
		return count_raw.getInt(0);
	}

	public void listUnsyncedNotes(NoteSyncer sync) {

		String columns[] = { "id", "title", "content", "date_created",
				"date_updated", "uid", "sync_ts","latitude","longitude","delete_pending"};

		String whereClause = "SYNC_TS = 0 OR FT_DIRTY = 1";
		int total_records = count("NOTES", whereClause);
		for (int offset = 0; offset <= total_records; offset += 500) {
			Cursor notelist = getReadableDatabase().query("notes", columns,
					whereClause, null, null, null,
					"date_created DESC LIMIT " + offset + ",500");
			if (notelist.moveToFirst()) {
				// Iterate over each cursor.
				Note new_notes_array[] = {}, updated_notes_array[] = {};
				ArrayList<Note> new_notes = new ArrayList<Note>(), updated_notes = new ArrayList<Note>();
				do {
					Note note = new Note();
					note.setId(notelist.getInt(0));
					note.setTitle(notelist.getString(1));
					note.setContent(notelist.getString(2));
					note.setUid(notelist.getString(5));
					Log.d(this.getClass().toString(),
							"loading uid " + notelist.getString(5));
					note.setSync_ts(notelist.getLong(6));
					note.setLatitude(notelist.getDouble(7));
					note.setLongitude(notelist.getDouble(8));
					note.setDelete_pending(notelist.getInt(9));
					SimpleDateFormat dateformat = new SimpleDateFormat(
							"yyyy-MM-dd HH:mm:ss");
					try {
						String datestr = notelist.getString(3);
						note.setDate_created(dateformat.parse(datestr));
						note.setDate_updated(notelist.getLong(4));
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					getMeta(note);
					if (note.getSync_ts() == 0) {
						new_notes.add(note);
					} else {
						updated_notes.add(note);
					}
				} while (notelist.moveToNext());

				new_notes_array = new_notes.toArray(new_notes_array);
				updated_notes_array = updated_notes
						.toArray(updated_notes_array);

				if (new_notes_array.length > 0) {
					Log.d(this.getClass().toString(), "syncing total = "
							+ new_notes_array.length);
					sync.process_new_records(new_notes_array);
				}
				if (updated_notes_array.length > 0) {
					Log.d(this.getClass().toString(), "syncing total = "
							+ updated_notes_array.length);
					sync.process_updated_records(updated_notes_array);
				}
			}
			notelist.close();
		}
		;

	}

	private String getWhereClause(String wheres[]) {
		ArrayList <String>whereClauses = new ArrayList<String>();
		whereClauses.add("DELETE_PENDING = 0");
		if (wheres != null) {
			for(String where : wheres) {
				whereClauses.add(where);
			}
		}
		return StringUtils.join(whereClauses, " AND ");
	}
	
	public List<Note> listNotes(String query) {
		Vector<Note> returnList = new Vector<Note>();

		String columns[] = { "id", "title", "content", "date_created" };

		String wheres[] = {"title LIKE '%" + query + "%'"};
		String whereClause = query!=null ? getWhereClause(wheres) : getWhereClause(null);
		
		Cursor notelist = getReadableDatabase().query("notes", columns,
				whereClause, null, null, null, "date_created DESC LIMIT 1000");
		if (notelist.moveToFirst()) {
			// Iterate over each cursor.
			do {
				Note note = new Note();
				note.setId(notelist.getInt(0));
				note.setTitle(notelist.getString(1));
				note.setContent(notelist.getString(2));
				SimpleDateFormat dateformat = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss");
				try {
					String datestr = notelist.getString(3);
					note.setDate_created(dateformat.parse(datestr));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				getMeta(note);
				returnList.add(note);
			} while (notelist.moveToNext());
		}
		notelist.close();
		return returnList;
	}

	public void getMeta(Note note) {
		SQLiteDatabase db = getReadableDatabase();
		String meta_columns[] = { "id", "note_id", "meta_type", "resource_url" };
		Cursor metalist = db.query("NOTE_META", meta_columns,
				"note_id=" + note.getId(), null, null, null, null);
		if (metalist.moveToFirst()) {
			do {
				NoteMeta meta = new NoteMeta();
				meta.setType(metalist.getInt(2));
				meta.setResource_url(metalist.getString(3));
				note.addMeta(meta);
			} while (metalist.moveToNext());
		}
		metalist.close();
	}

	public int getLastFtSync() {
		int date = 0;
		SQLiteDatabase db = getReadableDatabase();
		String columns[] = { "last_ft_sync" };
		Cursor versionCursor = db.query("note_version", columns, null, null,
				null, null, null);
		if (versionCursor.moveToFirst()) {
			date = versionCursor.getInt(0);
		}
		versionCursor.close();
		return date;
	}

	public String getLastModified() {
		String date = "";
		SQLiteDatabase db = getReadableDatabase();
		String columns[] = { "last_modified" };
		Cursor versionCursor = db.query("note_version", columns, null, null,
				null, null, null);
		if (versionCursor.moveToFirst()) {
			date = versionCursor.getString(0);
		}
		versionCursor.close();
		return date;
	}

	public Note load(String uid) {
		String columns[] = { "id", "title", "content", "uid", "date_created",
				"date_updated", "sync_ts", "ft_dirty" };
		SQLiteDatabase db = getReadableDatabase();
		Cursor notelist = db.query("notes", columns, "uid='" + uid + "'", null,
				null, null, "date_created DESC LIMIT 100");
		Log.d(this.getClass().toString(), "loading object uid = " + uid);
		if (notelist.moveToFirst()) {
			Log.d(this.getClass().toString(), "object uid = " + uid + " loaded.");
			Note note = new Note();
			note.setId(notelist.getInt(0));
			note.setTitle(notelist.getString(1));
			note.setContent(notelist.getString(2));
			note.setUid(notelist.getString(3));
			note.setDate_updated(notelist.getLong(5));
			note.setSync_ts(notelist.getLong(6));
			note.setFt_dirty(notelist.getInt(7));

			try {
				note.setDate_created(dateformat.parse(notelist.getString(4)));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			getMeta(note);
			notelist.close();
			return note;
		}
		notelist.close();
		return null;
	}

	public Note load(int id) {
		String columns[] = { "id", "title", "content", "uid", "date_created",
				"date_updated", "sync_ts", "latitude", "longitude", "ft_dirty" };
		SQLiteDatabase db = getReadableDatabase();
		Cursor notelist = db.query("notes", columns, "id=" + id, null, null,
				null, "date_created DESC LIMIT 100");
		if (notelist.moveToFirst()) {
			Note note = new Note();
			note.setId(id);
			note.setTitle(notelist.getString(1));
			note.setContent(notelist.getString(2));
			note.setUid(notelist.getString(3));

			note.setDate_updated(notelist.getLong(5));
			note.setSync_ts(notelist.getLong(6));
			note.setLatitude(notelist.getDouble(7));
			note.setLongitude(notelist.getDouble(8));
			note.setFt_dirty(notelist.getInt(9));
			
			try {
				note.setDate_created(dateformat.parse(notelist.getString(4)));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			getMeta(note);
			notelist.close();
			return note;
		}
		notelist.close();
		return null;
	}

	public void touch(int id) {
		touch(id, System.currentTimeMillis());
	}
	
	public void touchForDelete(int id) {
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("UPDATE NOTES SET DELETE_PENDING = 1, FT_DIRTY = 1"
				+ " WHERE id=" + id);
		db.close();
		onDataChanged();
	}
	
	public void touch(int id, long ts) {
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("UPDATE NOTES SET FT_DIRTY = 0, SYNC_TS=" + ts
				+ " WHERE id=" + id);
		db.close();
	}

	public void touchLastFTSync() {
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("UPDATE NOTE_VERSION SET LAST_FT_SYNC = "
				+ System.currentTimeMillis());
		db.close();
	}

	public long persist(Note note) {
		boolean newRecord = false;
		long row_id = 0;
		ContentValues newValues = new ContentValues();
		if (note.getId() == 0) {
			newRecord = true;
			Log.d(this.getClass().toString(),"Persist new record");
		} else {
			row_id = note.getId();
			newValues.put("ID", row_id);
			Log.d(this.getClass().toString(),"update record");
		}

		if (note.getTitle() == null || note.getTitle().trim().equals("")) {
			String content = note.getContent();
			if (content.length() > 30) {
				content = content.substring(0, 30);
			}
			if (content.indexOf("\n") != -1) {
				content = content.substring(0, content.indexOf("\n"));
			}

			note.setTitle(content);
		}
		newValues.put("TITLE", note.getTitle());
		newValues.put("CONTENT", note.getContent());
		newValues.put("LATITUDE", note.getLatitude());
		newValues.put("LONGITUDE", note.getLongitude());
		
		SimpleDateFormat dateformat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		if (note.getDate_created() == null)
			note.setDate_created(new Date());

		newValues
				.put("DATE_CREATED", dateformat.format(note.getDate_created()));

		String datestr = dateformat.format(note.getDate_created());

		SQLiteDatabase db = getWritableDatabase();

		if (newRecord) {
			newValues.put("DATE_UPDATED",
					note.getDate_updated() == null ? System.currentTimeMillis()
							: note.getDate_updated());
			newValues.put("SYNC_TS", note.getSync_ts());
			newValues.put("UID", note.getUid() == null ? UUID.randomUUID()
					.toString() : note.getUid());
		} else {
			if (note.getSync_ts() > 0) {
				newValues.put("FT_DIRTY", 1);
			}
			newValues.put("DATE_UPDATED", System.currentTimeMillis());
			newValues.put("UID", note.getUid());
			db.delete("NOTE_META", "NOTE_ID=" + note.getId(), null);
		}

		row_id = db.replace("notes", null, newValues);

		for (NoteMeta meta : note.getMeta()) {
			ContentValues metaNewValues = new ContentValues();
			metaNewValues.put("NOTE_ID", row_id);
			metaNewValues.put("META_TYPE", meta.getType());
			metaNewValues.put("RESOURCE_URL", meta.getResource_url());
			db.insert("NOTE_META", null, metaNewValues);
		}
		db.execSQL("UPDATE NOTE_VERSION SET LAST_MODIFIED=date('now')");
		db.close();
		return row_id;
	}

	public long persistAndSync(Note note) {
		onDataChanged();
		long row_id = persist(note);
		return row_id;
	}

	public void delete(int id) {
		SQLiteDatabase db = getWritableDatabase();
		db.delete("notes", "id=" + id, null);
		db.delete("NOTE_META", "NOTE_ID=" + id, null);
		db.close();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldversion, int newversion) {
		// TODO Auto-generated method stub
		for (int version = oldversion + 1; version <= newversion; ++version) {
			switch (version) {
			case 1:
				db.execSQL(DICTIONARY_TABLE_CREATE);
				break;
			case 2:
				String sqlStr1 = "CREATE TABLE NOTE_META"
						+ " ( ID INTEGER PRIMARY KEY ASC, NOTE_ID INTEGER, "
						+ " META_TYPE INTEGER, RESOURCE_URL TEXT);";
				db.execSQL(sqlStr1);
				String sqlStr2 = "ALTER TABLE NOTES ADD COLUMN LONGITUDE REAL";
				String sqlStr3 = "ALTER TABLE NOTES ADD COLUMN LATITUDE REAL";
				db.execSQL(sqlStr2);
				db.execSQL(sqlStr3);
				break;
			case 3:
				db.execSQL("CREATE TABLE NOTE_VERSION"
						+ " ( LAST_MODIFIED DATE);");
				db.execSQL("INSERT INTO NOTE_VERSION (LAST_MODIFIED) VALUES (date('now'));");
				break;
			case 4:
				db.execSQL("ALTER TABLE NOTES ADD COLUMN UID TEXT");
				break;
			case 5:
				db.execSQL("ALTER TABLE NOTES ADD COLUMN SYNC_TS INTEGER DEFAULT 0");
				break;
			case 6:
				db.execSQL("ALTER TABLE NOTE_VERSION ADD COLUMN LAST_FT_SYNC INTEGER DEFAULT 0");
				break;
			case 7:
				db.execSQL("ALTER TABLE NOTES ADD COLUMN FT_DIRTY INTEGER DEFAULT 0");
				break;
			case 8:
				db.execSQL("ALTER TABLE NOTES ADD COLUMN DELETE_PENDING INTEGER DEFAULT 0");
				break;
			}
		}
	}
}
