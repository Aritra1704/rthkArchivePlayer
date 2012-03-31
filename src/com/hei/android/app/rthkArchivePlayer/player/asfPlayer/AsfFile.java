package com.hei.android.app.rthkArchivePlayer.player.asfPlayer;

class AsfFile {
	private final String _url;
	private final String _path;
	private final double _length;
	private final long _size;

	public AsfFile(final String url, final String path, final double length, final long size) {
		_url = url;
		_path = path;
		_length = length;
		_size = size;
	}

	public String getUrl() {
		return _url;
	}

	public String getPath() {
		return _path;
	}

	public double getLength() {
		return _length;
	}

	public long getSize() {
		return _size;
	}
}
