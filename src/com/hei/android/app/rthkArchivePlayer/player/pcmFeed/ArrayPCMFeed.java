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
package com.hei.android.app.rthkArchivePlayer.player.pcmFeed;

import com.hei.android.app.rthkArchivePlayer.player.message.PlayerMessageHandler;



/**
 * This is the PCM Feeder which uses arrays (short[]).
 *
 * <pre>
 *  // 44100 Hz, stereo, bufferng of 1.5 seconds:
 *  ArrayPCMFeed pcmfeed = new ArrayPCMFeed( 44100, 2, ArrayPCMFeed.msToBytes( 1500 ));
 *
 *  // start the exectuin thread:
 *  new Thread( pcmfeed ).start();
 *
 *  while (...) {
 *      // obtain the PCM data:
 *      short[] samples = ...
 *
 *      // feed the audio buffer; on error break the loop:
 *      if (!pcmfeed.feed( samples, samples.length )) break;
 *  }
 * </pre>
 */
public class ArrayPCMFeed extends PCMFeed {

	////////////////////////////////////////////////////////////////////////////
	// Attributes
	////////////////////////////////////////////////////////////////////////////

	private short[] _samples;
	private int _n;


	////////////////////////////////////////////////////////////////////////////
	// Constructors
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new PCMFeed object.
	 * @param sampleRate the sampling rate in Hz (e.g. 44100)
	 * @param channels the number of channels - only allowed values are 1 (mono) and 2 (stereo).
	 * @param bufferSizeInBytes the size of the audio buffer in bytes
	 */
	public ArrayPCMFeed( final int sampleRate, final int channels, final int bufferSizeInBytes ) {
		this( sampleRate, channels, bufferSizeInBytes, null );
	}


	/**
	 * Creates a new PCMFeed object.
	 * @param sampleRate the sampling rate in Hz (e.g. 44100)
	 * @param channels the number of channels - only allowed values are 1 (mono) and 2 (stereo).
	 * @param bufferSizeInBytes the size of the audio buffer in bytes
	 * @param playerCallback the callback - may be null
	 */
	public ArrayPCMFeed( final int sampleRate, final int channels, final int bufferSizeInBytes, final PlayerMessageHandler playerCallback ) {
		super( sampleRate, channels, bufferSizeInBytes, playerCallback );
	}


	////////////////////////////////////////////////////////////////////////////
	// Public
	////////////////////////////////////////////////////////////////////////////

	/**
	 * This is called by main thread when a new data are available.
	 *
	 * @param samples the array containing the PCM data
	 * @param n the length of the PCM data
	 * @return true if ok, false if the execution thread is not responding
	 */
	public synchronized boolean feed( final short[] samples, final int n ) {
		while (this._samples != null && !_stopped) {
			try { wait(); } catch (final InterruptedException e) {}
		}

		this._samples = samples;
		this._n = n;

		notify();

		return !_stopped;
	}


	////////////////////////////////////////////////////////////////////////////
	// Protected
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Acquires samples into variable lsamples.
	 * @return the actual size (in shorts) of the lsamples
	 */
	@Override
	protected synchronized int acquireSamples() {
		while (_n == 0 && !_stopped) {
			try { wait(); } catch (final InterruptedException e) {}
		}

		_lsamples = _samples;
		final int ln = _n;

		_samples = null;
		_n = 0;
		notify();

		return ln;
	}


	/**
	 * Releases the lsamples variable.
	 * This method is called always after processing the acquired lsamples.
	 */
	@Override
	protected void releaseSamples() {
		// nothing to be done
	}

}

