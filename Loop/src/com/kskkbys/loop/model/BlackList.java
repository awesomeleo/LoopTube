package com.kskkbys.loop.model;

import java.util.List;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.kskkbys.loop.R;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.storage.SQLiteStorage;

/**
 * Black list of videos
 *
 */
public class BlackList {

	private static final String TAG = BlackList.class.getSimpleName();

	private static BlackList sInstance = null;

	private Context mContext;
	private List<String> mUserVideoIds;
	private List<String> mAppVideoIds;

	/**
	 * Constructor.
	 * @param context
	 */
	private BlackList(Context context) {
		mContext = context;
		// Restore black list if not restored
		SQLiteStorage storage = SQLiteStorage.getInstance(mContext);
		storage.restoreBlackList();
		// Get video IDs
		mUserVideoIds = storage.getRestoredUserBlackList();
		mAppVideoIds = storage.getRestoredAppBlackList();
	}

	public synchronized static BlackList getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new BlackList(context);
		}
		return sInstance;
	}

	public void addUserBlackList(String videoId) {
		KLog.v(TAG, "add: " + videoId);
		// On memory
		mUserVideoIds.add(videoId);
		// DB
		SQLiteStorage storage = SQLiteStorage.getInstance(mContext);
		storage.insertBlackList(videoId, true);
	}

	public void addAppBlackList(String videoId) {
		// Check shared preference
		if (isAutoBlackListEnabled()) {
			KLog.v(TAG, "addByApp: " + videoId);
			// On memory
			mAppVideoIds.add(videoId);
			// DB
			SQLiteStorage storage = SQLiteStorage.getInstance(mContext);
			storage.insertBlackList(videoId, false);
		} else {
			KLog.w(TAG, "Auto black list is disabled.");
		}
	}

	public void clear() {
		KLog.v(TAG, "clear");
		// On memory
		mUserVideoIds.clear();
		mAppVideoIds.clear();
		// DB
		SQLiteStorage storage = SQLiteStorage.getInstance(mContext);
		storage.clearBlackList();
	}

	/**
	 * Check the videoId is registered with black list or not.
	 * @param videoId
	 * @return
	 */
	public boolean containsByUser(String videoId) {
		// KLog.v(TAG, "contains: " + videoId);
		// Check users black list
		if (mUserVideoIds.contains(videoId)) {
			return true;
		}
		return false;
	}

	public boolean containsByApp(String videoId) {
		// Check app's black list
		if (mAppVideoIds.contains(videoId)) {
			return true;
		}
		return false;
	}

	/**
	 * Check whether the title contains NG words.
	 * @param videoTitle
	 * @return
	 */
	public boolean isBlackTitle(String videoTitle) {
		// KLog.v(TAG, "isBlackTitle");
		String[] blackWords = mContext.getResources().getStringArray(R.array.loop_black_word_list);
		for (String word : blackWords) {
			if (videoTitle.contains(word)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isAutoBlackListEnabled() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
		String key = mContext.getString(R.string.loop_pref_auto_black_list_key);
		return sharedPref.getBoolean(key, true);
	}
}
