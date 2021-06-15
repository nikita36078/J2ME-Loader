package com.sonivox.mmapi;

import java.io.*;

import javax.microedition.io.*;

/**
 * An implementation of DataSource and SourceStream for HTTP media. This class
 * extends input stream, so only opening is different from the base class.
 */
class DataSourceHTTP extends DataSourceInputStream {

	/**
	 * the length of this http stream, as reported by the http server
	 */
	private long contentLength = -1; // default unknown

	/**
	 * Constructor with locator and content type parameters. The input stream of
	 * this class will initially be set to null.
	 * <p>
	 * Pre-condition: locator must start with the http protocol!
	 * 
	 * @param locator the http locator
	 * @param contentType the MIME type of this data source
	 */
	public DataSourceHTTP(String locator, String contentType) {
		super(null, locator, contentType);
	}

	// DATASOURCE OVERRIDES

	/**
	 * Open the HTTP stream from the locator and set the input stream from it.
	 */
	protected void connectImpl() throws IOException {
		try {
			Connection c = Connector.open(getLocator());
			try {
				if (!(c instanceof HttpConnection)) {
					throw new IOException("not http");
				}
				HttpConnection hc = (HttpConnection) c;
				setInputStream(hc.openInputStream());
				String ct = hc.getType();
				// if we've already got a content type specified in the constructor,
				// it should be given precedence
				if (ct != null
						&& ct.length() > 0
						&& (getContentTypeImpl() == null || getContentTypeImpl().length() == 0)) {
					setContentType(ct);
				}
				// this method will correctly return -1 if the header field is
				// not set
				this.contentLength = hc.getLength();
				// may happen that it does not fail, although http error
				// response
				int rc = hc.getResponseCode();
				if (rc >= 400 && rc < 600) {
					// failed
					throw new IOException(hc.getResponseMessage());
				}
			} finally {
				// close the connection. This will not close the inputStream
				// retrieved from it!
				c.close();
			}
		} catch (Exception e) {
			disconnectImpl();
			if (e instanceof IOException) {
				throw (IOException) e;
			} else {
				throw new IOException(e.getMessage());
			}
		}
	}

	// SOURCESTREAM OVERRIDES

	/*
	 * (non-Javadoc) This implementation returns the length returned by the http
	 * server.
	 * 
	 * @see javax.microedition.media.protocol.SourceStream#getContentLength()
	 */
	public long getContentLength() {
		return this.contentLength;
	}

	/**
	 * This implementation can seek to the beginning by re-opening the http
	 * stream.
	 * 
	 * @see javax.microedition.media.protocol.SourceStream#getSeekType()
	 */
	public int getSeekType() {
		return SEEKABLE_TO_START;
	}

	/**
	 * Implement seek to 0 by re-opening the stream.
	 * 
	 * @see javax.microedition.media.protocol.SourceStream#seek(long)
	 */
	public long seek(long where) throws IOException {
		if (where < 0) {
			where = 0;
		}
		if (where == 0 && tell() > 0) {
			disconnect();
			connect();
			return tell();
		} else
			return super.seek(where);
	}

}
