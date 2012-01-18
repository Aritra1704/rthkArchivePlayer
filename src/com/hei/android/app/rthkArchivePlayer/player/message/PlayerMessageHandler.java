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
package com.hei.android.app.rthkArchivePlayer.player.message;

import android.os.Handler;
import android.os.Message;

/**
 * Callback from player to GUI.
 */
public abstract class PlayerMessageHandler extends Handler {

	/**
	 * This method is called when the player is started.
	 */
	protected abstract void playerStarted();


	/**
	 * This method is called periodically by PCMFeed.
	 *
	 * @param isPlaying false means that the PCM data are being buffered,
	 *          but the audio is not playing yet
	 *
	 * @param audioBufferSizeMs the buffered audio data expressed in milliseconds of playing
	 * @param audioBufferCapacityMs the total capacity of audio buffer expressed in milliseconds of playing
	 */
	protected abstract void playerPCMFeedBuffer( boolean isPlaying, int audioBufferSizeMs, int audioBufferCapacityMs );

	
	/**
	 * This method is called when the player is stopped.
	 * Note: __after__ this method the method playerException might be also called.
	 */
	protected abstract void playerStopped();


	/**
	 * This method is called when an exception is thrown by player.
	 */
	protected abstract void playerException( Throwable t );


	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
		case PlayerMessage.START:
			playerStarted();
			break;

		case PlayerMessage.BUFFER_UPDATE:
			final boolean isPlaying = PlayerMessage.getIsPlaying(msg);
			final int audioBufferSize = PlayerMessage.getAudioBufferSize(msg);
			final int audioBufferCapacity = PlayerMessage.getAudioBufferCapacity(msg);

			playerPCMFeedBuffer(isPlaying, audioBufferSize, audioBufferCapacity);
			break;

		case PlayerMessage.STOP:
			playerStopped();
			break;

		case PlayerMessage.EXCEPTION:
			final Throwable exception = PlayerMessage.getException(msg);
			playerException(exception);
			break;

		default:
			throw new RuntimeException("Unkown message type");
		}
	}
}

