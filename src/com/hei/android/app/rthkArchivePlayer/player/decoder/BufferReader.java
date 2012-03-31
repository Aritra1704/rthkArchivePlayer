package com.hei.android.app.rthkArchivePlayer.player.decoder;


public interface BufferReader {
	public Buffer next();

	public static class Buffer {
		private final byte[] _data;
		private int _size;

		public Buffer( final int capacity ) {
			_data = new byte[ capacity ];
		}

		public byte[] getData() {
			return _data;
		}

		public int getSize() {
			return _size;
		}

		public void setSize(final int size) {
			_size = size;
		}
	}
}
