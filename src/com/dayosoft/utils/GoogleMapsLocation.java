package com.dayosoft.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.os.Bundle;

public class GoogleMapsLocation implements LocationListener {
	LocationManager manager;
	Location previousLocation;
	LocationFixedListener listener;
	Context context;
	private static final int conn_timeout = 1000;
	private static final int read_timeout = 10000;

	public GoogleMapsLocation(Context context, LocationFixedListener listener) {
		manager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		this.previousLocation = manager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		this.listener = listener;
		this.context = context;
	}

	public void startGetFix() {
		manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
				this);
		manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
	}

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	public static String tinyURLize(String urlString) {
		HttpURLConnection conn = null;
		String tinyurl = urlString;
		try {
			URL tinyUrl = new URL("http://tinyurl.com/api-create.php?url="
					+ urlString);
			conn = (HttpURLConnection) tinyUrl.openConnection();
			conn.setConnectTimeout(conn_timeout);
			conn.setReadTimeout(read_timeout);
			conn.setDoOutput(true);
			conn.connect();
			boolean error = conn.getResponseCode() != HttpURLConnection.HTTP_OK;
			InputStream is = error ? conn.getErrorStream() : conn
					.getInputStream();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is));
			StringBuffer buf = new StringBuffer();
			while (reader.ready()) {
				buf.append(reader.readLine());
			}
			tinyurl = buf.toString();
			is.close();
			conn.disconnect();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tinyurl;
	}

	public static String generateMapsUrl(double latitude, double longitude) {
		return  "http://maps.google.com/maps/api/staticmap?center="
			+ latitude
			+ ","
			+ longitude
			+ "&zoom=17&size=400x400&sensor=true&markers=color:blue|label:A|"
			+ latitude + "," + longitude;	
	}
	@Override
	public void onLocationChanged(Location location) {
		if (isBetterLocation(location, previousLocation)) {

			// remove first to prevent multi posts
			manager.removeUpdates(this);

			double latitude = location.getLatitude();
			double longitude = location.getLongitude();
			String url = generateMapsUrl(latitude, longitude);

			String tinyurlized = latitude + "," + longitude;

			// do a quick tinyurl conversion
			if (ConnectivityManager
					.isNetworkTypeValid(ConnectivityManager.TYPE_MOBILE)
					|| ConnectivityManager
							.isNetworkTypeValid(ConnectivityManager.TYPE_WIFI)) {

				tinyurlized = GoogleMapsLocation.tinyURLize(url);
			}
			if (tinyurlized == null) {
				tinyurlized = url;
			}
			this.listener.onLocationFixed(location, tinyurlized);

		}
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}

	public void removeUpdates() {
		manager.removeUpdates(this);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		if ((status == LocationProvider.OUT_OF_SERVICE)
				|| (status == LocationProvider.TEMPORARILY_UNAVAILABLE)) {
			manager.removeUpdates(this);
			this.listener.onLocationError(status);
		}
	}

}
