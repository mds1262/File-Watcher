package net.catenoid.watcher.utils;

import org.json.JSONException;

public interface OnHttpResponseListener {
	public int onResponseListener(int nStatus, int contentLength, String responseBody, Object obj) throws JSONException;
}
