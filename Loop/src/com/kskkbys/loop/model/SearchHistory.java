package com.kskkbys.loop.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.provider.SearchRecentSuggestions;
import android.text.TextUtils;

import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.search.ArtistSuggestionsProvider;
import com.kskkbys.loop.storage.SQLiteStorage;
import com.kskkbys.loop.ui.fragments.MainHistoryFragment;

/**
 * This class manages a lits of artist objects.
 * @author Keisuke Kobayashi
 *
 */
public class SearchHistory {
	
	private static final String TAG = SearchHistory.class.getSimpleName();

	private Context mContext;
	private List<Artist> mArtists;

	private static SearchHistory sInstance = null;

	private SearchHistory(Context context) {
		mContext = context;
		mArtists = new ArrayList<Artist>();
	}

	public static synchronized SearchHistory getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new SearchHistory(context);
			sInstance.readHistory();
		}
		return sInstance;
	}
	
	public List<Artist> getArtists() {
		return mArtists;
	}
	
	/**
	 * Read search history which is already restored before.
	 */
	private void readHistory() {
		KLog.v(TAG, "readHistory");
		SQLiteStorage storage = SQLiteStorage.getInstance(mContext);
		List<Artist> entries = storage.getRestoredArtists();
		mArtists.clear();
		mArtists.addAll(entries);
	}

	/**
	 * Add artist
	 * @param artistName
	 */
	public void addArtist(String artistName) {
		KLog.v(TAG, "addArtist");
		Artist artist = findArtistFromHistory(artistName);
		if (artist != null) {
			mArtists.remove(artist);
			artist.date = new Date();
			mArtists.add(0, artist);
		} else {
			artist = new Artist();
			artist.name = artistName;
			artist.imageUrls = new ArrayList<String>();	// Before search video list, image URL is null.
			artist.date = new Date();
			mArtists.add(0, artist);
		}
	}

	/**
	 * Remove the specified artist.
	 * @param artist
	 */
	public void removeArtist(Artist artist) {
		mArtists.remove(artist);
		new AsyncTask<Artist, Integer, Boolean>() {
			@Override
			protected Boolean doInBackground(Artist... params) {
				SQLiteStorage storage = SQLiteStorage.getInstance(mContext);
				storage.deleteArtist(params[0]);
				return true;
			}
			@Override
			protected void onPostExecute(Boolean result) {
				// Do nothing
			}
		}.execute(artist);
	}
	
	/**
	 * Clear history.
	 */
	public void clearAllHistory() {
		// On memory
		mArtists.clear();
		// App's search history
		new Thread(new Runnable() {
			@Override
			public void run() {
				SQLiteStorage storage = SQLiteStorage.getInstance(mContext);
				storage.clearArtists();
			}
		}).start();
		// OS's search history
		SearchRecentSuggestions suggestions = new SearchRecentSuggestions(mContext,
				ArtistSuggestionsProvider.AUTHORITY, ArtistSuggestionsProvider.MODE);
		suggestions.clearHistory();
	}

	/**
	 * This method is called when YouTube API is completed.
	 * @param query
	 * @param videos
	 */
	public void updateHistory(String query, List<Video> videos) {
		// Update array list
		boolean alreadyAdded = false;
		Artist updatedEntry = null;
		for (int i=0; i<mArtists.size(); i++) {
			Artist entry = mArtists.get(i);
			if (entry.name.equals(query)) {
				entry.imageUrls = new ArrayList<String>();
				int count = 0;
				for (Video v: videos) {
					if (!TextUtils.isEmpty(v.getThumbnailUrl())) {
						entry.imageUrls.add(v.getThumbnailUrl());
						count++;
						if (count >= MainHistoryFragment.IMAGE_COUNT_PER_ROW) {
							break;
						}
					}
				}
				mArtists.set(i, entry);
				updatedEntry = entry;
				alreadyAdded = true;
				break;
			}
		}
		if (!alreadyAdded) {
			Artist newEntry = new Artist();
			newEntry.name = query;
			newEntry.imageUrls = new ArrayList<String>();
			int count = 0;
			for (Video v: videos) {
				if (!TextUtils.isEmpty(v.getThumbnailUrl())) {
					newEntry.imageUrls.add(v.getThumbnailUrl());
					count++;
					if (count >= MainHistoryFragment.IMAGE_COUNT_PER_ROW) {
						break;
					}
				}
			}
			mArtists.add(0, newEntry);
			updatedEntry = newEntry;
		}
		// Store to SQLite (async)
		saveArtistAsync(updatedEntry);
	}

	private void saveArtistAsync(final Artist updatedEntry) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (updatedEntry != null) {
					SQLiteStorage storage = SQLiteStorage.getInstance(mContext);
					storage.insertOrUpdateArtist(updatedEntry);
				}
			}
		}).start();
	}
	
	private Artist findArtistFromHistory(String artist) {
		KLog.v(TAG, "findArtistFromHistory");
		for (int i=0; i<mArtists.size(); i++) {
			Artist e = mArtists.get(i);
			if (e.name.equals(artist)) {
				return e;
			}
		}
		return null;
	}
}
