package com.kskkbys.loop.storage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.kskkbys.loop.logger.KLog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * This class store/restore search history in SQLite.
 * @author Keisuke Kobayashi
 *
 */
public class ArtistStorage {

	private static final String TAG = ArtistStorage.class.getSimpleName();

	private static final String DB_NAME = "artists.db";
	private static final int DB_VERSION = 1;

	private static final String TABLE_ARTIST_NAME = "artists";
	private static final String COL_ID= "_id";
	private static final String COL_NAME = "name";
	private static final String COL_DATE = "date";

	private static final String TABLE_IMAGE_NAME = "images";
	private static final String COL_ARTIST_NAME= "artist_name";
	private static final String COL_IMAGE_URL = "image_url";

	private Context mContext;
	private ArtistOpenHelper mHelper;

	private List<Entry> mEntryList;
	
	/**
	 * Constructor
	 * @param context
	 */
	public ArtistStorage(Context context) {
		mContext = context;
		mHelper = new ArtistOpenHelper(context);
		mEntryList = null;
	}

	/**
	 * Add entry (artist and its thumbnails).<br>
	 * Notice that this method may take a few seconds.
	 * If you don't need wait to complete, please set async flag true.
	 * @param entry
	 */
	public void insertOrUpdate(final Entry entry, final boolean isAsync) {
		KLog.v(TAG, "start insertOrUpdate");
		if (isAsync) {
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					insertOpUpdateInner(entry);
				}
			}).start();
		} else {
			insertOpUpdateInner(entry);
		}
		KLog.v(TAG, "end insertOrUpdate");
	}
	
	private void insertOpUpdateInner(Entry entry) {
		SQLiteDatabase db = mHelper.getWritableDatabase();

		db.beginTransaction();
		try {
			// Insert artist
			ContentValues values = new ContentValues();
			values.put(COL_NAME, entry.name);
			values.put(COL_DATE, entry.date.getTime());
			db.insertWithOnConflict(TABLE_ARTIST_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
			// Insert images
			for (int i=0; i<entry.imageUrls.size(); i++) {
				ContentValues imageValues = new ContentValues();
				imageValues.put(COL_ARTIST_NAME, entry.name);
				imageValues.put(COL_IMAGE_URL, entry.imageUrls.get(i));
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
	public void delete(Entry entry) {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.delete(TABLE_ARTIST_NAME, "name=?", new String[]{entry.name});
		db.delete(TABLE_IMAGE_NAME, "artist_name=?", new String[]{entry.name});
	}

	/**
	 * Clear all entries.
	 */
	public void clear() {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.delete(TABLE_ARTIST_NAME, null, null);
		db.delete(TABLE_IMAGE_NAME, null, null);
	}

	public void restore() {
		SQLiteDatabase db = mHelper.getReadableDatabase();
		String[] cols = {
				COL_NAME,
				COL_DATE
		};
		// Get artist list
		Cursor cursor = db.query(TABLE_ARTIST_NAME, cols, null, null, null, null, "date DESC");
		int nameIndex = cursor.getColumnIndex(COL_NAME);
		int dateIndex = cursor.getColumnIndex(COL_DATE);
		List<Entry> list = new ArrayList<ArtistStorage.Entry>();
		if (cursor.moveToFirst()) {
			while (!cursor.isAfterLast()) {
				Entry e = new Entry();
				e.name = cursor.getString(nameIndex);
				e.date = new Date(cursor.getLong(dateIndex));
				e.imageUrls = new ArrayList<String>();
				list.add(e);
				cursor.moveToNext();
			}
		}
		cursor.close();

		// Get thumbnails for each artist
		for (Entry e: list) {
			String[] imgCols = {
					COL_IMAGE_URL
			};
			Cursor imgCursor = db.query(TABLE_IMAGE_NAME, imgCols, "artist_name=?", new String[]{e.name}, null, null, "_id ASC");
			int urlIndex = imgCursor.getColumnIndex(COL_IMAGE_URL);
			if (imgCursor.moveToFirst()) {
				while (!imgCursor.isAfterLast()) {
					e.imageUrls.add(imgCursor.getString(urlIndex));
					imgCursor.moveToNext();
				}
			}
			imgCursor.close();
		}

		mEntryList = list;
	}
	
	public List<Entry> getRestoredArtists() {
		if (mEntryList == null) {
			throw new IllegalStateException("Entry list is null. Call restore() at first.");
		}
		return mEntryList;
	}

	public static class Entry {
		public String name;
		public List<String> imageUrls;
		public Date date;
		public Entry() {
			name = null;
			imageUrls = new ArrayList<String>();
			date = null;
		}
	}

	private static class ArtistOpenHelper extends SQLiteOpenHelper {

		public ArtistOpenHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			String CREATE_ARTIST_TABLE = "create table " + TABLE_ARTIST_NAME + " ("
					+ COL_ID + " integer primary key autoincrement, "
					+ COL_NAME + " text not null unique, "
					+ COL_DATE + " integer not null"
					+ ");";
			db.execSQL(CREATE_ARTIST_TABLE);

			String CREATE_IMAGE_TABLE = "create table " + TABLE_IMAGE_NAME + " ("
					+ COL_ID + " integer primary key autoincrement, "
					+ COL_ARTIST_NAME + " text not null, "
					+ COL_IMAGE_URL + " text not null"
					+ ");";
			db.execSQL(CREATE_IMAGE_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			String DROP_ARTIST_TABLE = "drop table " + TABLE_ARTIST_NAME + ";";
			db.execSQL(DROP_ARTIST_TABLE);

			String DROP_IMAGE_TABLE = "drop table " + TABLE_IMAGE_NAME + ";";
			db.execSQL(DROP_IMAGE_TABLE);

			onConfigure(db);
		}

	}
}
