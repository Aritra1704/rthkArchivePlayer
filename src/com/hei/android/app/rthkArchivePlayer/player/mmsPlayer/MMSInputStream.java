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
package com.hei.android.app.rthkArchivePlayer.player.mmsPlayer;

import java.io.IOException;
import java.io.InputStream;

import android.util.Log;


/**
 * This is a MMS input stream - reads data from a MMS stream.
 * <pre>
 *  MMSInputStream mmsis = new MMSInputStream( "mms://..." );
 *  ...
 *  mmsis.read( buffer );
 *  ...
 *  mmsis.close();
 * </pre>
 */
public class MMSInputStream extends InputStream {

	private static final String LOG = "MMSInputStream";

	private static boolean libLoaded = false;
	

	////////////////////////////////////////////////////////////////////////////
	// Attributes
	////////////////////////////////////////////////////////////////////////////

	/**
	 * The native handler.
	 */
	private final int _nativeHandlerPtr;
	
	/**
	 * Header
	 */
	private final byte[] _header;
	
	/**
	 * Position of current read head
	 */
	private int _headerReadPos;

	////////////////////////////////////////////////////////////////////////////
	// Constructors
	////////////////////////////////////////////////////////////////////////////

	public MMSInputStream( final String url ) throws IOException {
		ensureLibLoaded();

		_nativeHandlerPtr = nativeConnect( url );
		_header = nativeGetHeader(_nativeHandlerPtr);
		_headerReadPos = _header.length;
	}


	////////////////////////////////////////////////////////////////////////////
	// Public - InputStream
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Please do not use this method - this is not efficient !
	 */
	@Override
	public int read() throws IOException {
		final byte[] buf = new byte[1];

		int n;
		while ((n = read( buf, 0, 1)) == 0) {
		}

		if (n == 1) {
			return buf[0] & 0xff;
		}
		return n;
	}


	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		if(_headerReadPos < _header.length) {
			int pos = 0;
			for(_headerReadPos += off; pos < len && _headerReadPos < _header.length; pos++, _headerReadPos++) {
				b[pos] = _header[_headerReadPos];
			}
			return pos;
		}
		
		return nativeRead( _nativeHandlerPtr, b, off, len );
	}


	@Override
	public void close() throws IOException {
		nativeClose( _nativeHandlerPtr );
	}
	
	
	
	/**
	 * Get the length in seconds of the file
	 * @throws IOException
	 */
	public double getLength() throws IOException {
		final double length = nativeGetLength(_nativeHandlerPtr);
		
		if (length < 0) {
			throw new IOException("Failed to get the length of the file.");
		}
		
		return length;
	}
	
	
	
	public boolean seek(double time) {
		final boolean sought;
		try {
			sought = nativeSeek(_nativeHandlerPtr, time);
		} catch (IOException e) {
			return false;
		}
		
		if (sought) {
			_headerReadPos = 0;
		}
		
		return sought; 	
	}
	
	
	
	public long getSize() throws IOException {
		final long size = nativeGetSize(_nativeHandlerPtr);
		
		return size;
	}
	
	
	
	public long seekByte(long bytes) {
		try {
			final long pos = nativeSeekByte(_nativeHandlerPtr, bytes);
			return pos;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	
	public long getCurrentPos() {
		return nativeGetCurrentPos(_nativeHandlerPtr);
	}
	
	////////////////////////////////////////////////////////////////////////////
	// Private
	////////////////////////////////////////////////////////////////////////////

	/**
	 * This is a conversion method used by libmms.
	 */
	private static synchronized void ensureLibLoaded() {
		if (!libLoaded) {
			System.loadLibrary( "mms" );
			libLoaded = true;
		}
	}


	private static final String CHARSET_NAME_UNICODE = "UTF-16LE";

	/**
	 * This is a conversion method used by libmms.
	 */
	private static byte[] string2unicode( final String s ) {
		Log.d( LOG, "string2unicode(): '" + s  + "'" );

		// return s.getBytes( CHARSET_UNICODE );

		try {
			return s.getBytes( CHARSET_NAME_UNICODE );
		}
		catch (final java.io.UnsupportedEncodingException e) {
			Log.e( LOG, "Cannot convert string --> unicode", e );
			throw new RuntimeException( e );
		}
	}

	/**
	 * This is a conversion method used by libmms.
	 */
	private static String unicode2string( final byte[] bytes ) {
		try {
			final String ret = new String( bytes, CHARSET_NAME_UNICODE );
			Log.d( LOG, "unicode2string(): '" + ret  + "'" );

			return ret;
		}
		catch (final java.io.UnsupportedEncodingException e) {
			Log.e( LOG, "Cannot convert unicode --> string", e );
			throw new RuntimeException( e );
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		nativeClose(_nativeHandlerPtr);
	};


	private native int nativeConnect( String url ) throws IOException;
	
	private native int nativeRead( int nativeHandlerPtr, byte[] b, int off, int len ) throws IOException;
	
	private native void nativeClose( int nativeHandlerPtr ) throws IOException;
	
	private native double nativeGetLength( int nativeHandlerPtr ) throws IOException;
	
	private native boolean nativeSeek( int nativeHandlerPtr, double time ) throws IOException;
	
	private native byte[] nativeGetHeader(int nativeHandlerPtr) throws IOException;
	
	private native long nativeGetSize( int nativeHandlerPtr ) throws IOException;
	
	private native long nativeSeekByte( int nativeHandlerPtr, long bytes ) throws IOException;
	
	private native long nativeGetCurrentPos ( int nativeHandlerPtr );

}
