package com.hei.android.app.rthkArchivePlayer.model;

import java.io.Serializable;

public class ProgrammeModel implements Serializable{
	private static final long serialVersionUID = 1L;

	private final String _name;
	private final String _pageUrl;
	private boolean _starred;

	public ProgrammeModel(final String name, final String pageUrl) {
		_name = name;
		_pageUrl = pageUrl;
	}

	public String getName() {
		return _name;
	}

	public String getPageUrl() {
		return _pageUrl;
	}
	
	public boolean isStarred() {
		return _starred;
	}
	
	public void setStarred(boolean starred) {
		_starred = starred;
	}
}
