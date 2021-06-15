package com.sonivox.mmapi;

import javax.microedition.media.*;
import javax.microedition.media.protocol.*;
import java.util.*;

/**
 * A base class for Players. It implements some of the basic functionality like
 * listener handling, state transitions, etc.
 */
abstract class PlayerBase implements Player, TimeBase {

	/**
	 * private DEBUG flag: if set to false, the debug code will be removed from
	 * the .class file.
	 */
	private final static boolean DEBUG = false;

	/**
	 * The list of player listeners
	 */
	private Vector listeners; // = null

	/**
	 * The current state (CLOSED, REALIZED, ...)
	 */
	private int state = UNREALIZED;

	/**
	 * The data source to read from.
	 */
	private DataSource dataSource; // = null

	/**
	 * The source stream to read the audio data from (or null)
	 */
	protected SourceStream sourceStream; // = null

	/**
	 * The duration. Descendant classes call setDuration() to update it.
	 */
	private long duration = TIME_UNKNOWN;

	/**
	 * The time base for this player. Usually, this is itself. Only if
	 * setTimeBase was called will the time base change to the master. In that
	 * case, the master's slaveTimeBase is set to this player.
	 */
	private PlayerBase timeBase; // = null;

	/**
	 * If non-null, the slave player is synced with setTimeBase(). All transport
	 * methods are executed on that player, too.
	 */
	protected PlayerBase slavePlayer; // = null;

	/**
	 * The time base offset. Is changed when the player enters/leaves STARTED
	 * mode, so that during playback the time base progresses with the rate of
	 * media time.
	 */
	private long timeBaseOffset; // = null;
	
	/**
	 * realize() is specified that it can be interrupted by a concurrent 
	 * call to deallocate. If that happens, this flag is set to true.
	 */
	protected boolean realizeInterrupted; // = false; 

	/**
	 * Constructs a player with the provided datasource.
	 * 
	 * @param ds the data source to construct this player from.
	 */
	protected PlayerBase(DataSource ds) {
		this.dataSource = ds;
	}

	protected DataSource getDataSource() {
		return dataSource;
	}

	protected SourceStream getSourceStream() {
		return sourceStream;
	}

	// STATE MACHINE

