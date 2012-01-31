package com.hei.android.app.rthkArchivePlayer.player.message;

import java.io.Serializable;

import android.os.Bundle;
import android.os.Message;


public class PlayerMessage {
	public static final int START = 0;
	public static final int BUFFER_UPDATE = 1;
	public static final int STOP = 2;
	public static final int EXCEPTION = 3;
	public static final int CURRENT_POS_UPDATE = 4;
	
	private static final String START_LENGTH = "length";

	private static final String BUFFER_UPDATE_IS_PLAYING = "isPlaying";
	private static final String BUFFER_UPDATE_AUDIO_BUFFER_SIZE = "audioBufferSizeMs";
	private static final String BUFFER_UPDATE_AUDIO_BUFFER_CAPACITY = "audioBufferCapacityMs";

	private static final String EXCEPTION_THROWABLE = "throwable";

	private static final String CURRENT_POS_UPDATE_POS = "pos";

	public static Message createStartMessage(final double length) {
		final Message msg =  new Message();
		msg.what = START;

		final Bundle data = new Bundle();
		data.putDouble(START_LENGTH, length);
		msg.setData(data);
		
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
	
	public static Message createCurrentPosUpdateMessage(final double pos) {
		final Message msg = new Message();
		msg.what = CURRENT_POS_UPDATE;
		
		final Bundle data = new Bundle();
		data.putDouble(CURRENT_POS_UPDATE_POS, pos);
		msg.setData(data);
		
		return msg;
	}

	public static double getLength(final Message msg) {
		if (msg.what == START) {
			final Bundle data = msg.getData();
			final double length = data.getDouble(START_LENGTH);
			return length;
		}
		else {
			throw new IllegalArgumentException("Invalid message type. Not a start message");
		}
	}

	public static boolean getIsPlaying(final Message msg) {
		if (msg.what == BUFFER_UPDATE) {
			final Bundle data = msg.getData();
			final boolean isPlaying = data.getBoolean(BUFFER_UPDATE_IS_PLAYING);
			return isPlaying;
		}
		else {
			throw new IllegalArgumentException("Invalid message type. Not a buffer update message");
		}
	}

	public static int getAudioBufferSize(final Message msg) {
		if (msg.what == BUFFER_UPDATE) {
			final Bundle data = msg.getData();
			final int bufferSize = data.getInt(BUFFER_UPDATE_AUDIO_BUFFER_SIZE);
			return bufferSize;
		}
		else {
			throw new IllegalArgumentException("Invalid message type. Not a buffer update message");
		}
	}

	public static int getAudioBufferCapacity(final Message msg) {
		if (msg.what == BUFFER_UPDATE) {
			final Bundle data = msg.getData();
			final int bufferCapacity = data.getInt(BUFFER_UPDATE_AUDIO_BUFFER_CAPACITY);
			return bufferCapacity;
		}
		else {
			throw new IllegalArgumentException("Invalid message type. Not a buffer update message");
		}
	}

	public static Throwable getException(final Message msg) {
		if (msg.what == EXCEPTION) {
			final Bundle data = msg.getData();
			final Serializable serializable = data.getSerializable(EXCEPTION_THROWABLE);
			return (Throwable) serializable;
		}
		else {
			throw new IllegalArgumentException("Invalid message type. Not a exception message");
		}
	}

	public static double getCurrentPos(final Message msg) {
		if (msg.what == CURRENT_POS_UPDATE) {
			final Bundle data = msg.getData();
			final double pos = data.getDouble(CURRENT_POS_UPDATE_POS);
			return pos;
		}
		else {
			throw new IllegalArgumentException("Invalid message type. Not a current position update message");
		}
	}
}
