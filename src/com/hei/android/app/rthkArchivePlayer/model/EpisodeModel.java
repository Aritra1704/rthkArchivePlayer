package com.hei.android.app.rthkArchivePlayer.model;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EpisodeModel implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String _name;
	private final String _pageUrl;
	private final Date _date;
	private String _asxUrl = null;

	public EpisodeModel(final String name, final String pageUrl, final Date date) {
		_name = name;
		_pageUrl = pageUrl;
		_date = date;
	}

	public String getName() {
		return _name;
	}

	public String getPageUrl() {
		return _pageUrl;
	}

	public Date getDate() {
		return _date;
	}

	public String getAsxUrl() {
		if(_asxUrl == null) {
			final Connection conn = Jsoup.connect(_pageUrl);
			try {
				final Document document = conn.get();
				final Elements links = document.select("a");
				for (final Element link : links) {
					final String href = link.attr("href");
					if (href.endsWith(".asx")) {
						_asxUrl = href;
						break;
					}
				}
			} catch (final IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		return _asxUrl;
	}
}
