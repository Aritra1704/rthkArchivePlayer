package com.hei.android.app.rthkArchivePlayer.player.asfPlayer;

import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AsfFileDbAdapter {
	public static final String KEY_URL = "URL";
	public static final String KEY_PATH = "PATH";
	public static final String KEY_LENGTH = "LENGTH";
	public static final String KEY_SIZE = "SIZE";

	private static final String LOG_TAG = "AsfFileDbAdapter";

	private static final String NAME = "asfFiles";
	private static final String TABLE_NAME = "FILES";
	private static final int VERSION = 1;

	/**
	 * Handler of open and create database
	 */
	private static class AsfFileDbHelper extends SQLiteOpenHelper {
		private static final String CREATE_SCRIPT = 
				"CREATE  TABLE " + TABLE_NAME + " (" +
						KEY_URL + " TEXT PRIMARY KEY NOT NULL, " +
						KEY_PATH + " TEXT NOT NULL, " +
						KEY_LENGTH + " DOUBLE NOT NULL, " +
						KEY_SIZE + " INTEGER NOT NULL)";

		private AsfFileDbHelper(final Context context) {
			super(context, NAME, null, VERSION);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			db.execSQL(CREATE_SCRIPT);
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
			Log.w(LOG_TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);
		}

	}

	private final Context _context;
	private AsfFileDbHelper _dbHelper;
	private SQLiteDatabase _db;

	/**
	 * Constructor of EventDbAdapter
	 * 
	 * @param context Context of the application, used to create and open database
	 */
	public AsfFileDbAdapter(final Context context) {
		_context = context;
	}

	/**
	 * Open the event database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialisation call)
	 * @throws SQLException if the database could be neither opened or created
	 */
	public AsfFileDbAdapter open() throws SQLException {
		_dbHelper = new AsfFileDbHelper(_context);
		_db = _dbHelper.getWritableDatabase();

		return this;
	}

	/**
	 * Close the file database
	 */
	public void close() {
		_db.close();
		_dbHelper.close();
	}

	/**
	 * Return all files in the database
	 * 
	 * @return All files in a list
	 */
	public List<AsfFile> fetchAllFiles() {
		final Cursor cursor = _db.query(TABLE_NAME, new String[] {KEY_URL, KEY_PATH, KEY_LENGTH, KEY_SIZE}, 
				null, null, null, null, null);

		final List<AsfFile> files = new LinkedList<AsfFile>();

		for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			final String url = cursor.getString(0);
			final String path = cursor.getString(1);
			final double length = cursor.getDouble(2);
			final long size = cursor.getLong(3);

			final AsfFile file = new AsfFile(url, path, length, size);
			files.add(file);
		}

		cursor.close();

		return files;
	}

	public AsfFile getFile(final String url) {
		final String where = KEY_URL + " = '" + url + "'";
		final Cursor cursor = _db.query(TABLE_NAME, new String[] {KEY_URL, KEY_PATH, KEY_LENGTH, KEY_SIZE},
				where, null, null, null, null);

		if (!cursor.isAfterLast()) {
			cursor.moveToFirst();
			final String fileUrl = cursor.getString(0);
			final String path = cursor.getString(1);
			final double length = cursor.getDouble(2);
			final long size = cursor.getLong(3);

			final AsfFile file = new AsfFile(fileUrl, path, length, size);
			return file;
		}

		return null;
	}


	/**
	 * Add a file to the database
	 * 
	 * @param urld
	 * @param path
	 * @param length
	 * @param size
	 * @param headerSize
	 * @return created event model, null if failed
	 */
	public AsfFile createFile (final String url, final String path,
			final double length, final long size) {
		final ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_URL, url);
		initialValues.put(KEY_PATH, path);
		initialValues.put(KEY_LENGTH, length);
		initialValues.put(KEY_SIZE, size);

		try {
			final long id = _db.insert(TABLE_NAME, null, initialValues);
			if(id != -1) {
				final AsfFile file = new AsfFile(url, path, length, size);
				return file;
			}
			else {
				return null;
			}
		} catch(Exception e) {
			Log.e(LOG_TAG, e.getMessage());
			return null;
		}
	}

	/**
	 * Delete the file with the given ID
	 * 
	 * @param url ID of file to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteFile(final String url) {
		final String whereClause = KEY_URL + " = '" + url + "'";
		final int rowNum = _db.delete(TABLE_NAME, whereClause, null);

		return rowNum > 0;
	}
}
