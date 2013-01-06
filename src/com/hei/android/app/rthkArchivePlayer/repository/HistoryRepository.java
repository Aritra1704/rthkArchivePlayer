package com.hei.android.app.rthkArchivePlayer.repository;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.hei.android.app.rthkArchivePlayer.model.EpisodeModel;
import com.hei.android.app.rthkArchivePlayer.model.HistoryModel;
import com.hei.android.app.rthkArchivePlayer.model.ProgrammeModel;

public class HistoryRepository {

	private static Object CACHE_LOCK = new Object();
	private static List<HistoryModel> CACHE;

	private static HistoryRepository INSTANCE;

	private static void ensureHistoryCached(final Context context) {
		if(CACHE == null) {
			synchronized (CACHE_LOCK) {
				if(CACHE == null) {
					CACHE = new HistoryDatabase(context).getHistory();
				}
			}
		}
	}

	public static HistoryRepository getInstance(final Context context) {
		if(INSTANCE == null) {
			INSTANCE = new HistoryRepository(context);
		}

		return INSTANCE;
	}

	private HistoryRepository(final Context context) {
		ensureHistoryCached(context);
	}

	public List<HistoryModel> getHistory(final Context context) {
		return new ArrayList<HistoryModel>(CACHE);
	}

	public void addHistory(final Context context, final EpisodeModel episode) {
		synchronized (CACHE) {
			final Date accessTime = new HistoryDatabase(context).addHistory(episode);
			CACHE.add(0, new HistoryModel(accessTime, episode));
		}
	}

	public void removeHistory(final Context context, final EpisodeModel episode) {
		synchronized (CACHE) {
			final Date accessTime = new HistoryDatabase(context).removeHistory(episode);
			CACHE.remove(new HistoryModel(accessTime, episode));
		}
	}

	private static class HistoryDatabase extends SQLiteOpenHelper {
		private static final int DATABASE_VERSION = 1;
		private static final String DATABASE_NAME = "history.db";

		private static final String TABLE = "history";
		private static final String PROGRAMME_ID = "programme_id";
		private static final String PROGRAMME_NAME = "programme_name";
		private static final String EPISODE_NAME = "episode_name";
		private static final String EPISODE_DATE = "episode_date";
		private static final String ACCESS_TIME = "access_time";

		private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

		private final Context context;

		private HistoryDatabase(final Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			this.context = context;
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE + " (" + PROGRAMME_ID + " TEXT, " +
					EPISODE_DATE + " TEXT, " +
					ACCESS_TIME + " TEXT, " +
					PROGRAMME_NAME + " TEXT, " +
					EPISODE_NAME + " TEXT, " +
					"PRIMARY KEY (" + PROGRAMME_ID + ", " + EPISODE_DATE + "));");
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE + ";");
		}

		public List<HistoryModel> getHistory() {
			final SQLiteDatabase db = getReadableDatabase();
			final Cursor cursor = db.query(TABLE, new String[]{PROGRAMME_ID, PROGRAMME_NAME,
					EPISODE_NAME, EPISODE_DATE, ACCESS_TIME}, null, null, null, null, ACCESS_TIME + " DESC");

			final List<HistoryModel> history = new LinkedList<HistoryModel>();

			for (cursor.moveToFirst(); cursor.moveToNext(); cursor.isAfterLast()) {
				try {
					final String programmeId = cursor.getString(0);
					final String programmeName = cursor.getString(1);
					final ProgrammeModel programme = new ProgrammeModel(context, programmeName, programmeId);

					final String episodeName = cursor.getString(2);
					final Date episodeDate = DATE_FORMAT.parse(cursor.getString(3));
					final EpisodeModel episode = new EpisodeModel(programme, episodeName, episodeDate);

					final Date accessTime = DATE_FORMAT.parse(cursor.getString(4));

					history.add(new HistoryModel(accessTime, episode));

				} catch (final ParseException e) {
				}
			}

			return history;
		}

		public Date addHistory(final EpisodeModel model) {
			final ProgrammeModel programme = model.getProgramme();
			final String programmeId = programme.getId();
			final String programmeName = programme.getName();
			final String episodeName = model.getName();
			final Date episodeDate = model.getDate();
			final Date accessTime = new Date();

			final SQLiteDatabase db = getWritableDatabase();
			db.execSQL("INSERT OR REPLACE INTO " + TABLE + " (" +
					PROGRAMME_ID + ", " + EPISODE_DATE + ", " + ACCESS_TIME + ", " +
					PROGRAMME_NAME + ", " + EPISODE_NAME + ") VALUES (?, ?, ?, ?, ?);",
					new Object[]{programmeId, DATE_FORMAT.format(episodeDate), DATE_FORMAT.format(accessTime), programmeName, episodeName});
			db.close();
			return accessTime;
		}
		
		public Date removeHistory(final EpisodeModel model) {
			final ProgrammeModel programme = model.getProgramme();
			final String programmeId = programme.getId();
			final Date episodeDate = model.getDate();

			Date accessTime = null;
			
			final String where = PROGRAMME_ID + "=? AND " + EPISODE_DATE + "=?";
			final String[] whereArg = new String[]{programmeId, DATE_FORMAT.format(episodeDate)};
			
			final SQLiteDatabase db = getWritableDatabase();
			db.beginTransaction();
			final Cursor cursor = db.query(TABLE, new String[]{ACCESS_TIME}, where, whereArg, null, null, null);
			final int count = cursor.getCount();
			if(count > 0) {
				cursor.moveToFirst();
				try {
					accessTime = DATE_FORMAT.parse(cursor.getString(0));
					db.delete(TABLE, where, whereArg);
				} catch (ParseException e) {
				}
			}
			db.endTransaction();

			return accessTime;
		}
	}
}
