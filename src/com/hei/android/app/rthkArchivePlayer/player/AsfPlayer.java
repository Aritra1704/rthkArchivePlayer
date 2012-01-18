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

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import com.hei.android.app.rthkArchivePlayer.player.decoder.AsfDecoder;
import com.hei.android.app.rthkArchivePlayer.player.decoder.Decoder;
import com.hei.android.app.rthkArchivePlayer.player.message.PlayerMessage;
import com.hei.android.app.rthkArchivePlayer.player.message.PlayerMessageHandler;
import com.hei.android.app.rthkArchivePlayer.player.pcmFeed.ArrayPCMFeed;
import com.hei.android.app.rthkArchivePlayer.player.pcmFeed.PCMFeed;

import android.os.Message;
import android.util.Log;


/**
 * This is the AsfPlayer class.
 * It uses Decoder to decode ASF stream into PCM samples.
 * This class is not thread safe.
 */
public class AsfPlayer {

	/**
	 * The default expected bitrate.
	 * Used only if not specified in play() methods.
	 */
	public static final int DEFAULT_EXPECTED_KBITSEC_RATE = 64;


	/**
	 * The default capacity of the audio buffer (AudioTrack) in ms.
	 * @see setAudioBufferCapacityMs(int)
	 */
	public static final int DEFAULT_AUDIO_BUFFER_CAPACITY_MS = 1500;


	/**
	 * The default capacity of the output buffer used for decoding in ms.
	 * @see setDecodeBufferCapacityMs(int)
	 */
	public static final int DEFAULT_DECODE_BUFFER_CAPACITY_MS = 700;


	private static final String LOG = "AACPlayer";


	////////////////////////////////////////////////////////////////////////////
	// Attributes
	////////////////////////////////////////////////////////////////////////////

	protected AsfDecoder _decoder;
	
	protected boolean _stopped;

	protected int _audioBufferCapacityMs;
	protected int _decodeBufferCapacityMs;
	protected PlayerMessageHandler _messageHandler;

	// variables used for computing average bitrate
	private int _sumKBitSecRate = 0;
	private int _countKBitSecRate = 0;
	private int _avgKBitSecRate = 0;


	////////////////////////////////////////////////////////////////////////////
	// Constructors
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new player.
	 */
	public AsfPlayer() {
		this( null );
	}


	/**
	 * Creates a new player.
	 * @param playerCallback the callback, can be null
	 */
	public AsfPlayer( final PlayerMessageHandler playerCallback ) {
		this( playerCallback, DEFAULT_AUDIO_BUFFER_CAPACITY_MS, DEFAULT_DECODE_BUFFER_CAPACITY_MS );
	}


	/**
	 * Creates a new player.
	 * @param playerCallback the callback, can be null
	 * @param audioBufferCapacityMs the capacity of the audio buffer (AudioTrack) in ms
	 * @param decodeBufferCapacityMs the capacity of the buffer used for decoding in ms
	 * @see setAudioBufferCapacityMs(int)
	 * @see setDecodeBufferCapacityMs(int)
	 */
	public AsfPlayer( final PlayerMessageHandler playerCallback, final int audioBufferCapacityMs, final int decodeBufferCapacityMs ) {
		setPlayerCallback( playerCallback );
		setAudioBufferCapacityMs( audioBufferCapacityMs );
		setDecodeBufferCapacityMs( decodeBufferCapacityMs );
		_decoder = new AsfDecoder();
	}


	////////////////////////////////////////////////////////////////////////////
	// Public
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Sets the audio buffer (AudioTrack) capacity.
	 * The capacity can be expressed in time of audio playing of such buffer.
	 * For example 1 second buffer capacity is 88100 samples for 44kHz stereo.
	 * By setting this the audio will start playing after the audio buffer is first filled.
	 *
	 * NOTE: this should be set BEFORE any of the play methods are called.
	 *
	 * @param audioBufferCapacityMs the capacity of the buffer in milliseconds
	 */
	public void setAudioBufferCapacityMs( final int audioBufferCapacityMs ) {
		this._audioBufferCapacityMs = audioBufferCapacityMs;
	}


	/**
	 * Gets the audio buffer capacity as the audio playing time.
	 * @return the capacity of the audio buffer in milliseconds
	 */
	public int getAudioBufferCapacityMs() {
		return _audioBufferCapacityMs;
	}


	/**
	 * Sets the capacity of the output buffer used for decoding.
	 * The capacity can be expressed in time of audio playing of such buffer.
	 * For example 1 second buffer capacity is 88100 samples for 44kHz stereo.
	 * Decoder tries to fill out the whole buffer in each round.
	 *
	 * NOTE: this should be set BEFORE any of the play methods are called.
	 *
	 * @param decodeBufferCapacityMs the capacity of the buffer in milliseconds
	 */
	public void setDecodeBufferCapacityMs( final int decodeBufferCapacityMs ) {
		this._decodeBufferCapacityMs = decodeBufferCapacityMs;
	}


