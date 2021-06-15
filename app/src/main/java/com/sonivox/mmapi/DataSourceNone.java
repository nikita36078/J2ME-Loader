package com.sonivox.mmapi;

import java.io.*;
import javax.microedition.media.protocol.*;

/**
 * A DataSource that is merely a place holder for media which either do
 * not have data associated (like tone/MIDI devices), or are directly read
 * from native. This latter is true, for example, for file based media.
 */
class DataSourceNone extends DataSourceBase {

	/**
	 * Constructor with locator and content type parameters.
	 * 
	 * @param locator the locator of this data source.
	 * @param contentType the MIME type of this data source
	 */
	public DataSourceNone(String locator, String contentType) {
		super(locator, contentType);
	}

	/*
	 * (non-Javadoc) This implementation does not do anything (nothing to
	 * connect to).
	 * 
	 * @see com.sonivox.mmapi.DataSourceBase#connectImpl()
	 */
	protected void connectImpl() throws IOException {
		// nothing to do
	}

	/*
	 * (non-Javadoc) This implementation does not do anything.
	 * 
	 * @see com.sonivox.mmapi.DataSourceBase#disconnectImpl()
	 */
	protected void disconnectImpl() {
		// nothing to do
	}

	/*
	 * (non-Javadoc) This implementation does not do anything.
	 * 
	 * @see com.sonivox.mmapi.DataSourceBase#startImpl()
	 */
	protected void startImpl() throws IOException {
		// nothing to do
	}

	/*
	 * (non-Javadoc) This implementation does not do anything.
	 * 
	 * @see com.sonivox.mmapi.DataSourceBase#stopImpl()
	 */
	protected void stopImpl() throws IOException {
		// nothing to do
	}

	/*
	 * (non-Javadoc) This implementation always returns null (no stream
	 * available).
	 * 
	 * @see com.sonivox.mmapi.DataSourceBase#getStream()
	 */
	SourceStream getStream() {
		return null;
	}

}
