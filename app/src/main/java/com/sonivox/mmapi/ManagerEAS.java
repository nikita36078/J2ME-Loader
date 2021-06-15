package com.sonivox.mmapi;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.microedition.media.*;
import javax.microedition.media.protocol.*;

/**
 * Internal class to implement MMAPI on the EAS.
 * <p>
 * By moving this class to the sonivox package, all other classes can be made
 * package private. This minimizes security risks.
 */
public class ManagerEAS {

	/**
	 * private DEBUG flag: if set to false, the debug code will be removed from
	 * the .class file.
	 */
	private final static boolean DEBUG = false;

	/**
	 * The static system time base
	 */
	private static SystemTimeBase systemTimeBase; // = null

	/**
	 * prevent instanciation
	 */
	private ManagerEAS() {
	}

	/**
	 * @see Manager#getSupportedContentTypes(String)
	 */
	public static String[] getSupportedContentTypes(String protocol) {
		// NOTE: if protocol == null, all supported protocols are returned
		// NOTE: if editing this, make sure to edit getSupportedProtocols()
		// and getPlayerType(), too!

		// 1. parse the protocol

		boolean isHTTP = Config.HAS_HTTP
				&& ((protocol == null) || (protocol.equals(Constants.PROTO_HTTP)));
		boolean isDevice = ((protocol == null) || (protocol.equals(Constants.PROTO_DEVICE)));
		boolean isFile = Config.HAS_FILE
				&& ((protocol == null) || (protocol.equals(Constants.PROTO_FILE)));
		boolean isCapture = ((protocol == null) || (protocol.equals(Constants.PROTO_CAPTURE)));

		// 2. retrieve the mime types
		Vector v = new Vector(10);

		// WAVE playback: HTTP or file
		boolean wavePlayback = Config.HAS_WAVE_PLAYBACK && (isHTTP || isFile);
		// WAVE capture: capture
		boolean waveCapture = Config.HAS_WAVE_CAPTURE
				&& Config.HAS_WAVE_PLAYBACK && isCapture;
		if (wavePlayback || waveCapture) {
			v.addElement(Constants.MIME_WAV);
		}

		// MIDI file playback: HTTP or file
		boolean midiPlayback = Config.HAS_MIDI_PLAYBACK && (isHTTP || isFile);
		// MIDI device (interactive MIDI): device
		boolean midiDevice = Config.HAS_DEVICE_MIDICONTROL && isDevice;
		if (midiPlayback || midiDevice) {
			// MIME type for both midiPlayback and midiDevice
			v.addElement(Constants.MIME_MIDI1);
		}
		if (midiPlayback) {
			// MIME types only for MIDI file playback
			v.addElement(Constants.MIME_MIDI2);
			v.addElement(Constants.MIME_MIDI3);
			v.addElement(Constants.MIME_MIDI4);
			v.addElement(Constants.MIME_MIDI5);
			v.addElement(Constants.MIME_SPMIDI);
		}

		// XMF playback: HTTP or file
		boolean xmfPlayback = Config.HAS_XMF_PLAYBACK && (isHTTP || isFile);
		if (xmfPlayback) {
			v.addElement(Constants.MIME_XMF1);
			v.addElement(Constants.MIME_XMF2);
		}

		// Tone playback: .jts files (http/file)
		boolean tonePlayback = Config.HAS_TONE_PLAYBACK && (isHTTP || isFile);
		// Tone device: device protocol
		boolean toneDevice = Config.HAS_DEVICE_TONECONTROL && isDevice;
		if (tonePlayback || toneDevice) {
			// MIME type for both tonePlayback and toneDevice
			v.addElement(Constants.MIME_TONE1);
		}
		if (tonePlayback) {
			// MIME type only for tone file playback
			v.addElement(Constants.MIME_TONE2);
		}

		String[] ret = new String[v.size()];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = (String) v.elementAt(i);
		}
		return ret;
	}

	/**
	 * @see Manager#getSupportedProtocols(String)
	 */
	public static String[] getSupportedProtocols(String ct) {
		// NOTE: if protocol == null, all supported protocols are returned
		// NOTE: if editing this, make sure to edit getSupportedContentTypes()
		// and getPlayerType(), too!

		// 1. parse mime type
		boolean wavePlayback = Config.HAS_WAVE_PLAYBACK
				&& ((ct == null) || ct.equals(Constants.MIME_WAV));
		boolean waveCapture = Config.HAS_WAVE_CAPTURE
				&& Config.HAS_WAVE_PLAYBACK
				&& ((ct == null) || ct.equals(Constants.MIME_WAV));
		boolean midiPlayback = Config.HAS_MIDI_PLAYBACK
				&& ((ct == null) || ct.equals(Constants.MIME_MIDI1)
						|| ct.equals(Constants.MIME_MIDI2)
						|| ct.equals(Constants.MIME_MIDI3)
						|| ct.equals(Constants.MIME_MIDI4)
						|| ct.equals(Constants.MIME_MIDI5) || ct.equals(Constants.MIME_SPMIDI));
		boolean midiDevice = Config.HAS_DEVICE_MIDICONTROL
				&& ((ct == null) || ct.equals(Constants.MIME_MIDI1));

		boolean xmfPlayback = Config.HAS_XMF_PLAYBACK
				&& ((ct == null) || ct.equals(Constants.MIME_XMF1) || ct.equals(Constants.MIME_XMF2));

		boolean tonePlayback = Config.HAS_TONE_PLAYBACK
				&& ((ct == null) || ct.equals(Constants.MIME_TONE1) || ct.equals(Constants.MIME_TONE2));
		boolean toneDevice = Config.HAS_DEVICE_TONECONTROL
				&& ((ct == null) || ct.equals(Constants.MIME_TONE1));

		// 2. retrieve the protocols
		Vector v = new Vector(4);

		// file and HTTP are allowed for all "playback" media
		if (wavePlayback || midiPlayback || xmfPlayback || tonePlayback) {
			if (Config.HAS_FILE) {
				v.addElement(Constants.PROTO_FILE);
			}
			if (Config.HAS_HTTP) {
				v.addElement(Constants.PROTO_HTTP);
			}
		}
		// device is allowed for ToneControl and MIDIControl
		if (toneDevice || midiDevice) {
			v.addElement(Constants.PROTO_DEVICE);
		}
		// capture
		if (waveCapture) {
			v.addElement(Constants.PROTO_CAPTURE);
		}

		String[] ret = new String[v.size()];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = (String) v.elementAt(i);
		}
		return ret;
	}

	/**
	 * @see Manager#createPlayer(String)
	 */
	public static Player createPlayer(String locator) throws IOException,
			MediaException {

		// can we access this locator at all?
		Security.checkLocatorAccess(locator);

		if (locator == null) {
			throw new IllegalArgumentException("locator is null");
		}

		// use lower case locator for getting the protocol
		String lcLocator = locator.toLowerCase();

		String protocol = Utils.getProtocol(lcLocator);
		if (protocol == null) {
			throw new MediaException("locator does not specify a protocol");
		}

		String contentType = null;
		DataSource ds = null;

		// handle device: protocol

		if (protocol.equals(Constants.PROTO_DEVICE)) {
			int playerType = getPlayerType(locator, null);
			switch (playerType) {
			case PlayerEAS.TYPE_TONE_DEVICE:
				contentType = Constants.MIME_TONE1;
				break;
			case PlayerEAS.TYPE_MIDI_DEVICE:
				contentType = Constants.MIME_MIDI1;
				break;
			}
			if (playerType >= 0) {
				ds = new DataSourceNone(lcLocator, contentType);
				ds.connect();
				return new PlayerEAS(playerType, ds);
			}
			throw new MediaException("device not supported");
		}

		// handle capture: protocol

		if (protocol.equals(Constants.PROTO_CAPTURE)) {
			String device = Utils.getDevice(locator);
			// can only capture from "audio" device
			if (Config.HAS_WAVE_CAPTURE
					&& (device != null && device.equals("audio"))) {
				contentType = Constants.MIME_WAV;
				// constructor throws exception if format is not supported
				ds = new DataSourceCapture(locator, contentType);

			} else {
				throw new MediaException(
						"capture device or format not supported");
			}
		}

		if (contentType == null) {
			contentType = guessContentType(lcLocator);
			if (contentType == null) {
				throw new MediaException(
						"cannot guess content type from locator");
			}

			// check if this protocol is supported for the content type
			String[] protocols = getSupportedProtocols(contentType);
			boolean found = false;
			for (int i = 0; i < protocols.length; i++) {
				if (protocol.equals(protocols[i])) {
					found = true;
					break;
				}
			}
			if (!found) {
				throw new MediaException(
						"content type or protocol not supported");
			}
		}

		if (ds == null) {
			// now create a data source from the locator
			if (Config.HAS_HTTP && protocol.equals(Constants.PROTO_HTTP)) {
				// streaming data source
				ds = new DataSourceHTTP(locator, contentType);
			} else if (Config.HAS_FILE && protocol.equals(Constants.PROTO_FILE)) {
				// native handling of protocol
				ds = new DataSourceNone(locator, contentType);
			} else {
				throw new MediaException("protocol not supported");
			}
		}

		// finally create the Player from the data source
		return createPlayer(ds);
	}

	/**
	 * @see Manager#createPlayer(InputStream, String)
	 */
	public static Player createPlayer(InputStream stream, String ct)
			throws IOException, MediaException {
		if (stream == null) {
			throw new IllegalArgumentException("stream is null");
		}

		// create a data source
		DataSource ds = new DataSourceInputStream(stream, ct);

		// create the Player from the data source
		return createPlayer(ds);
	}

	/**
	 * @see Manager#createPlayer(DataSource)
	 */
	public static Player createPlayer(DataSource ds) throws IOException,
			MediaException {

		if (ds == null) {
			throw new IllegalArgumentException("source is null");
		}

		Security.checkLocatorAccess(ds.getLocator());

		// DataSource.getContentType() requires the source to be connected
		ds.connect();

		try {
			int playerType;
			String ct = ds.getContentType();
			String locator = ds.getLocator();

			playerType = getPlayerType(locator, ct);
			if (playerType < 0) {
				throw new MediaException("content type is not supported");
			}
			return new PlayerEAS(playerType, ds);
		} catch (Exception e) {
			ds.disconnect();
			if (e instanceof IOException) {
				throw (IOException) e;
			} else if (e instanceof MediaException) {
				throw (MediaException) e;
			} else if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new MediaException(e.getMessage());
		}
	}

	/**
	 * @see Manager#playTone(int, int, int)
	 */
	public static void playTone(int note, int duration, int volume)
			throws MediaException {

		if (note < 0 || note > 127 || duration <= 0) {
			throw new IllegalArgumentException("note or duration out of range");
		}
		if (volume <= 0) {
			// nothing to do
			return;
		}
		if (Config.HAS_DEVICE_MIDICONTROL) {
			PlayTone.playTone(note, duration, volume);
		} else {
			throw new MediaException("playTone not available");
		}
	}

	/**
	 * @see Manager#getSystemTimeBase()
	 */
	public static TimeBase getSystemTimeBase() {
		if (systemTimeBase == null) {
			systemTimeBase = new SystemTimeBase();
		}
		return systemTimeBase;

	}

	// private implementation

	/**
	 * Guess the MIME content type from the specified locator. For this, the
	 * locator's extension is evaluated.
	 * 
	 * @param locator the locator to evaluate
	 * @return the content type, or null
	 */
	private static String guessContentType(String locator) {
		String ext = Utils.getExtension(locator);
		if (ext == null) {
			return null;
		}
		// compare the extension
		if (ext.equals(Constants.EXT_WAVE)) {
			return Constants.MIME_WAV;
		} else if (ext.equals(Constants.EXT_MIDI1)
				|| ext.equals(Constants.EXT_MIDI2)
				|| ext.equals(Constants.EXT_MIDI3)) {
			return Constants.MIME_MIDI1;
		} else if (ext.equals(Constants.EXT_TONE)) {
			return Constants.MIME_TONE1;
		} else if (ext.equals(Constants.EXT_XMF)
				|| ext.equals(Constants.EXT_MXMF)) {
			return Constants.MIME_XMF1;
		}
		// out of options
		return null;
	}

	/**
	 * Return the player type (one of the PlayerImpl.TYPE_ constants) for the
	 * given locator and content type. For device locators, the content type
	 * parameter is ignored. For non-device locators, the locator is ignored and
	 * can be null.
	 * 
	 * @param locator the locator of this Player, or null
	 * @param ct the content type to convert to player type constant
	 * @return the player constant, or -1 if none can be found.
	 */
	private static int getPlayerType(String locator, String ct) {
		if (DEBUG) {
			System.out.println("getPlayerType: locator=" + locator + " content type=" + ct);
		}
		// first check if this is a supported DEVICE
		String protocol = Utils.getProtocol(locator);
		if (protocol != null && protocol.equals(Constants.PROTO_DEVICE)) {
			if (Config.HAS_DEVICE_TONECONTROL
					&& locator.equals(Manager.TONE_DEVICE_LOCATOR)) {
				return PlayerEAS.TYPE_TONE_DEVICE;
			}

			if (Config.HAS_DEVICE_MIDICONTROL
					&& locator.equals(Manager.MIDI_DEVICE_LOCATOR)) {
				return PlayerEAS.TYPE_MIDI_DEVICE;
			}
			// no other supported device type
			return -1;
		}

		if (ct == null) {
			return -1;
		}

		if (Config.HAS_WAVE_PLAYBACK && ct.equals(Constants.MIME_WAV)) {
			// check out if this is a capture device
			if (protocol != null && protocol.equals(Constants.PROTO_CAPTURE)) {
				if (Config.HAS_WAVE_CAPTURE) {
					return PlayerEAS.TYPE_WAVE_CAPTURE_PLAYER;
				}
			} else {
				return PlayerEAS.TYPE_WAVE_PLAYER;
			}
		} else if (Config.HAS_MIDI_PLAYBACK
				&& (ct.equals(Constants.MIME_MIDI1)
						|| ct.equals(Constants.MIME_MIDI2)
						|| ct.equals(Constants.MIME_MIDI3)
						|| ct.equals(Constants.MIME_MIDI4)
						|| ct.equals(Constants.MIME_MIDI5) || ct.equals(Constants.MIME_SPMIDI))) {
			return PlayerEAS.TYPE_MIDI_PLAYER;
		} else if (Config.HAS_XMF_PLAYBACK
				&& (ct.equals(Constants.MIME_XMF1) || ct.equals(Constants.MIME_XMF2))) {
			return PlayerEAS.TYPE_XMF_PLAYER;
		} else if (Config.HAS_TONE_PLAYBACK
				&& (ct.equals(Constants.MIME_TONE1) || ct.equals(Constants.MIME_TONE2))) {
			return PlayerEAS.TYPE_TONE_PLAYER;
		}
		// out of options
		return -1;
	}

} // class ManagerImpl
