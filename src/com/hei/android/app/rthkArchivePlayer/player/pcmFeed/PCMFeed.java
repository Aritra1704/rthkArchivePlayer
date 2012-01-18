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


import com.hei.android.app.rthkArchivePlayer.player.message.PlayerMessage;
import com.hei.android.app.rthkArchivePlayer.player.message.PlayerMessageHandler;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Message;
import android.util.Log;


/**
 * This is the parent of PCM Feeders.
 */
public abstract class PCMFeed implements Runnable, AudioTrack.OnPlaybackPositionUpdateListener {

	private static final String LOG = "PCMFeed";


	////////////////////////////////////////////////////////////////////////////
	// Attributes
	////////////////////////////////////////////////////////////////////////////

	protected int _sampleRate;
	protected int _channels;
	protected int _bufferSizeInMs;
	protected int _bufferSizeInBytes;


	/**
	 * The callback - may be null.
	 */
	protected PlayerMessageHandler _playerMessageHandler;


	/**
	 * True iff the AudioTrack is playing.
	 */
	protected boolean _isPlaying;

	protected boolean _stopped;


	/**
	 * The local variable in run() method set by method acquireSamples().
	 */
	protected short[] _lsamples;


	/**
	 * Total samples written to AudioTrack.
	 */
	protected int _writtenTotal = 0;


	////////////////////////////////////////////////////////////////////////////
	// Constructors
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new PCMFeed object.
	 * @param sampleRate the sampling rate in Hz (e.g. 44100)
	 * @param channels the number of channels - only allowed values are 1 (mono) and 2 (stereo).
	 * @param bufferSizeInBytes the size of the audio buffer in bytes
	 * @param playerCallback the callback - may be null
	 */
	protected PCMFeed( final int sampleRate, final int channels, final int bufferSizeInBytes, final PlayerMessageHandler playerCallback ) {
		this._sampleRate = sampleRate;
		this._channels = channels;
		this._bufferSizeInBytes = bufferSizeInBytes;
		this._bufferSizeInMs = bytesToMs( bufferSizeInBytes, sampleRate, channels );
		this._playerMessageHandler = playerCallback;
	}


	////////////////////////////////////////////////////////////////////////////
	// Public
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the sampling rate.
	 */
	public final int getSampleRate() {
		return _sampleRate;
	}


	/**
	 * Returns the number of channels.
	 */
	public final int getChannels() {
		return _channels;
	}


	/**
	 * Returns the buffer size in bytes.
	 */
	public final int getBufferSizeInBytes() {
		return _bufferSizeInBytes;
	}


	/**
	 * Returns the buffer size in milliseconds.
	 */
	public final int getBufferSizeInMs() {
		return _bufferSizeInMs;
	}


	/**
	 * Stops the PCM feeder.
	 * This method just asynchronously notifies the execution thread.
	 * This can be called in any state.
	 */
	public synchronized void stop() {
		_stopped = true;
		notify();
	}


	/**
	 * Converts milliseconds to bytes of buffer.
	 * @param ms the time in milliseconds
	 * @return the size of the buffer in bytes
	 */
	public static int msToBytes( final int ms, final int sampleRate, final int channels ) {
		return (int)(((long) ms) * sampleRate * channels / 500);
	}


	/**
	 * Converts milliseconds to samples of buffer.
	 * @param ms the time in milliseconds
	 * @return the size of the buffer in samples
	 */
	public static int msToSamples( final int ms, final int sampleRate, final int channels ) {
		return (int)(((long) ms) * sampleRate * channels / 1000);
	}


	/**
	 * Converts bytes of buffer to milliseconds.
	 * @param bytes the size of the buffer in bytes
	 * @return the time in milliseconds
	 */
	public static int bytesToMs( final int bytes, final int sampleRate, final int channels ) {
		return (int)(500L * bytes / (sampleRate * channels));
	}


