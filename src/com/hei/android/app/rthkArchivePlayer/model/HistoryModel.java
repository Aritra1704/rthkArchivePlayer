package com.hei.android.app.rthkArchivePlayer.model;

import java.util.Date;

public class HistoryModel {
	private final Date accessTime;
	private final EpisodeModel episode;

	public HistoryModel(final Date accessTime, final EpisodeModel episode) {
		this.accessTime = accessTime;
		this.episode = episode;
	}

	public Date getAccessTime() {
		return accessTime;
	}

	public EpisodeModel getEpisode() {
		return episode;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o != null && o instanceof HistoryModel) {
			final HistoryModel model = (HistoryModel) o;
			return episode.equals(model.getEpisode());
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return episode.hashCode();
	}
	
	@Override
	public String toString() {
		return "{accessTime=" + accessTime + ", episode=" + episode + "}";
	}
}
