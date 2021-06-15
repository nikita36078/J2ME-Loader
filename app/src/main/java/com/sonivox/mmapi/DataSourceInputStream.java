package com.sonivox.mmapi;

import java.io.*;

import javax.microedition.media.protocol.*;

/**
 * A DataSource and SourceStream implementation wrapping an InputStream as
 * source of the media data.
 * <p>
 * This DataSource's locator is always null.
 */
class DataSourceInputStream extends DataSourceBase implements SourceStream {

	/**
	 * The current read position, in bytes.
	 */
	private long pos; // = 0

	/**
	 * The input stream to read from.
	 */
	private InputStream inputStream; // = null

	/**
	 * Constructor with InputStream and content type parameters. The locator
	 * will be set to null.
	 * 
	 * @param is the input stream to read data from (may be null)
	 * @param contentType the MIME type of this data source
	 */
	public DataSourceInputStream(InputStream is, String contentType) {
		this(is, null, contentType);
	}

	/**
	 * Internal constructor for sub classes with InputStream, locator and
	 * content type parameters.
	 * 
	 * @param locator the http locator
	 * @param contentType the MIME type of this data source
	 */
	protected DataSourceInputStream(InputStream is, String locator,
			String contentType) {
		super(locator, contentType);
		this.inputStream = is;
	}

	/**
	 * Internal accessor method for subclasses.
	 * 
	 * @return the inputStream
	 */
	protected InputStream getInputStream() {
		return inputStream;
	}

	/**
	 * @param inputStream the inputStream to set
	 */
	protected void setInputStream(InputStream inputStream) {
		if (this.inputStream != inputStream) {
			this.inputStream = inputStream;
			pos = 0;
		}
	}

	// IMPLEMENTATION OF DATASOURCEBASE METHODS

	/*
	 * (non-Javadoc) @throws IOException if inputStream is null
	 * 
	 * @see com.sonivox.mmapi.DataSourceBase#connectImpl()
	 */
	protected void connectImpl() throws IOException {
		// cannot connect if the stream is not set.
		if (inputStream == null) {
			throw new IOException("cannot re-open stream");
		}
		// otherwise nothing to do, the InputStream is already open
	}

	/*
	 * (non-Javadoc) Closes inputStream and sets it to null.
	 * 
	 * @see com.sonivox.mmapi.DataSourceBase#disconnectImpl()
	 */
	protected void disconnectImpl() {
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException ioe) {
				// nothing to do
			}
			inputStream = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sonivox.mmapi.DataSourceBase#startImpl()
	 */
	protected void startImpl() throws IOException {
		// nothing to do
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sonivox.mmapi.DataSourceBase#stopImpl()
	 */
	protected void stopImpl() throws IOException {
		// nothing to do
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sonivox.mmapi.DataSourceBase#getStream()
	 */
	SourceStream getStream() {
		return this;
	}

	// IMPLEMENTATION OF SOURCESTREAM METHODS

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.protocol.SourceStream#getContentDescriptor()
	 */
	public ContentDescriptor getContentDescriptor() {
		return new ContentDescriptor(getContentType());
	}

	/*
	 * (non-Javadoc) This implementation always returns -1 (unknown length)
	 * 
	 * @see javax.microedition.media.protocol.SourceStream#getContentLength()
	 */
	public long getContentLength() {
		return -1;
	}

	/*
	 * (non-Javadoc) This implementation always returns NOT_SEEKABLE
	 * 
	 * @see javax.microedition.media.protocol.SourceStream#getSeekType()
	 */
	public int getSeekType() {
		return NOT_SEEKABLE;
	}

	/*
	 * (non-Javadoc) This implementation always returns -1 (unknown size).
	 * 
	 * @see javax.microedition.media.protocol.SourceStream#getTransferSize()
	 */
	public int getTransferSize() {
		return -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.protocol.SourceStream#read(byte[], int,
	 *      int)
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		if (!isStarted()) {
			throw new IOException("data source is not started");
		}
		if (inputStream == null) {
			// this is really an internal error
			return -1;
		}
		if (b == null) {
			throw new NullPointerException("b is null");
		}
		if (off < 0 || len < 0 || (off + len) > b.length) {
			throw new IndexOutOfBoundsException();
		}
		int ret = inputStream.read(b, off, len);
		if (ret > 0) {
			pos += ret;
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.protocol.SourceStream#seek(long)
	 */
	public long seek(long where) throws IOException {
		// can only seek forward by skipping bytes
		if (inputStream != null && where > pos) {
			long skipped = inputStream.skip(where - pos);
			pos += skipped;
		}
		return pos;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.protocol.SourceStream#tell()
	 */
	public long tell() {
		return pos;
	}

}