	/**
	 * Converts samples of buffer to milliseconds.
	 * @param samples the size of the buffer in samples (all channels)
	 * @return the time in milliseconds
	 */
	public static int samplesToMs( final int samples, final int sampleRate, final int channels ) {
		return (int)(1000L * samples / (sampleRate * channels));
	}


	////////////////////////////////////////////////////////////////////////////
	// OnPlaybackPositionUpdateListener
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Called on the listener to notify it that the previously set marker
	 * has been reached by the playback head.
	 */
	@Override
	public void onMarkerReached( final AudioTrack track ) {
	}


	/**
	 * Called on the listener to periodically notify it that the playback head
	 * has reached a multiple of the notification period.
	 */
	@Override
	public void onPeriodicNotification( final AudioTrack track ) {
		if (_playerMessageHandler != null) {
			final int buffered = _writtenTotal - track.getPlaybackHeadPosition()*_channels;
			final int ms = samplesToMs( buffered, _sampleRate, _channels );

			final Message msg = PlayerMessage.createBufferUpdateMessage(_isPlaying, ms, _bufferSizeInMs);
			_playerMessageHandler.sendMessage(msg);
		}
	}


	////////////////////////////////////////////////////////////////////////////
	// Runnable
	////////////////////////////////////////////////////////////////////////////

	/**
	 * The main execution loop which should be executed in its own thread.
	 */
	@Override
	public void run() {
		Log.d( LOG, "run(): sampleRate=" + _sampleRate + ", channels=" + _channels
				+ ", bufferSizeInBytes=" + _bufferSizeInBytes
				+ " (" + _bufferSizeInMs + " ms)");

		final AudioTrack atrack = new AudioTrack(
				AudioManager.STREAM_MUSIC,
				_sampleRate,
				_channels == 1 ?
						AudioFormat.CHANNEL_CONFIGURATION_MONO :
							AudioFormat.CHANNEL_CONFIGURATION_STEREO,
							AudioFormat.ENCODING_PCM_16BIT,
							_bufferSizeInBytes,
							AudioTrack.MODE_STREAM );

		atrack.setPlaybackPositionUpdateListener( this );
		atrack.setPositionNotificationPeriod( msToSamples( 200, _sampleRate, _channels ));

		_isPlaying = false;

		while (!_stopped) {
			// fetch the samples into our "local" variable lsamples:
			int ln = acquireSamples();

			if (_stopped) {
				releaseSamples();
				break;
			}

			// samples written to AudioTrack in this round:
			int writtenNow = 0;

			do {
				if (writtenNow != 0) {
					Log.d( LOG, "too fast for playback, sleeping...");
					try { Thread.sleep( 50 ); } catch (final InterruptedException e) {}
				}

				final int written = atrack.write( _lsamples, writtenNow, ln );

				if (written < 0) {
					Log.e( LOG, "error in playback feed: " + written );
					_stopped = true;
					break;
				}

				_writtenTotal += written;
				final int buffered = _writtenTotal - atrack.getPlaybackHeadPosition()*_channels;

				// Log.d( LOG, "PCM fed by " + ln + " and written " + written + " samples - buffered " + buffered);

				if (!_isPlaying) {
					if (buffered*2 >= _bufferSizeInBytes) {
						Log.d( LOG, "start of AudioTrack - buffered " + buffered + " samples");
						atrack.play();
						_isPlaying = true;
					}
					else {
						Log.d( LOG, "start buffer not filled enough - AudioTrack not started yet");
					}
				}

				writtenNow += written;
				ln -= written;
			} while (ln > 0);

			releaseSamples();
		}

		if (_isPlaying) {
			atrack.stop();
		}
		atrack.flush();
		atrack.release();

		Log.d( LOG, "run() stopped." );
	}



	////////////////////////////////////////////////////////////////////////////
	// Protected
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Acquires samples into variable lsamples.
	 * @return the actual size (in shorts) of the lsamples
	 */
	protected abstract int acquireSamples();


	/**
	 * Releases the lsamples variable.
	 * This method is called always after processing the acquired lsamples.
	 */
	protected abstract void releaseSamples();

}