	/**
	 * Gets the capacity of the output buffer used for decoding as the audio playing time.
	 * @return the capacity of the decoding buffer in milliseconds
	 */
	public int getDecodeBufferCapacityMs() {
		return _decodeBufferCapacityMs;
	}


	/**
	 * Sets the PlayerCallback.
	 * NOTE: this should be set BEFORE any of the play methods are called.
	 */
	public void setPlayerCallback( final PlayerMessageHandler playerCallback ) {
		this._messageHandler = playerCallback;
	}


	/**
	 * Returns the PlayerCallback or null if no PlayerCallback was set.
	 */
	public PlayerMessageHandler getPlayerCallback() {
		return _messageHandler;
	}


	/**
	 * Plays a stream asynchronously.
	 * This method starts a new thread.
	 * @param url the URL of the stream or file
	 */
	public void playAsync( final String url ) {
		playAsync( url, -1 );
	}


	/**
	 * Plays a stream asynchronously.
	 * This method starts a new thread.
	 * @param url the URL of the stream or file
	 * @param expectedKBitSecRate the expected average bitrate in kbit/sec; -1 means unknown
	 */
	public void playAsync( final String url, final int expectedKBitSecRate ) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					play( url, expectedKBitSecRate );
				}
				catch (final Exception e) {
					Log.e( LOG, "playAsync():", e);

					if (_messageHandler != null) {
						final Message msg = PlayerMessage.createExceptionMessage(e);
						_messageHandler.sendMessage(msg);
					}
				}
			}
		}).start();
	}


	/**
	 * Plays a stream synchronously.
	 * @param url the URL of the stream or file
	 */
	public void play( final String url ) throws Exception {
		play( url, -1 );
	}


	/**
	 * Plays a stream synchronously.
	 * @param url the URL of the stream or file
	 * @param expectedKBitSecRate the expected average bitrate in kbit/sec; -1 means unknown
	 */
	public void play( final String url, final int expectedKBitSecRate ) throws Exception {
		if (url.startsWith( "mms://" )) {
			play( new MMSInputStream( url ), expectedKBitSecRate );
		}
		else if (url.indexOf( ':' ) > 0) {
			final URLConnection cn = new URL( url ).openConnection();
			cn.connect();

			dumpHeaders( cn );

			// TODO: try to get the expectedKBitSecRate from headers
			play( cn.getInputStream(), expectedKBitSecRate);
		} else {
			play( new FileInputStream( url ), expectedKBitSecRate );
		}
	}


	/**
	 * Plays a stream synchronously.
	 * @param is the input stream
	 */
	public void play( final InputStream is ) throws Exception {
		play( is, -1 );
	}


	/**
	 * Plays a stream synchronously.
	 * @param is the input stream
	 * @param expectedKBitSecRate the expected average bitrate in kbit/sec; -1 means unknown
	 */
	public final void play( final InputStream is, int expectedKBitSecRate ) throws Exception {
		_stopped = false;

		if (_messageHandler != null) {
			final Message msg = PlayerMessage.createStartMessage();
			_messageHandler.sendMessage(msg);
		}

		if (expectedKBitSecRate <= 0) {
			expectedKBitSecRate = DEFAULT_EXPECTED_KBITSEC_RATE;
		}

		_sumKBitSecRate = 0;
		_countKBitSecRate = 0;

		playImpl( is, expectedKBitSecRate );
	}


	/**
	 * Stops the execution thread.
	 */
	public void stop() {
		_stopped = true;
	}


	////////////////////////////////////////////////////////////////////////////
	// Protected
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Plays a stream synchronously.
	 * This is the implementation method calle by every play() and playAsync() methods.
	 * @param is the input stream
	 * @param expectedKBitSecRate the expected average bitrate in kbit/sec
	 */
	protected void playImpl( final InputStream is, int expectedKBitSecRate ) throws Exception {
		final ArrayBufferReader reader = new ArrayBufferReader(
				computeInputBufferSize( expectedKBitSecRate, _decodeBufferCapacityMs ),
				is );
		new Thread( reader ).start();

		ArrayPCMFeed pcmfeed = null;
		Thread pcmfeedThread = null;

		// profiling info
		long profMs = 0;
		long profSamples = 0;
		long profSampleRate = 0;
		int profCount = 0;

		try {
			Decoder.Info info = _decoder.start( reader );

			Log.d( LOG, "play(): samplerate=" + info.getSampleRate() + ", channels=" + info.getChannels());

			profSampleRate = info.getSampleRate() * info.getChannels();

			if (info.getChannels() > 2) {
				throw new RuntimeException("Too many channels detected: " + info.getChannels());
			}

			// 3 buffers for result samples:
			//   - one is used by decoder
			//   - one is used by the PCMFeeder
			//   - one is enqueued / passed to PCMFeeder - non-blocking op
			final short[][] decodeBuffers = createDecodeBuffers( 3, info );
			short[] decodeBuffer = decodeBuffers[0];
			int decodeBufferIndex = 0;

			pcmfeed = createArrayPCMFeed( info );
			pcmfeedThread = new Thread( pcmfeed );
			pcmfeedThread.start();

			do {
				final long tsStart = System.currentTimeMillis();

				info = _decoder.decode( decodeBuffer, decodeBuffer.length );
				final int nsamp = info.getRoundSamples();

				profMs += System.currentTimeMillis() - tsStart;
				profSamples += nsamp;
				profCount++;

				Log.d( LOG, "play(): decoded " + nsamp + " samples" );

				if (nsamp == 0 || _stopped) {
					break;
				}
				if (!pcmfeed.feed( decodeBuffer, nsamp ) || _stopped) {
					break;
				}

				final int kBitSecRate = computeAvgKBitSecRate( info );
				if (Math.abs(expectedKBitSecRate - kBitSecRate) > 1) {
					Log.i( LOG, "play(): changing kBitSecRate: " + expectedKBitSecRate + " -> " + kBitSecRate );
					reader.setCapacity( computeInputBufferSize( kBitSecRate, _decodeBufferCapacityMs ));
					expectedKBitSecRate = kBitSecRate;
				}

				decodeBuffer = decodeBuffers[ ++decodeBufferIndex % 3 ];
			} while (!_stopped);
		}
		finally {
			_stopped = true;

			if (pcmfeed != null) {
				pcmfeed.stop();
			}
			_decoder.stop();
			reader.stop();

			int perf = 0;

			if (profCount > 0) {
				Log.i( LOG, "play(): average decoding time: " + profMs / profCount + " ms");
			}

			if (profMs > 0) {
				perf = (int)((1000*profSamples / profMs - profSampleRate) * 100 / profSampleRate);

				Log.i( LOG, "play(): average rate (samples/sec): audio=" + profSampleRate
						+ ", decoding=" + (1000*profSamples / profMs)
						+ ", audio/decoding= " + perf
						+ " %  (the higher, the better; negative means that decoding is slower than needed by audio)");
			}

			if (pcmfeedThread != null) {
				pcmfeedThread.join();
			}

			if (_messageHandler != null) {
				final Message msg = PlayerMessage.createStopMessage();
				_messageHandler.sendMessage(msg);
			}
		}
	}


	protected void dumpHeaders( final URLConnection cn ) {
		for (final java.util.Map.Entry<String, java.util.List<String>> me : cn.getHeaderFields().entrySet()) {
			for (final String s : me.getValue()) {
				Log.d( LOG, "header: key=" + me.getKey() + ", val=" + s);

			}
		}
	}


	protected int computeAvgKBitSecRate( final Decoder.Info info ) {
		// do not change the value after a while - avoid changing of the out buffer:
		if (_countKBitSecRate < 64) {
			final int kBitSecRate = computeKBitSecRate( info );
			final int frames = info.getRoundFrames();

			_sumKBitSecRate += kBitSecRate * frames;
			_countKBitSecRate += frames;
			_avgKBitSecRate = _sumKBitSecRate / _countKBitSecRate;
		}

		return _avgKBitSecRate;
	}


	protected static int computeKBitSecRate( final Decoder.Info info ) {
		if (info.getRoundSamples() <= 0) {
			return -1;
		}

		return computeKBitSecRate( info.getRoundBytesConsumed(), info.getRoundSamples(),
				info.getSampleRate(), info.getChannels());
	}


	protected static int computeKBitSecRate( final int bytesconsumed, final int samples, final int sampleRate, final int channels ) {
		final long ret = 8L * bytesconsumed * channels * sampleRate / samples;

		return (((int)ret) + 500) / 1000;
	}


	protected static int computeInputBufferSize( final int kbitSec, final int durationMs ) {
		return kbitSec * durationMs / 8;
	}


	protected static int computeInputBufferSize( final Decoder.Info info, final int durationMs ) {

		return computeInputBufferSize( info.getRoundBytesConsumed(), info.getRoundSamples(),
				info.getSampleRate(), info.getChannels(), durationMs );
	}


	protected static int computeInputBufferSize( final int bytesconsumed, final int samples,
			final int sampleRate, final int channels, final int durationMs ) {

		return (int)(((long) bytesconsumed) * channels * sampleRate * durationMs  / (1000L * samples));
	}
	

	////////////////////////////////////////////////////////////////////////////
	// Private
	////////////////////////////////////////////////////////////////////////////
	
	private short[][] createDecodeBuffers( final int count, final Decoder.Info info ) {
		final int size = PCMFeed.msToSamples( _decodeBufferCapacityMs, info.getSampleRate(), info.getChannels());

		final short[][] ret = new short[ count ][];

		for (int i=0; i < ret.length; i++) {
			ret[i] = new short[ size ];
		}

		return ret;
	}


	private ArrayPCMFeed createArrayPCMFeed( final Decoder.Info info ) {
		final int size = PCMFeed.msToBytes( _audioBufferCapacityMs, info.getSampleRate(), info.getChannels());

		return new ArrayPCMFeed( info.getSampleRate(), info.getChannels(), size, _messageHandler );
	}


}
