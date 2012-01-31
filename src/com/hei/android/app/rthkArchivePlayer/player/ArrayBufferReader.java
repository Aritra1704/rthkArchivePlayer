/*
 ** AACPlayer - Freeware Advanced Audio (AAC) Player for Android
 ** Copyright (C) 2011 Spolecne s.r.o., http://www.spoledge.com
 **
 ** This program is free software; you can redistribute it and/or modify
 ** it under the terms of the GNU General Public License as published by
 ** the Free Software Foundation; either version 3 of the License, or
 ** (at your option) any later version.
 **
 ** This program is distributed in the hope that it will be useful,
 ** but WITHOUT ANY WARRANTY; without even the implied warranty of
 ** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 ** GNU General Public License for more details.
 **
 ** You should have received a copy of the GNU General Public License
 ** along with this program. If not, see <http://www.gnu.org/licenses/>.
 **/
package com.hei.android.app.rthkArchivePlayer.player;

import java.io.IOException;

import android.util.Log;


/**
 * This is a separate thread for reading data from a stream.
 * The buffer creates 3 buffer instances - one is being filled by the execution thread,
 * one is prepared with data, one can be processed by a consumer.
 * <pre>
 *  InputStream is = ...;
 *
 *  // create a new reader with capacity of 4096 bytes per buffer:
 *  ArrayBufferReader reader = new ArrayBufferReader( 4096, is );
 *
 *  // start the execution thread which reads the stream and fills the buffers:
 *  new Thread(reader).start();
 *
 *  // get the data
 *  while (...) {
 *      ArrayBufferReader.Buffer buf = reader.next();
 *
 *      if (!buf.getSize() == 0) break;
 *
 *      // process data
 *      ...
 *  }
 * </pre>
 */
public class ArrayBufferReader implements Runnable {

	public static class Buffer {
		private final byte[] data;
		private int size;

		Buffer( final int capacity ) {
			data = new byte[ capacity ];
		}

		public final byte[] getData() {
			return data;
		}

		public final int getSize() {
			return size;
		}
	}

	private static String LOG = "ArrayBufferReader";

	int _capacity;

	private final Buffer[] _buffers;

	/**
	 * The buffer to be write into.
	 */
	private int _indexMine;

	/**
	 * The index of the buffer last returned in the next() method.
	 */
	private int _indexBlocked;

	private boolean _stopped;

	private boolean _sought;
	
	private boolean _bufferReset;

	private final MMSInputStream _mmsStream;



	////////////////////////////////////////////////////////////////////////////
	// Constructors
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new buffer.
	 *
	 * @param capacity the capacity of one buffer in bytes
	 *          = total allocated memory
	 *
	 * @param is the input stream
	 */
	public ArrayBufferReader( final int capacity, final MMSInputStream is ) {
		this._capacity = capacity;
		this._mmsStream = is;

		Log.d( LOG, "init(): capacity=" + capacity );

		_buffers = new Buffer[30];

		for (int i=0; i < _buffers.length; i++) {
			_buffers[i] = new Buffer( capacity );
		}

		_indexMine = 0;
		_indexBlocked = _buffers.length-1;
	}


	////////////////////////////////////////////////////////////////////////////
	// Public
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Changes the capacity of the buffer.
	 */
	public synchronized void setCapacity( final int capacity ) {
		Log.d( LOG, "setCapacity(): " + capacity );
		this._capacity = capacity;
	}


	/**
	 * The main loop.
	 */
	@Override
	public void run() {
		Log.d( LOG, "run() started...." );

		int cap = _capacity;
		int total = 0;

		while (!_stopped) {
			Buffer buffer = _buffers[ _indexMine ];
			total = 0;

			if (cap != buffer.data.length) {
				Log.d( LOG, "run() capacity changed: " + buffer.data.length + " -> " + cap);
				_buffers[ _indexMine ] = buffer = null;
				_buffers[ _indexMine ] = buffer = new Buffer( cap );
			}

			synchronized (this) {
				while (!_stopped && total < cap) {
					try {
						final int n = _mmsStream.read( buffer.data, total, cap - total );

						if (n == -1) {
							_stopped = true;
							Log.e( LOG, "run() the stream ended.");
						} else {
							total += n;
						}
					}
					catch (final IOException e) {
						Log.e( LOG, "Exception when reading: " + e );
						_stopped = true;
					}
				}	
			}

			buffer.size = total;

			synchronized (this) {
				notifyAll();

				final int indexNew = (_indexMine + 1) % _buffers.length;

				while (!_stopped && indexNew == _indexBlocked && !_sought) {
					Log.d( LOG, "run() waiting...." );
					try { wait(); } catch (final InterruptedException e) {}
					Log.d( LOG, "run() awaken" );
				}
				
				_indexMine = indexNew;
				cap = _capacity;

				if (_sought) {
					_indexBlocked = (_indexMine + _buffers.length - 1) % _buffers.length;
					_sought = false;
					_bufferReset = true;
				}
			}
		}

		Log.d( LOG, "run() stopped." );
	}

	public synchronized boolean seek(double time) {
		Log.d( LOG, "seek() notify all" );
		notifyAll();

		boolean sought = false;
		try {
			sought = _mmsStream.seek(time);
			_sought = sought;
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			while (_sought) {
				Log.d( LOG, "seek() waiting...." );
				wait();
				Log.d( LOG, "seek() awaken" );
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return sought;
	}

	/**
	 * Stops the thread - the object cannot be longer used.
	 */
	public synchronized void stop() {
		_stopped = true;
		notify();
	}


	/**
	 * Returns true if this thread was stopped.
	 */
	public boolean isStopped() {
		return _stopped;
	}


	/**
	 * Returns next available buffer instance.
	 * The returned instance can be freely used by another thread.
	 * Blocks the caller until a buffer is ready.
	 */
	public synchronized Buffer next() {
		if (_bufferReset) {
			_bufferReset = false;
		}
		
		int indexNew = (_indexBlocked + 1) % _buffers.length;

		while (!_stopped && indexNew == _indexMine) {
			Log.d( LOG, "next() waiting...." );
			try { wait(); } catch (final InterruptedException e) {}
			Log.d( LOG, "next() awaken" );
			
			if (_bufferReset) {
				Log.d(LOG, "Buffer reset while waiting.");
				indexNew = (_indexBlocked + 1) % _buffers.length;
				_bufferReset = false;
			}
		}

		if (indexNew == _indexMine) {
			return null;
		}

		_indexBlocked = indexNew;

		notifyAll();

		return _buffers[ _indexBlocked ];
	}

}

