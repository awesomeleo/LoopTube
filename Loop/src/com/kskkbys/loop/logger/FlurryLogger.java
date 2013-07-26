package com.kskkbys.loop.logger;

import java.util.Map;

import android.content.Context;

import com.flurry.android.FlurryAgent;
import com.kskkbys.loop.BuildConfig;

/**
 * Flurry logger
 *
 */
public class FlurryLogger {
	
	/**
	 * 
	 */
	private static final String TAG = FlurryLogger.class.getSimpleName();
	
	/**
	 * Flurry API Key
	 */
	private static final String apiKey = "GNRQ5VMSSVJ2WR2W38SD";
	
	public static final String SEE_SPLASH = "See splash screen";
	public static final String SEE_SEARCH = "See search screen";
	public static final String SEE_PLAYER = "See video player screen";
	public static final String SEARCH_ARTIST = "Search artist";
	public static final String PLAY_VIDEO = "Play video";
	/**
	 * @deprecated
	 */
	public static final String SEE_FACEBOOK_DIALOG = "See Facebook dialog";
	public static final String SHARE_VIDEO = "Share video";
	public static final String PUBLISH_FACEBOOK = "Publish facebook";
	
	/**
	 * 
	 * @param context
	 * @param apiKey
	 */
	public static void onStartSession(Context context) {
		KLog.v(TAG, "onStartSession");
		if (!BuildConfig.DEBUG) {
			FlurryAgent.onStartSession(context, apiKey);
		}
	}
	
	/**
	 * 
	 * @param context
	 */
	public static void onEndSession(Context context) {
		KLog.v(TAG, "onEndSession");
		if (!BuildConfig.DEBUG) {
			FlurryAgent.onEndSession(context);
		}
	}
	
	/**
	 * 
	 */
	public static void onPageView() {
		KLog.v(TAG, "onPageView");
		if (!BuildConfig.DEBUG) {
			FlurryAgent.onPageView();
		}
	}
	
	/**
	 * 
	 * @param action
	 * @param param
	 */
	public static void logEvent(String action) {
		KLog.v(TAG, "logEvent 1: " + action);
		if (!BuildConfig.DEBUG) {
			FlurryAgent.logEvent(action);
		}
	}
	
	/**
	 * 
	 * @param action
	 * @param param
	 */
	public static void logEvent(String action, Map<String, String> param) {
		KLog.v(TAG, "logEvent 2: " + action);
		if (!BuildConfig.DEBUG) {
			FlurryAgent.logEvent(action, param);
		}
	}
	
	/**
	 * 
	 * @param action
	 * @param param
	 * @param isTimeEvent
	 */
	public static void logEvent(String action, Map<String, String> param, boolean isTimeEvent) {
		KLog.v(TAG, "logEvent 3: " + action);
		if (!BuildConfig.DEBUG) {
			FlurryAgent.logEvent(action, param, isTimeEvent);
		}
	}
	
	/**
	 * 
	 * @param action
	 */
	public static void endTimedEvent(String action) {
		KLog.v(TAG, "endTimedEvent: " + action);
		if (!BuildConfig.DEBUG) {
			FlurryAgent.endTimedEvent(action);
		}
	}
}
