package com.sonivox.mmapi;

import javax.microedition.media.*;

/**
 * Bridge to the native EAS library. It handles initialization and shutdown of
 * the synthesizer, as well as all other communication with the engine.
 */
class EAS {

	/**
	 * private DEBUG flag: if set to false, the debug code will be removed from
	 * the .class file.
	 */
	private final static boolean DEBUG = false;

	// OPEN MODE CONSTANTS

	// Note: these constants must start with 1, so that 0 is properly detected
	// as "not initialized"

	/**
	 * file open mode: locator is a filename or other locator understood and to
	 * be opened by the native implementation.
	 */
	public final static int MODE_NATIVE = 1;

	/**
	 * file open mode: locator is ignored, the file is pushed completely to
	 * native before calling nPrepare().
	 */
	public final static int MODE_MEMORY = 2;

	/**
	 * file open mode: locator is ignored, the file is pushed sequentially to
	 * native. Only a few KB are written to native before calling nPrepare().
	 */
	public final static int MODE_STREAM = 3;

	/**
	 * file open mode: locator is ignored, an interactive MIDI handle is opened.
	 * Use write() to write raw MIDI data to the stream. Son't call prefetch(),
	 * start(), getMediaTime().
	 */
	public final static int MODE_INTERACTIVE_MIDI = 4;

	/**
	 * file open mode: like MODE_MEMORY, but opened as Java Tone Sequence file.
	 */
	public final static int MODE_TONE_SEQUENCE = 5;

	/**
	 * file open mode: like MODE_MEMORY, but opening capture device, too
	 */
	public final static int MODE_CAPTURE = 6;

	// EAS STATEs

	public final static int STATE_READY = 0;
	public final static int STATE_PLAY = 1;
	public final static int STATE_STOPPING = 2;
	public final static int STATE_PAUSING = 3;
	public final static int STATE_STOPPED = 4;
	public final static int STATE_PAUSED = 5;
	public final static int STATE_OPEN = 6;
	public final static int STATE_ERROR = 7;
	public final static int STATE_EMPTY = 8;

	// EAS META DATA constants
	// If adding constants, also add to ControlMetaData
	public final static int METADATA_TITLE = 0;
	public final static int METADATA_AUTHOR = 1;
	public final static int METADATA_COPYRIGHT = 2;
	public final static int METADATA_LYRIC = 3;
	public final static int METADATA_LAST = 3; // highest meta data constant

	// Capture Encodings
	public final static int CAPTURE_ENCODING_PCM = 1;

	// COMMAND CODES

	/**
	 * Command code for nGeneral(): get the current open mode, one of the MODE_*
	 * constants. The parameter is ignored.
	 */
	private final static int COMMAND_GET_MODE = 1;

	/**
	 * Command code for nGeneral(): start recording
	 */
	private final static int COMMAND_START_RECORDING = 2;

	/**
	 * Command code for nGeneral(): stop recording
	 */
	private final static int COMMAND_STOP_RECORDING = 3;

	/**
	 * Command code for nGeneral(): commit recording
	 */
	private final static int COMMAND_COMMIT_RECORDING = 4;

	/**
	 * Command code for nGeneral(): close the recording resources
	 */
	private final static int COMMAND_CLOSE_RECORDING = 5;

	/**
	 * Command code for nGeneral(): retrieve the current recording state
	 */
	private final static int COMMAND_GET_RECORDING_STATE = 6;

	/**
	 * Command code for nGeneral(): set the file size limit
	 */
	private final static int COMMAND_LIMIT_RECORDING = 7;

	/**
	 * Command code for nGeneral(): set the tempo in milli-bpm
	 */
	private final static int COMMAND_SET_TEMPO = 8;

	/**
	 * Command code for nGeneral(): get the tempo in milli-bpm
	 */
	private final static int COMMAND_GET_TEMPO = 9;

	/**
	 * Command code for nGeneral(): re-open a STREAM file
	 */
	private final static int COMMAND_REOPEN = 10;

	/**
	 * Command code for nGeneral(): open an interactive MIDI stream
	 */
	private final static int COMMAND_OPEN_INTERACTIVE_MIDI = 11;

	/**
	 * Command code for nGeneral(): close an interactive MIDI stream
	 */
	private final static int COMMAND_CLOSE_INTERACTIVE_MIDI = 12;

	// SELECTED EAS ERROR CODES. see errorCodeToString()

