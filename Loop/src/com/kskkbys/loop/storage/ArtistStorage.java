package com.kskkbys.loop.storage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
	
	private static final String TABLE_NAME = "artists";
	
	private static final String COL_ID= "_id";
	private static final String COL_NAME = "name";
	private static final String COL_IMAGE_URL = "image_url";
	private static final String COL_DATE = "date";
	
	private Context mContext;
	private ArtistOpenHelper mHelper;
	
	/**
	 * Constructor
	 * @param context
	 */
	public ArtistStorage(Context context) {
		mContext = context;
		mHelper = new ArtistOpenHelper(context);
	}
	
	public void insertOrUpdate(Entry entry) {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(COL_NAME, entry.name);
		values.put(COL_IMAGE_URL, entry.imageUrl);
		values.put(COL_DATE, entry.date.getTime());
		db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}
	
	public void delete(Entry entry) {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.delete(TABLE_NAME, "name=?", new String[]{entry.name});
	}
	
	public void clear() {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.delete(TABLE_NAME, null, null);
	}
	
	public List<Entry> restore() {
		SQLiteDatabase db = mHelper.getReadableDatabase();
		String[] cols = {
				COL_NAME,
				COL_IMAGE_URL,
				COL_DATE
		};
		Cursor cursor = db.query(TABLE_NAME, cols, null, null, null, null, "date DESC");
		int nameIndex = cursor.getColumnIndex(COL_NAME);
		int imageIndex = cursor.getColumnIndex(COL_IMAGE_URL);
		int dateIndex = cursor.getColumnIndex(COL_DATE);
		List<Entry> list = new ArrayList<ArtistStorage.Entry>();
		if (cursor.moveToFirst()) {
			while (!cursor.isAfterLast()) {
				Entry e = new Entry();
				e.name = cursor.getString(nameIndex);
				e.date = new Date(cursor.getLong(dateIndex));
				e.imageUrl = cursor.getString(imageIndex);
				list.add(e);
				cursor.moveToNext();
			}
		}
		cursor.close();
		return list;
	}
	
	public static class Entry {
		public String name;
		public String imageUrl;
		public Date date;
	}
	
	private static class ArtistOpenHelper extends SQLiteOpenHelper {

		public ArtistOpenHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			String CREATE_TABLE = "create table " + TABLE_NAME + " ("
					+ COL_ID + " integer primary key autoincrement, "
					+ COL_NAME + " text not null unique, "
					+ COL_IMAGE_URL + " text, "
					+ COL_DATE + " integer not null"
					+ ");";
			db.execSQL(CREATE_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			String DROP_TABLE = "drop table " + TABLE_NAME + ";";
			db.execSQL(DROP_TABLE);
			onConfigure(db);
		}
		
	}
}
