package com.sonivox.mmapi;

import javax.microedition.media.*;
import javax.microedition.media.control.*;
import javax.microedition.media.protocol.*;

import java.io.*;

/**
 * The implementation of the Player interface for the EAS synth.
 */
class PlayerEAS extends PlayerBase {

	// TODO: detect underruns in native's host functions and issue BUFFERING_STARTED 
	//       etc. events from the maintenance method.

	/**
	 * private DEBUG flag: if set to false, the debug code will be removed from
	 * the .class file.
	 */
	private final static boolean DEBUG = false;

	// player types.
	// NOTE: if you add a type, also extend ManagerImpl.getPlayerType() and this
	// class' playerTypeToMode().
	public static final int TYPE_TONE_DEVICE = 1;

	public static final int TYPE_MIDI_DEVICE = 2;

	public static final int TYPE_MIDI_PLAYER = 3;

	public static final int TYPE_XMF_PLAYER = 4;

	public static final int TYPE_TONE_PLAYER = 5;

	public static final int TYPE_WAVE_PLAYER = 6;

	public static final int TYPE_WAVE_CAPTURE_PLAYER = 7;

	/**
	 * The loop count used for infinite loops in order to still retrieve EAS'
	 * loop counter.
	 */
	private static final int INFINITE_LOOPCOUNT = 1 << 20;

	/**
	 * The type of this player, one of the TYPE_ constants.
	 */
	private int playerType; // = 0

	/**
	 * The native handle as used by the EAS class. Package private to give
	 * controls access to it.
	 */
	int handle; // = 0

	/**
	 * The source stream to read data from (for MODE_STREAM)
	 */
	private SourceStream source; // = null

	/**
	 * The open mode, one of the EAS_MODE_* constants.
	 */
	private int thisMode; // = 0;

	/**
	 * The intermediate buffer used for streaming data
	 */
	private byte[] streamingBuffer; // = null

	/**
	 * Support for setting media time in stopped mode. Set to -1 to disable.
	 */
	private long queuedMediaTime = -1;

	/**
	 * Set EAS' loop count only when starting. This makes sure that the loop
	 * count is reset everytime upon play(). Set to 1 for no looping.
	 */
	private int cachedLoopCount = 1;

	/**
	 * Remember the last current loop counter returned by EAS. In the
	 * maintenance() method, it is used to calculate the number of loopbacks
	 * since the previous maintenance call.
	 */
	private int lastRemainingLoops; // = 0

	/**
	 * the instance of the volume control
	 */
	private ControlVolume vc; // = null

	/**
	 * the instance of the rate control
	 */
	private ControlRate rc; // = null

	/**
	 * the instance of the tempo control
	 */
	private ControlTempo tec; // = null

	/**
	 * the instance of the pitch control
	 */
	private ControlPitch pc; // = null

	/**
	 * the instance of the MIDI control
	 */
	private ControlMIDI mc; // = null

	/**
	 * the instance of the tone control
	 */
	private ControlTone tc; // = null

	/**
	 * the instance of the meta data control
	 */
	private ControlMetaData mdc; // = null

	/**
	 * the instance of the meta data control
	 */
	private ControlStopTime stc; // = null

	/**
	 * the instance of the record control
	 */
	private ControlRecord rec; // = null

	/**
	 * Create a new Player object of the specified type from the DataSource.
	 * 
	 * @param playerType one of the TYPE_ constants
	 * @param ds the datasource to read the media stream from, or null for the
	 *            *_DEVICE players.
	 */
	PlayerEAS(int playerType, DataSource ds) {
		super(ds);
		this.playerType = playerType;
		if (DEBUG) {
			System.out.println("Creating player with player type = "
					+ playerType + " -> open mode = " + getMode());
		}
	}