	private final static int ERROR_MALLOC_FAILED = -3;
	private final static int ERROR_FILE_OPEN_FAILED = -10;
	private final static int ERROR_MAX_FILES_OPEN = -14;
	private final static int ERROR_UNRECOGNIZED_FORMAT = -15;
	private final static int ERROR_FILE_FORMAT = -17;
	private final static int ERROR_INVALID_PCM_TYPE = -20;
	private final static int ERROR_MAX_PCM_STREAMS = -21;
	private final static int ERROR_MAX_STREAMS_OPEN = -27;
	private final static int ERROR_SOUND_LIBRARY = -30;
	private final static int ERROR_NO_VIRTUAL_SYNTHESIZER = -32;
	private final static int ERROR_MAX = -50; // maximum possible error number

	// WRITE FLAGS
	private final static int WRITE_FLAG_MORE_COMING = 1;
	private final static int WRITE_FLAG_INTERACTIVE_MIDI = 2;

	// STATIC VARIABLES

	/**
	 * How many instances hold the EAS open.
	 */
	private static int refCount; // = 0

	/**
	 * The native handle.
	 * <p>
	 * NOTE: for 64-bit environments, make this handle long
	 */
	private static int easHandle; // = 0

	static {
		System.loadLibrary("mmapi");
	}

	/**
	 * Init the EAS synth, if necessary, and increase the ref count.
	 * 
	 * @throws MediaException if there is an error initializing the EAS synth
	 */
	private static synchronized void openSynth() throws MediaException {
		if (refCount == 0) {
			easHandle = nInit();
			if (easHandle == 0) {
				throw new MediaException("cannot initialize");
			}
			if (!Config.NATIVE_RENDERING_THREAD) {
				// create the rendering thread
				createRenderThread();
			}
		}
		refCount++;
	}

	/**
	 * Decrease the ref count. If the ref count reaches 0, shutdown the EAS
	 * synth.
	 */
	private static synchronized void closeSynth() {
		refCount--;
		if (refCount <= 0) {
			refCount = 0;
			if (!Config.NATIVE_RENDERING_THREAD) {
				// kill the rendering thread, wait until it's finished using the
				// EAS engine
				destroyRenderThread();
			}
			nShutdown(easHandle);
			easHandle = 0;
		}
	}

	// PUBLIC INTERFACE

	/**
	 * Open the file in the synth (for Player.realize()). Since EAS requires
	 * data to be present for the EAS_OpenFile() call to succeed, for
	 * MODE_MEMORY and MODE_STREAM, this method does not actually call
	 * EAS_OpenFile(). For MODE_MEMORY/TONE_SEQUENCE, EAS_OpenFile() will be
	 * called when all of the media file's data is written to native. For
	 * MODE_STREAM, EAS_OpenFile() will be called with the first chunk of data
	 * written to the native layer.
	 * 
	 * @param locator the locator to open (for mode=MODE_NATIVE)
	 * @param mode the open mode, one of the MODE_ constants
	 * @return a file handle needed for subsequent calls
	 * @throws MediaException if there is an error initializing the EAS synth,
	 *             or opening the file.
	 */
	static synchronized int openMedia(String locator, int mode)
			throws MediaException {
		// first open the synth
		openSynth();
		// then open the file
		int fileHandle = nOpenFile(easHandle, locator, mode);
		if (DEBUG) {
			System.out.println("EAS.openMedia: file handle=" + fileHandle);
		}
		if (fileHandle <= 0 && fileHandle >= ERROR_MAX) {
			closeSynth();
			String err = errorCodeToString(fileHandle);
			if (err == null) {
				err = "error opening media";
			}
			throw new MediaException(err);
		}
		return fileHandle;
	}

	/**
	 * Open the capture device (for Player.realize()).
	 * 
	 * @param encoding one of the CAPTURE_ENCODING_* constants
	 * @param rate the sampling rate in Hz
	 * @param bits the number of bits per sample, e.g. 8 nor 16
	 * @param channels the number of channels, e.g. 1 for mono
	 * @param bigEndian if true, the capture device is opened in bigEndian
	 *            (ignored for bits<=8)
	 * @param isSigned if true, the capture device is opened with signed samples
	 *            (ignored for bits<=8)
	 * @return a file handle needed for subsequent calls
	 * @throws MediaException if there is an error initializing the EAS synth,
	 *             or opening the file.
	 */
	static synchronized int openCapture(int encoding, int rate, int bits,
			int channels, boolean bigEndian, boolean isSigned)
			throws MediaException {
		int fileHandle = openMedia(null, MODE_CAPTURE);
		if (!nOpenCapture(easHandle, fileHandle, encoding, rate, bits,
				channels, bigEndian, isSigned)) {
			closeMedia(fileHandle);
			throw new MediaException("error opening capture device");
		}
		return fileHandle;
	}

