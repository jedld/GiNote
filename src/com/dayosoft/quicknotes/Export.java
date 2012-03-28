package com.dayosoft.quicknotes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import com.dayosoft.utils.DialogUtils;
import com.dayosoft.utils.DictionaryOpenHelper;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import au.com.bytecode.opencsv.CSVWriter;

public class Export extends AsyncTask<Void, Void, Integer> {

	Context context;
	ProgressDialog dialog;
	String location;

	public Export(Context context) {
		super();
		this.context = context;
	}

	@Override
	protected void onPreExecute() {
		dialog = ProgressDialog.show(context, "", "Exporting...", true);
	}

	@Override
	protected Integer doInBackground(Void... params) {
		if (DialogUtils.isStorageWritable()) {
			DictionaryOpenHelper helper = new DictionaryOpenHelper(context);

			File path = Environment.getExternalStorageDirectory();
			try {
				SimpleDateFormat filenamedateformat = new SimpleDateFormat(
						"yyyyMMddHHmm");

				String dirlocation = path.getCanonicalPath();
				Log.d(this.getClass().toString(), dirlocation);

				File file = new File(dirlocation, "ginote-export_"
						+ filenamedateformat.format(new Date()) + ".csv");
				if (file.createNewFile()) {
					location = file.getCanonicalPath();
					List<Note> list = helper.listNotes();
					SimpleDateFormat dateformat = new SimpleDateFormat(
							"hh:mma MM/dd/yyyy Z");
					FileWriter writer = new FileWriter(file);
					CSVWriter exporter = new CSVWriter(writer);
					String header[] = { "Id", "Date", "Title", "Content",
							"Longitude", "Latitude", "Meta" };
					exporter.writeNext(header);
					for (Note note : list) {
						StringBuffer metastr = new StringBuffer();
						for (NoteMeta meta : note.getMeta()) {
							switch (meta.getType()) {
							case NoteMeta.IMAGE:
								metastr.append("img='" + meta.getResource_url()
										+ "';");
								break;
							case NoteMeta.GOOGLEMAPSURL:
								metastr.append("gpsurl='"
										+ meta.getResource_url() + "';");
								break;
							case NoteMeta.LOCATIONNAME:
								metastr.append("locname='"
										+ meta.getResource_url() + "';");
								break;
							}
						}

						String finalDateTime = dateformat.format(note.getDate_created());

						String data[] = { note.getUid(),
								finalDateTime, note.getTitle(),
								note.getContent(),
								Double.toString(note.getLongitude()),
								Double.toString(note.getLatitude()),
								metastr.toString() };
						exporter.writeNext(data);
					}
					exporter.flush();
					exporter.close();
				} else {
					return -3;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return -1;
			}
			// TODO Auto-generated method stub
		} else {
			return -2;
		}
		return 0;
	}

	@Override
	protected void onPostExecute(Integer result) {
		if (dialog != null && dialog.isShowing())
			dialog.dismiss();
		if (result == -1) {
			DialogUtils.showMessageAlert("Failed to export", context);
		} else if (result == -3) {
			DialogUtils.showMessageAlert("Unable to write to card", context);
		} else if (result == -2) {
			DialogUtils.showMessageAlert("SD Card not available", context);
		}
		if (result == 0) {
			DialogUtils.showMessageAlert("Exported to\n" + location, context);
		}
	}

}
