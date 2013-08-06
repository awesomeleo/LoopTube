package com.kskkbys.loop.storage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.model.Artist;
import com.kskkbys.loop.model.Video;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * This class store/restore search history in SQLite.
 * @author Keisuke Kobayashi
 *
 */
public class SQLiteStorage {

	private static final String TAG = SQLiteStorage.class.getSimpleName();

	private static final String TABLE_ARTIST_NAME = "artists";
	private static final String COL_ID= "_id";
	private static final String COL_ART_NAME = "name";
	private static final String COL_ART_DATE = "date";

	private static final String TABLE_IMAGE_NAME = "images";
	private static final String COL_IMG_ARTIST_NAME= "artist_name";
	private static final String COL_IMG_IMAGE_URL = "image_url";
	
	private static final String TABLE_FAVORITE_NAME = "favorites";
	private static final String COL_FAV_VIDEO_ID = "video_id";
	private static final String COL_FAV_TITLE = "video_title";
	private static final String COL_FAV_IMAGE_URL = "image_url";
	private static final String COL_FAV_ARTIST = "artist";
	private static final String COL_FAV_DURATION = "duration";
	
	private static final String TABLE_BLACK_LIST_NAME = "black_lists";
	private static final String COL_BL_VIDEO_ID = "video_id";
	private static final String COL_BL_ADDED_BY = "added_by";

	private DatabaseOpenHelper mHelper;

	private List<Artist> mArtistList;
	private List<Video> mFavoriteList;
	private List<String> mAppBlackList;
	private List<String> mUserBlackList;
	
	private static SQLiteStorage sInstance = null;
	
	/**
	 * Constructor
	 * @param context
	 */
	private SQLiteStorage(Context context) {
		mHelper = new DatabaseOpenHelper(context);
		mArtistList = null;
		mFavoriteList = null;
		mAppBlackList = null;
		mUserBlackList = null;
	}
	
