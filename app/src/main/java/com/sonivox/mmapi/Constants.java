package com.sonivox.mmapi;

/**
 * Container for miscellaneous constants in SONiVOX's MMAPI implementation.
 */
class Constants {

	// MIME TYPES
	// if you add a MIME type, need to extend ManagerImpl.getSupported*(),
	// ManagerImpl.guessContentType, and ManagerImpl.getPlayerType(), too

	/**
	 * MIME type for WAV
	 */
	public static final String MIME_WAV = "audio/x-wav";

	/**
	 * MIME type for MIDI/SMF
	 */
	public static final String MIME_MIDI1 = "audio/midi";

	/**
	 * MIME type for MIDI/SMF
	 */
	public static final String MIME_MIDI2 = "audio/mid";

	/**
	 * MIME type for MIDI/SMF
	 */
	public static final String MIME_MIDI3 = "audio/x-mid";

	/**
	 * MIME type for MIDI/SMF
	 */
	public static final String MIME_MIDI4 = "audio/x-midi";

	/**
	 * MIME type for MIDI/SMF
	 */
	public static final String MIME_MIDI5 = "audio/x-smf";

	/**
	 * MIME type for SP-MIDI
	 */
	public static final String MIME_SPMIDI = "audio/sp-midi";

	/**
	 * MIME type for tone sequence
	 */
	public static final String MIME_TONE1 = "audio/x-tone-seq";

	/**
	 * MIME type for tone sequence
	 */
	public static final String MIME_TONE2 = "audio/x-jts";

	/**
	 * MIME type for MIDI/XMF
	 */
	public static final String MIME_XMF1 = "audio/xmf";

	/**
	 * MIME type for MIDI/XMF
	 */
	public static final String MIME_XMF2 = "audio/x-xmf";

	// PROTOCOLS
	// if you add a protocol, need to extend ManagerImpl.getSupported*(), too

	/**
	 * Protocol string for FILE protocol
	 */
	public static final String PROTO_FILE = "file";

	/**
	 * Protocol string for DEVICE protocol
	 */
	public static final String PROTO_DEVICE = "device";

	/**
	 * Protocol string for CAPTURE protocol
	 */
	public static final String PROTO_CAPTURE = "capture";

	/**
	 * Protocol string for HTTP protocol
	 */
	public static final String PROTO_HTTP = "http";

	// FILENAME EXTENSIONS
	// if you add an extension, need extend ManagerImpl.guessContentType, too

	/**
	 * Extension string without dot for MIDI files
	 */
	public static final String EXT_MIDI1 = "mid";

	/**
	 * Extension string without dot for MIDI files
	 */
	public static final String EXT_MIDI2 = "midi";

	/**
	 * Extension string without dot for MIDI files
	 */
	public static final String EXT_MIDI3 = "smf";

	/**
	 * Extension string without dot for XMF files
	 */
	public static final String EXT_XMF = "xmf";

	/**
	 * Extension string without dot for mXMF files
	 */
	public static final String EXT_MXMF = "mxmf";

	/**
	 * Extension string without dot for Tone Sequence files
	 */
	public static final String EXT_TONE = "jts";

	/**
	 * Extension string without dot for WAVE files
	 */
	public static final String EXT_WAVE = "wav";

}
