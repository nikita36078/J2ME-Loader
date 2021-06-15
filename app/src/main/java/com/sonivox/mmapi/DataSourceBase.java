package com.sonivox.mmapi;

import java.io.*;

import javax.microedition.media.*;
import javax.microedition.media.protocol.*;

/**
 * The base class for DataSource implementations. It introduces ...Impl()
 * methods so that implementation classes do not need to handle a lot of the
 * logic. Also, some features are simplified, e.g. a maximum of one stream is
 * supported, no controls, etc.
 */
abstract class DataSourceBase extends DataSource {

	/**
	 * The content type of this data source. This is enforced to be non-null.
	 */
	private String contentType;

	/**
	 * Flag if this data source is connected
	 */
	private boolean connected; // = false

	/**
	 * Flag if this data source is started
	 */
	private boolean started; // = false

	/**
	 * Constructor with locator and content type parameters/
	 * 
	 * @param locator the locator of this data source (may be null)
	 * @param contentType the MIME type of this data source
	 */
	protected DataSourceBase(String locator, String contentType) {
		super(locator);
		setContentType(contentType);
	}

	// IMPLEMENTATION OF DATASOURCE'S ABSTRACT METHODS

	/**
	 * By the spec, must be connected before returning the content type.
	 * 
	 * @see DataSource#getContentType()
	 */
	public final String getContentType() {
		checkConnected();
		return getContentTypeImpl();
	}

	/**
	 * Evaluates and sets the connected flag, and calls connectedImpl().
	 * 
	 * @see DataSource#connect()
	 */
	public final void connect() throws IOException {
		if (!connected) {
			connectImpl();
			connected = true;
		}
	}

	/**
	 * Evaluates and sets the connected flag, and calls disconnectedImpl().
	 * 
	 * @see DataSource#disconnect()
	 */
	public final void disconnect() {
		if (connected) {
			try {
				stop();
			} catch (IOException ioe) {
				// what to do here? Spec shortcoming
			}
			disconnectImpl();
			connected = false;
		}
	}

	/**
	 * Checks correct state, then evaluates and sets the started flag, and calls
	 * startImpl().
	 * 
	 * @see DataSource#start()
	 */
	public final void start() throws IOException {
		checkConnected();
		if (!started) {
			startImpl();
			started = true;
		}
	}

	/*
	 * (non-Javadoc) Evaluates and sets the started flag, and calls stopImpl().
	 * 
	 * @see javax.microedition.media.protocol.DataSource#stop()
	 */
	public final void stop() throws IOException {
		if (started) {
			stopImpl();
			started = false;
		}
	}

	/*
	 * (non-Javadoc) This implementation checks correct state, then returns an
	 * array with maximum one entry.
	 * 
	 * @see javax.microedition.media.protocol.DataSource#getStreams()
	 */
	public final SourceStream[] getStreams() {
		checkConnected();
		SourceStream s = getStream();
		if (s == null) {
			return new SourceStream[0];
		}
		return new SourceStream[] {
			s
		};
	}

	/*
	 * (non-Javadoc) This implementation checks correct state, then always
	 * returns a zero-sized array.
	 * 
	 * @see javax.microedition.media.Controllable#getControls()
	 */
	public final Control[] getControls() {
		checkConnected();
		return new Control[0];
	}

	/*
	 * (non-Javadoc) Possibly throw exception for wrong state, otherwise returns
	 * null.
	 * 
	 * @see javax.microedition.media.Controllable#getControl(java.lang.String)
	 */
	public Control getControl(String controlType) {
		checkConnected();
		if (controlType == null) {
			throw new IllegalArgumentException("controlType is null");
		}
		return null;
	}

	// ABSTRACT METHODS

	/**
	 * Abstract connect() implementation: it is only called when this data
	 * source is not already open.
	 * 
	 * @throws IOException
	 */
	protected abstract void connectImpl() throws IOException;

	/**
	 * Abstract disconnect() implementation: it is only called when this data
	 * source is actually connected.
	 */
	protected abstract void disconnectImpl();

	/**
	 * Abstract start() implementation: it is only called when this data source
	 * is not already started.
	 */
	protected abstract void startImpl() throws IOException;

	/**
	 * Abstract stop() implementation: it is only called when this data source
	 * is actually started.
	 */
	protected abstract void stopImpl() throws IOException;

	// PACKAGE PRIVATE METHODS FOR USE BY OTHER CLASSES

	/**
	 * @return the stream of this data source, or null if there is no stream
	 */
	abstract SourceStream getStream();

	/**
	 * @return true if this data source is connected
	 */
	final boolean isConnected() {
		return connected;
	}

	/**
	 * Returns that started flag. The source streams may only provide data if
	 * the underlying DataSource object is in the started state.
	 * 
	 * @return true if this data source is started
	 */
	final boolean isStarted() {
		return started;
	}

	/**
	 * Allow subclasses to get the content type without being connected.
	 *
	 * @see DataSource#getContentType()
	 */
	protected final String getContentTypeImpl() {
		return contentType;
	}

	/**
	 * Allow subclasses to change the content type.
	 */
	protected void setContentType(String ct) {
		if (ct == null) {
			ct = "";
		}
		this.contentType = ct;
	}

	// HELPER METHODS

	/**
	 * @throws IllegalStateException if this data source is not connected
	 */
	private void checkConnected() {
		if (!connected) {
			throw new IllegalStateException("not connected");
		}
	}
}
