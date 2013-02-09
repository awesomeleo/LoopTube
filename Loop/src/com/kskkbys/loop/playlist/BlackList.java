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
	private static final String FILENAME_BLACKLIST = "black_list.txt";

	private static BlackList mInstance = new BlackList();
	// Black list regstered by user
	private List<String> mUserVideoIds;
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
		load();
	}

	public void add(String videoId) {
		KLog.v(TAG, "add: " + videoId);
		mUserVideoIds.add(videoId);
		save();
	}

	public void remove(String videoId) {
		KLog.v(TAG, "remove: " + videoId);
		mUserVideoIds.remove(videoId);
		save();
	}

	public void clear() {
		KLog.v(TAG, "clear");
		mUserVideoIds.clear();
		save();
	}

	/**
	 * Check the videoId is registered with black list or not.
	 * @param videoId
	 * @return
	 */
	public boolean contains(String videoId) {
		// KLog.v(TAG, "contains: " + videoId);
		// Check users black list
		if (mUserVideoIds.contains(videoId)) {
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

	private void save() {
		KLog.v(TAG, "save");
		if (mContext != null) {
			FileOutputStream fos;
			try {
				fos = mContext.openFileOutput(FILENAME_BLACKLIST, Context.MODE_PRIVATE);
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

	private void load() {
		KLog.v(TAG, "load");
		if (mContext != null) {
			mUserVideoIds = new ArrayList<String>();
			FileInputStream fis;
			try {
				fis = mContext.openFileInput(FILENAME_BLACKLIST);
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
}
