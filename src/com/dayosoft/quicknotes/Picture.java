package com.dayosoft.quicknotes;

import java.util.List;

import com.dayosoft.utils.DialogUtils;
import com.dayosoft.utils.DictionaryOpenHelper;
import com.dayosoft.utils.ImageDownloadTask;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class Picture extends Activity {

	Note note;
	DictionaryOpenHelper helper;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.picture);
		Intent intent = getIntent();
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		int note_id = Integer.parseInt(intent.getStringExtra("note_id"));
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		Button backButton = (Button) findViewById(R.id.backButton);
		LinearLayout layout = (LinearLayout) findViewById(R.id.images);
		backButton.setOnClickListener(DialogUtils.closeNavigator(this));

		helper = new DictionaryOpenHelper(this);
		note = helper.load(note_id);
		int height = dm.heightPixels - backButton.getHeight() - 90;

		List<NoteMeta> imagelist = note.getMeta(NoteMeta.IMAGE);

		int count = 0;

		for (NoteMeta imagemeta : imagelist) {
			ImageView image = new ImageView(this);
			image.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.FILL_PARENT));
			image.setMaxWidth(dm.widthPixels);
			image.setMaxHeight(height);
			image.setPadding(10, 0, 0, 0);
			image.setAdjustViewBounds(true);
			image.setId(note.getId());
			image.setImageResource(R.drawable.stop);
			String uri = imagemeta.getResource_url();
			image.setContentDescription(uri);
			Log.d(this.getClass().toString(), "uri=" + uri);
			ImageDownloadTask imagetask = new ImageDownloadTask(this, image,
					uri, height, dm.widthPixels);
			imagetask.execute((Void) null);
			layout.addView(image);
			if (++count >= 5)
				break;
		}

	}

}
