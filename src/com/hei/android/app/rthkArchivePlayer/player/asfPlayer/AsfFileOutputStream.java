package com.hei.android.app.rthkArchivePlayer.player.asfPlayer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

public class AsfFileOutputStream extends FileOutputStream {

	public static AsfFileOutputStream createAsfFileOutputStream(final Context context, final String url, final String path,
			final double length, final long size) throws FileNotFoundException {

		final AsfFileDbAdapter dbAdapter = new AsfFileDbAdapter(context);
		dbAdapter.open();
		dbAdapter.createFile(url, path, length, size);
		dbAdapter.close();

		return new AsfFileOutputStream(path);
	}

	public static List<AsfFileOutputStream> createAsfFileOutputStreams(final Context context, final String url, final String path,
			final double length, final long size, final int threadNum) throws FileNotFoundException {

		final AsfFileDbAdapter dbAdapter = new AsfFileDbAdapter(context);
		dbAdapter.open();
		dbAdapter.createFile(url, path, length, size);
		dbAdapter.close();

		final List<AsfFileOutputStream> streams = new ArrayList<AsfFileOutputStream>(threadNum);
		String ext = "";
		for(int i = 0; i < threadNum; i++, ext = ".part" + i) {
			final AsfFileOutputStream stream = new AsfFileOutputStream(path + ext);
			streams.add(stream);
		}

		return streams;
	}
	
	public static List<String> getPathName(final String path, final int threadNum) {
		final List<String> filesList = new ArrayList<String>(threadNum);
		String ext = "";
		for(int i = 0; i < threadNum; i++, ext = ".part" + i) {
			filesList.add(path + ext);
		}
		
		
		return filesList;
	}

	protected AsfFileOutputStream(final String path) throws FileNotFoundException {
		super(path);
	}
}
