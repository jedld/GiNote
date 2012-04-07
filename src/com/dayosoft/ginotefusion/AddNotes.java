/**
 * 
 */
package com.dayosoft.ginotefusion;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dayosoft.utils.DialogUtils;
import com.dayosoft.utils.DictionaryOpenHelper;
import com.dayosoft.utils.GoogleMapsLocation;
import com.dayosoft.utils.LocationFixedListener;

/**
 * @author Joseph Emmanuel Dayo
 * 
 */
public class AddNotes extends Activity implements LocationFixedListener,
		OnClickListener {
	DictionaryOpenHelper helper;
	EditText titleField, contentField;
	TextView urlField;
	SharedPreferences prefs;
	Location latestloc;
	String googleurl;
	GoogleMapsLocation locator;
	Uri imageUri;
	int currentNoteId;
	boolean alreadySaved = true;
	Note current_note;
	LinearLayout metaContent;

	Vector<NoteMeta> metalist = new Vector<NoteMeta>();

	static final int ACTION_PICK_IMAGE = 0;
	static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1;

	OnClickListener imageClickedLister = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(AddNotes.this, Picture.class);
			intent.putExtra("note_id", Integer.toString(currentNoteId));
			AddNotes.this.startActivity(intent);
		}

	};

	OnClickListener lockGPSOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			locator = new GoogleMapsLocation(AddNotes.this, AddNotes.this);
			locator.startGetFix();
		}

	};

	OnClickListener deleteNoteListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			DialogUtils.showConfirmation("Are you sure you want to delete?",
					AddNotes.this,
					new android.content.DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							helper.touchForDelete(currentNoteId);
							helper.onDataChanged();
							AddNotes.this.finish();
						}

					}

			);

		}

	};

	OnClickListener updateNoteListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Note note = new Note();
			note.setId(currentNoteId);
			note.setTitle(titleField.getText().toString());
			note.setContent(contentField.getText().toString());
			note.setDate_created(current_note.getDate_created());
			note.setSync_ts(current_note.getSync_ts());
			note.setUid(current_note.getUid());
			if (latestloc != null) {
				note.setLongitude(latestloc.getLongitude());
				note.setLatitude(latestloc.getLatitude());
				note.clearMeta(NoteMeta.GOOGLEMAPSURL);
				NoteMeta meta = new NoteMeta();
				meta.setType(NoteMeta.GOOGLEMAPSURL);
				meta.setResource_url(googleurl);
				note.addMeta(meta);
			}
			for (NoteMeta meta : metalist) {
				note.addMeta(meta);
			}
			helper.persistAndSync(note);
			AddNotes.this.finish();
		}

	};

	OnClickListener saveNoteListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Note note = new Note();
			String title = AddNotes.this.titleField.getText().toString();
			String content = AddNotes.this.contentField.getText().toString();
			note.setTitle(title);
			note.setContent(content);
			note.setDate_created(new Date());
			if (latestloc != null) {
				note.setLongitude(latestloc.getLongitude());
				note.setLatitude(latestloc.getLatitude());
				NoteMeta meta = new NoteMeta();
				meta.setType(NoteMeta.GOOGLEMAPSURL);
				meta.setResource_url(googleurl);
				note.addMeta(meta);
			}
			for (NoteMeta meta : metalist) {
				note.addMeta(meta);
			}
			helper.persistAndSync(note);
			AddNotes.this.finish();
		}

	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addnote);
		prefs = getSharedPreferences("ginote_settings", MODE_PRIVATE);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		helper = new DictionaryOpenHelper(this);
		Button saveButton = (Button) findViewById(R.id.CreateNewNote);
		TextView timelabel = (TextView) findViewById(R.id.time);
		urlField = (TextView) findViewById(R.id.url);

		ImageView homeButton = (ImageView) findViewById(R.id.home);
		homeButton.setOnClickListener(DialogUtils.closeNavigator(this));
		
		ImageView imageIcon = (ImageView) findViewById(R.id.imageIcon);
		imageIcon.setOnClickListener(DialogUtils.closeNavigator(this));

		ImageView camera = (ImageView) findViewById(R.id.itemCamera);
		camera.setOnClickListener(this);

		ImageView barcode = (ImageView) findViewById(R.id.itemBarcode);
		barcode.setOnClickListener(this);

		metaContent = (LinearLayout) findViewById(R.id.linearLayoutMeta);
		saveButton.setOnClickListener(saveNoteListener);
		// saveButton.getBackground().setColorFilter(0xFF00FF00, Mode.MULTIPLY);

		titleField = (EditText) findViewById(R.id.NoteTitle);
		contentField = (EditText) findViewById(R.id.ContentField);

		urlField.requestFocus();
		Intent intent = getIntent();
		int row_id = (int) intent.getLongExtra("view_id", 0);

		if (row_id != 0) {
			currentNoteId = row_id;
			Note note = helper.load(row_id);

			current_note = note;

			List<NoteMeta> metalist = note.getMeta(NoteMeta.GOOGLEMAPSURL);
			if (metalist.size() > 0) {
				urlField.setText(metalist.get(0).getResource_url());
			}
			View layout = findViewById(R.id.newnotelayout);
			layout.setVisibility(View.GONE);
			layout = findViewById(R.id.updatenotelayout);
			layout.setVisibility(View.VISIBLE);
			metaContent.setVisibility(View.VISIBLE);
			this.metalist = new Vector<NoteMeta>(note.getMeta());
			titleField.setText(note.getTitle());
			contentField.setText(note.getContent());

			SimpleDateFormat dateformat = new SimpleDateFormat(
					"hh:mma MM-dd-yyyy");
			Button deleteNoteButton = (Button) findViewById(R.id.DeleteNote);
			deleteNoteButton.setOnClickListener(deleteNoteListener);
			Button saveNoteButton = (Button) findViewById(R.id.UpdateNote);

			saveNoteButton.setOnClickListener(updateNoteListener);
			timelabel.setText(dateformat.format(note.getDate_created()));
		}
		if (currentNoteId == 0) {
			contentField.requestFocus();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.noteoptions, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		if (!selectionHandlers(item.getItemId())) {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.notecontext, menu);
	}

	@Override
	public void onResume() {
		super.onResume();
		boolean use_gps = prefs.getBoolean("use_gps", false);
		if (use_gps) {
			locator = new GoogleMapsLocation(this, this);
			locator.startGetFix();
		}

	}

	@Override
	public void onLocationFixed(Location location, String url) {
		this.latestloc = location;
		this.googleurl = url;

		urlField.setText(url);
		metaContent.setVisibility(View.VISIBLE);
		Toast.makeText(this, "Geotagging complete", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onLocationError(int status) {
		// TODO Auto-generated method stub

	}

	public boolean hasImageCaptureBug() {

		// list of known devices that have the bug
		ArrayList<String> devices = new ArrayList<String>();
		devices.add("android-devphone1/dream_devphone/dream");
		devices.add("generic/sdk/generic");
		devices.add("vodafone/vfpioneer/sapphire");
		devices.add("tmobile/kila/dream");
		devices.add("verizon/voles/sholes");
		devices.add("google_ion/google_ion/sapphire");

		return devices.contains(android.os.Build.BRAND + "/"
				+ android.os.Build.PRODUCT + "/" + android.os.Build.DEVICE);

	}

	private void showCamera() {
		if (DialogUtils.isStorageWritable()) {
			SimpleDateFormat format = new SimpleDateFormat("hhmmssMMDDyyyy");
			String fileName = "ginote_" + format.format(new Date())
					+ ".jpg.tmp";
			ContentValues values = new ContentValues();
			values.put(MediaColumns.TITLE, fileName);
			values.put(ImageColumns.DESCRIPTION, "GiNote image");
			// imageUri is the current activity attribute, define and save
			// it
			// for later usage (also in onSaveInstanceState)
			imageUri = getContentResolver().insert(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
			// create new Intent
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

			intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageUri);
			intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
			startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
		} else {
			DialogUtils.showMessageAlert("SD Card is not mounted!", this);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.itemPhotos:
			Intent i = new Intent(
					Intent.ACTION_PICK,
					android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
			startActivityForResult(i, ACTION_PICK_IMAGE);
			break;
		case R.id.itemCamera:
			showCamera();
			break;
		default:
			return super.onContextItemSelected(item);
		}
		return true;
	}

	@Override
	public void onStop() {
		super.onStop();
		if (locator != null) {
			locator.removeUpdates();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == IntentIntegrator.REQUEST_CODE) {
			IntentResult scanResult = IntentIntegrator.parseActivityResult(
					requestCode, resultCode, data);
			if (scanResult != null) {
				contentField.setText(contentField.getText().toString()
						+ scanResult.getContents());
				titleField.requestFocus();
			}
		} else if (requestCode == ACTION_PICK_IMAGE) {
			if (resultCode == RESULT_OK) {
				Uri selectedImage = data.getData();
				String[] filePathColumn = { MediaColumns.DATA };

				Cursor cursor = getContentResolver().query(selectedImage,
						filePathColumn, null, null, null);
				cursor.moveToFirst();

				int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
				String filePath = cursor.getString(columnIndex);
				cursor.close();

				NoteMeta meta = new NoteMeta();
				meta.setResource_url(filePath);
				meta.setType(NoteMeta.IMAGE);
				metalist.add(meta);
				Toast.makeText(this, "Picture added to note",
						Toast.LENGTH_SHORT).show();
			}
		} else if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				// use imageUri here to access the image
				if (imageUri != null) {
					File f = DialogUtils.convertImageUriToFile(imageUri, this);
					String origfile = "";
					String newfilename = "";
					try {
						newfilename = f.getCanonicalPath();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					NoteMeta meta = new NoteMeta();
					meta.setResource_url(newfilename);

					meta.setType(NoteMeta.IMAGE);
					metalist.add(meta);
					Toast.makeText(this, "Picture added to note",
							Toast.LENGTH_SHORT).show();
				}
			} else if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, "Picture was not taken",
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, "Picture was not taken",
						Toast.LENGTH_SHORT).show();
			}
		}

	}

	private boolean selectionHandlers(int resId) {
		switch (resId) {
		case R.id.about:
			DialogUtils
					.showMessageAlert(
							"GiNote 2.13\nJoseph Dayo\nbugs? email jedld.android@gmail.com",
							this);
			return true;
		case R.id.home:
			finish();
			return true;
		case R.id.itemOptions:
			DialogUtils.switchActivity(Options.class, this);
			return true;
		case R.id.itemLockGPS:
			locator = new GoogleMapsLocation(AddNotes.this, AddNotes.this);
			locator.startGetFix();
			return true;
		case R.id.itemCamera:
			showCamera();
			return true;
		case R.id.itemGallery:
			Intent i = new Intent(
					Intent.ACTION_PICK,
					android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
			startActivityForResult(i, ACTION_PICK_IMAGE);
			return true;
		case R.id.itemBarcode:
			IntentIntegrator integrator = new IntentIntegrator(this);
			integrator.initiateScan();
			return true;
		}
		return true;
	}

	@Override
	public void onClick(View v) {
		selectionHandlers(v.getId());
	}
}
