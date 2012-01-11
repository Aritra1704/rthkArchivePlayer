package com.hei.android.android.rthkArchiveListener.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class AsxModel {
	private final String _title;
	private final String _abstract;
	private final String _author;
	private final List<AsxEntryModel> _entries;

	public static AsxModel createModelFromUrl(final String url) {
		try {
			/* Create a URL we want to load some xml-data from. */
			final URL asxUrl = new URL(url);

			/* Get a SAXParser from the SAXPArserFactory. */
			final SAXParserFactory spf = SAXParserFactory.newInstance();
			final SAXParser sp = spf.newSAXParser();

			/* Get the XMLReader of the SAXParser we created. */
			final XMLReader xr = sp.getXMLReader();
			/* Create a new ContentHandler and apply it to the XML-Reader*/
			final AsxHandler handeler = new AsxHandler();
			xr.setContentHandler(handeler);

			/* Parse the xml-data from our URL. */
			final InputStream stream = asxUrl.openStream();
			final AsxXmlAdaptInputStream xmlStream = new AsxXmlAdaptInputStream(stream);
			final InputSource source = new InputSource(xmlStream);
			source.setEncoding("UTF-8");
			xr.parse(source);
			/* Parsing has finished. */

			/* Our Handler now provides the parsed data to us. */
			final AsxModel model = handeler.getModel();
			return model;
		} catch (final Exception e) {
			throw new RuntimeException("Failed to parse " + url, e);

		}
	}

	public AsxModel(final String title, final String author, final String asxAbstract, final List<AsxEntryModel> entries) {
		_title = title;
		_author = author;
		_abstract = asxAbstract;
		_entries = entries;
	}

	public String getTitle() {
		return _title;
	}

	public String getAuthor() {
		return _author;
	}

	public String getAbstract() {
		return _abstract;
	}

	public List<AsxEntryModel> getEntries() {
		return _entries;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("title = " + _title + "; ");
		builder.append("abstract = " + _abstract + "; ");
		builder.append("author = " + _author + "; ");
		builder.append("entries = {");

		String delim = "";
		for (final AsxEntryModel entryModel : _entries) {
			builder.append(delim).append(entryModel);
			delim = ", ";
		}
		builder.append("}");

		return builder.toString();
	}

	public static class AsxEntryModel {
		private final String _title;
		private final String _abstract;
		private final String _author;
		private final String _ref;

		public AsxEntryModel(final String title, final String author, final String entryAbstract, final String ref) {
			_title = title;
			_author = author;
			_abstract = entryAbstract;
			_ref = ref;
		}

		public String getTitle() {
			return _title;
		}

		public String getAuthor() {
			return _author;
		}

		public String getAbstract() {
			return _abstract;
		}

		public String getRef() {
			return _ref;
		}

		@Override
		public String toString() {
			return "title = " + _title + "; " +
					"abstract = " + _abstract + "; " +
					"author = " + _author + "; " +
					"ref = " + _ref;
		}
	}

	private static class AsxXmlAdaptInputStream extends InputStream {
		private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";

		private final InputStream _stream;

		public AsxXmlAdaptInputStream(final InputStream stream) {
			final StringBuilder big5Builder = new StringBuilder(XML_HEADER);
			final byte[] buffer = new byte[1024];
			int length = safeRead(stream, buffer);

			while (length != -1) {
				big5Builder.append(new String(buffer, 0, length, Charset.forName("Big5")));
				length = safeRead(stream, buffer);
			}

			final String big5Asx = big5Builder.toString();
			Log.d("ASX", big5Asx);

			byte[] utf8AsxBytes;
			try {
				utf8AsxBytes = big5Asx.getBytes("UTF-8");
			} catch (final UnsupportedEncodingException e) {
				utf8AsxBytes = new byte[0];
			}

			_stream = new ByteArrayInputStream(utf8AsxBytes);
		}

		private int safeRead(final InputStream stream, final byte[] buffer) {
			try {
				final int length = stream.read(buffer);
				return length;
			} catch (final IOException e) {
				return -1;
			}
		}

		@Override
		public int read() throws IOException {
			final int read = _stream.read();
			return read;
		}

	}

	private static class AsxHandler extends DefaultHandler{
		private static final String ASX = "asx";
		private static final String ENTRY = "entry";
		private static final String TITLE = "title";
		private static final String ABSTRACT = "abstract";
		private static final String AUTHOR = "author";
		private static final String REF = "ref";

		private boolean _inAsx = false;
		private boolean _inEntry = false;
		private boolean _inTitle = false;
		private boolean _inAbstract = false;
		private boolean _inAuthor = false;
		private boolean _inRef = false;

		private final Map<String, String> _cache;
		private final List<AsxEntryModel> _entries;
		private AsxModel _asxModel;

		public AsxHandler() {
			super();

			_cache = new HashMap<String, String>();
			_entries = new LinkedList<AsxModel.AsxEntryModel>();
		}

		@Override
		public void startElement(final String uri, final String localName, final String name,
				final Attributes attributes) throws SAXException {
			if (ASX.equalsIgnoreCase(localName)){
				_inAsx = true;
			}
			else if (ENTRY.equalsIgnoreCase(localName)){
				_inEntry = true;
			}
			else if (TITLE.equalsIgnoreCase(localName)){
				_inTitle = true;
			}
			else if (ABSTRACT.equalsIgnoreCase(localName)){
				_inAbstract = true;
			}
			else if (AUTHOR.equalsIgnoreCase(localName)){
				_inAuthor = true;
			}
			else if (REF.equalsIgnoreCase(localName)){
				_inRef = true;

				final String href = attributes.getValue("href");
				_cache.put(REF, href);
			}
		}

		@Override
		public void characters(final char[] ch, final int start, final int length)
				throws SAXException {
			final String value = new String(ch, start, length);

			if(_inAsx) {
				if(!_inEntry) {
					if (_inTitle) {
						_cache.put(ASX+TITLE, value);
						return;
					}

					if (_inAuthor) {
						_cache.put(ASX+AUTHOR, value);
						return;
					}
					if (_inAbstract) {
						_cache.put(ASX+ABSTRACT, value);
						return;
					}
				}
				else {
					if (_inTitle) {
						_cache.put(TITLE, value);
						return;
					}

					if (_inAuthor) {
						_cache.put(AUTHOR, value);
						return;
					}

					if (_inAbstract) {
						_cache.put(ABSTRACT, value);
						return;
					}

					if (_inRef) {
						_cache.put(REF, value);
						return;
					}
				}
			}

		}

		@Override
		public void endElement(final String uri, final String localName, final String name)
				throws SAXException {
			if (ASX.equalsIgnoreCase(localName)){
				_inAsx = false;
			}
			else if (ENTRY.equalsIgnoreCase(localName)){
				_inEntry = false;

				final String title = _cache.get(TITLE);
				final String entryAbstract = _cache.get(ABSTRACT);
				final String author = _cache.get(AUTHOR);
				final String ref = _cache.get(REF);

				final AsxEntryModel entry = new AsxEntryModel(title, author, entryAbstract, ref);
				_entries.add(entry);
			}
			else if (TITLE.equalsIgnoreCase(localName)){
				_inTitle = false;
			}
			else if (ABSTRACT.equalsIgnoreCase(localName)){
				_inAbstract = false;
			}
			else if (AUTHOR.equalsIgnoreCase(localName)){
				_inAuthor = false;
			}
			else if (REF.equalsIgnoreCase(localName)){
				_inRef = false;


				final String title = _cache.get(ASX+TITLE);
				final String author = _cache.get(ASX+AUTHOR);
				final String asxAbstract = _cache.get(ASX+ABSTRACT);
				_asxModel = new AsxModel(title, author, asxAbstract, _entries);
			}
		}

		public AsxModel getModel() {
			return _asxModel;
		}
	}
}
