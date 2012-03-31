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
package com.hei.android.app.rthkArchivePlayer.player.decoder;



/**
 * Parent class for all array decoders.
 */
public class AsfDecoder extends Decoder {

	private static enum State { IDLE, RUNNING };

	private static boolean libLoaded = false;

	{
		loadLibrary();
	}


	////////////////////////////////////////////////////////////////////////////
	// Attributes
	////////////////////////////////////////////////////////////////////////////

	private int aacdw;
	private State state = State.IDLE;


	////////////////////////////////////////////////////////////////////////////
	// Public
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Starts decoding AAC stream.
	 */
	public Info start( final BufferReader reader ) {
		if (state != State.IDLE) {
			throw new IllegalStateException();
		}

		info = new Info();

		aacdw = nativeStart( reader, info );

		if (aacdw == 0) {
			throw new RuntimeException("Cannot start native decoder");
		}

		state = State.RUNNING;

		return info;
	}	


	/**
	 * Decodes AAC stream.
	 * @return the number of samples produced (totally all channels = the length of the filled array)
	 */
	public Info decode( final short[] samples, final int outLen ) {
		if (state != State.RUNNING) {
			throw new IllegalStateException();
		}

		nativeDecode( aacdw, samples, outLen );

		return info;
	}


	/**
	 * Stops the decoder and releases all resources.
	 */
	public void stop() {
		if (aacdw != 0) {
			nativeStop( aacdw );
			aacdw = 0;
		}

		state = State.IDLE;
	}


	////////////////////////////////////////////////////////////////////////////
	// Protected
	////////////////////////////////////////////////////////////////////////////

	@Override
	protected void finalize() {
		try {
			stop();
		}
		catch (final Throwable t) {
			t.printStackTrace();
		}
	}


	////////////////////////////////////////////////////////////////////////////
	// Private
	////////////////////////////////////////////////////////////////////////////

	private static synchronized void loadLibrary() {
		if (!libLoaded) {
			System.loadLibrary("asf");

			libLoaded = true;
		}
	}


	private native int nativeStart( BufferReader reader, Info info );

	private native int nativeDecode( int aacdw, short[] samples, int outLen );

	private native void nativeStop( int aacdw );
}


