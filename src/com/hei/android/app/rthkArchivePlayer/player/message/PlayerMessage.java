package com.hei.android.app.rthkArchivePlayer.player.message;

import java.io.Serializable;

import android.os.Bundle;
import android.os.Message;


public class PlayerMessage {
	public static final int START = 0;
	public static final int BUFFER_UPDATE = 1;
	public static final int STOP = 2;
	public static final int EXCEPTION = 3;

	private static final String BUFFER_UPDATE_IS_PLAYING = "isPlaying";
	private static final String BUFFER_UPDATE_AUDIO_BUFFER_SIZE = "audioBufferSizeMs";
	private static final String BUFFER_UPDATE_AUDIO_BUFFER_CAPACITY = "audioBufferCapacityMs";

	private static final String EXCEPTION_THROWABLE = "throwable";

	public static Message createStartMessage() {
		final Message msg =  new Message();
		msg.what = START;

		return msg;
	}

	public static Message createBufferUpdateMessage(final boolean isPlaying, final int audioBufferSizeMs, final int audioBufferCapacityMs) {
		final Message msg =  new Message();
		msg.what = BUFFER_UPDATE;

		final Bundle data = new Bundle();
		data.putBoolean(BUFFER_UPDATE_IS_PLAYING, isPlaying);
		data.putInt(BUFFER_UPDATE_AUDIO_BUFFER_SIZE, audioBufferSizeMs);
		data.putInt(BUFFER_UPDATE_AUDIO_BUFFER_CAPACITY, audioBufferCapacityMs);
		msg.setData(data);

		return msg;
	}

	public static Message createStopMessage() {
		final Message msg =  new Message();
		msg.what = STOP;
		return msg;
	}

	public static Message createExceptionMessage(final Throwable t) {
		final Message msg =  new Message();
		msg.what = EXCEPTION;

		final Bundle data = new Bundle();
		data.putSerializable(EXCEPTION_THROWABLE, t);
		msg.setData(data);

		return msg;
	}

	public static boolean getIsPlaying(final Message msg) {
		if (msg.what == BUFFER_UPDATE) {
			final Bundle data = msg.getData();
			final boolean isPlaying = data.getBoolean(BUFFER_UPDATE_IS_PLAYING);
			return isPlaying;
		}
		else {
			throw new RuntimeException("Invalid message type. Not a buffer update message");
		}
	}

	public static int getAudioBufferSize(final Message msg) {
		if (msg.what == BUFFER_UPDATE) {
			final Bundle data = msg.getData();
			final int bufferSize = data.getInt(BUFFER_UPDATE_AUDIO_BUFFER_SIZE);
			return bufferSize;
		}
		else {
			throw new RuntimeException("Invalid message type. Not a buffer update message");
		}
	}

	public static int getAudioBufferCapacity(final Message msg) {
		if (msg.what == BUFFER_UPDATE) {
			final Bundle data = msg.getData();
			final int bufferCapacity = data.getInt(BUFFER_UPDATE_AUDIO_BUFFER_CAPACITY);
			return bufferCapacity;
		}
		else {
			throw new RuntimeException("Invalid message type. Not a buffer update message");
		}
	}

	public static Throwable getException(final Message msg) {
		if (msg.what == EXCEPTION) {
			final Bundle data = msg.getData();
			final Serializable serializable = data.getSerializable(EXCEPTION_THROWABLE);
			return (Throwable) serializable;
		}
		else {
			throw new RuntimeException("Invalid message type. Not a exception message");
		}
	}
}
