package com.sonivox.mmapi;

import java.io.IOException;
import java.util.*;

import javax.microedition.media.*;
import javax.microedition.media.control.*;

/**
 * Implementation of Manager.playTone() with device://midi (i.e. the
 * MIDIControl). It uses a thread to start/stop the tones.
 */
class PlayTone extends Thread {

	/**
	 * private DEBUG flag: if set to false, the debug code will be removed from
	 * the .class file.
	 */
	private final static boolean DEBUG = false;

	/**
	 * Time to wait after all queued tones are played before closing the device.
	 */
	private final static int WAIT_TIME_AFTER_PLAY = 2000;

	/**
	 * the queue of tones. It is static in order to be able to reuse it.
	 */
	private static Vector queue = new Vector(2);

	/**
	 * the queue of tones.
	 */
	private static PlayTone pt; // = null

	/**
	 * a lock object for thread-safe access to <code>queue</code> and
	 * <code>instance</code>.
	 */
	private static Object lock = new Object();

	/**
	 * the device://midi Player owning the MIDIControl
	 */
	private Player p;

	/**
	 * the MIDIControl instance to play the tones on
	 */
	private MIDIControl mc;

	/**
	 * Create a new instance of this PlayTone thread.
	 * 
	 * @throws MediaException if impossible to get a MIDIControl.
	 */
	private PlayTone() throws MediaException {
		super();
		try {
			p = Manager.createPlayer(Manager.MIDI_DEVICE_LOCATOR);
			p.realize();
			mc = (MIDIControl) p.getControl("MIDIControl");
			p.prefetch();
			// initialize the patch to use
			mc.setProgram(0, Config.TONE_MIDI_BANK, Config.TONE_MIDI_PROGRAM);
		} catch (IOException ioe) {
			throw new MediaException(ioe.getMessage());
		}
	}

	/**
	 * @param note the MIDI note to play
	 * @param duration the duration in milliseconds
	 * @param volume the volume, 1..100
	 * @throws MediaException if impossible to queue this tone
	 */
	static void playTone(int note, int duration, int volume)
			throws MediaException {
		synchronized (lock) {
			if (pt == null) {
				pt = new PlayTone();
				pt.start();
			}
			// need to convert volume on scale 1..100 to MIDI scale 1..127
			queue.addElement(new Event(note, duration, (volume * 127) / 100));
			lock.notifyAll();
		}
		// TODO: wait for thread to actually start execution?
		// as safeguard to not leave EAS open...
	}

	/**
	 * Go through the events queue and play each event.
	 * 
	 * @see Thread#run()
	 */
	public void run() {
		if (DEBUG) System.out.println("PlayTone: thread begin");
		boolean interrupted = false;
		while (!interrupted) {
			// get the next event
			Event e = null;
			synchronized (lock) {
				// wait round
				while (true) {
					if (queue.size() > 0) {
						e = (Event) queue.elementAt(0);
						queue.removeElementAt(0);
						// exit waitRound
						break;
					} else {
						// no more entries in the queue. Wait for a few seconds
						// -- possibly there is another tone following.
						try {
							lock.wait(WAIT_TIME_AFTER_PLAY);
						} catch (InterruptedException ie) {
							interrupted = true;
						}
						if (queue.size() == 0) {
							// exit waitRound
							break;
						}
					}
				}
				// if no event could be found, despite the additional wait time,
				// exit this thread.
				if (e == null) {
					// notify the playTOne() method that it will have to create
					// a new instance of PlayTone for subsequent tones.
					pt = null;
					break;
				}
			}
			// got an event. Play it!
			mc.shortMidiEvent(0x90 /* NOTE ON, channel 0 */, e.note, e.volume);
			try {
				long stopTime = System.currentTimeMillis() + e.duration;
				// wait for the ending of the tone
				synchronized (lock) {
					int waitTime = e.duration;
					do {
						try {
							lock.wait(waitTime);
						} catch (InterruptedException ie) {
							interrupted = true;
						}
						waitTime = (int) (stopTime - System.currentTimeMillis());
					} while (waitTime > 0);
				}
				// stop the note
			} finally {
				mc.shortMidiEvent(0x90 /* NOTE ON, channel 0 */, e.note, 0);
			}
		}
		mc = null;
		p.close();
		if (DEBUG) System.out.println("PlayTone: thread exit");
	}

	/**
	 * A class to encapsulate a play tone event.
	 */
	private static class Event {
		private int note, duration, volume;

		private Event(int note, int duration, int volume) {
			this.note = note;
			this.duration = duration;
			this.volume = volume;
		}
	}
}
