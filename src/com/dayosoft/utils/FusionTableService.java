package com.dayosoft.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import android.os.AsyncTask;
import android.util.Log;

public class FusionTableService extends AsyncTask<Void, Void, Void> {

	String auth_token;
	String service = "query";
	HashMap <String, String>other_params = new HashMap <String, String>();
	FTQueryCompleteListener listener;
	
	private static final int conn_timeout = 1000;
	private static final int read_timeout = 10000;
	
	public FusionTableService(String auth_token) {
		super();
		this.auth_token = auth_token;
	}
	
	public void query(String queryStr, boolean enc ,FTQueryCompleteListener listener) {
			this.other_params.put("sql", queryStr);
			if (enc) {this.other_params.put("encid", "true");};
			this.execute();
	}
	
	@Override
	protected Void doInBackground(Void... arg0) {
		HttpURLConnection conn = null;
		URL google_ft_url = null;
		try {
			String params = URLUtils.urlParamsToString(other_params);
			String url_str = "https://www.google.com/fusiontables/api/" + service + "?" + params;
			Log.d(this.getClass().toString(), ">>" + url_str);
			google_ft_url = new URL(url_str);
			conn = (HttpURLConnection) google_ft_url.openConnection();
			
			conn.setConnectTimeout(conn_timeout);
			conn.setRequestProperty("Authorization", "Bearer " + auth_token);
			conn.setReadTimeout(read_timeout);
			conn.setDoOutput(true);
			conn.connect();
			Log.d(this.getClass().toString(),"Response Code = " + conn.getResponseCode());
			boolean error = conn.getResponseCode() != HttpURLConnection.HTTP_OK;
			InputStream is = error ? conn.getErrorStream() : conn
					.getInputStream();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is));
			StringBuffer buf = new StringBuffer();
			while (reader.ready()) {
				buf.append(reader.readLine());
			}
			Log.d(this.getClass().toString(), "Response from ft: " + buf.toString());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		// TODO Auto-generated method stub
		super.onPostExecute(result);
		if (listener!=null) {
//			listener.onQueryComplete(result)
		}
	}

}
