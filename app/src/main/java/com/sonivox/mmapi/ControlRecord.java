package com.sonivox.mmapi;

import java.io.*;

import javax.microedition.media.*;
import javax.microedition.media.control.RecordControl;

/**
 * Implementation of RecordControl for EAS. Writes the currently playing stream
 * to a wave file.
 */
class ControlRecord extends ControlBase implements RecordControl {

	/** constant for recording state: blank */
	private static final int RS_NONE = 0;

	/** constant for recording state: location has been set to a stream */
	private static final int RS_LOCATION_STREAM = 1;

	/** constant for recording state: location has been set to a locator */
	private static final int RS_LOCATION_LOCATOR = 2;

	/** constant for recording state: actively recording media */
	private static final int RS_RECORDING = 3;

	/** constant for error from native: some native error happened */
	private static final int RS_NATIVE_ERROR = -1;

	/**
	 * constant for error from native: native stopped recording, e.g. because of
	 * record size limit
	 */
	private static final int RS_NATIVE_STOPPED = -2;

	/**
	 * The state of this record control, one of the RS_ constants above.
	 */
	private int state; // = RS_NONE;

	/**
	 * The output stream, or null
	 */
	private OutputStream stream; // = null;

	/**
	 * Create a new instance of this EAS record control.
	 * 
	 * @param player the owning player
	 */
	ControlRecord(PlayerEAS player) {
		super(player);
		// initialize record size limit
		try {
			setRecordSizeLimit(Integer.MAX_VALUE);
		} catch (MediaException me) {
			// will never be thrown
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.RecordControl#setRecordStream(java.io.OutputStream)
	 */
	public synchronized void setRecordStream(OutputStream stream) {
		if (stream == null) {
			throw new IllegalArgumentException("stream is null");
		}

		// RS_LOCATION_SET only allowed if stream is set
		if (state != RS_NONE && state != RS_LOCATION_STREAM) {
			throw new IllegalStateException("wrong record state");
		}
		Security.checkRecordPermission(player, stream);
		if (EAS.openRecording(player.handle, null)) {
			this.stream = stream;
			this.state = RS_LOCATION_STREAM;
		} else {
			//throw new MediaException("cannot open recording");
			//what to do here?
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.RecordControl#setRecordLocation(java.lang.String)
	 */
	public synchronized void setRecordLocation(String locator)
			throws IOException, MediaException {
		if (locator == null) {
			throw new IllegalArgumentException("locator is null");
		}
		if (state != RS_NONE && state != RS_LOCATION_LOCATOR) {
			throw new IllegalStateException("wrong record state");
		}
		Security.checkRecordPermission(player, stream);
		// currently handle only file protocol
		String proto = Utils.getProtocol(locator);
		if (proto == null || !proto.equals("file")) {
			throw new MediaException("protocol not supported: " + locator);
		}
		if (EAS.openRecording(player.handle, locator)) {
			this.stream = null;
			this.state = RS_LOCATION_LOCATOR;
		} else {
			throw new MediaException("cannot open recording");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.RecordControl#getContentType()
	 */
	public String getContentType() {
		return player.getContentType();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.RecordControl#startRecord()
	 */
	public synchronized void startRecord() {
		if (state == RS_RECORDING) {
			// nothing to do
			return;
		}
		if (state == RS_NONE) {
			throw new IllegalStateException("no record location");
		}
		EAS.startRecording(player.handle);
		state = RS_RECORDING;
		// the documentation does not mandate that RECORD_STARTED be sent when
		// recording actually starts. It says when startRecord() returns, the
		// RECORD_STARTED event will be sent. So we don't need to wait for the
		// player to start playback to send this event.
		player.dispatchMessage(PlayerListener.RECORD_STARTED, new Long(
				player.getMediaTime()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.RecordControl#stopRecord()
	 */
	public synchronized void stopRecord() {
		if (state != RS_RECORDING) {
			// nothing to do
			return;
		}
		EAS.stopRecording(player.handle);
		if (stream != null) {
			state = RS_LOCATION_STREAM;
		} else {
			state = RS_LOCATION_LOCATOR;
		}
		player.dispatchMessage(PlayerListener.RECORD_STOPPED, new Long(
				player.getMediaTime()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.RecordControl#commit()
	 */
	public synchronized void commit() throws IOException {
		if (state > RS_NONE) {
			commitImpl();
		}
	}

	/**
	 * Carry out the actual commit operation, and reset this RecordControl. This
	 * method is called from commit(), and when native signals end of recording.
	 * 
	 * @param ioDone if true, stopRecord() and update() are assumed to already
	 *            carried out.
	 * @throws IOException
	 */
	private synchronized void commitImpl() throws IOException {
		try {
			stopRecord();
			if (EAS.commitRecording(player.handle)) {
				// commitRecording may require some more bytes to be sent to the
				// OutputStream, or error handling
				update(true);
			}
		} finally {
			reset();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.RecordControl#setRecordSizeLimit(int)
	 */
	public int setRecordSizeLimit(int size) throws MediaException {
		if (size <= 0) {
			throw new IllegalArgumentException("non-positive record size limit");
		}
		EAS.setRecordSizeLimit(player.handle, size);
		try {
			// will possibly commit, if record size limit is already reached.
			// by the spec, this method may not throw an exception in case of error.
			// notify the error through the player listener instead
			update(false);
		} catch (IOException ioe) {
			// satisfy compiler
		}
		return size;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.RecordControl#reset()
	 */
	public synchronized void reset() throws IOException {
		stopRecord();
		buffer = null;
		stream = null;
		state = RS_NONE;
		EAS.closeRecording(player.handle);
	}

	/**
	 * internal method to signal an error
	 */
	private void error(String err) {
		try {
			reset();
		} catch (Exception e) {
			// cannot do anything here
		}
		player.dispatchMessage(PlayerListener.RECORD_ERROR, err);
	}

	/**
	 * The temporary buffer used for transferring native bytes to the
	 * OutputStream.
	 */
	private byte[] buffer; // = null;

	/**
	 * Internal method to update recording state depending on player state. If
	 * an OutputStream is set as record location, it will read data from native
	 * and write it to the OutputStream. If currently engaging in recording, the
	 * current native state is queried. If it is -1, an error happened, and
	 * either an IOException is thrown (if <code>canThrowException</code>=<code>true</code>)
	 * or a RECORD_ERROR event is sent to player listeners. If the native state
	 * is -2, recording is finished (usually because the record size limit is
	 * reached) and should be implicitly committed.
	 * 
	 * @return true if update() did anything.
	 */
	synchronized boolean update(boolean canThrowException) throws IOException {
		if (state == RS_NONE) {
			return false;
		}
		int recordState = 0;
		String err = null;
		boolean result = false;
		if (stream == null) {
			recordState = EAS.getRecordingState(player.handle);
		} else {
			if (buffer == null) {
				buffer = new byte[128];
			}
			while (true) {
				recordState = EAS.readRecordedBytes(player.handle, buffer, 0, buffer.length);
				if (recordState > 0) {
					result = true;
					try {
						stream.write(buffer, 0, recordState);
					} catch (Throwable t) {
						err = "error writing to stream: " + t.getMessage();
						recordState = RS_NATIVE_ERROR;
						break;
					}
				} else {
					break;
				}
			}
		}
		if (recordState == RS_NATIVE_STOPPED) {
			// native signaled that we need to commit. 
			// Be aware of recursive calls to update().
			try {
				commitImpl();
			} catch (IOException ioe) {
				err = "error committing: " + ioe.getMessage();
				recordState = RS_NATIVE_ERROR;
			}
		}
		if (recordState == RS_NATIVE_ERROR) {
			if (err == null) {
				err = "recording error";
			}
			if (canThrowException) {
				throw new IOException(err);
			} else {
				error(err);
				result = false;
			}
		}
		return result;
	}

}
