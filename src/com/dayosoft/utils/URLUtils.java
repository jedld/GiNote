package com.dayosoft.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

public class URLUtils {
	public static String urlParamsToString(HashMap<String, String> params) {
		ArrayList<String> paramList = new ArrayList<String>();
		for (String key : params.keySet()) {
			try {
				paramList.add(key + "="
						+ URLEncoder.encode(params.get(key), "utf-8"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		String paramArray[] = {};
		paramArray = paramList.toArray(paramArray);
		return StringUtils.join(paramArray, "&");
	}
}
