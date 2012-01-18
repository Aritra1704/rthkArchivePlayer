package com.hei.android.app.rthkArchivePlayer.model;

import java.io.Serializable;

public class BasicProgrammeModel implements Serializable{
	private static final long serialVersionUID = 1L;

	final String _name;
	final String _pageUrl;

	public BasicProgrammeModel(final String name, final String pageUrl) {
		_name = name;
		_pageUrl = pageUrl;
	}

	public String getName() {
		return _name;
	}

	public String getPageUrl() {
		return _pageUrl;
	}
}
