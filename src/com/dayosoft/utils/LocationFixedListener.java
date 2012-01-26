package com.dayosoft.utils;

import android.location.Location;

public interface LocationFixedListener {

	public void onLocationFixed(Location location, String url);
	public void onLocationError(int status);
}
