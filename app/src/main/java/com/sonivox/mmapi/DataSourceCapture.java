/**
 * 
 */
package com.sonivox.mmapi;

import java.io.IOException;
import javax.microedition.media.protocol.SourceStream;
import javax.microedition.media.MediaException;

/**
 * @author florian
 */
class DataSourceCapture extends DataSourceBase {

	/**
	 * private DEBUG flag: if set to false, the debug code will be removed from
	 * the .class file.
	 */
	private final static boolean DEBUG = false;

	/** default value for encoding name */
	private final static String DEFAULT_ENCODING = "pcm";
	/** default value for sample rate in hertz */
	private final static int DEFAULT_RATE = 8000;
	/** default value for bits per sample */
	private final static int DEFAULT_BITS = 16;
	/** default value for number of audio channels */
	private final static int DEFAULT_CHANNELS = 1;
	/** default value for endianness: "little" or "big" */
	private final static String DEFAULT_ENDIAN = "little";

	/** one of the EAS.CAPTURE_ENCODING_* constants */
	private int encoding; // = 0
	private int sampleRate; // = 0
	private int bits; // = 0
	private int channels; // = 0
	private boolean bigEndian; // = false
	private boolean isSigned; // = false

	/**
	 * Constructor with locator and content type parameters.
	 * 
	 * @param locator the locator of this data source.
	 * @param contentType the MIME type of this data source
	 */
	public DataSourceCapture(String locator, String contentType)
			throws MediaException {
		super(locator, contentType);
		if (!Constants.MIME_WAV.equals(contentType)) {
			throw new MediaException("content type not supported for capture");
		}
		parseLocator();
	}

	/*
	 * Parse the locator and verify that the capture format is supported.
	 * @throws MediaException if the format is not supported, or the locator is
	 * not correct
	 */
	private void parseLocator() throws MediaException {
		String loc = getLocator();
		// note: the last parameters specify the default capture format.
		String sEncoding = Utils.getParameterValue(loc, "encoding",
				DEFAULT_ENCODING);
		encoding = enc2code(sEncoding);
		sampleRate = Utils.getParameterValue(loc, "rate", DEFAULT_RATE);
		bits = Utils.getParameterValue(loc, "bits", DEFAULT_BITS);
		channels = Utils.getParameterValue(loc, "channels", DEFAULT_CHANNELS);
		String sBigEndian = Utils.getParameterValue(loc, "endian",
				DEFAULT_ENDIAN);
		bigEndian = sBigEndian.equals("big");
		String sIsSigned = Utils.getParameterValue(loc, "signed",
				bits <= 8 ? "unsigned" : "signed");
		isSigned = sIsSigned.equals("signed");

		if (DEBUG) {
			System.out.println("Requested Capture format:" + " encodingCode="
					+ encoding + " sampleRate=" + sampleRate + " bits=" + bits
					+ " channels=" + channels + " bigEndian=" + bigEndian
					+ " signed=" + isSigned);
		}

		if ((encoding < 0) || (sampleRate < 4000 || sampleRate > 44100)
				|| (bits != 8 && bits != 16) || (channels < 1 || channels > 2)
				|| (!bigEndian && !sBigEndian.equals("little"))
				|| (!isSigned && !sIsSigned.equals("unsigned"))) {
			throw new MediaException("capture format not supported");
		}
	}

	/**
	 * Return the EAS.CAPTURE_ENCODING_* code for the given string encoding
	 * 
	 * @return the code, or -1 on error
	 */
	private int enc2code(String sEncoding) {
		if (sEncoding.equals("pcm")) {
			return EAS.CAPTURE_ENCODING_PCM;
		}
		return -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sonivox.mmapi.DataSourceBase#connectImpl()
	 */
	protected void connectImpl() throws IOException {
		// nothing to do
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sonivox.mmapi.DataSourceBase#disconnectImpl()
	 */
	protected void disconnectImpl() {
		// nothing to do
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sonivox.mmapi.DataSourceBase#getStream()
	 */
	SourceStream getStream() {
		// nothing to do
		return null;
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

	/**
	 * @return the bigEndian
	 */
	boolean isBigEndian() {
		return bigEndian;
	}

	/**
	 * @return the bits
	 */
	int getBits() {
		return bits;
	}

	/**
	 * @return the channels
	 */
	int getChannels() {
		return channels;
	}

	/**
	 * @return the encoding
	 */
	int getEncoding() {
		return encoding;
	}

	/**
	 * @return the isSigned
	 */
	boolean isSigned() {
		return isSigned;
	}

	/**
	 * @return the sampleRate
	 */
	int getSampleRate() {
		return sampleRate;
	}

}
