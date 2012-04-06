package com.dayosoft.utils;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

class ActivityNavigator implements OnClickListener {

	Class activityClass;
	Activity context;
	Integer requestCode;

	public ActivityNavigator(Activity context, Class activityClass,
			int requestCode) {
		this.activityClass = activityClass;
		this.context = context;
		this.requestCode = requestCode;
	}

	public ActivityNavigator(Activity context, Class activityClass) {
		this.activityClass = activityClass;
		this.context = context;
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent(context, activityClass);
		Log.d("QuickNote:", "Putting ID in extra =" + v.getId());
		intent.putExtra("view_id", v.getId());
		if (requestCode == null) {
			context.startActivity(intent);
		} else {
			context.startActivityForResult(intent, requestCode);
		}
	}
}

public class DialogUtils {

	public static void showConfirmation(String message, Context context,
			android.content.DialogInterface.OnClickListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message)
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
							}
						}).setPositiveButton("Ok", listener);
		builder.show();
	}

	public static void showMessageAlert(String message, Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message).setNegativeButton("OK",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
		builder.show();
	}

	public static OnClickListener closeNavigator(final Activity context) {
		OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(View v) {

				System.gc();
				context.finish();

			}
		};
		return listener;
	}

	public static OnClickListener setNavigator(Class page, Activity context,
			int requestCode) {
		ActivityNavigator event = new ActivityNavigator(context, page,
				requestCode);
		return event;
	}

	public static OnClickListener setNavigator(Class page, Activity context) {
		ActivityNavigator event = new ActivityNavigator(context, page);
		return event;
	}

	public static boolean isStorageWritable() {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		return mExternalStorageWriteable;
	}

	public static void switchActivity(Class page, Context context) {
		context.startActivity(new Intent(context, page));
	}

	public static void linkToContextMenu(final Activity activity,
			final Button button) {
		activity.registerForContextMenu(button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				button.showContextMenu();
			}
		});
	}

	public static void linkBoxToPrefs(CheckBox checkbox,
			final SharedPreferences prefs, final String keyname) {
		boolean value = prefs.getBoolean(keyname, false);
		checkbox.setChecked(value);
		checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				Editor edit = prefs.edit();
				edit.putBoolean(keyname, arg1);
				edit.commit();
			}

		});
	}

	public static boolean hasINet(Context context) {
		boolean haveConnectedWifi = false;
		boolean haveConnectedMobile = false;

		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo) {
			if (ni.getTypeName().equalsIgnoreCase("WIFI"))
				if (ni.isConnected())
					haveConnectedWifi = true;
			if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
				if (ni.isConnected())
					haveConnectedMobile = true;
		}
		return haveConnectedWifi || haveConnectedMobile;
	}

	public static void isNetworkAvailable(final Context context,
			final OnInternetReadyListener handler, final int timeout) {
		// ask fo message '0' (not connected) or '1' (connected) on 'handler'
		// the answer must be send before before within the 'timeout' (in
		// milliseconds)
		Thread thread = new Thread() {
			public boolean responded = false;

			@Override
			public void run() {
				// set 'responded' to TRUE if is able to connect with google
				// mobile (responds fast)
				Looper.prepare();
				if (hasINet(context)) {
					new Thread() {
						@Override
						public void run() {
							HttpGet requestForTest = new HttpGet(
									"http://m.google.com");
							try {
								new DefaultHttpClient().execute(requestForTest); // can
								responded = true;
							} catch (Exception e) {
							}
						}
					}.start();
					try {
						int waited = 0;
						while (!responded && (waited < timeout)) {
							sleep(100);
							if (!responded) {
								waited += 100;
							}
						}
					} catch (InterruptedException e) {
					} // do nothing

					finally {
						if (!responded) {
							handler.onInternetReady(false);
						} else {
							handler.onInternetReady(true);
						}
					}
				} else {
					handler.onInternetReady(false);
				}
			}
		};
		thread.start();
	}

	public static File convertImageUriToFile(Uri imageUri, Activity activity) {
		Cursor cursor = null;
		try {
			String[] proj = { MediaColumns.DATA, BaseColumns._ID,
					MediaStore.Images.ImageColumns.ORIENTATION };
			cursor = activity.managedQuery(imageUri, proj, // Which columns to
															// return
					null, // WHERE clause; which rows to return (all rows)
					null, // WHERE clause selection arguments (none)
					null); // Order-by clause (ascending by name)
			int file_ColumnIndex = cursor
					.getColumnIndexOrThrow(MediaColumns.DATA);
			int orientation_ColumnIndex = cursor
					.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION);
			if (cursor.moveToFirst()) {
				String orientation = cursor.getString(orientation_ColumnIndex);
				return new File(cursor.getString(file_ColumnIndex));
			}
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static Document getDomElement(String xml) {
		Document doc = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {

			DocumentBuilder db = dbf.newDocumentBuilder();

			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xml));
			doc = db.parse(is);

		} catch (ParserConfigurationException e) {
			Log.e("Error: ", e.getMessage());
			return null;
		} catch (SAXException e) {
			Log.e("Error: ", e.getMessage());
			return null;
		} catch (IOException e) {
			Log.e("Error: ", e.getMessage());
			return null;
		}
		// return DOM
		return doc;
	}

}