	/**
	 * Re-open a file that was previously already opened. For NATIVE and MEMORY,
	 * it will just seek to the beginning of the file. For STREAM it will
	 * discard all data in the file.
	 * 
	 * @param fileHandle the file to be re-opened
	 */
	static synchronized void reopenMedia(int fileHandle) {
		nGeneral(easHandle, fileHandle, COMMAND_REOPEN, 0);
	}

	/**
	 * Close the opened media file (for Player.deallocate()). The handle is not
	 * valid anymore after this call.
	 * 
	 * @param fileHandle the handle retrieved with openMedia()
	 */
	static synchronized void closeMedia(int fileHandle) {
		if (fileHandle != 0) {
			// don't close the file during a rendering action
			synchronized (renderLock) {
				nCloseFile(easHandle, fileHandle);
			}
			closeSynth();
		}
	}

	/**
	 * Write data to the synth from a buffer. This is for the modes MODE_MEMORY,
	 * MODE_TONE_SEQUENCE, and MODE_STREAM.
	 * <p>
	 * The native layer is responsible for caching the data provided in this
	 * method.
	 * <p>
	 * If there is more data than transferred in this call to write(), then the
	 * moreComing flag should be set to true. Subsequent calls to write() can
	 * add more data.
	 * <p>
	 * For MODE_MEMORY/TONE_SEQUENCE, each call to write() will enlarge the
	 * cached data. Then, the EAS file will be opened when the final call to
	 * write() (i.e. moreComing=false) is executed. If totalLength is known,
	 * then the native layer will not accept more then a total of totalLength
	 * bytes.
	 * <p>
	 * For MODE_STREAM, each call to write() will add as much data as possible
	 * to the native circular buffer.
	 * <p>
	 * For MODE_INTERACTIVE_MIDI, this call writes MIDI data to the interactive
	 * MIDI data stream. totalLength and moreComing are ignored in that case. In
	 * case of error, -1 is returned.
	 * 
	 * @param fileHandle the handle retrieved with openMedia()
	 * @param buffer the buffer with (part of) the media data
	 * @param offset the offset in buffer
	 * @param count the number of bytes usable in buffer, from offset index
	 * @param totalLength total number of bytes for the media file, or -1 if not
	 *            known
	 * @param moreComing if true, one or more subsequent calls to write() will
	 *            provide more data.
	 * @return the number of bytes written to native
	 * @throws MediaException if an unrecoverable error occured in the native
	 *             layer
	 */
	static int write(int fileHandle, byte[] buffer, int offset, int count,
			long totalLength, boolean moreComing) throws MediaException {
		return writeImpl(fileHandle, buffer, offset, count, totalLength,
				moreComing ? WRITE_FLAG_MORE_COMING : 0);
	}

	/**
	 * internal write function, unifying the write functions for media data and
	 * interactive MIDI stream data. If flags include
	 * WRITE_FLAG_INTERACTIVE_MIDI, then fileHandle points directly to an EAS
	 * MIDIStream device. Otherwise it's the usual file handle.
	 */
	private static int writeImpl(int fileHandle, byte[] buffer, int offset,
			int count, long totalLength, int flags) throws MediaException {
		synchronized (renderLock) {
			// if (DEBUG) System.out.println(">EAS.nWrite()");
			int ret = nWrite(easHandle, fileHandle, buffer, offset, count,
					(int) totalLength, flags);
			// if (DEBUG) System.out.println("<EAS.nWrite()=" + ret);
			if (ret < 0) {
				String err = errorCodeToString(ret);
				if (err == null) {
					err = "error writing to device";
				}
				throw new MediaException(err);
			}
			return ret;
		}
	}

	/**
	 * Send interactive MIDI data to the EAS engine. If midiHandle is non-null,
	 * then the data is written to that MIDI stream, otherwise it is assumed
	 * that fileHandle points to a MODE_INTERACTIVE_MIDI device.
	 * 
	 * @param fileHandle the handle retrieved with openMedia()
	 * @param midiHandle optional handle to the interactive MIDI device
	 * @param buffer the buffer with (part of) the media data
	 * @param offset the offset in buffer
	 * @param count the number of bytes usable in buffer, from offset index
	 * @return the number of bytes written to native
	 * @throws MediaException
	 */
	static synchronized int writeMIDI(int handle, byte[] buffer, int offset,
			int count, boolean isMIDIStreamHandle) throws MediaException {
		return writeImpl(handle, buffer, offset, count, 0,
				isMIDIStreamHandle ? WRITE_FLAG_INTERACTIVE_MIDI : 0);
	}

	/**
	 * Get the mode (one of the MODE_* constants) from native. The native layer
	 * may change the mode internally.
	 * 
	 * @param fileHandle the handle retrieved with openMedia()
	 * @return the current mode
	 */
	static int getMode(int fileHandle) {
		return nGeneral(easHandle, fileHandle, COMMAND_GET_MODE, 0);
	}

