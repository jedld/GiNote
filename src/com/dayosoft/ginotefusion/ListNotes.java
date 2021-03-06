package com.dayosoft.ginotefusion;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import com.dayosoft.utils.DialogUtils;
import com.dayosoft.utils.DictionaryOpenHelper;
import com.dayosoft.utils.GoogleFTSyncer;
import com.dayosoft.utils.ImageDownloadTask;
import com.dayosoft.utils.OnInternetReadyListener;
import com.dayosoft.utils.TimeUtils;

import android.app.Activity;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ListNotes extends Activity {
	TableLayout notesview;
	DictionaryOpenHelper helper;
	public static NoteListAdapter listAdapter;
	ListView listview;
	SharedPreferences settings;
	public static final int NOTE_ADDED = 0;
	public static final int NOTE_UPDATED = 1;
	public static final int NOTE_DELETED = 2;

	OnItemClickListener onItemClicked = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long rowid) {
			Intent intent = new Intent(ListNotes.this, ViewNotes.class);
			Log.d(this.getClass().toString(), "returning rowid=" + rowid);
			intent.putExtra("view_id", rowid);
			ListNotes.this.startActivityForResult(intent, NOTE_UPDATED);
		}

	};

	OnClickListener imageClickedLister = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ImageView image = (ImageView) v;
			Intent intent = new Intent(ListNotes.this, Picture.class);
			intent.putExtra("image_uri", image.getContentDescription());
			ListNotes.this.startActivity(intent);
		}

	};

	@Override
	public void onResume() {
		super.onResume();
		// notesview.removeAllViews();

		Intent intent = getIntent();
		List<Note> list;
		String query = null;
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			query = intent.getStringExtra(SearchManager.QUERY);
			listAdapter = new NoteListAdapter(helper, query, this);
			listview.setAdapter(listAdapter);
		}
		OnClickListener navigator = DialogUtils.setNavigator(AddNotes.class,
				this, ListNotes.NOTE_UPDATED);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);

	    // Get the SearchView and set the searchable configuration
	    SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
		SearchView searchView = (SearchView) menu.findItem(R.id.itemSearch).getActionView();
	    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		return true;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.list);
		// notesview = (TableLayout) findViewById(R.id.notesList);
		Button addNoteButton = (Button) findViewById(R.id.addButton);
		listview = (ListView) findViewById(R.id.list);
		settings = getSharedPreferences("ginote_settings", MODE_PRIVATE);
		// setup simple button navigation
		addNoteButton.setOnClickListener(DialogUtils.setNavigator(
				AddNotes.class, this, ListNotes.NOTE_ADDED));
		helper = new DictionaryOpenHelper(this);
		listAdapter = new NoteListAdapter(helper, null, this);
		listview.setAdapter(listAdapter);
		listview.setOnItemClickListener(onItemClicked);

		if (settings.getBoolean("auto_add_note", false)) {
			Intent intent = new Intent(ListNotes.this, AddNotes.class);

			ListNotes.this.startActivityForResult(intent,
					ListNotes.NOTE_UPDATED);
		}

		if (requestRefresh()) {
			GoogleFTUpdater updater = new GoogleFTUpdater(this);
			updater.execute();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.about:
			// this.getPackageManager().getApplicationInfo(this.getClass().getPackage().getName(),
			// PackageManager.GET_ACTIVITIES).;
			DialogUtils
					.showMessageAlert(
							"GiNote 3.00\nJoseph Dayo\nbugs? email jedld.android@gmail.com",
							this);
			return true;
		case R.id.deleteAllOption:
			DialogUtils.showConfirmation(
					"Are you sure you want to delete ALL Notes?", this,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							ListNotes.this.helper.clearall();
							ListNotes.this.listAdapter.notifyInvalidate();
						}

					});
			return true;
		case R.id.itemquit:
			this.finish();
			return true;
		case R.id.itemExport:
			Export export = new Export(this);
			export.execute();
			break;
		case R.id.itemOptions:
			DialogUtils.switchActivity(Options.class, this);
			break;
		case R.id.itemSync:
			View menuitem = (View) findViewById(R.id.itemSync);
			if (getTableName() == null) {
				Toast.makeText(
						this,
						"Fusion Tables needs to be setup, visit the options menu to do so.",
						Toast.LENGTH_LONG).show();
			} else {
				if (DialogUtils.hasINet(this)) {
					GoogleFTSyncer syncer = new GoogleFTSyncer(ListNotes.this,
							menuitem);
					syncer.execute();
				} else {
					Toast.makeText(
							this,
							"Unable to sync. Please ensure your device is connected to the internet",
							Toast.LENGTH_LONG).show();
				}
			}
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ListNotes.NOTE_ADDED) {
			listAdapter.notifyChange();
		} else if (requestCode == ListNotes.NOTE_UPDATED) {
			listAdapter.notifyChange();
		}
	}

	private String getTableName() {
		return settings.getString("sync_table", null);
	}

	private boolean requestRefresh() {
		return settings.getBoolean("request_sync", false);
	}

}