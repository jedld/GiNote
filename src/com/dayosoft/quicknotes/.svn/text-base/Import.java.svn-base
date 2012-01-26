package com.dayosoft.quicknotes;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import com.dayosoft.utils.DialogUtils;
import com.dayosoft.utils.DictionaryOpenHelper;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import au.com.bytecode.opencsv.CSVReader;

public class Import extends AsyncTask<Void, Void, Integer> {

	Context context;
	ProgressDialog dialog;
	String location;
	String filename;
	DictionaryOpenHelper helper;
	boolean overwrite = false;

	public Import(Context context, String filename,
			DictionaryOpenHelper helper, boolean overwrite) {
		super();
		this.context = context;
		this.overwrite = overwrite;
		this.filename = filename;
		this.helper = helper;
	}

	@Override
	protected void onPreExecute() {
		dialog = ProgressDialog.show(context, "", "Importing...", true);
	}

	@Override
	protected Integer doInBackground(Void... params) {
		if (DialogUtils.isStorageWritable()) {
			DictionaryOpenHelper helper = new DictionaryOpenHelper(context);

			File path = Environment.getExternalStorageDirectory();
			try {
				SimpleDateFormat filenamedateformat = new SimpleDateFormat(
						"HHmmMMddyyyy");

				String dirlocation = path.getCanonicalPath();
				Log.d(this.getClass().toString(), dirlocation);

				File file = new File(dirlocation, filename);
				if (file.exists()) {
					location = file.getCanonicalPath();
					List<Note> list = helper.listNotes();
					SimpleDateFormat dateformat = new SimpleDateFormat(
							"hh:mma MM/dd/yyyy");
					FileReader reader = new FileReader(file);
					CSVReader importer = new CSVReader(reader);
					// skip the header
					importer.readNext();
					String line[];

					do {
						Note note = new Note();
						line = importer.readNext();
						try {
							note.setDate_created(dateformat.parse(line[1]));
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						note.setTitle(line[2]);
						note.setContent(line[3]);
						note.setLongitude(Double.parseDouble(line[4]));
						note.setLatitude(Double.parseDouble(line[5]));
						helper.persist(note);
					} while (line != null);
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
			DialogUtils.showMessageAlert("Failed to import", context);
		} else if (result == -3) {
			DialogUtils.showMessageAlert("Unable to write to card", context);
		} else if (result == -2) {
			DialogUtils.showMessageAlert("SD Card not available", context);
		}
		if (result == 0) {
			DialogUtils.showMessageAlert("Import complete", context);
		}
	}

}