	/**
	 * Prepare the opened file in the synth (for Player.prefetch()).
	 * 
	 * @param fileHandle the handle retrieved with openMedia()
	 * @throws MediaException if there is an error preparing the media
	 */
	static synchronized void prepareMedia(int fileHandle) throws MediaException {
		if (DEBUG) System.out.println(">EAS.nPrepare()");
		boolean success = nPrepare(easHandle, fileHandle);
		if (!success) {
			throw new MediaException("error prefetching media");
		}
		if (DEBUG) System.out.println("<EAS.nPrepare()");
	}

	/**
	 * Start playback of the opened file in the synth (for Player.start()).
	 * 
	 * @param fileHandle the handle retrieved with openMedia()
	 * @throws MediaException if there is an error starting the media
	 */
	static synchronized void startMedia(int fileHandle) throws MediaException {
		if (DEBUG) System.out.println(">EAS.nResume()");
		int state = getState(fileHandle);
		if (state != STATE_PLAY) {
			if (!nResume(easHandle, fileHandle)) {
				throw new MediaException("error starting media");
			}
		}
		if (DEBUG) System.out.println("<EAS.nResume()");
	}

	/**
	 * Stop playback of the opened file in the synth (for Player.stop()).
	 * 
	 * @param fileHandle the handle retrieved with openMedia()
	 * @throws MediaException if there is an error starting the media
	 */
	static synchronized void stopMedia(int fileHandle) throws MediaException {
		if (DEBUG) System.out.println(">EAS.nPause()");
		int state = getState(fileHandle);
		if (state != STATE_STOPPED && state != STATE_STOPPING
				&& state != STATE_PAUSED && state != STATE_PAUSING) {
			if (!nPause(easHandle, fileHandle)) {
				throw new MediaException("error stopping media");
			}
		}
		if (DEBUG) System.out.println("<EAS.nPause()");
	}

	/**
	 * Get the current media state.
	 * 
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return the state, one of the STATE_* constants, or -1 on error
	 */
	static synchronized int getState(int fileHandle) {
		if (fileHandle != 0) {
			return nGetState(easHandle, fileHandle);
		}
		return -1;
	}

	/**
	 * Get the current media time.
	 * 
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return the media time in microseconds, or -1 for error
	 */
	static synchronized long getMediaTime(int fileHandle) {
		if (fileHandle != 0) {
			long ret = nGetLocation(easHandle, fileHandle);
			if (ret > 0) {
				ret = ret * 1000L;
			}
			return ret;
		}
		return -1L;
	}

	/**
	 * Set the current media time.
	 * 
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @param millis the requested media time in microseconds
	 * @return the media time in microseconds, or -1 for error
	 */
	static synchronized long setMediaTime(int fileHandle, long mediaTime)
			throws MediaException {
		if (fileHandle != 0) {
			long ret = (long) nLocate(easHandle, fileHandle,
					(int) (mediaTime / 1000L));
			if (ret >= 0) {
				return ret * 1000L;
			}
		}
		throw new MediaException("media time cannot be set");
	}

	/**
	 * Set the volume level for this media file.
	 * 
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @param level the linear volume level, 0...100
	 * @return false on error
	 */
	static boolean setLevel(int fileHandle, int level) {
		// TODO: EAS uses log scale, MMAPI linear scale
		return nSetVolume(easHandle, fileHandle, level);
	}

	/**
	 * Get the volume level for this media file.
	 * 
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return the linear volume level, 0...100, or -1 on error
	 */
	static int getLevel(int fileHandle) {
		// TODO: EAS uses log scale, MMAPI linear scale
		return nGetVolume(easHandle, fileHandle);
	}

	/**
	 * Set the loop count.
	 * 
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @param loopCount the number of repeats, 1 for no repetitions, -1 for
	 *            infinite repeats
	 */
	static void setLoopCount(int fileHandle, int loopCount) {
		if (loopCount > 0) {
			// convert from Java paradigm to EAS
			loopCount--;
		}
		if (!nSetRepeat(easHandle, fileHandle, loopCount)) {
			// should throw exception
		}
	}

	/**
	 * Get the current loop counter.
	 * 
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return the remaining count: [loopCount] for the first playback,
	 *         [loopCount-1] for first repetition, ... 1 for the last
	 *         repetition. Or -1 for error
	 */
	static int getRemainingLoops(int fileHandle) {
		int ret = nGetCurrentRepeat(easHandle, fileHandle);
		if (ret >= 0) {
			ret++;
		}
		return ret;
	}

