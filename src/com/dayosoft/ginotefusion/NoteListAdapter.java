package com.dayosoft.ginotefusion;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.dayosoft.utils.DictionaryOpenHelper;
import com.dayosoft.utils.ImageDownloadTask;
import com.dayosoft.utils.TimeUtils;

public class NoteListAdapter implements ListAdapter {

	DictionaryOpenHelper helper;
	String query;
	Vector<Integer> idlist;
	Activity context;
	HashMap<Object, DataSetObserver> dataObservers = new HashMap<Object, DataSetObserver>();

	public NoteListAdapter(DictionaryOpenHelper helper, String query,
			Activity context) {
		this.helper = helper;
		this.query = query;
		this.context = context;
	}

	@Override
	public int getCount() {
		return helper.countNotes(query);
	}

	@Override
	public Object getItem(int index) {
		if (idlist == null) {
			idlist = helper.getNoteIds(query);
		}

		int note_id = idlist.get(index);
		return helper.load(note_id);

	}

	@Override
	public long getItemId(int index) {
		if (idlist == null) {
			idlist = helper.getNoteIds(query);
		}

		return idlist.get(index);
	}

	@Override
	public int getItemViewType(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	OnClickListener imageClickedLister = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ImageView image = (ImageView) v;
			Intent intent = new Intent(NoteListAdapter.this.context,
					Picture.class);
			intent.putExtra("note_id", (String) image.getTag());
			intent.putExtra("image_uri", image.getContentDescription());
			NoteListAdapter.this.context.startActivity(intent);
		}

	};

	@Override
	public View getView(int index, View convertView, ViewGroup group) {
		if (idlist == null) {
			Log.d(this.getClass().toString(), "loading note list");
			idlist = helper.getNoteIds(query);
		}

		int note_id = idlist.get(index);
		Note note = helper.load(note_id);
		View view = convertView;
		if (convertView == null) {
			view = context.getLayoutInflater().inflate(R.layout.notelistitem,
					null);
		}

		LinearLayout layout = (LinearLayout) view.findViewById(R.id.itempanel);
		LinearLayout imagelayout = (LinearLayout) view
				.findViewById(R.id.imagepanel);
		TextView text = (TextView) view.findViewById(R.id.title);
		TextView datetime = (TextView) view.findViewById(R.id.datetime);
		TextView gpslocation = (TextView) view.findViewById(R.id.gpslocation);
		TextView todayIndicator = (TextView) view
				.findViewById(R.id.todayIndicator);
		TextView yesterdayIndicator = (TextView) view
				.findViewById(R.id.yesterdayIndicator);
		TextView fewdaysIndicator = (TextView) view
				.findViewById(R.id.fewdaysIndicator);
		
		
		todayIndicator.setVisibility(View.GONE);
		yesterdayIndicator.setVisibility(View.GONE);
		fewdaysIndicator.setVisibility(View.GONE);
		Locale.setDefault(Locale.US);
		SimpleDateFormat dividerDateFormat = new SimpleDateFormat(
				"E, MMMMMMMMMMMMMM dd yyyy ");
		if (index == 0) {
			if (TimeUtils.isToday(note.getDate_created())) {
				todayIndicator.setVisibility(View.VISIBLE);
				todayIndicator.setText("Today ("
						+ dividerDateFormat.format(note.getDate_created())
						+ ")");
			} else if (TimeUtils.isYesterday(note.getDate_created())) {
				yesterdayIndicator.setVisibility(View.VISIBLE);
				yesterdayIndicator.setText("Yesterday ("
						+ dividerDateFormat.format(note.getDate_created())
						+ ")");
			} else {
				fewdaysIndicator.setVisibility(View.VISIBLE);
				fewdaysIndicator.setText(dividerDateFormat.format(note
						.getDate_created()));
			}
		} else {

			int prev_note_id = idlist.get(index - 1);
			Note prev_note = helper.load(prev_note_id);

			if (!TimeUtils.isSameDay(note.date_created, prev_note.date_created)) {
				if (TimeUtils.isYesterday(note.getDate_created())) {
					yesterdayIndicator.setVisibility(View.VISIBLE);
				} else {
					fewdaysIndicator.setVisibility(View.VISIBLE);
					fewdaysIndicator.setText(dividerDateFormat.format(note
							.getDate_created()));
				}
			}
		}

		if (index == 0 && TimeUtils.isToday(note.getDate_created())) {
			todayIndicator.setVisibility(View.VISIBLE);
		} else {
			todayIndicator.setVisibility(View.GONE);
		}
	
		String title = note.getTitle();
		if (note.getTitle().length() > 30) {
			title = note.getTitle().substring(0, 30);
		}

		text.setText(title);
		datetime.setText(TimeUtils.computeRelativeTimeString(note
				.getDate_created()));

		List<NoteMeta> metalist = note.getMeta(NoteMeta.GOOGLEMAPSURL);
		if (metalist.size() > 0) {
			String locationstr = metalist.get(0).getResource_url();
			gpslocation.setText(locationstr);
			gpslocation.setVisibility(View.VISIBLE);
		} else {
			gpslocation.setVisibility(View.GONE);
		}

		List<NoteMeta> imagelist = note.getMeta(NoteMeta.IMAGE);
		if (imagelist.size() > 0) {
			imagelayout.setVisibility(View.VISIBLE);
			LinearLayout images = (LinearLayout) view.findViewById(R.id.images);
			images.removeAllViews();
			int count = 0;
			System.gc();
			for (NoteMeta imagemeta : imagelist) {
				ImageView image = new ImageView(context);
				image.setLayoutParams(new LayoutParams(90, 90));
				image.setOnClickListener(imageClickedLister);
				image.setMaxWidth(90);
				image.setMaxHeight(90);
				image.setPadding(5, 0, 0, 0);
				image.setAdjustViewBounds(true);
				image.setTag(Integer.toString(note_id));
				image.setImageResource(R.drawable.stop);
				String uri = imagemeta.getResource_url();
				image.setContentDescription(uri);
				Log.d(this.getClass().toString(), "uri=" + uri);
				ImageDownloadTask imagetask = new ImageDownloadTask(context,
						image, uri);
				imagetask.execute();
				images.addView(image);
				if (++count >= 5)
					break;
			}
		} else {
			imagelayout.setVisibility(View.GONE);
		}

		return layout;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return (helper.countNotes(query) > 0);
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		dataObservers.put(observer, observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		dataObservers.remove(observer);
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		// TODO Auto-generated method stub
		return true;
	}

	public void notifyChange() {
		idlist = helper.getNoteIds(query);
		for (DataSetObserver observer : dataObservers.values()) {
			observer.onChanged();
		}
	}

	public void notifyInvalidate() {
		for (DataSetObserver observer : dataObservers.values()) {
			observer.onInvalidated();
		}
	}

}
