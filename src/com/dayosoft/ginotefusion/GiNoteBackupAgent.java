package com.dayosoft.ginotefusion;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.dayosoft.utils.DictionaryOpenHelper;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class GiNoteBackupAgent extends BackupAgent {
	DictionaryOpenHelper helper;

	@Override
	public void onCreate() {
		helper = new DictionaryOpenHelper(this);
	}

	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
			ParcelFileDescriptor newState) throws IOException {
		// Get the oldState input stream
		FileInputStream instream = new FileInputStream(
				oldState.getFileDescriptor());
		DataInputStream in = new DataInputStream(instream);
		Log.d(this.getClass().toString(), "Performing backup");
		try {
			// Get the last modified timestamp from the state file and data file
			String stateModified = in.readLine();
			String fileModified = helper.getLastModified();

			if (stateModified != fileModified) {
				ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
				DataOutputStream outWriter = new DataOutputStream(bufStream);

				JSONObject noteset = new JSONObject();

				for (Note note : helper.listNotes()) {
					Log.d(this.getClass().toString(),
							"Backup id " + note.getId());
					// Create buffer stream and data output stream for our data

					// Write structured data
					JSONObject notejson = new JSONObject();
					SimpleDateFormat dateformat = new SimpleDateFormat(
							"yyyy-MM-dd HH:mm:ss");
					notejson.put("id", note.getId());
					notejson.put("title", note.getTitle());
					notejson.put("content", note.getContent());
					notejson.put("date_created",
							dateformat.format(note.getDate_created()));

					for (NoteMeta meta : note.getMeta()) {
						JSONObject metajson = new JSONObject();
						metajson.put("id", meta.getId());
						metajson.put("type", meta.getType());
						metajson.put("resource_url", meta.getResource_url());
						notejson.accumulate("meta", metajson);
					}
					noteset.accumulate("notes", notejson);
				}
				// Send the data to the Backup Manager via the
				// BackupDataOutput
				// if (Log.isLoggable(this.getClass().toString(),
				// Log.DEBUG)) {
				Log.d(this.getClass().toString(), noteset.toString());
				// }
				outWriter.writeChars(noteset.toString());
				byte[] buffer = bufStream.toByteArray();
				int len = buffer.length;
				data.writeEntityHeader("note_data", len);
				data.writeEntityData(buffer, len);

				FileOutputStream outstream = new FileOutputStream(
						newState.getFileDescriptor());
				DataOutputStream out = new DataOutputStream(outstream);
				out.writeChars(helper.getLastModified());
				return;
			}

		} catch (IOException e) {
			// Unable to read state file... be safe and do a backup
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode,
			ParcelFileDescriptor newState) throws IOException {
		SimpleDateFormat dateformat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		switch (appVersionCode) {
		case 20:
		default:
			Log.d(this.getClass().toString(), "Restoring backup");
			Charset charset = Charset.forName("UTF-16");
			while (data.readNextHeader()) {
				Log.d(this.getClass().toString(),
						"Getting key [" + data.getKey() + "]");
				if (data.getKey().equalsIgnoreCase("note_data")) {
					int size = data.getDataSize();
					byte buffer[] = new byte[size];
					data.readEntityData(buffer, 0, size);
					try {

						CharsetDecoder decoder = charset.newDecoder();
						String restoreData = decoder.decode(
								ByteBuffer.wrap(buffer)).toString();
						Log.d(this.getClass().toString(), "restoring "
								+ restoreData);
						JSONObject notes = new JSONObject(restoreData);
						JSONArray jsonarray = new JSONArray();
						if (notes.has("notes")) {
							if (notes.get("notes") instanceof JSONObject) {
								Log.d(this.getClass().toString(),
										"JSONObject detected.");
								jsonarray.put(notes.get("notes"));
							} else if (notes.get("notes") instanceof JSONArray) {
								jsonarray = notes.getJSONArray("notes");
								Log.d(this.getClass().toString(),
										"JSONArray detected.");
							}
						}

						for (int i2 = 0; i2 < jsonarray.length(); i2++) {
							JSONObject notejson = jsonarray.getJSONObject(i2);
							Note note = new Note();
							Log.d(this.getClass().toString(), "Processing "
									+ notejson.getInt("id"));
							note.setId(notejson.getInt("id"));
							note.setTitle(notejson.getString("title"));
							note.setContent(notejson.getString("content"));
							note.setDate_created(dateformat.parse(notejson
									.getString("date_created")));
							if (notejson.has("meta")) {

								if (notejson.get("meta") instanceof JSONObject) {
									NoteMeta notemeta = new NoteMeta();
									JSONObject metaobject = notejson
											.getJSONObject("meta");
									notemeta.setId(metaobject.getInt("id"));
									notemeta.setType(metaobject.getInt("type"));
									notemeta.setResource_url(metaobject
											.getString("resource_url"));
									note.addMeta(notemeta);
								} else {
									JSONArray meta_array = notejson
											.getJSONArray("meta");
									for (int i = 0; i < meta_array.length(); i++) {
										NoteMeta notemeta = new NoteMeta();
										JSONObject metaobject = meta_array
												.getJSONObject(i);
										notemeta.setId(metaobject.getInt("id"));
										notemeta.setType(metaobject
												.getInt("type"));
										notemeta.setResource_url(metaobject
												.getString("resource_url"));
										note.addMeta(notemeta);
									}
								}
							}
							helper.persist(note);
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
			FileOutputStream outstream = new FileOutputStream(
					newState.getFileDescriptor());
			DataOutputStream out = new DataOutputStream(outstream);
			out.writeChars(helper.getLastModified());
			break;
		}

	}

}