	/**
	 * Set the playback rate.
	 * 
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @param rate the playback rate in milli-percent
	 * @return the actual rate in milli-percent
	 */
	static int setPlaybackRate(int fileHandle, int milliRate) {
		// conversion to 4.28 fixed point
		int rate = (int) ((((long) milliRate) * 0x10000000L) / 100000L);
		int actualRate = nSetPlaybackRate(easHandle, fileHandle, rate);
		return (int) ((((long) actualRate) * 100000L) / 0x10000000L);
	}

	/**
	 * Set the playback tempo (for MIDI/tone only).
	 * 
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @param tempo the playback tempo in milli-bpm
	 * @return the actual tempo in milli-bpm, or -1 on error
	 */
	static int setPlaybackTempo(int fileHandle, int tempo) {
		int actualTempo = nGeneral(easHandle, fileHandle, COMMAND_SET_TEMPO,
				tempo);
		return actualTempo;
	}

	/**
	 * Get the playback tempo (for MIDI/tone only).
	 * 
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return the actual tempo in milli-bpm, or -1 on error
	 */
	static int getPlaybackTempo(int fileHandle) {
		int actualTempo = nGeneral(easHandle, fileHandle, COMMAND_GET_TEMPO, 0);
		return actualTempo;
	}

	/**
	 * Set the transpose factor.
	 * 
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @param transposition the number of milli-semitones
	 * @return the actual transposition in milli-semitones
	 */
	static int setTransposition(int fileHandle, int milliPitch) {
		// round the milli-pitch to integer numbers
		int transposition = (milliPitch >= 0) ? (milliPitch + 500) / 1000
				: (milliPitch - 500) / 1000;
		int actualTransposition = nSetTransposition(easHandle, fileHandle,
				transposition);
		return actualTransposition * 1000;
	}

	/**
	 * Retrieve the current type of the next meta data entry. If this method
	 * returns a value >= 0, call getNextMetaDataValue() in order to retrieve
	 * the value for this type.
	 * 
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return one of the METADATA_* constants, or -1 if no meta data available
	 */
	static int getNextMetaDataType(int fileHandle) {
		synchronized (renderLock) {
			return nGetNextMetaDataType(easHandle, fileHandle);
		}
	}

	/**
	 * Retrieve the value of the next meta data entry. Upon successful
	 * completion, this entry is removed and getNextMetaDataType() returns the
	 * next meta data type.
	 * 
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return a string with the meta data of the type of the last call to
	 *         nGetNextMetaDataType, or null on error, or if no meta data is
	 *         available
	 */
	static String getNextMetaDataValue(int fileHandle) {
		synchronized (renderLock) {
			return nGetNextMetaDataValue(easHandle, fileHandle);
		}
	}

	/**
	 * Retrieve the media duration. If duration is not already known, call it
	 * after calling getNextMetaDataType(), since it will have calculated the
	 * duration already.
	 * <p>
	 * This is a potentially time-consuming call.
	 * 
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return the media duration in microseconds, or -1 for error/not known, 
	 *         or -2 for cannot calculate now
	 */
	static long getDuration(int fileHandle) {
		synchronized (renderLock) {
			long dur = nGetDuration(easHandle, fileHandle);
			if (dur > 0) {
				dur *= 1000L;
			}
			return dur;
		}
	}

	// RECORDING

	/**
	 * Prepare the specified file stream for recording. If no locator is given,
	 * the recorded media data needs to be read with readRecordedBytes().
	 * Otherwise, the native implementation will write directly to the specified
	 * locator. This will possibly native resources that needs to be cleared up
	 * with closeRecording.
	 * 
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @param locator the file URL to write the data to, or null if the data
	 *            should be made available through readRecordedBytes().
	 * @return true if successful, false on error
	 */
	static boolean openRecording(int fileHandle, String locator) {
		return nOpenRecording(easHandle, fileHandle, locator);
	}

	/**
	 * Unprepare the specified file stream for recording. Any native resources
	 * allocated for recording are released.
	 * 
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 */
	static void closeRecording(int fileHandle) {
		nGeneral(easHandle, fileHandle, COMMAND_CLOSE_RECORDING, 0);
	}

	/**
	 * Finish this recording, correct the file header, etc. If this method
	 * returns true, readRecordedBytes() need to be called until it returns 0.
	 * After a call to commmitRecording, readRecordedBytes() will not return
	 * RS_NATIVE_STOPPED, just the number of bytes still read, and 0 when the
	 * last remaining bytes have been read.
	 * 
	 * @param fileHandle the file to commit the recording.
	 * @return true if in OutputStream recording mode and still more bytes need
	 *         to be read from the stream.
	 */
	static boolean commitRecording(int fileHandle) {
		return nGeneral(easHandle, fileHandle, COMMAND_COMMIT_RECORDING, 0) != 0;
	}

