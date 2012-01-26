package com.dayosoft.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import com.dayosoft.quicknotes.Note;
import com.dayosoft.quicknotes.NoteMeta;

import android.app.backup.BackupManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DictionaryOpenHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 3;
	private static final String DATABASE_NAME = "QUICKNOTES";
	private static final String DICTIONARY_TABLE_NAME = "notes";
	private static final String DICTIONARY_TABLE_CREATE = "CREATE TABLE "
			+ DICTIONARY_TABLE_NAME
			+ " ( ID INTEGER PRIMARY KEY AUTOINCREMENT, TITLE TEXT, "
			+ " CONTENT TEXT, DATE_CREATED DATE DEFAULT CURRENT_TIMESTAMP, DATE_UPDATED DATE);";
	SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	BackupManager manager;

	public DictionaryOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
			manager = new BackupManager(context);
		}
	}

	public void onDataChanged() {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
			manager.dataChanged();
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
		String whereClause = null;
		if (query != null) {
			whereClause = "title LIKE '%" + query + "%'";
		}
		String columns[] = { "id" };
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
		String whereClause = null;
		if (query != null) {
			whereClause = "title LIKE '%" + query + "%'";
		}
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

	public List<Note> listNotes(String query) {
		Vector<Note> returnList = new Vector<Note>();

		String columns[] = { "id", "title", "content", "date_created" };

		String whereClause = null;
		if (query != null) {
			whereClause = "title LIKE '%" + query + "%'";
		}
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

	public Note load(int id) {
		String columns[] = { "id", "title", "content", "date_created" };
		SQLiteDatabase db = getReadableDatabase();
		Cursor notelist = db.query("notes", columns, "id=" + id, null, null,
				null, "date_created DESC LIMIT 100");
		if (notelist.moveToFirst()) {
			Note note = new Note();
			note.setId(id);
			note.setTitle(notelist.getString(1));
			note.setContent(notelist.getString(2));

			try {
				note.setDate_created(dateformat.parse(notelist.getString(3)));
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

	public void persist(Note note) {
		boolean newRecord = false;
		long row_id = 0;
		ContentValues newValues = new ContentValues();
		if (note.getId() == 0) {
			newRecord = true;
		} else {
			row_id = note.getId();
			newValues.put("ID", row_id);
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
		SimpleDateFormat dateformat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		if (note.getDate_created() == null)
			note.setDate_created(new Date());
		
		newValues.put("DATE_CREATED", dateformat.format(note.getDate_created()));
		
		String datestr = dateformat.format(note.getDate_created());

		SQLiteDatabase db = getWritableDatabase();

		if (newRecord) {
			newValues.put("DATE_UPDATED", datestr);
		} else {
			newValues.put("DATE_UPDATED", datestr);
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
		onDataChanged();
	}

	public void delete(int id) {
		SQLiteDatabase db = getWritableDatabase();
		db.delete("notes", "id=" + id, null);
		db.delete("NOTE_META", "NOTE_ID=" + id, null);
		db.close();
		onDataChanged();
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
				String sqlStr2 = "ALTER TABLE NOTES ADD COLUMN LONGTITUDE REAL";
				String sqlStr3 = "ALTER TABLE NOTES ADD COLUMN LATITUDE REAL";
				db.execSQL(sqlStr2);
				db.execSQL(sqlStr3);
				break;
			case 3:
				db.execSQL("CREATE TABLE NOTE_VERSION"
						+ " ( LAST_MODIFIED DATE);");
				db.execSQL("INSERT INTO NOTE_VERSION (LAST_MODIFIED) VALUES (date('now'));");
				break;
			}
		}
	}
}
