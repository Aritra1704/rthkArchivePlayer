package com.hei.android.app.rthkArchivePlayer.model;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EpisodeModel implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private static final SimpleDateFormat ASX_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.US);

	private final ProgrammeModel _programme;
	private final String _name;
	private final Date _date;
	private String _asxUrl = null;

	public EpisodeModel(final ProgrammeModel programme, final String name, final Date date) {
		_programme = programme;
		_name = name;
		_date = date;
	}
	
	public ProgrammeModel getProgramme() {
		return _programme;
	}

	public String getName() {
		return _name;
	}

	public Date getDate() {
		return _date;
	}

	public String getAsxUrl() {
		if(_asxUrl == null) {
//			TODO: Remove this
//			final Connection conn = Jsoup.connect(_pageUrl);
//			try {
//				final Document document = conn.get();
//				final Elements links = document.select("a");
//				for (final Element link : links) {
//					final String href = link.attr("href");
//					if (href.endsWith(".asx")) {
//						_asxUrl = href;
//						break;
//					}
//				}
//			} catch (final IOException e) {
//				e.printStackTrace();
//				return null;
//			}
			final String id = _programme.getId();
			final String date = ASX_DATE_FORMAT.format(_date);
			final String asxUrl = "http://www.rthk.org.hk/asx/rthk/" + id + "/" + date + ".asx";
			try {
				new URL(asxUrl).openStream();
			} catch (MalformedURLException e) {
				return null;
			} catch (IOException e) {
				return null;
			}
			_asxUrl = asxUrl;
		}

		return _asxUrl;
	}
	
	@Override
	public int hashCode() {
		return _programme.hashCode() + _date.hashCode() * 31;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o != null && o instanceof EpisodeModel) {
			final EpisodeModel that = (EpisodeModel) o;
			final ProgrammeModel programme = that.getProgramme();
			final Date date = that.getDate();
			
			return _programme.equals(programme) && _date.equals(date);
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return "{programme=" + _programme + ", name=" + _name + ", date=" + _date +", asxUrl=" + _asxUrl + "}";
	}
}