	/**
	 * @return the mode (EAS.MODE_...) used for the type of this player
	 */
	private int getMode() {
		if (thisMode == 0) {
			if ((playerType != TYPE_MIDI_DEVICE)
					&& (playerType != TYPE_TONE_DEVICE)
					&& (playerType != TYPE_WAVE_CAPTURE_PLAYER)
					&& ((getDataSource() == null) || (getDataSource() instanceof DataSourceNone))) {
				// if we don't have a data source, the locator has to be handled
				// by native
				return EAS.MODE_NATIVE;
			}
			switch (playerType) {
			case TYPE_TONE_DEVICE:
				return EAS.MODE_TONE_SEQUENCE;

			case TYPE_MIDI_DEVICE:
				return EAS.MODE_INTERACTIVE_MIDI;

			case TYPE_MIDI_PLAYER:
				return EAS.MODE_MEMORY;

			case TYPE_XMF_PLAYER:
				return EAS.MODE_MEMORY;

			case TYPE_TONE_PLAYER:
				return EAS.MODE_TONE_SEQUENCE;

			case TYPE_WAVE_PLAYER:
				return EAS.MODE_STREAM;

			case TYPE_WAVE_CAPTURE_PLAYER:
				return EAS.MODE_CAPTURE;
			}
		}
		return thisMode;
	}

	/**
	 * @return the player type, one of the TYPE_ constants
	 */
	int getPlayerType() {
		return playerType;
	}
	
	/**
	 * @return true, if this player has associated media. This is currently only false
	 * for device only players, in particular for players created from the magic locator
	 * MIDI_DEVICE_LOCATOR
	 */
	private boolean hasTimeBasedMedia() {
		return (playerType != TYPE_MIDI_DEVICE); 
	}
	
	/**
	 * @return true, if this player is associated with real time media,
	 * such as a capture stream or an RTP stream. 
	 */
	private boolean isRealtimeStream() {
		return (playerType == TYPE_WAVE_CAPTURE_PLAYER); 
	}

	/**
	 * @see PlayerBase#realizeImpl()
	 */
	protected void realizeImpl() throws MediaException {
		String locator;
		DataSource ds = getDataSource();
		if (ds != null) {
			locator = ds.getLocator();
		} else {
			locator = "";
		}
		int mode = getMode();
		if (mode <= 0) {
			// this is really an internal error and should never happen
			throw new MediaException("unsupported media type");
		}

		// set up data source stream
		boolean needToPushData = (mode == EAS.MODE_MEMORY
				|| mode == EAS.MODE_STREAM || mode == EAS.MODE_TONE_SEQUENCE)
				&& (playerType != TYPE_TONE_DEVICE)
				&& hasTimeBasedMedia() && !realizeInterrupted;

		if (needToPushData) {
			if (ds instanceof DataSourceBase) {
				// shortcut to get the stream
				source = ((DataSourceBase) ds).getStream();
			} else {
				SourceStream[] srcs = ds.getStreams();
				if (srcs.length > 0) {
					source = srcs[0];
				}
			}
			if (source == null) {
				throw new MediaException("no source stream available");
			}
			/* don't unnecessarily transfer locator to native */
			locator = null;
		}

		if (!realizeInterrupted) {
			// open the media file. Throws an exception on error
			// if handle is not 0, and we're in stream mode, we're re-opening
			if (mode == EAS.MODE_STREAM && handle != 0) {
				/* re-initialize this stream */
				EAS.reopenMedia(handle);
				totalRead = 0;
				streamingBufferRead = 0;
				streamingBufferWritten = 0;
			} else {
				if (mode == EAS.MODE_CAPTURE) {
					// special open mode for capture: need to tell the native
					// implementation the capture format
					DataSourceCapture dsc;
					if (ds instanceof DataSourceCapture) {
						dsc = (DataSourceCapture) ds;
					} else {
						throw new MediaException(
								"cannot capture from this data source");
					}
					handle = EAS.openCapture(dsc.getEncoding(),
							dsc.getSampleRate(), dsc.getBits(),
							dsc.getChannels(), dsc.isBigEndian(),
							dsc.isSigned());
				} else {
					handle = EAS.openMedia(locator, mode);
				}
			}
		}
		if (!realizeInterrupted && needToPushData) {
			// for MODE_MEMORY (and MODE_STREAM, MODE_TONE_SEQUENCE), we need to
			// push (some) data to native
			try {
				ds.connect();
				ds.start();
				doDataIO();
			} catch (IOException ioe) {
				EAS.closeMedia(handle);
				throw new MediaException(ioe.getMessage());
			} catch (MediaException me) {
				EAS.closeMedia(handle);
				throw me;
			}
		}
		if (!realizeInterrupted
				&& (!hasTimeBasedMedia() || playerType == TYPE_TONE_DEVICE)) {
			if (DEBUG) {
				System.out.println("PlayerEAS.realizeImpl: set duration to 0 because player is a device.");
			}
			setDuration(0, false);
		}
	}

