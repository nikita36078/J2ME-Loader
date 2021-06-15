package com.sonivox.mmapi;

import java.util.Vector;

import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;

/**
 * Thread for asynchronously dispatching PlayerListener events.
 */
public class EventDispatcher extends Thread {
	/**
	 * private DEBUG flag: if set to false, the debug code will be removed from
	 * the .class file.
	 */
	private final static boolean DEBUG = false;

	/**
	 * Time to wait after all queued events are played before exiting the thread
	 * and requiring a new thread to be created for the next event to be 
	 * dispatched.
	 */
	private final static int WAIT_TIME_BEFORE_THREAD_EXIT = 15000;

	/**
	 * the queue of events. It is static in order to be able to reuse the queue.
	 */
	private static Vector queue = new Vector(2);

	/**
	 * the EventDispatcher instance
	 */
	private static EventDispatcher ed; // = null

	/**
	 * a lock object for thread-safe access to <code>queue</code> and
	 * <code>instance</code>.
	 */
	private static Object lock = new Object();

	/**
	 * Create a new instance of this EventDispatcher thread. Only one 
	 * instance will ever live at once.
	 */
	private EventDispatcher() {
		super();
	}

	/**
	 * Dispatch this message asynchronously
	 * @param player the player that sends the message
	 * @param event the event type
	 * @param param the event parameter
	 * @param listeners the listeners
	 */
	static void dispatchEvent(Player player, String event, Object param, Vector listeners) {
		if (!event.equals(PlayerListener.ERROR)
				&& (listeners == null || listeners.size() == 0)) {
			// no need to dispatch events to zero listeners
			return;
		}
		synchronized (lock) {
			if (ed == null) {
				ed = new EventDispatcher();
				ed.start();
			}
			queue.addElement(new Event(player, event, param, listeners));
			lock.notifyAll();
		}
	}

	/**
	 * Go through the events queue and dispatch them to the individual listeners.
	 * 
	 * @see Thread#run()
	 */
	public void run() {
		if (DEBUG) System.out.println("EventDispatcher: thread begin");
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
						// -- possibly there is another event following.
						try {
							lock.wait(WAIT_TIME_BEFORE_THREAD_EXIT);
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
					// notify the dispatchEvent() method that it will have to create
					// a new instance of EventDispatcher for subsequent events.
					ed = null;
					break;
				}
			}
			// got an event. Send it!
			// special case: sending the ERROR event requires the player to be closed 
			if (e.event.equals(PlayerListener.ERROR)) {
				try {
					e.player.close();
				} catch (Throwable t) {
					if (DEBUG) {
						System.out.println("EventDispatcher: called close() before ERROR event");
						System.out.println("                 causing exception: "+t);
					}
				}
			}
			for (int i = 0; i < e.listeners.size(); i++) {
				try {
					((PlayerListener) e.listeners.elementAt(i)).playerUpdate(
							e.player, e.event, e.param);
				} catch (Throwable t) {
					// ignore
				}
			}
		}
		if (DEBUG) System.out.println("EventDispatcher: thread exit");
	}

	/**
	 * A class to encapsulate a player event.
	 */
	private static class Event {
		private Player player;
		private String event;
		private Object param;
		private Vector listeners;

		public Event(Player player, String event, Object param, Vector listeners) {
			this.player = player;
			this.event = event;
			this.param = param;
			this.listeners = listeners;
		}

	}
}