	/**
	 * Signal the native implementation to start recording. If an error occurs,
	 * a subsequent call to getRecordingState() or readRecordedBytes() should
	 * return an error code.
	 * 
	 * @param fileHandle the file to commit the recording.
	 */
	static void startRecording(int fileHandle) {
		nGeneral(easHandle, fileHandle, COMMAND_START_RECORDING, 0);
	}

	/**
	 * Signal the native implementation to stop recording. If an error occurs, a
	 * subsequent call to getRecordingState() or readRecordedBytes() should
	 * return an error code.
	 * 
	 * @param fileHandle the file to commit the recording.
	 */
	static void stopRecording(int fileHandle) {
		nGeneral(easHandle, fileHandle, COMMAND_STOP_RECORDING, 0);
	}

	/**
	 * Set the file size limit for recording. This method must work even if
	 * recording is not opened.
	 * 
	 * @param fileHandle the file to commit the recording.
	 * @param limit the file size limit, or Integer.MAX_INTEGER if not limit is
	 *            seeked.
	 */
	static void setRecordSizeLimit(int fileHandle, int limit) {
		nGeneral(easHandle, fileHandle, COMMAND_LIMIT_RECORDING, limit);
	}

	/**
	 * Get the recording state: 0 for OK, -1 for error, -2 for end of recording,
	 * requiring stop and commit.
	 * 
	 * @param fileHandle the file to commit the recording.
	 * @return the recording state
	 */
	static int getRecordingState(int fileHandle) {
		return nGeneral(easHandle, fileHandle, COMMAND_GET_RECORDING_STATE, 0);
	}

	/**
	 * Read recorded bytes. If no bytes are currently available, return 0. If an
	 * unrecoverable error occured, -1 is returned. If the end of recording is
	 * reached, because of a file size limit, -2 is returned, requiring
	 * subsequent stop and commit.
	 * 
	 * @param fileHandle the file to commit the recording.
	 * @param buffer the buffer to receive the recorded bytes
	 * @param offset the offset in buffer, where to start filling
	 * @param count the number of bytes to fill at maximum into buffer
	 * @return the number of bytes read into buffer, or the negative recording
	 *         state
	 */
	static int readRecordedBytes(int fileHandle, byte[] buffer, int offset,
			int count) {
		synchronized (renderLock) {
			return nReadRecordedBytes(easHandle, fileHandle, buffer, offset,
					count);
		}
	}

	/**
	 * Open an interactive MIDI stream on an existing MIDI file player.
	 * 
	 * @param fileHandle the handle to the existing MIDI player
	 * @return the MIDI stream handle, or 0 if not successful
	 */
	static int openInteractiveMIDI(int fileHandle) {
		return nGeneral(easHandle, fileHandle, COMMAND_OPEN_INTERACTIVE_MIDI, 0);
	}

	/**
	 * Close an interactive MIDI stream opened with openInteractiveMIDI().
	 * 
	 * @param fileHandle the handle to the existing MIDI player
	 * @param midiHandle the MIDI stream handle
	 */
	static int closeInteractiveMIDI(int fileHandle, int midiHandle) {
		return nGeneral(easHandle, fileHandle, COMMAND_CLOSE_INTERACTIVE_MIDI,
				midiHandle);
	}

	// ERROR CODE handling

	/**
	 * @return the error code string, or null
	 */
	public static String errorCodeToString(int errorCode) {
		switch (errorCode) {
		case ERROR_MALLOC_FAILED:
			return "Out of memory";
		case ERROR_FILE_OPEN_FAILED:
			return "file open failed";
		case ERROR_MAX_FILES_OPEN:
			return "max files open";
		case ERROR_UNRECOGNIZED_FORMAT:
			return "format not recognized";
		case ERROR_FILE_FORMAT:
			return "file format not supported";
		case ERROR_INVALID_PCM_TYPE:
			return "invalid pcm type";
		case ERROR_MAX_PCM_STREAMS:
			return "max PCM streams";
		case ERROR_MAX_STREAMS_OPEN:
			return "max streams open";
		case ERROR_SOUND_LIBRARY:
			return "sound library";
		case ERROR_NO_VIRTUAL_SYNTHESIZER:
			return "no virtual synthesizer";
		}
		return null;
	}

	// RENDERING THREAD

	private static RenderingThread renderingThread; // = null
	private static final Object renderLock = new Object();

	private static synchronized void createRenderThread() {
		renderingThread = new RenderingThread();
		renderingThread.doStart();
	}

	private static synchronized void destroyRenderThread() {
		if (renderingThread != null) {
			renderingThread.doStop();
			renderingThread = null;
		}
	}