	/**
	 * @see PlayerBase#prefetchImpl()
	 */
	protected void prefetchImpl() throws MediaException {
		// for in-memory media, need to read the entire file and push it to
		// native.
		// for streamed media, just push some data
		// for native media, do not do anything, let native handle it
		int mode = getMode();
		if (mode <= 0) {
			// this is really an internal error and should never happen
			throw new MediaException("invalid player mode");
		}
		EAS.prepareMedia(handle);

		if (playerType == TYPE_TONE_DEVICE) {
			// send duration updated event
			calcDuration(true);
		}
		if (hasTimeBasedMedia()) {
			// prepare the poll thread
			createPollThread();
		}
		if (mode == EAS.MODE_STREAM) {
			startPollThread();
		}
	}

	/**
	 * @see PlayerBase#startImpl()
	 */
	protected void startImpl() throws MediaException {
		if (!hasTimeBasedMedia()) {
			// nothing to do
			return;
		}
		if (queuedMediaTime >= 0) {
			// EAS cannot set media time in paused mode
			EAS.setMediaTime(handle, queuedMediaTime);
			// do not reset queuedMediaTime yet... (see below)
		}
		// commit loop count
		commitLoopCount();

		EAS.startMedia(handle);
		if (queuedMediaTime >= 0) {
			// sometimes, setting the media time before playback
			// starts is ignored by EAS
			EAS.setMediaTime(handle, queuedMediaTime);
			queuedMediaTime = -1;
		}
		startPollThread();
	}

	/**
	 * @see PlayerBase#stopImpl()
	 */
	protected void stopImpl() throws MediaException {
		if (!hasTimeBasedMedia()) {
			// nothing to do
			return;
		}
		// need to tell the maintenance method that we're about to be stopped.
		setState(PREFETCHED);
		stopPollThread();
		try {
			EAS.stopMedia(handle);
		} catch (MediaException me) {
			setState(STARTED);
			throw me;
		}
	}

	/**
	 * @see PlayerBase#deallocateImpl()
	 */
	protected void deallocateImpl() {
		/*
		 * discard any pending recording. NOTE: this should usually happen in
		 * close(), but then there is no handle for the file stream!
		 */
		if (rec != null) {
			try {
				rec.reset();
			} catch (Throwable t) {
				// ignore
			}
		}
		// close MIDIControl, if open
		if (mc != null) {
			mc.close();
		}
		// close stream, if necessary
		EAS.closeMedia(handle);
		handle = 0;
		if (getDataSource() != null) {
			getDataSource().disconnect();
		}
		destroyPollThread();
	}

