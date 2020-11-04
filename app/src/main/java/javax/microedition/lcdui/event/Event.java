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

import android.util.Log;

/**
 * The base class for all events.
 */
public abstract class Event implements Runnable {
	/**
	 * Event handling.
	 * This is where you need to perform the required actions.
	 */
	public abstract void process();

	/**
	 * Event recycling
	 * <p>
	 * If a pool of events is used, then the event must be reset
	 * and returned to the pool.
	 */
	public abstract void recycle();

	/**
	 * Handle the event and recycle it.
	 */
	@Override
	public void run() {
		try {
			process();
		} catch (Exception e) {
			Log.e(getClass().getName(), "process: ", e);
		} finally {
			leaveQueue();
			recycle();
		}
	}

	/**
	 * Called when an event has entered the queue.
	 * Here you can increase the count of such events in the queue.
	 */
	public abstract void enterQueue();

	/**
	 * Called when an event has left the queue.
	 * Here you can increase the count of such events in the queue.
	 */
	public abstract void leaveQueue();

	/**
	 * Check if this event can be queued
	 * immediately after some other event.
	 *
	 * @param event event after which we can put in queue
	 * @return true, if we agree to that
	 */
	public abstract boolean placeableAfter(Event event);
}