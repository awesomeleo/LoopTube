package com.kskkbys.loop.model;

import java.util.List;

import com.kskkbys.loop.storage.SQLiteStorage;

import android.content.Context;

/**
 * This class manages a list of favorite videos.
 * @author Keisuke Kobayashi
 *
 */
public class FavoriteList {

	private Context mContext;
	
	private static FavoriteList sInstance = null;
	
	private FavoriteList(Context context) {
		mContext = context;
	}
	
	public static synchronized FavoriteList getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new FavoriteList(context);
		}
		return sInstance;
	}
	
	/**
	 * Get restored favorite videos.
	 * @return
	 */
	public List<Video> getVideos() {
		return SQLiteStorage.getInstance(mContext).getRestoredFavorites();
	}
	
	/**
	 * Restore favorite videos in SQLite.
	 * @return
	 */
	public boolean restore() {
		return SQLiteStorage.getInstance(mContext).restoreFavorites();
	}
	
	/**
	 * Add video of the artist.
	 * @param video
	 * @param artist
	 * @return
	 */
	public boolean addFavorite(Video video, String artist) {
		SQLiteStorage storage = SQLiteStorage.getInstance(mContext);
		return storage.insertFavorite(video, artist);
	}
	
	
}
