/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017 Nikita Shakarun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.microedition.lcdui.event;

import javax.microedition.util.LinkedEntry;
import javax.microedition.util.LinkedList;

/**
 * The event queue. A really complicated thing.
 */
public class EventQueue implements Runnable {
	protected LinkedList<Event> queue;
	protected Event event;

	protected boolean enabled;
	protected Thread thread;

	private final Object waiter;
	private final Object interlock;

	private boolean running;
	private boolean continuerun;

	private static boolean immediate;

	public EventQueue() {
		queue = new LinkedList<>();

		waiter = new Object();
		interlock = new Object();

		immediate = false;
	}

	/**
	 * Enable immediate processing mode.
	 * <p>
	 * In this mode event are processed as soon as they arrive,
	 * without queue (violates serialization principle).
	 * <p>
	 * You can try to turn on this mode if every frame counts,
	 * but the midlet behavior will be unpredictable.
	 *
	 * @param value true if the immediate processing mode shoud be enabled
	 */
	public static void setImmediate(boolean value) {
		immediate = value;
	}

	/**
	 * Add event to the queue.
	 * <p>
	 * If the immediate processing mode is enabled,
	 * the event is processed here,
	 * in this case there is no queue at all
	 * <p>
	 * If an event has been added to the queue,
	 * its enterQueue() method is called.
	 *
	 * @param event the added event
	 */
	public void postEvent(Event event) {

		if (immediate)        // the immediate processing mode is enabled
		{
			event.run();    // process event on the spot
			return;            // and nothing to do here
		}

		boolean empty;

		synchronized (queue)    // all operations with the queue must be synchronized (on itself)
		{
			empty = queue.isEmpty();

			if (empty || event.placeableAfter(queue.getLast())) {
				/*
				 * If the queue itself is empty, then this already implies that either
				 * exactly one event remains and it is now being processed,
				 * or there is not a single event left at all.
				 *
				 * In both cases, a new event should be added to the queue,
				 * regardless of event.placeableAfter() value.
				 */

				queue.addLast(event);
				event.enterQueue();
			} else {
				// it is more correct, but additional checks are required
				// queue.setLast(event).recycle(); // remove the previous event and add the new one.
				event.recycle(); // more reliable // leave the previous event, recycle the new one.
			}
		}

		if (empty) {
			/*
			 * on the other hand, if the queue was non-empty,
			 * there is at least one more iteration for the events,
			 * and this is not necessary
			 */

			synchronized (waiter) {
				if (running) {
					continuerun = true;
				} else {
					waiter.notifyAll();
				}
			}
		}
	}

	/**
	 * Remove events from the queue that match the specified filter.
	 *
	 * @param filter the event filter for deletion
	 * @return true, if something has been removed
	 */
	public boolean removeEvents(EventFilter filter) {
		if (queue.isEmpty()) {
			return false;
		}

		boolean removed = false;

		synchronized (queue) {

			LinkedEntry<Event> entry = queue.firstEntry();
			LinkedEntry<Event> last = queue.lastEntry();
			LinkedEntry<Event> next;

			while (true) {

				next = entry.nextEntry();

				Event element = entry.getElement();
				if (filter.accept(element)) {
					element.leaveQueue();
					element.recycle();
					queue.recycleEntry(entry);
					removed = true;
				}

				if (entry != last) {
					entry = next;
				} else {
					break;
				}
			}
		}

		return removed;
	}

	/**
	 * Check if there is anything in the queue.
	 *
	 * @return true, if the queue is empty
	 */
	public boolean isEmpty() {
		return queue.isEmpty();
	}

	/**
	 * Clear the queue.
	 */
	public void clear() {
		synchronized (queue) {
			queue.clear();
		}
	}

	/**
	 * Start the event loop.
	 * Repeated calls to this method are ignored.
	 */
	public void startProcessing() {
		enabled = true;

		if (thread == null) {
			thread = new Thread(this, "MIDletEventQueue");
			thread.start();
		}
	}

	/**
	 * Stop the event loop.
	 * This method is blocked until the loop is completely stopped.
	 */
	public void stopProcessing() {
		enabled = false;

		synchronized (waiter) {
			waiter.notifyAll();
		}

		synchronized (interlock) {
			thread = null;
		}
	}

	/**
	 * @return the current event being processed, or null
	 */
	public Event currentEvent() {
		return event;
	}

	/**
	 * Here is the main event loop.
	 */
	@Override
	public void run() {
		synchronized (interlock) {
			running = true;

			while (enabled) {
				/*
				 * blocking order:
				 *
				 * 1 - this
				 * 2 - queue
				 *
				 * accordingly, inside the Canvas.serviceRepaints(), the order must be the same,
				 * otherwise mutual blocking of two threads is possible (everything will hang)
				 */

				synchronized (this)        // needed for Canvas.serviceRepaints()
				{
					synchronized (queue)    // needed for postEvent()
					{
						event = queue.removeFirst();    // get the first item and immediately remove from the queue
					}
				}

				if (event != null) {
					event.run();

					synchronized (this) {
						synchronized (queue) {
							event = null;
						}

						this.notifyAll();
					}
				} else {
					synchronized (waiter) {
						if (continuerun) {
							continuerun = false;
						} else {
							running = false;

							try {
								waiter.wait();
							} catch (InterruptedException ie) {
								ie.printStackTrace();
							}

							running = true;
						}
					}
				}
			}
		}
	}
}