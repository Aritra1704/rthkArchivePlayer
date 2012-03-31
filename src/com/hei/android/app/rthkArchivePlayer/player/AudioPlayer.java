package com.hei.android.app.rthkArchivePlayer.player;

public interface AudioPlayer {
	public void playAsync(String uri);
	
	public void play(String uri);

	public void seek( final double sec );

	public void pause();

	public void resume();

	/**
	 * Stops the execution thread.
	 */
	public void stop();
}