	public static synchronized SQLiteStorage getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new SQLiteStorage(context);
		}
		return sInstance;
	}

	/**
	 * Add entry (artist and its thumbnails).
	 * @param entry
	 */
	public void insertOrUpdateArtist(final Artist entry) {
		KLog.v(TAG, "insertOrUpdateArtistInner");
		SQLiteDatabase db = mHelper.getWritableDatabase();

		db.beginTransaction();
		try {
			// Insert artist
			ContentValues values = new ContentValues();
			values.put(COL_ART_NAME, entry.name);
			values.put(COL_ART_DATE, entry.date.getTime());
			db.insertWithOnConflict(TABLE_ARTIST_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
			// Insert images
			for (int i=0; i<entry.imageUrls.size(); i++) {
				ContentValues imageValues = new ContentValues();
				imageValues.put(COL_IMG_ARTIST_NAME, entry.name);
				imageValues.put(COL_IMG_IMAGE_URL, entry.imageUrls.get(i));
				db.insert(TABLE_IMAGE_NAME, null, imageValues);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * Delete artist and its thumbnails
	 * @param entry
	 */
	public void deleteArtist(Artist entry) {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.delete(TABLE_ARTIST_NAME, "name=?", new String[]{entry.name});
		db.delete(TABLE_IMAGE_NAME, "artist_name=?", new String[]{entry.name});
	}

	/**
	 * Clear all entries.
	 */
	public void clearArtists() {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.delete(TABLE_ARTIST_NAME, null, null);
		db.delete(TABLE_IMAGE_NAME, null, null);
	}

	public synchronized void restoreArtists() {
		KLog.v(TAG, "restoreArtists");
		SQLiteDatabase db = mHelper.getReadableDatabase();
		db.beginTransaction();
		
		String[] cols = {
				COL_ART_NAME,
				COL_ART_DATE
		};
		// Get artist list
		Cursor cursor = db.query(TABLE_ARTIST_NAME, cols, null, null, null, null, "date DESC");
		int nameIndex = cursor.getColumnIndex(COL_ART_NAME);
		int dateIndex = cursor.getColumnIndex(COL_ART_DATE);
		List<Artist> list = new ArrayList<Artist>();
		if (cursor.moveToFirst()) {
			while (!cursor.isAfterLast()) {
				Artist e = new Artist();
				e.name = cursor.getString(nameIndex);
				e.date = new Date(cursor.getLong(dateIndex));
				e.imageUrls = new ArrayList<String>();
				list.add(e);
				cursor.moveToNext();
			}
		}
		cursor.close();

		// Get thumbnails for each artist
		for (Artist e: list) {
			String[] imgCols = {
					COL_IMG_IMAGE_URL
			};
			Cursor imgCursor = db.query(TABLE_IMAGE_NAME, imgCols, "artist_name=?", new String[]{e.name}, null, null, "_id ASC");
			int urlIndex = imgCursor.getColumnIndex(COL_IMG_IMAGE_URL);
			if (imgCursor.moveToFirst()) {
				while (!imgCursor.isAfterLast()) {
					e.imageUrls.add(imgCursor.getString(urlIndex));
					imgCursor.moveToNext();
				}
			}
			imgCursor.close();
		}

		mArtistList = list;
		KLog.v(TAG, "Count of restored artists: " + mArtistList.size());
		
		db.setTransactionSuccessful();
		db.endTransaction();
	}
	
	public List<Artist> getRestoredArtists() {
		if (mArtistList == null) {
			throw new IllegalStateException("Entry list is null. Call restore() at first.");
		}
		return mArtistList;
	}
	
	public boolean insertFavorite(Video video, String artist) {
		KLog.v(TAG, "insertFavorite");
		SQLiteDatabase db = mHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(COL_FAV_VIDEO_ID, video.getId());
		values.put(COL_FAV_IMAGE_URL, video.getThumbnailUrl());
		values.put(COL_FAV_TITLE, video.getTitle());
		values.put(COL_FAV_DURATION, video.getDuration());
		values.put(COL_FAV_ARTIST, artist);
		if (db.insert(TABLE_FAVORITE_NAME, null, values) == -1) {
			return false;
		} else {
			return true;
		}
	}
	
	public boolean deleteFavorite(Video video) {
		KLog.v(TAG, "deleteFavorite");
		// On memory
		mFavoriteList.remove(video);
		// DB
		SQLiteDatabase db = mHelper.getWritableDatabase();
		String where = COL_FAV_VIDEO_ID + "=?";
		if (db.delete(TABLE_FAVORITE_NAME, where, new String[]{video.getId()}) > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public synchronized boolean restoreFavorites() {
		KLog.v(TAG, "restoreFavorites");
		SQLiteDatabase db = mHelper.getReadableDatabase();
		String[] columns = new String[]{
			COL_FAV_ARTIST,
			COL_FAV_IMAGE_URL,
			COL_FAV_TITLE,
			COL_FAV_VIDEO_ID,
			COL_FAV_DURATION
		};
		Cursor cursor = db.query(TABLE_FAVORITE_NAME, columns, null, null, null, null, null);
		mFavoriteList = new ArrayList<Video>();
		if (cursor.moveToFirst()) {
			while (!cursor.isAfterLast()) {
				String id = cursor.getString(cursor.getColumnIndex(COL_FAV_VIDEO_ID));
				String title = cursor.getString(cursor.getColumnIndex(COL_FAV_TITLE));
				int duration = cursor.getInt(cursor.getColumnIndex(COL_FAV_DURATION));
				String thumbnailUrl = cursor.getString(cursor.getColumnIndex(COL_FAV_IMAGE_URL));
				Video v = new Video(id, title, duration, null, null, thumbnailUrl);
				mFavoriteList.add(v);
				cursor.moveToNext();
			}
		}
		cursor.close();
		
		KLog.v(TAG, "Count of favorites: " + mFavoriteList.size());
		return true;
	}
	
	public boolean clearFavorites() {
		KLog.v(TAG, "clearFavorite");
		SQLiteDatabase db = mHelper.getWritableDatabase();
		if (db.delete(TABLE_FAVORITE_NAME, "1", null) > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public List<Video> getRestoredFavorites() {
		return mFavoriteList;
	}
	
	public boolean insertBlackList(String videoId, boolean byUser) {
		KLog.v(TAG, "insertBlackList");
		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.beginTransaction();
		ContentValues values = new ContentValues();
		values.put(COL_BL_VIDEO_ID, videoId);
		if (byUser) {
			values.put(COL_BL_ADDED_BY, "user");
		} else {
			values.put(COL_BL_ADDED_BY, "app");
		}
		db.insertWithOnConflict(TABLE_BLACK_LIST_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
		db.setTransactionSuccessful();
		db.endTransaction();
		return true;
	}
	
	public boolean deleteBlackList(String videoId) {
		KLog.v(TAG, "deleteBlackList");
		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.beginTransaction();
		String whereClause = COL_BL_VIDEO_ID + "=?";
		String[] whereArgs = new String[]{videoId};
		int num = db.delete(TABLE_BLACK_LIST_NAME, whereClause, whereArgs);
		db.setTransactionSuccessful();
		db.endTransaction();
		if (num > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean clearBlackList() {
		KLog.v(TAG, "clearBlackList");
		SQLiteDatabase db = mHelper.getWritableDatabase();
		if (db.delete(TABLE_BLACK_LIST_NAME, "1", null) > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public synchronized boolean restoreBlackList() {
		KLog.v(TAG, "restoreBlackList");
		// Already restored
		if (mAppBlackList != null && mUserBlackList != null) {
			return false;
		}
		SQLiteDatabase db = mHelper.getReadableDatabase();
		String[] columns = new String[]{
				COL_BL_VIDEO_ID,
				COL_BL_ADDED_BY
		};
		// Restore from DB
		Cursor cursor = db.query(TABLE_BLACK_LIST_NAME, columns, null, null, null, null, null);
		mAppBlackList = new ArrayList<String>();
		mUserBlackList = new ArrayList<String>();
		if (cursor.moveToFirst()) {
			while (cursor.isAfterLast()) {
				cursor.moveToNext();
				String addedBy = cursor.getString(cursor.getColumnIndex(COL_BL_ADDED_BY));
				if (addedBy.equals("user")) {
					mUserBlackList.add(cursor.getString(cursor.getColumnIndex(COL_BL_VIDEO_ID)));
				} else {
					mAppBlackList.add(cursor.getString(cursor.getColumnIndex(COL_BL_VIDEO_ID)));
				}
			}
		}
		cursor.close();
		return true;
	}
	
	public List<String> getRestoredAppBlackList() {
		return mAppBlackList;
	}
	
	public List<String> getRestoredUserBlackList() {
		return mUserBlackList;
	}

	/**
	 * Database helper class for LoopTube.
	 * @author Keisuke Kobayashi
	 *
	 */
	private static class DatabaseOpenHelper extends SQLiteOpenHelper {

		private static final String DB_NAME = "looptube.db";
		private static final int DB_VERSION = 1;
		
		public DatabaseOpenHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			KLog.v(TAG, "onCreate");
			createArtistTable(db);
			createImageTable(db);
			createFavoriteTable(db);
			createBlackListTable(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			KLog.v(TAG, "onUpgrade");
			String DROP_ARTIST_TABLE = "drop table " + TABLE_ARTIST_NAME + ";";
			db.execSQL(DROP_ARTIST_TABLE);

			String DROP_IMAGE_TABLE = "drop table " + TABLE_IMAGE_NAME + ";";
			db.execSQL(DROP_IMAGE_TABLE);
			
			String DROP_FAVORITE_TABLE = "drop table " + TABLE_FAVORITE_NAME + ";";
			db.execSQL(DROP_FAVORITE_TABLE);
			
			String DROP_BL_TABLE = "drop table " + TABLE_BLACK_LIST_NAME + ";";
			db.execSQL(DROP_BL_TABLE);
		}
		
		private void createArtistTable(SQLiteDatabase db) {
			KLog.v(TAG, "createArtistTable");
			String CREATE_ARTIST_TABLE = "create table " + TABLE_ARTIST_NAME + " ("
					+ COL_ID + " integer primary key autoincrement, "
					+ COL_ART_NAME + " text not null unique, "
					+ COL_ART_DATE + " integer not null"
					+ ");";
			db.execSQL(CREATE_ARTIST_TABLE);
		}
		
		private void createImageTable(SQLiteDatabase db) {
			KLog.v(TAG, "createImageTable");
			String CREATE_IMAGE_TABLE = "create table " + TABLE_IMAGE_NAME + " ("
					+ COL_ID + " integer primary key autoincrement, "
					+ COL_IMG_ARTIST_NAME + " text not null, "
					+ COL_IMG_IMAGE_URL + " text not null"
					+ ");";
			db.execSQL(CREATE_IMAGE_TABLE);
		}
		
		private void createFavoriteTable(SQLiteDatabase db) {
			KLog.v(TAG, "createFavoriteTable");
			String CREATE_FAV_TABLE = "create table " + TABLE_FAVORITE_NAME + " ("
					+ COL_ID + " integer primary key autoincrement, "
					+ COL_FAV_VIDEO_ID + " text not null unique, "
					+ COL_FAV_TITLE + " text not null,"
					+ COL_FAV_IMAGE_URL + " text not null, "
					+ COL_FAV_ARTIST + " text not null, "
					+ COL_FAV_DURATION + " integer not null"
					+ ");";
			db.execSQL(CREATE_FAV_TABLE);
		}
		
		private void createBlackListTable(SQLiteDatabase db) {
			KLog.v(TAG, "createBlackListTable");
			String CREATE_BL_TABLE = "create table " + TABLE_BLACK_LIST_NAME + " ("
					+ COL_ID + " integer primary key autoincrement, "
					+ COL_BL_VIDEO_ID + " text not null unique, "
					+ COL_BL_ADDED_BY + " text not null unique "
					+ ");";
			db.execSQL(CREATE_BL_TABLE);
		}

	}
}
