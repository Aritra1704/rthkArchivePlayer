package com.hei.android.app.rthkArchivePlayer.player.asfPlayer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;

public class AsfFileInputStream extends FileInputStream {

	private final double _length;
	private final long _size;

	public static AsfFileInputStream createAsfFileInputStream(final Context context, final String url) throws FileNotFoundException {
		final AsfFileDbAdapter dbAdapter = new AsfFileDbAdapter(context);
		dbAdapter.open();
		final AsfFile asf = dbAdapter.getFile(url);

		if (asf == null) {
			dbAdapter.close();
			throw new FileNotFoundException("No record of ASF file for " + url);
		}

		final String path = asf.getPath();
		final double length = asf.getLength();
		final long size = asf.getSize();

		try {
			final AsfFileInputStream stream = new AsfFileInputStream(path, length, size);
			return stream;
		} catch (FileNotFoundException e) {
			dbAdapter.deleteFile(url);
			throw e;
		} finally {
			dbAdapter.close();
		}
	}

	private AsfFileInputStream(final String path, final double length, final long size) throws FileNotFoundException{
		super(path);
		_length = length;
		_size = size;
	}


	public double getLength() throws IOException {
		return _length;
	}

	public long getSize() throws IOException {
		return _size;
	}
}