	private static class RenderingThread extends Thread {
		private volatile boolean started; // = false

		/**
		 * Starts this thread.
		 */
		public synchronized void doStart() {
			if (!started) {
				started = true;
				try {
					start();
				} catch (IllegalThreadStateException itse) {
					// ignore this theoretical exception
				}
			}
		}

		/**
		 * Stops this thread and waits until it is finished.
		 */
		public void doStop() {
			if (started) {
				synchronized (this) {
					started = false;
					// this.notifyAll();
				}
				try {
					join();
				} catch (InterruptedException ie) {
					// ignore
				}
			}
		}

		public void run() {
			boolean renderResult;
			if (DEBUG) System.out.println("Start Java rendering thread.");
			while (started) {
				synchronized (renderLock) {
					renderResult = nRender(easHandle);
				}
				if (!renderResult) {
					// error condition!
					// TODO: throw ERROR to all players' listeners?
					if (DEBUG) {
						System.out.println("Java rendering thread: nRender returned error");
					}
					started = false;
				}
				if (started) {
					Thread.yield();
					// or (enable the notifyAll() above):
					// synchronized(this) { this.wait(5); }
				}
			}
			if (DEBUG) System.out.println("End Java rendering thread.");
		}
	} // of class RenderingThread

	// NATIVE METHODS

	/**
	 * Wrapper for EAS_Init()
	 * 
	 * @return the native EAS_DATA_HANDLE (easHandle), or 0 on error
	 */
	private native static int nInit();

	/**
	 * Wrapper for EAS_Shutdown()
	 * 
	 * @param easHandle the native EAS_DATA_HANDLE
	 */
	private native static void nShutdown(int easHandle);

	/**
	 * Wrapper for EAS_OpenFile().
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param locator the locator to open
	 * @param mode one of the MODE_ constants
	 * @return the instance handle of the opened file (EAS_HANDLE), or a
	 *         negative EAS error code (-1 >= err code >= ERROR_MAX)
	 */
	private native static int nOpenFile(int easHandle, String locator, int mode);

	/**
	 * Wrapper for EAS_CloseFile().
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 */
	private native static void nCloseFile(int easHandle, int fileHandle);

	/**
	 * Write data to the native layer, where it should be cached.
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @param buffer the media data buffer
	 * @param count number of valid bytes in buffer
	 * @param totalLength total file size, or -1 if not known
	 * @param moreComing if true, more data will come
	 * @return number of bytes successfully written, or a negative EAS error
	 *         code
	 */
	private native static int nWrite(int easHandle, int fileHandle,
			byte[] buffer, int offset, int count, int totalLength, int flags);

	/**
	 * A way of executing getters/setters with just one native Java method. The
	 * commandCode (one of the COMMAND_* constants) defines which native
	 * function to execute. One optional integer parameter can be passed to the
	 * native function. The function can return an integer return code.
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @param commandCode which native function to execute, one of the COMMAND_*
	 *            constants.
	 * @param param an optional parameter for the native function, depends on
	 *            commandCode what it means
	 * @return an integer return value, the meaning depends on commandCode.
	 */
	private native static int nGeneral(int easHandle, int fileHandle,
			int commandCode, int param);

	/**
	 * Wrapper for EAS_Prepare(). The file does not automatically start playing.
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return true if successful
	 */
	private native static boolean nPrepare(int easHandle, int fileHandle);

	/**
	 * Wrapper for EAS_Resume().
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return true if successful
	 */
	private native static boolean nResume(int easHandle, int fileHandle);

	/**
	 * Wrapper for EAS_Pause().
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return true if successful
	 */
	private native static boolean nPause(int easHandle, int fileHandle);

	/**
	 * Wrapper for EAS_Render(). This is only used when
	 * Config.NATIVE_RENDERING_THREAD is not set.
	 * <p>
	 * The implementation should not block! This could make the MIDP system
	 * sluggish in a green threads MIDP implementation.
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @return true if successful
	 */
	private native static boolean nRender(int easHandle);

	/**
	 * Wrapper for EAS_State().
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return the state of the synth, one of the STATE_* constants or -1 on
	 *         error
	 */
	private native static int nGetState(int easHandle, int fileHandle);

	/**
	 * Wrapper for EAS_GetLocation().
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return the media time in millis, or -1 for error
	 */
	private native static int nGetLocation(int easHandle, int fileHandle);

	/**
	 * Wrapper for EAS_Locate().
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @param time the desired media time
	 * @return the media time in millis, or -1 for error
	 */
	private native static int nLocate(int easHandle, int fileHandle, int time);

	/**
	 * Wrapper for EAS_SetVolume().
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @param level the linear volume level, 0...100
	 * @return true if successful
	 */
	private native static boolean nSetVolume(int easHandle, int fileHandle,
			int level);

