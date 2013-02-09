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

import com.kskkbys.loop.logger.KLog;

/**
 * Black list of videos
 *
 */
public class BlackList {

	private static final String TAG = BlackList.class.getSimpleName();
	private static final String FILENAME_BLACKLIST = "black_list.txt";

	private static BlackList mInstance = new BlackList();
	private List<String> mVideoIds;
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
		mVideoIds.add(videoId);
		save();
	}

	public void remove(String videoId) {
		KLog.v(TAG, "remove: " + videoId);
		mVideoIds.remove(videoId);
		save();
	}
	
	public void clear() {
		KLog.v(TAG, "clear");
		mVideoIds.clear();
		save();
	}

	public boolean contains(String videoId) {
		KLog.v(TAG, "contains: " + videoId);
		return mVideoIds.contains(videoId);
	}

	private void save() {
		KLog.v(TAG, "save");
		if (mContext != null) {
			FileOutputStream fos;
			try {
				fos = mContext.openFileOutput(FILENAME_BLACKLIST, Context.MODE_PRIVATE);
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
				for (String videoId : mVideoIds) {
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
			mVideoIds = new ArrayList<String>();
			FileInputStream fis;
			try {
				fis = mContext.openFileInput(FILENAME_BLACKLIST);
				BufferedReader br = new BufferedReader(new InputStreamReader(fis));
				String line;
				while ((line = br.readLine()) != null) {
					mVideoIds.add(line);
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
