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
	
	private List<Video> mVideos;
	
	/**
	 * Constructor.
	 * @param context
	 */
	private FavoriteList(Context context) {
		mContext = context;
		// Restore if needed
		SQLiteStorage storage = SQLiteStorage.getInstance(mContext);
		storage.restoreFavorites();
		mVideos = storage.getRestoredFavorites();
	}
	
	/**
	 * Get singleton instance.
	 * @param context
	 * @return
	 */
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
		return mVideos;
	}
	
	/**
	 * Add video of the artist.
	 * @param video
	 * @param artist
	 * @return
	 */
	public boolean addFavorite(Video video, String artist) {
		// Memory
		if (!mVideos.contains(video)) {
			mVideos.add(video);
			// DB
			SQLiteStorage storage = SQLiteStorage.getInstance(mContext);
			return storage.insertFavorite(video, artist);
		}
		return false;
	}
	
	/**
	 * Delete the video from favorite list.
	 * @param video
	 * @return
	 */
	public boolean deleteFavorite(Video video) {
		// Memory
		if (mVideos.contains(video)) {
			mVideos.remove(video);
			// DB
			SQLiteStorage storage = SQLiteStorage.getInstance(mContext);
			return storage.deleteFavorite(video);
		}
		return false;
	}
	
	/**
	 * Clear favorites
	 * @return
	 */
	public boolean clearFavorites() {
		mVideos.clear();
		SQLiteStorage storage = SQLiteStorage.getInstance(mContext);
		return storage.clearFavorites();
	}
}