	/**
	 * @see PlayerBase#closeImpl()
	 */
	protected void closeImpl() {
		source = null;
		// facilitate garbage collection
		vc = null;
		rc = null;
		pc = null;
		mc = null;
		tc = null;
		mdc = null;
		stc = null;
		rec = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#getMediaTime()
	 */
	protected long getMediaTimeImpl() {
		if (!hasTimeBasedMedia()) {
			return 0;
		}
		// Sometimes, EAS cannot set media time in paused mode
		if (getState() != STARTED && queuedMediaTime >= 0) {
			return queuedMediaTime;
		}
		long mt = EAS.getMediaTime(handle);
		/*
		 * if (DEBUG) { System.out.println("Curr media time = "+(mt/1000)+"ms"); }
		 */
		return mt;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#setMediaTime(long)
	 */
	protected long setMediaTimeImpl(long now) throws MediaException {
		if (!hasTimeBasedMedia()) {
			return 0;
		}
		if (isRealtimeStream()) {
			// cannot seek
			return getMediaTimeImpl();
		}
		SourceStream ss = getSourceStream();
		// special handling for STREAM mode
		if ((getMode() == EAS.MODE_STREAM) 
				&& (ss != null)
				&& (handle != 0)) {
			long currTime = EAS.getMediaTime(handle);
			
			// always allow seeking to current position
			if (currTime == now) {
				return now;
			}
		
			// if not seeking to beginning, disallow seeking
			if (now != 0) {
				throw new MediaException("cannot seek in stream");
			}
			// otherwise, if we can seek the stream to the beginning, reopen the stream
			if ((ss.getSeekType() == SourceStream.RANDOM_ACCESSIBLE) 
					|| (ss.getSeekType() == SourceStream.SEEKABLE_TO_START)) {
				// can seek to the beginning by re-opening the data source
				try {
					stopPollThread();
					EAS.stopMedia(handle);
					getSourceStream().seek(0);
					realizeImpl();
					if (getState() >= PREFETCHED) {
						prefetchImpl();
					}
					if (getState() >= STARTED) {
						startImpl();
					}
					queuedMediaTime = -1;
					return 0;
				} catch (IOException ioe) {
					throw new MediaException(ioe.getMessage());
				}
			}
		}
		if (DEBUG) {
			if (now == 0) {
				System.out.println("SetMediaTime to 0, but: mode="+getMode()+"  getSourceStream()="+getSourceStream()+"  handle="+handle+" EAS.getMediaTime(handle)="+EAS.getMediaTime(handle));
			}
		}
		// EAS cannot set media time in paused mode
		if (getState() != STARTED) {
			queuedMediaTime = now;
			return now;
		} else {
			return EAS.setMediaTime(handle, now);
		}
	}

	private boolean triedToGetDuration; // = false

	void calcDuration(boolean force) {
		if (getState() != UNREALIZED && getState() != STARTED
				&& ((getDurationImpl() == -1 && !triedToGetDuration) || force)) {
			long dur = EAS.getDuration(handle);
			if (DEBUG) {
				System.out.println("PlayerEAS.calcDuration: retrieved EAS duration: "
						+ dur + " microseconds");
			}
			/* prevent infinite calls to getDuration */
			triedToGetDuration = (dur != -2);
			if (dur < 0) {
				// the native implementation uses different negative codes
				dur = -1;
			}
			setDuration(dur, force);
		}
	}

	protected Control getControlImpl(String controlType) {
		if (DEBUG) System.out.print("Request for '" + controlType + "':");
		boolean isMIDITone = (playerType == TYPE_MIDI_PLAYER
				|| playerType == TYPE_XMF_PLAYER
				|| playerType == TYPE_TONE_PLAYER || playerType == TYPE_TONE_DEVICE);
		boolean isWavePlayer = (playerType == TYPE_WAVE_PLAYER);
		boolean isWave = isWavePlayer
				|| (playerType == TYPE_WAVE_CAPTURE_PLAYER);
		if (playerType != TYPE_MIDI_DEVICE && Config.HAS_VOLUMECONTROL
				&& controlType.equals(CONTROL_VOLUME)) {
			// VolumeControl
			if (vc == null) {
				vc = new ControlVolume(this);
			}
			if (DEBUG) System.out.println(vc.toString());
			return vc;
		} else if (((Config.HAS_MIDITONE_RATECONTROL && isMIDITone) || (Config.HAS_WAVE_RATECONTROL && isWavePlayer))
				&& controlType.equals(CONTROL_RATE)) {
			// RateControl
			if (rc == null) {
				rc = new ControlRate(this);
			}
			if (DEBUG) System.out.println(rc.toString());
			return rc;
		} else if (((Config.HAS_MIDITONE_TEMPOCONTROL && isMIDITone))
				&& controlType.equals(CONTROL_TEMPO)) {
			// TempoControl
			if (tec == null) {
				tec = new ControlTempo(this);
			}
			if (DEBUG) System.out.println(tec.toString());
			return tec;
		} else if ((Config.HAS_MIDITONE_PITCHCONTROL && isMIDITone)
				&& controlType.equals(CONTROL_PITCH)) {
			// PitchControl
			if (pc == null) {
				pc = new ControlPitch(this);
			}
			if (DEBUG) System.out.println(pc.toString());
			return pc;
		} else if (((Config.HAS_DEVICE_MIDICONTROL && (playerType == TYPE_MIDI_DEVICE)) || (Config.HAS_MIDI_MIDICONTROL && (playerType == TYPE_MIDI_PLAYER)))
				&& controlType.equals(CONTROL_MIDI)) {
			// MIDIControl
			if (mc == null) {
				try {
					mc = new ControlMIDI(this);
				} catch (MediaException me) {
					if (DEBUG)
						System.out.println("While getting MIDIControl: "
								+ me.toString());
					// nothing to do, mc will remain null
				}
			}
			if (DEBUG && mc != null) System.out.println(mc.toString());
			return mc;
		} else if (Config.HAS_DEVICE_TONECONTROL
				&& (playerType == TYPE_TONE_DEVICE)
				&& controlType.equals(CONTROL_TONE)) {
			// ToneControl
			if (tc == null) {
				tc = new ControlTone(this);
			}
			if (DEBUG) System.out.println(tc.toString());
			return tc;
		} else if (Config.HAS_METADATACONTROL
				&& (playerType != TYPE_TONE_DEVICE)
				&& (playerType != TYPE_TONE_PLAYER)
				&& (Config.HAS_WAVE_METADATACONTROL || playerType != TYPE_WAVE_PLAYER)
				&& (playerType != TYPE_MIDI_DEVICE)
				&& controlType.equals(CONTROL_META)) {
			// MetaDataControl
			// meta data only for media players
			if (mdc == null) {
				mdc = new ControlMetaData(this);
			}
			if (DEBUG) System.out.println(mdc.toString());
			return mdc;
		} else if (Config.HAS_STOPTIMECONTROL
				&& (playerType != TYPE_MIDI_DEVICE)
				&& controlType.equals(CONTROL_STOPTIME)) {
			// StopTimeControl
			if (stc == null) {
				stc = new ControlStopTime(this);
			}
			if (DEBUG) System.out.println(stc.toString());
			return stc;
		} else if (Config.HAS_WAVE_RECORDCONTROL && (isWave)
				&& controlType.equals(CONTROL_RECORD)) {
			// RecordControl
			if (rec == null) {
				rec = new ControlRecord(this);
			}
			if (DEBUG) System.out.println(rec.toString());
			return rec;
		}
		if (DEBUG) System.out.println("null");
		return null;
	}

	/**
	 * Implementation of seeting the loop count. The player is verified to be in
	 * the correct state, and count will never be 0. This implementation will
	 * just set cachedLoopCount to the value of <code>count</code>. The
	 * start() implementation will then actually commit the loop count to EAS.
	 * This is because MMAPI will always restart the loop count when you call
	 * start().
	 */
	public void setLoopCountImpl(int count) {
		if (isRealtimeStream()) {
			// cannot loop
			return;
		}
		if (count < 0) {
			cachedLoopCount = INFINITE_LOOPCOUNT;
		} else {
			cachedLoopCount = count;
		}
	}

	/**
	 * Commit the loop count to EAS and set the lastRemainingLoops variable to
	 * the same value, so that the maintenance method can accurately detect a
	 * loopback.
	 */
	private void commitLoopCount() {
		if (!isRealtimeStream()) {
			EAS.setLoopCount(handle, cachedLoopCount);
			lastRemainingLoops = cachedLoopCount;
		}
	}

	// DATA IO

	private int streamingBufferRead; // = 0;
	private int streamingBufferWritten; // = 0;
	private long totalRead; // = 0;
	private long totalSize; // = 0;

	/**
	 * Read some data from the data source and write it to native. If the
	 * current mode is MODE_MEMORY, the entire stream will be written to native.
	 * Otherwise, one buffer worth of data is written to native.
	 * 
	 * @return true if it finished writing to native
	 * @throws IOException if anything with reading from the source goes wrong
	 * @throws MediaException if writing the data to native fails (or, opening
	 *             the EAS stream from within the native write() function)
	 */
	private boolean doDataIO() throws IOException, MediaException {
		// for MODE_MEMORY (and MODE_STREAM, MODE_TONE_SEQUENCE), we need to
		// push (some) data
		// to native

		// set up the streaming buffer
		if (streamingBuffer == null) {
			streamingBuffer = new byte[Config.STREAM_BUFFER_SIZE];
		}
		if (totalSize == 0) {
			totalSize = source.getContentLength();
		}
		do {
			// must be able to be interrupted by a call to deallocate()
			if (getState() == UNREALIZED && realizeInterrupted) break;
			int read = 0;
			if (source != null
					&& (Config.STREAM_BUFFER_SIZE - streamingBufferRead > 0)) {
				// read a portion from the stream and write it to the native
				// layer
				read = source.read(streamingBuffer, streamingBufferRead,
						Config.STREAM_BUFFER_SIZE - streamingBufferRead);
				if (DEBUG) {
					System.out.println("Player.doDataIO(): read " + read
							+ " bytes from source");
				}
				if (getState() == UNREALIZED && realizeInterrupted) break;

				if (read == 0) {
					Thread.yield();
				} else if (read > 0) {
					totalRead += read;
					streamingBufferRead += read;
				}
			}
			boolean EOF = (totalRead == totalSize || read == -1);

			if (EOF) {
				// remember the total size
				totalSize = totalRead;
				// can close the stream here
				getDataSource().disconnect();
				source = null;
			}
			if (getState() == UNREALIZED && realizeInterrupted) break;

			// for STREAM, always write what we got (to minimize risk of buffer
			// underruns). The native layer uses a circular buffer anyway.
			// for MEMORY, only write in full buffers.
			if (EOF || (getMode() == EAS.MODE_STREAM)
					|| streamingBufferRead == Config.STREAM_BUFFER_SIZE) {
				// write a buffer to native
				int written = EAS.write(handle, streamingBuffer,
						streamingBufferWritten, streamingBufferRead
								- streamingBufferWritten, totalSize, !EOF);
				if (DEBUG) {
					System.out.println("Player.doDataIO(): wrote " + written
							+ " bytes to native");
				}
				if (written > 0) {
					streamingBufferWritten += written;
				}
				// MEMORY requires that always the full buffer is written
				if ((getMode() == EAS.MODE_MEMORY || getMode() == EAS.MODE_TONE_SEQUENCE)
						&& written != streamingBufferRead) {
					throw new MediaException("native buffering error");
				}
				if (streamingBufferWritten >= streamingBufferRead) {
					streamingBufferRead = 0;
					streamingBufferWritten = 0;
				} else {
					// only full EOF if we've written all data to native
					EOF = false;
				}
			}

			if (EOF) {
				streamingBuffer = null;
				// native may have changed mode
				thisMode = EAS.getMode(handle);
				if (DEBUG) System.out.println("Got mode from native: "+thisMode);
				return true;
			}
			// repeat for MEMORY mode, or if not a full buffer was read
		} while (totalRead < Config.STREAM_BUFFER_SIZE
				* Config.STREAM_BUFFER_PREFETCH_COUNT
				|| getMode() == EAS.MODE_MEMORY
				|| getMode() == EAS.MODE_TONE_SEQUENCE);
		return false;
	}

	// MAINTENANCE / POLL THREAD

	/**
	 * The number of milliseconds to wait in between calls to maintenance() from
	 * the poll thread.
	 */
	private final static int POLL_THREAD_WAIT_MILLIS = 50;

	/**
	 * The number of milliseconds to wait in between calls to maintenance() from
	 * the poll thread while still pushing data to native
	 */
	private final static int POLL_THREAD_WAIT_MILLIS_IO = 20;

	/**
	 * carry out regular maintenance tasks:
	 * <li>verifying EAS state
	 * <li>checking EAS loop count
	 * <li>sending EOM event
	 * <li>checking stop time control
	 * <li>reading/pushing streaming data
	 * <li>checking new meta data
	 * 
	 * @return the number of milliseconds to wait
	 */
	// sync: should never be called during stop, deallocate, method, etc.
	int maintenance() {
		int waitTime = POLL_THREAD_WAIT_MILLIS;

		int state = EAS.getState(handle); // sync on EAS
		if (state == EAS.STATE_ERROR) {
			error("media playback error");
			return waitTime;
		}
		if (getState() == STARTED) {

			// sync on EAS
			int remainingLoops = EAS.getRemainingLoops(handle);
			
			// check recording
			if (rec != null) {
				try {
					if (rec.update(false)) {
						waitTime = POLL_THREAD_WAIT_MILLIS_IO;
					}
				} catch(IOException ioe) {
					// cannot happen
				}
			}

			// check for end of media
			if (remainingLoops == 1 && state != EAS.STATE_PLAY) {
				if (DEBUG) {
					System.out.println("PlayerEAS.maintenance: found end of media.");
				}
				// we have an end of media

				// sync on this and EAS
				long mediaTime = getMediaTime();
				try {
					stopImpl();
				} catch (MediaException e) {
					// nothing we can do here
					if (DEBUG) {
						System.out.println("Unexpected exception: " + e);
					}
				}

				// manually change the state (usually done in stop())
				setState(PREFETCHED);
				dispatchMessage(PlayerListener.END_OF_MEDIA,
						new Long(mediaTime));

				// if duration isn't set, do it now
				if (getDurationImpl() == TIME_UNKNOWN && mediaTime > 0) {
					setDuration(mediaTime, false);
				}
				return waitTime;
			} else

			// check repeat count
			if (cachedLoopCount != 1) {
				// number of loop backs to the beginning
				int loopbacks = lastRemainingLoops - remainingLoops;
				if (loopbacks > 0) {
					if (DEBUG) {
						System.out.println("PlayerEAS: Looped back "
								+ loopbacks + " times. Remaining loops:"
								+ remainingLoops);
					}
					// send EOM+STARTED for every loop back
					// sync on this and EAS
					Long dur = new Long(getDuration());
					for (; loopbacks > 0; loopbacks--) {
						dispatchMessage(PlayerListener.END_OF_MEDIA, dur);
						dispatchMessage(PlayerListener.STARTED, new Long(0));
					}
					// remember the new remaining loop count
					lastRemainingLoops = remainingLoops;
				}
				if (cachedLoopCount == INFINITE_LOOPCOUNT
						&& remainingLoops < 100) {
					// safeguard against remainingLoops becoming 1 while actual
					// looping is infinite
					commitLoopCount();
				}
			}

			// check stop time control
			if (stc != null && stc.getStopTime() != StopTimeControl.RESET) {
				// sync on this and EAS
				long mediaTime = getMediaTime();
				if (mediaTime > stc.getStopTime()) {
					if (DEBUG) {
						System.out.println("PlayerEAS.maintenance: stop because of StopTimeControl.");
					}
					try {
						stopImpl();
					} catch (MediaException e) {
						// nothing we can do here
						if (DEBUG) {
							System.out.println("Unexpected exception: " + e);
						}
					}
					// manually change the state (usually done in stop())
					setState(PREFETCHED);
					stc.setStopTime(StopTimeControl.RESET);
					dispatchMessage(PlayerListener.STOPPED_AT_TIME, new Long(
							mediaTime));
					if (DEBUG) {
						System.out.println("PlayerEAS.maintenance: StopTimeControl handling done.");
					}
					return waitTime;
				}
			}
		}

		// need to execute data io?
		if (streamingBuffer != null) {
			try {
				if (!doDataIO()) {
					waitTime = POLL_THREAD_WAIT_MILLIS_IO;
				}
			} catch (Exception e) {
				// unrecoverable error
				if (DEBUG)
					System.out.println("PlayerEAS.maintenance(): error during doDataIO(): "
							+ e.getMessage());
				streamingBuffer = null;
			}
		}
		return waitTime;
	}

	/**
	 * Called from maintenance(), ManagerEAS or EAS when a non-recoverable error
	 * occured.
	 */
	synchronized void error(String message) {
		if (getState() != CLOSED) {
			// need to close, but will 100% cause a deadlock if called
			// from a different thread. So let the close() be executed
			// from the message dispatch thread?
			dispatchMessage(PlayerListener.ERROR, message);
		}
	}

	// POLL THREAD

	private PollThread pollThread; // = null

	private synchronized void createPollThread() {
		if (pollThread != null) {
			destroyPollThread();
		}
		pollThread = new PollThread();
	}

	private synchronized void startPollThread() {
		if (pollThread == null) {
			createPollThread();
		}
		pollThread.doStart();
	}

	private synchronized void stopPollThread() {
		if (pollThread != null) {
			pollThread.doStop();
		}
	}

	private synchronized void destroyPollThread() {
		if (pollThread != null) {
			pollThread.doDestroy();
			pollThread = null;
		}
	}

	private class PollThread extends Thread {
		private volatile boolean destroyed; // = false
		private volatile boolean paused = true;

		PollThread() {
			super();
			try {
				start();
			} catch (IllegalThreadStateException itse) {
				// ignore this theoretical exception
			}
		}

		/**
		 * Starts this thread.
		 */
		public void doStart() {
			if (!destroyed && paused) {
				paused = false;
				synchronized (this) {
					this.notifyAll();
				}
			}
		}

		/**
		 * Pause this thread.
		 */
		public void doStop() {
			if (!destroyed && !paused) {
				paused = true;
			}
		}

		/**
		 * Stops this thread and waits until it is finished.
		 */
		public void doDestroy() {
			if (!destroyed) {
				destroyed = true;
				synchronized (this) {
					this.notifyAll();
				}
				// do not use join here! Will deadlock on PlayerEAS.this
				synchronized (PlayerEAS.this) {
					try {
						if (this.isAlive()) {
							PlayerEAS.this.wait();
						}
					} catch (InterruptedException ie) {
						// ignore
					}
				}
			}
		}

		public void run() {
			if (DEBUG) {
				System.out.println("Start Java poll thread for player with handle="
						+ handle);
			}
			try {
				int waitTime = POLL_THREAD_WAIT_MILLIS;
				while (!destroyed) {
					synchronized (this) {
						if (paused) {
							this.wait();
						} else {
							this.wait(waitTime);
						}
					}
					if (!destroyed && !paused) {
						waitTime = maintenance();
					}
				}
			} catch (Throwable t) {
				if (DEBUG) {
					System.out.println("Exception in poll thread: " + t);
					error(t.getMessage());
				}
			}
			if (DEBUG) {
				System.out.println("End Java poll thread for player with handle="
						+ handle);
			}
			synchronized (PlayerEAS.this) {
				PlayerEAS.this.notifyAll();
			}
		}
	} // of class PollThread
}
