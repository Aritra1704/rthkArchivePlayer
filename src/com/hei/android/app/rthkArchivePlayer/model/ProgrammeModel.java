package com.hei.android.app.rthkArchivePlayer.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ProgrammeModel implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final Object STARRED_LOCK = new Object();
	private static Map<String, String> STARRED_CACHE;
	
	private static void ensureStarredCacheLoaded(Context context) {
		if(STARRED_CACHE == null) {
			synchronized (STARRED_LOCK) {
				if(STARRED_CACHE == null) {
					STARRED_CACHE = new StarredDatabase(context).getStarred();
				}
			}
		}
	}
	
	public static List<ProgrammeModel> getStarredProgrames(Context context) {
		ensureStarredCacheLoaded(context);
		final Set<Entry<String, String>> entrySet;
		synchronized (STARRED_LOCK) {
			entrySet = STARRED_CACHE.entrySet();
		}
		
		final List<ProgrammeModel> starred = new ArrayList<ProgrammeModel>(entrySet.size());
		for (Entry<String, String> entry : entrySet) {
			final String id = entry.getKey();
			final String name = entry.getValue();
			starred.add(new ProgrammeModel(context, name, id));
		}
		
		return starred;
	}

	private final String _name;
	private final String _id;

	public ProgrammeModel(final Context context, final String name, final String id) {
		ensureStarredCacheLoaded(context);

		_name = name;
		_id = id;
	}

	public String getName() {
		return _name;
	}

	public String getId() {
		return _id;
	}

	public String getPageUrl(int pageNum) {
		return "http://programme.rthk.hk/channel/radio/programme.php?name=" + _id + "&m=archive&item=50&page=" + pageNum;
	}

	public boolean isStarred() {
		synchronized (STARRED_LOCK) {
			return STARRED_CACHE.containsKey(_id);
		}
	}

	public void setStarred(final Context context, boolean starred) {
		final boolean _starred = isStarred();
		if(_starred != starred) {
			synchronized (STARRED_LOCK) {
				if(starred) {
					new StarredDatabase(context).addStarred(_id, _name);
					STARRED_CACHE.put(_id, _name);
				}
				else {
					new StarredDatabase(context).removeStarred(_id);
					STARRED_CACHE.remove(_id);
				}
			}
		}
	}
	
	@Override
	public int hashCode() {
		return _id.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if(o != null && (o instanceof ProgrammeModel)) {
			final String id = ((ProgrammeModel) o).getId();
			return _id.equals(id);
		}
		return super.equals(o);
	}
	
	@Override
	public String toString() {
		return "{id=" + _id + ", name=" + _name + "}";
	}

	private static class StarredDatabase extends SQLiteOpenHelper {
		private static final int DATABASE_VERSION = 1;
		private static final String DATABASE_NAME = "starred_programmes.db";

		private static final String TABLE = "starred";
		private static final String ID = "id";
		private static final String NAME = "name";

		private StarredDatabase(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE + " (" + ID + " TEXT PRIMARY KEY, " + NAME + " TEXT)");

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE + ";");
		}

		public Map<String, String> getStarred() {
			final SQLiteDatabase db = getReadableDatabase();
			final Cursor cursor = db.query(TABLE, new String[]{ID, NAME}, null, null, null, null, null);

			final Map<String, String> starred = new HashMap<String, String>();
			for (cursor.moveToFirst(); cursor.moveToNext(); cursor.isAfterLast()) {
				final String id = cursor.getString(cursor.getColumnIndexOrThrow(ID));
				final String name = cursor.getString(cursor.getColumnIndexOrThrow(NAME));
				starred.put(id, name);
			}

			db.close();

			return starred;
		}

		public void addStarred(String id, String name) {
			final SQLiteDatabase db = getWritableDatabase();
			db.execSQL("INSERT INTO " + TABLE + " (" + ID + ", " + NAME + ") VALUES (?, ?);", new Object[]{id, name});
		}

		public void removeStarred(String id) {
			final SQLiteDatabase db = getWritableDatabase();
			db.execSQL("DELETE FROM " + TABLE + " WHERE " + ID + "=?", new Object[]{id});
		}

	}
}