	/**
	 * utility method: throws IllegalStateException if the player is in the
	 * specified state. This is necessary to be called in most methods in order
	 * to satisfy the spec.
	 */
	protected void mustNotBe(int forbiddenState) {
		if (state == forbiddenState) {
			String sState = "";
			switch (forbiddenState) {
			case UNREALIZED:
				sState = "unrealized";
				break;
			case REALIZED:
				sState = "realized";
				break;
			case PREFETCHED:
				sState = "prefetched";
				break;
			case STARTED:
				sState = "started";
				break;
			case CLOSED:
				sState = "closed";
				break;
			}
			throw new IllegalStateException("player is " + sState);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#getState()
	 */
	public final int getState() {
		return state;
	}

	/**
	 * internal function to set the time base. May trigger some special actions,
	 * like changing the time base's offset.
	 */
	protected void setState(int newState) {
		if (DEBUG) System.out.println(">setState(" + newState + ")");
		if (newState == STARTED && state < STARTED) {
			// modify the time base to tick from the media time
			timeBaseOffset = getTime() - getMediaTime();
		} else if (state == STARTED && newState < STARTED) {
			// modify the time base to tick from the system time from now on
			timeBaseOffset = (System.currentTimeMillis() * 1000L) - getTime();
		}
		this.state = newState;
		if (DEBUG) System.out.println("<setState()");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#realize()
	 */
	public final void realize() throws MediaException {
		if (DEBUG) System.out.println(">realize()");
		realizeInterrupted = false;
		mustNotBe(CLOSED);
		if (state < REALIZED) {
			// prepare the source stream for reading
			SourceStream[] streams = dataSource.getStreams();
			if (streams.length > 0) {
				sourceStream = streams[0];
			} else {
				// case for device: and DataSourceNative
				sourceStream = null;
			}
			if (!realizeInterrupted) {
				// call the implementation of realize
				realizeImpl();
			}
			if (!realizeInterrupted) {
				setState(REALIZED);
			}
		}
		if (DEBUG) System.out.println("<realize()");
	}

	/**
	 * Actual implementation method of the realize() method.
	 * 
	 * @throws MediaException
	 */
	protected abstract void realizeImpl() throws MediaException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#prefetch()
	 */
	public final synchronized void prefetch() throws MediaException {
		if (DEBUG) System.out.println(">prefetch()");
		mustNotBe(CLOSED);

		if (state < PREFETCHED) {
			realize();
			prefetchImpl();
			setState(PREFETCHED);
		}
		if (DEBUG) System.out.println("<prefetch()");
	}

	/**
	 * Actual implementation method of the prefetch() method.
	 * 
	 * @throws MediaException
	 */
	protected abstract void prefetchImpl() throws MediaException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#start()
	 */
	public final synchronized void start() throws MediaException {
		mustNotBe(CLOSED);

		if (DEBUG) System.out.println(">start()");
		// sync support
		if (slavePlayer != null) {
			try {
				slavePlayer.prefetch();
			} catch (Exception e) {
				if (DEBUG) {
					System.out.println("Caught slave player exception: " + e);
				}
			}
		}
		if (state < STARTED) {
			prefetch();
			startImpl();
			setState(STARTED);
			dispatchMessage(PlayerListener.STARTED, new Long(getMediaTime()));
			// if the duration is zero (e.g. MIDI device player), stop
			// immediately
			if (duration == 0) {
				if (DEBUG) {
					System.out.println("PlayerBase.start: stop because duration is 0.");
				}
				stop();
			}
		}
		// sync support
		if (slavePlayer != null) {
			try {
				slavePlayer.start();
			} catch (Exception e) {
				if (DEBUG) {
					System.out.println("Caught slave player exception: " + e);
				}
			}
		}
		if (DEBUG) System.out.println("<start()");
	}

	/**
	 * Actual implementation method of the start() method.
	 * 
	 * @throws MediaException
	 */
	protected abstract void startImpl() throws MediaException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#stop()
	 */
	public final synchronized void stop() throws MediaException {
		if (DEBUG) System.out.println(">stop()");
		mustNotBe(CLOSED);

		if (state >= STARTED) {
			stopImpl();
			setState(PREFETCHED);
			dispatchMessage(PlayerListener.STOPPED, new Long(getMediaTime()));
		}
		// sync support
		if (slavePlayer != null) {
			try {
				slavePlayer.stop();
			} catch (Exception e) {
				if (DEBUG) {
					System.out.println("Caught slave player exception: " + e);
				}
			}
		}
		if (DEBUG) System.out.println("<stop()");
	}

	/**
	 * Actual implementation method of the stop() method.
	 * 
	 * @throws MediaException
	 */
	protected abstract void stopImpl() throws MediaException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#deallocate()
	 */
	public final synchronized void deallocate() {
		if (DEBUG) System.out.println(">deallocate()");
		realizeInterrupted = true;
		mustNotBe(CLOSED);

		if (state >= PREFETCHED) {
			try {
				stop();
			} catch (MediaException e) {
				// what here? spec shortcoming
			}
			deallocateImpl();
			setState(REALIZED);
		}
		if (DEBUG) System.out.println("<deallocate()");
	}

	/**
	 * Actual implementation method of the deallocate() method.
	 */
	protected abstract void deallocateImpl();

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#close()
	 */
	public final synchronized void close() {
		if (DEBUG) System.out.println(">close()");
		if (state > CLOSED) {
			deallocate();
			closeImpl();
			if (dataSource != null) {
				dataSource.disconnect();
			}
			setState(CLOSED);
			dispatchMessage(PlayerListener.CLOSED, null);
			// don't need listeners (except for dispatching?)
			listeners = null;
		}
		if (DEBUG) System.out.println("<close()");
	}

	/**
	 * Actual implementation method of the close() method.
	 */
	protected abstract void closeImpl();

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#setLoopCount(int)
	 */
	public final void setLoopCount(int count) {
		mustNotBe(CLOSED);
		mustNotBe(STARTED);
		if (count == 0) {
			throw new IllegalArgumentException("loop count must not be 0");
		}
		setLoopCountImpl(count);
		// sync support
		if (slavePlayer != null) {
			try {
				slavePlayer.setLoopCount(count);
			} catch (Exception e) {
				if (DEBUG) {
					System.out.println("Caught slave player exception: " + e);
				}
			}
		}
	}

	/**
	 * Actual implementation method of the setLoopCount() method.
	 */
	public abstract void setLoopCountImpl(int count);

	// LISTENER SUPPORT

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#addPlayerListener(javax.microedition.media.PlayerListener)
	 */
	public final void addPlayerListener(PlayerListener playerListener) {
		mustNotBe(CLOSED);
		if (listeners == null) {
			listeners = new Vector(2);
		}
		if (!listeners.contains(playerListener)) {
			listeners.addElement(playerListener);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#removePlayerListener(javax.microedition.media.PlayerListener)
	 */
	public final void removePlayerListener(PlayerListener playerListener) {
		mustNotBe(CLOSED);
		if (listeners != null) {
			listeners.removeElement(playerListener);
		}
	}

	/**
	 * Sends the message to all registered listeners.
	 * 
	 * @param message the message to dispatch, typically one of the
	 *            PlayerListener constants.
	 */
	void dispatchMessage(String event, Object eventData) {
		// do not check listeners here, event dispatcher is responsible for
		// closing the player in ERROR case
		EventDispatcher.dispatchEvent(this, event, eventData, this.listeners);
	}

	// media time related

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#getDuration()
	 */
	public final long getDuration() {
		mustNotBe(CLOSED);
		if (duration == -1) {
			calcDuration(false);
		}
		return duration;
	}

	/**
	 * Method for descendants to retrieve the value of the duration field
	 * without triggering calcDuration().
	 */
	protected final long getDurationImpl() {
		return duration;
	}

	/**
	 * Calculate the duration of the media file. Use setDuration to set an
	 * updated duration.
	 * @param force: force the update
	 */
	abstract void calcDuration(boolean force);

	protected void setDuration(long newDuration, boolean force) {
		if (newDuration != -1 || force) {
			boolean sendEvent = 
				    (getState() != UNREALIZED)
					&& (newDuration != TIME_UNKNOWN)
					&& (newDuration != duration)
					&& (newDuration != -1);
			if (DEBUG) {
				System.out.println("PlayerBase.setDuration("+newDuration+")");
			}
			duration = newDuration;
			if (sendEvent) {
				// send event
				dispatchMessage(PlayerListener.DURATION_UPDATED, new Long(
						duration));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#getMediaTime()
	 */
	public final long getMediaTime() {
		mustNotBe(CLOSED);
		mustNotBe(UNREALIZED);
		// return TIME_UNKNOWN;
		return getMediaTimeImpl();
	}

	protected abstract long getMediaTimeImpl();

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#setMediaTime(long)
	 */
	public final long setMediaTime(long now) throws MediaException {
		if (DEBUG) {
			System.out.println("Entering setMediaTime("+now+")");
		}
		mustNotBe(CLOSED);
		mustNotBe(UNREALIZED);
		if (now < 0) {
			now = 0;
		} else {
			long dur = getDurationImpl();
			if (dur != -1 && now > dur) {
				now = dur;
			}
		}
		// throw new MediaException("cannot set media time");
		now = setMediaTimeImpl(now);
		// sync support
		if (slavePlayer != null) {
			try {
				slavePlayer.setMediaTime(now);
			} catch (Exception e) {
				if (DEBUG) {
					System.out.println("Caught slave player exception: " + e);
				}
			}
		}
		return now;
	}

	protected abstract long setMediaTimeImpl(long now) throws MediaException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#getTimeBase()
	 */
	public TimeBase getTimeBase() {
		mustNotBe(CLOSED);
		mustNotBe(UNREALIZED);
		if (timeBaseOffset == 0) {
			timeBaseOffset = (System.currentTimeMillis() * 1000L);
		}
		if (timeBase == null) {
			return this;
		}
		return timeBase;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#setTimeBase(javax.microedition.media.TimeBase)
	 */
	public void setTimeBase(TimeBase master) throws MediaException {
		mustNotBe(CLOSED);
		mustNotBe(UNREALIZED);
		mustNotBe(STARTED);

		if (master == null || (master == this)) {
			if (timeBase != null && timeBase.slavePlayer == this) {
				timeBase.slavePlayer = null;
			}
			// reset time base
			timeBase = null;
		} else if (master instanceof PlayerBase) {
			timeBase = (PlayerBase) master;
			timeBase.slavePlayer = this;
		} else {
			throw new MediaException("setting time base not supported");
		}
	}

	/**
	 * Implementation of the TimeBase interface.
	 * 
	 * @see TimeBase#getTime()
	 */
	public long getTime() {
		if (state == STARTED) {
			return getMediaTime() + timeBaseOffset;
		}
		return (System.currentTimeMillis() * 1000L) - timeBaseOffset;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Player#getContentType()
	 */
	public final String getContentType() {
		mustNotBe(CLOSED);
		mustNotBe(UNREALIZED);
		if (dataSource != null) {
			return dataSource.getContentType();
		}
		// should not return null here
		return "";
	}

	// CONTROLS

	/**
	 * the package name where the default MMAPI controls are located. Note the
	 * dot at the end
	 */
	private final static String CONTROL_PACKAGE = "javax.microedition.media.control.";

	// if adding a control to this list, also add it in the Players'
	// getControlImpl() method
	// and getControls() below.

	protected final static String CONTROL_META = "MetaDataControl";
	protected final static String CONTROL_MIDI = "MIDIControl";
	protected final static String CONTROL_PITCH = "PitchControl";
	protected final static String CONTROL_RATE = "RateControl";
	protected final static String CONTROL_RECORD = "RecordControl";
	protected final static String CONTROL_STOPTIME = "StopTimeControl";
	protected final static String CONTROL_TEMPO = "TempoControl";
	protected final static String CONTROL_TONE = "ToneControl";
	protected final static String CONTROL_VOLUME = "VolumeControl";

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Controllable#getControl(java.lang.String)
	 */
	public final Control getControl(String controlType) {
		mustNotBe(CLOSED);
		mustNotBe(UNREALIZED);
		if (controlType == null) {
			throw new IllegalArgumentException();
		}
		// allow fully qualified path names, or just the remainder
		if (controlType.startsWith(CONTROL_PACKAGE)) {
			controlType = controlType.substring(CONTROL_PACKAGE.length());
		}
		return getControlImpl(controlType);
	}

	/**
	 * Retrieve the requested control.
	 * 
	 * @param controlType the name of the control class without prefixed
	 *            &quot;javax.microedition.media.control&quot;
	 * @return the control, or null if not available
	 */
	protected abstract Control getControlImpl(String controlType);

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.Controllable#getControls()
	 */
	public final Control[] getControls() {
		mustNotBe(CLOSED);
		mustNotBe(UNREALIZED);

		Vector v = new Vector(6);
		Control c;

		if ((c = getControl(CONTROL_META)) != null) v.addElement(c);
		if ((c = getControl(CONTROL_MIDI)) != null) v.addElement(c);
		if ((c = getControl(CONTROL_PITCH)) != null) v.addElement(c);
		if ((c = getControl(CONTROL_RATE)) != null) v.addElement(c);
		if ((c = getControl(CONTROL_RECORD)) != null) v.addElement(c);
		if ((c = getControl(CONTROL_STOPTIME)) != null) v.addElement(c);
		if ((c = getControl(CONTROL_TEMPO)) != null) v.addElement(c);
		if ((c = getControl(CONTROL_TONE)) != null) v.addElement(c);
		if ((c = getControl(CONTROL_VOLUME)) != null) v.addElement(c);

		Control[] ret = new Control[v.size()];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = (Control) v.elementAt(i);
		}
		return ret;
	}
}
