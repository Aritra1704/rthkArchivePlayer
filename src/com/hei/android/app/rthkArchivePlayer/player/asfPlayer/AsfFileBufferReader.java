package com.hei.android.app.rthkArchivePlayer.player.asfPlayer;

import java.io.IOException;

import android.util.Log;

import com.hei.android.app.rthkArchivePlayer.player.decoder.BufferReader;

public class AsfFileBufferReader implements BufferReader {
	private final static String LOG = "AsfFileBufferReader";
	
	private final AsfFileInputStream _stream;
	private final Buffer _buffer;
	private final int _capacity;
	
	public AsfFileBufferReader(final int capacity, final AsfFileInputStream stream) {
		_stream = stream;
		_buffer = new Buffer(capacity);
		_capacity = capacity;
	}
	
	@Override
	public Buffer next() {
		final byte[] data = _buffer.getData();
		int total = 0;
		
		synchronized (this) {
			while (total < _capacity) {
				try {
					final int n = _stream.read( data, total, _capacity - total );
					total += n;
				}
				catch (final IOException e) {
					Log.e( LOG, "Exception when reading: " + e );
				}
			}	
		}

		_buffer.setSize(total);

		return _buffer;
	}

}
