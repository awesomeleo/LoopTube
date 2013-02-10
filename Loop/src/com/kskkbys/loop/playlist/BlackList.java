package com.kskkbys.loop.playlist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;

import com.kskkbys.loop.R;
import com.kskkbys.loop.logger.KLog;

/**
 * Black list of videos
 *
 */
public class BlackList {

	private static final String TAG = BlackList.class.getSimpleName();
	private static final String FILENAME_USER_BLACKLIST = "user_black_list.txt";
	private static final String FILENAME_APP_BLACKLIST = "app_black_list.txt";

	private static BlackList mInstance = new BlackList();
	// Black list registered by user
	private List<String> mUserVideoIds;
	// Black list registered by app
	private List<String> mAppVideoIds;
	private Context mContext;

	private BlackList() {
		// nothing to do
		// Please call initialize before calling APIs
	}

	public static BlackList getInstance() {
		return mInstance;
	}

	public void initialize(Context context) {
		mContext = context;
		loadUserBlackList();
		loadAppBlackList();
	}

	public void addUserBlackList(String videoId) {
		KLog.v(TAG, "add: " + videoId);
		mUserVideoIds.add(videoId);
		saveUserBlackList();
	}

	public void addAppBlackList(String videoId) {
		KLog.v(TAG, "addByApp: " + videoId);
		mAppVideoIds.add(videoId);
		saveAppBlackList();
	}

	public void clear() {
		KLog.v(TAG, "clear");
		mUserVideoIds.clear();
		mAppVideoIds.clear();
		saveAppBlackList();
		saveUserBlackList();
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

	public boolean isBlackTitle(String videoTitle) {
		// KLog.v(TAG, "isBlackTitle");
		if (mContext != null) {
			String[] blackWords = mContext.getResources().getStringArray(R.array.loop_black_word_list);
			for (String word : blackWords) {
				if (videoTitle.contains(word)) {
					return true;
				}
			}
		}
		return false;
	}

	private void saveUserBlackList() {
		KLog.v(TAG, "save user");
		if (mContext != null) {
			FileOutputStream fos;
			try {
				fos = mContext.openFileOutput(FILENAME_USER_BLACKLIST, Context.MODE_PRIVATE);
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
				for (String videoId : mUserVideoIds) {
					bw.write(videoId);
					bw.newLine();
				}
				bw.flush();
				bw.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void saveAppBlackList() {
		KLog.v(TAG, "save app");
		if (mContext != null) {
			FileOutputStream fos;
			try {
				fos = mContext.openFileOutput(FILENAME_APP_BLACKLIST, Context.MODE_PRIVATE);
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
				for (String videoId : mAppVideoIds) {
					bw.write(videoId);
					bw.newLine();
				}
				bw.flush();
				bw.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadUserBlackList() {
		KLog.v(TAG, "load");
		if (mContext != null) {
			mUserVideoIds = new ArrayList<String>();
			FileInputStream fis;
			try {
				fis = mContext.openFileInput(FILENAME_USER_BLACKLIST);
				BufferedReader br = new BufferedReader(new InputStreamReader(fis));
				String line;
				while ((line = br.readLine()) != null) {
					mUserVideoIds.add(line);
				}
				br.close();
			} catch (FileNotFoundException e) {
				// Fisrt launching
				KLog.w(TAG,"FileNotFound of black list. May be first launch.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadAppBlackList() {
		KLog.v(TAG, "load");
		if (mContext != null) {
			mAppVideoIds = new ArrayList<String>();
			FileInputStream fis;
			try {
				fis = mContext.openFileInput(FILENAME_APP_BLACKLIST);
				BufferedReader br = new BufferedReader(new InputStreamReader(fis));
				String line;
				while ((line = br.readLine()) != null) {
					mAppVideoIds.add(line);
				}
				br.close();
			} catch (FileNotFoundException e) {
				// Fisrt launching
				KLog.w(TAG,"FileNotFound of black list. May be first launch.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
