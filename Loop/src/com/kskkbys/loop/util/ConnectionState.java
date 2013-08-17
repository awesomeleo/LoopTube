package com.kskkbys.loop.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectionState {

	/**
	 * Check whether the device is connected to wifi
	 * @param context
	 * @return
	 */
	public static boolean isConnected(Context context) {
		ConnectivityManager manager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		return (info != null && info.isConnected());
	}
	
	/**
	 * Check the network is roaming or not
	 * @param context
	 * @return
	 */
	public static boolean isRoaming(Context context) {
		ConnectivityManager manager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		return (info != null && info.isRoaming());
	}
	
}
