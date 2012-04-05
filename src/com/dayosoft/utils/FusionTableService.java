package com.dayosoft.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.api.client.http.HttpRequest;

import android.os.AsyncTask;
import android.util.Log;
import au.com.bytecode.opencsv.CSVReader;

public class FusionTableService extends
		AsyncTask<Void, Void, ArrayList<HashMap<String, String>>> {
	
	public static final String STRING = "string";
	public static final String LOCATION = "location";
	public static final String DATETIME = "datetime";
	public static final String NUMBER = "number";
	
	String auth_token;
	String service = "query";
	HashMap<String, String> other_params = new HashMap<String, String>();
	FTQueryCompleteListener listener;

	private static final int conn_timeout = 1000;
	private static final int read_timeout = 10000;

	public FusionTableService(String auth_token) {
		super();
		this.auth_token = auth_token;
	}

	public ArrayList<HashMap<String, String>> create_sync(String queryStr,
			boolean enc) {
		this.other_params.put("sql", queryStr);
		if (enc) {
			this.other_params.put("encid", "true");
		}
		this.service = "create";
		return doInBackground();
	}

	public void create(String queryStr, boolean enc,
			FTQueryCompleteListener listener) {
		this.other_params.put("sql", queryStr);
		if (enc) {
			this.other_params.put("encid", "true");
		}
		this.listener = listener;
		this.service = "create";
		this.execute();
	}

	public ArrayList<HashMap<String, String>> query_sync(String queryStr,
			boolean enc) {
		this.other_params.put("sql", queryStr);
		if (enc) {
			this.other_params.put("encid", "true");
		}
		this.service = "query";
		return doInBackground();
	}

	public void query(String queryStr, boolean enc,
			FTQueryCompleteListener listener) {
		this.other_params.put("sql", queryStr);
		if (enc) {
			this.other_params.put("encid", "true");
		}
		this.listener = listener;
		this.service = "query";
		this.execute();
	}

	@Override
	protected ArrayList<HashMap<String, String>> doInBackground(Void... arg0) {
		HttpURLConnection conn = null;
		URL google_ft_url = null;
		try {
			String params = URLUtils.urlParamsToString(other_params);
			String url_str = "https://www.google.com/fusiontables/api/query"
					+ "?" + params;
			Log.d(this.getClass().toString(), ">>" + url_str);
			google_ft_url = new URL(url_str);
			conn = (HttpURLConnection) google_ft_url.openConnection();
			HttpClient httpclient = new DefaultHttpClient();

			HttpRequestBase request = null;
			if (service.equalsIgnoreCase("create")) {
				request = new HttpPost(url_str);
			} else {
				request = new HttpGet(url_str);
			}

			request.setHeader("Authorization", "GoogleLogin auth=" + auth_token);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String responseBody = httpclient.execute(request, responseHandler);
			ArrayList<HashMap<String, String>> resultList = new ArrayList<HashMap<String, String>>();
			CSVReader reader = new CSVReader(new StringReader(responseBody));
			String[] columns = null;
			String[] nextLine;
			boolean first_line = true;
			while ((nextLine = reader.readNext()) != null) {
				if (first_line) {
					columns = nextLine;
					first_line = false;
				} else {
					HashMap<String, String> row = new HashMap<String, String>();

					for (int i = 0; i < nextLine.length; i++) {
						row.put(columns[i].toUpperCase(), nextLine[i]);
					}
					resultList.add(row);
				}
			}

			Log.d(this.getClass().toString(), "Response from ft: "
					+ responseBody);
			return resultList;
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
	protected void onPostExecute(ArrayList<HashMap<String, String>> result) {
		// TODO Auto-generated method stub
		super.onPostExecute(result);
		if (listener != null) {
			Log.d(this.getClass().toString(), "Calling on query");
			listener.onQueryComplete(result);
		}
	}

}