	/**
	 * Wrapper for EAS_GetVolume().
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return level the linear volume level, 0...100, or -1 on error
	 */
	private native static int nGetVolume(int easHandle, int fileHandle);

	/**
	 * Wrapper for EAS_SetRepeat().
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @param repeatCount the number of repeats, 0 for no repetitions, -1 for
	 *            infinite repeats
	 * @return true if successful
	 */
	private native static boolean nSetRepeat(int easHandle, int fileHandle,
			int repeatCount);

	/**
	 * Wrapper for EAS_GetCurrentRepeat().
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return the current repeat count: <repeatCount+1> for the first playback,
	 *         <repeatCount> for the first repetition, 1 when playing the last
	 *         time the media stream. Or -1 for error.
	 */
	private native static int nGetCurrentRepeat(int easHandle, int fileHandle);

	/**
	 * Wrapper for EAS_SetPlaybackRate().
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @param rate the playback rate 4.28-bit fixed point
	 * @return the actually set playback rate
	 */
	private native static int nSetPlaybackRate(int easHandle, int fileHandle,
			int rate);

	/**
	 * Wrapper for EAS_SetTransposition().
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @param transposition the number of semitones to transpose
	 * @return the actually set transposition
	 */
	private native static int nSetTransposition(int easHandle, int fileHandle,
			int transposition);

	/**
	 * Retrieve the current type of the next meta data entry. If this method
	 * returns a value >= 0, call nGetNextMetaDataValue() in order to retrieve
	 * the value for this type.
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return one of the METADATA_* constants, or -1 if no meta data available
	 */
	private native static int nGetNextMetaDataType(int easHandle, int fileHandle);

	/**
	 * Retrieve the value of the next meta data entry. Upon successful
	 * completion, this entry is removed and nGetNextMetaDataType() returns the
	 * next meta data type.
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return a string with the meta data of the type of the last call to
	 *         nGetNextMetaDataType, or null on error, or if no meta data is
	 *         available
	 */
	private native static String nGetNextMetaDataValue(int easHandle,
			int fileHandle);

	/**
	 * Retrieve the media duration. The native layer uses EAS_ParseMetaData() to
	 * retrieve it, if it's not already called by way of the
	 * nGetNextMetaDataType() function.
	 * <p>
	 * This is a potentially time-consuming call. It only succeeds if the player
	 * is currently stopped/paused, or if the duration was previously retrieved.
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @return the media duration in millis, or -1 for error
	 */
	private native static int nGetDuration(int easHandle, int fileHandle);

	// RECORDING

	/**
	 * Prepare the specified file stream for recording. If no locator is given,
	 * the recorded media data needs to be read with nReadRecordedBytes().
	 * Otherwise, the native implementation will write directly to the specified
	 * locator.
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the handle to the open file (EAS_HANDLE)
	 * @param locator the file URL to write the data to, or null if the data
	 *            should be made available through readRecordedBytes().
	 * @return true if successful, false on error
	 */
	private native static boolean nOpenRecording(int easHandle, int fileHandle,
			String locator);

	/**
	 * Read recorded bytes. If no bytes are currently available, return 0. If an
	 * unrecoverable error occured, -1 is returned. If the end of recording is
	 * reached, because of a file size limit, -2 is returned, requiring
	 * subsequent stop and commit.
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the file to commit the recording.
	 * @param buffer the buffer to receive the recorded bytes
	 * @param offset the offset in buffer, where to start filling
	 * @param count the number of bytes to fill at maximum into buffer
	 * @return the number of bytes read into buffer, or the negative recording
	 *         state
	 */
	private native static int nReadRecordedBytes(int easHandle, int fileHandle,
			byte[] buffer, int offset, int count);

	/**
	 * Set the capture format and complete opening the device. Only after this
	 * call will the playback device and the capture device actually be opened.
	 * 
	 * @param easHandle the EAS synth handle (EAS_DATA_HANDLE)
	 * @param fileHandle the file to commit the recording.
	 * @param encoding one of the CAPTURE_ENCODING_* constants
	 * @param rate the sampling rate in Hz
	 * @param bits the number of bits per sample, e.g. 8 nor 16
	 * @param channels the number of channels, e.g. 1 for mono
	 * @param bigEndian if true, the capture device is opened in bigEndian
	 *            (ignored for bits<=8)
	 * @param isSigned if true, the capture device is opened with signed samples
	 *            (ignored for bits<=8)
	 * @return true if successful
	 */
	private native static boolean nOpenCapture(int easHandle, int fileHandle,
			int encoding, int rate, int bits, int channels, boolean bigEndian,
			boolean isSigned);
}
