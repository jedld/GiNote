package com.dayosoft.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

public class ImageDownloadTask extends AsyncTask<Void, Void, Void> {
	private final WeakReference<ImageView> imageref;
	Drawable image;
	String url;
	Context context;
	int pheight = 90, pwidth = 90;

	public ImageDownloadTask(Context context, ImageView imageView, String url,
			int pheight, int pwidth) {
		super();
		this.imageref = new WeakReference<ImageView>(imageView);
		this.url = url;
		this.context = context;
		this.pheight = pheight;
		this.pwidth = pwidth;
	}

	// decodes image and scales it to reduce memory consumption
	private Bitmap decodeFile(File f) {
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);

			// The new size we want to scale to
			final int REQUIRED_SIZE = pheight;

			// Find the correct scale value. It should be the power of 2.
			int width_tmp = o.outWidth, height_tmp = o.outHeight;
			int scale = 1;
			while (true) {
				if ((width_tmp / 2 < pwidth) && (height_tmp / 2 < pheight))
					break;
				width_tmp /= 2;
				height_tmp /= 2;
				scale++;
			}

			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public ImageDownloadTask(Context context, ImageView imageView, String url) {
		super();
		this.imageref = new WeakReference<ImageView>(imageView);
		this.url = url;
		this.context = context;
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		// TODO Auto-generated method stub
		image = imageOperations(context, this.url, "image.jpg");
		return null;
	}

	private Drawable imageOperations(Context ctx, String url,
			String saveFilename) {
		Bitmap image = decodeFile(new File(url));
		Drawable d = new BitmapDrawable(image);
		image = null;
		return d;
	}

	public Object fetch(String address) throws MalformedURLException,
			IOException {
		URL url = new URL(address);
		Object content = url.getContent();
		return content;
	}

	@Override
	protected void onPostExecute(Void v) {
		if (imageref != null && image != null) {
			ImageView imageView = imageref.get();
			if (imageView != null) {

				imageView.setImageDrawable(image);
			}
		}
	}

}