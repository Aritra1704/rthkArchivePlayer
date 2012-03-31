package com.hei.android.app.rthkArchivePlayer.player.asfPlayer;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Message;
import android.util.Log;

import com.hei.android.app.rthkArchivePlayer.player.AudioPlayer;
import com.hei.android.app.rthkArchivePlayer.player.decoder.AsfDecoder;
import com.hei.android.app.rthkArchivePlayer.player.decoder.Decoder.Info;
import com.hei.android.app.rthkArchivePlayer.player.message.PlayerMessage;
import com.hei.android.app.rthkArchivePlayer.player.message.PlayerMessageHandler;

public class AsfPlayer implements AudioPlayer, AudioTrack.OnPlaybackPositionUpdateListener {

	private static final String LOG = "AsfPlayer";

	/**
	 * The default expected bitrate.
	 * Used only if not specified in play() methods.
	 */
	public static final int DEFAULT_EXPECTED_KBITSEC_RATE = 64;

	/**
	 * The default capacity of the output buffer used for decoding in ms.
	 * @see setDecodeBufferCapacityMs(int)
	 */
	public static final int DEFAULT_DECODE_BUFFER_CAPACITY_MS = 1500;

	private final Context _context;
	private boolean _stop;
	private PlayerMessageHandler _handler;
	private int _decodeBufferCapacityMs;
	private AudioTrack _audioTrack = null;
	private int _sampleRate;
	private int _channels;

	public AsfPlayer(final Context context, final PlayerMessageHandler playerCallback) {
		this(context, playerCallback, -1);
	}
	
	public AsfPlayer(final Context context, final PlayerMessageHandler playerCallback, final int decodeBufferCapacityMs ) {
		_context = context;
		_handler = playerCallback;
		_decodeBufferCapacityMs = decodeBufferCapacityMs > 0 ? decodeBufferCapacityMs : DEFAULT_DECODE_BUFFER_CAPACITY_MS;
	}

	@Override
	public void playAsync(final String uri) {
		try {
			final AsfFileInputStream asfStream = AsfFileInputStream.createAsfFileInputStream(_context, uri);
			new Thread("AudioPlayer"){
				public void run() {
					play(asfStream);
				}
			}.start();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void play(String uri) {
		try {
			final AsfFileInputStream asfStream = AsfFileInputStream.createAsfFileInputStream(_context, uri);
			play(asfStream);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public void play(AsfFileInputStream asfStream) {
		final AsfDecoder decoder = new AsfDecoder();
		final int bufferSize = computeInputBufferSize(DEFAULT_EXPECTED_KBITSEC_RATE, _decodeBufferCapacityMs);
		Info info = decoder.start(new AsfFileBufferReader(bufferSize, asfStream));

		_sampleRate = info.getSampleRate();
		_channels = info.getChannels();

		_audioTrack = new AudioTrack(
				AudioManager.STREAM_MUSIC,
				_sampleRate,
				_channels == 1 ?
						AudioFormat.CHANNEL_CONFIGURATION_MONO :
							AudioFormat.CHANNEL_CONFIGURATION_STEREO,
							AudioFormat.ENCODING_PCM_16BIT,
							bufferSize,
							AudioTrack.MODE_STREAM );

		_audioTrack.setPlaybackPositionUpdateListener( this );
		_audioTrack.setPositionNotificationPeriod( msToSamples( 200, _sampleRate, _channels ));
		
		final long startTime = System.currentTimeMillis();
		boolean playing = false;
		while (!_stop) {
			final int size = msToSamples( _decodeBufferCapacityMs, _sampleRate, _channels);
			short[] samples = new short[size];
			info = decoder.decode(samples, size);

			int remaining = info.getRoundSamples();
			Log.d(LOG, "Decoded " + remaining);

			int totalWritten = 0;
			
			// samples written to AudioTrack in this round:
			int writtenNow = 0;
			do {
				if (writtenNow != 0) {
					Log.d( LOG, "too fast for playback, sleeping...");
					try { Thread.sleep( 50 ); } catch (final InterruptedException e) {}
				}

				final int written = _audioTrack.write( samples, writtenNow, remaining );
				totalWritten += written;
				
				if (written < 0) {
					Log.e( LOG, "error in playback feed: " + written );
					_stop = true;
					break;
				}

				Log.d( LOG, "Written " + written);

				if (!playing) {
					Log.d( LOG, "start of AudioTrack - buffered " + totalWritten + " samples");
					_audioTrack.play();
					playing = true;
					
					double length;
					try {
						length = asfStream.getLength();
					} catch (IOException e) {
						length = 0;
						Log.e(LOG, "Failed to get the lenght of the AsfFileInputStream");
					}
					final Message msg = PlayerMessage.createStartMessage(length);
					_handler.sendMessage(msg);
				}
				else {
					Log.d( LOG, "start buffer not filled enough - AudioTrack not started yet");
				}

				writtenNow += written;
				remaining -= written;
			} while (remaining > 0);

		}
		
		final long endTime = System.currentTimeMillis();
		final long runTime = startTime - endTime;
		Log.d("TIME", "" + runTime);
	}
	

	@Override
	public void seek(double sec) {
		if(_audioTrack != null) {
			int ms = (int) (sec * 1000);
			final int bytes = msToBytes(ms, _sampleRate, _channels);
			_audioTrack.stop();
			final int retval = _audioTrack.setPlaybackHeadPosition(bytes);
			switch (retval) {
			case AudioTrack.SUCCESS:
				Log.d(LOG, "Seek success");
				break;
			case AudioTrack.ERROR_BAD_VALUE:
				Log.e(LOG, "Failed to set the play back head position (bad value)");
				break;
			case AudioTrack.ERROR_INVALID_OPERATION:
				Log.e(LOG, "Failed to set the play back head position (invalid operation)");
			}
			_audioTrack.play();
		}
	}

	@Override
	public void pause() {
		if(_audioTrack != null) {
			_audioTrack.pause();
		}
	}

	@Override
	public void resume() {
		if(_audioTrack != null) {
			_audioTrack.play();
		}
	}

	@Override
	public void stop() {
		_stop = true;
		_audioTrack.stop();
		_audioTrack.release();
		
		final Message msg = PlayerMessage.createStopMessage();
		_handler.sendMessage(msg);
	}

	@Override
	public void onMarkerReached(AudioTrack arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPeriodicNotification(AudioTrack track) {
		final int bytes = track.getPlaybackHeadPosition();
		final int time = bytesToMs(bytes, _sampleRate, _channels);
		
		final Message msg = PlayerMessage.createCurrentPosUpdateMessage(time / 1000.0);
		_handler.sendMessage(msg);
	}

	protected static int computeInputBufferSize( final int kbitSec, final int durationMs ) {
		return kbitSec * durationMs / 8;
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
}
