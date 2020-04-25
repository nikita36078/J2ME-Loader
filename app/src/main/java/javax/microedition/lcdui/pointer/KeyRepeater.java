/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2018 Nikita Shakarun
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

package javax.microedition.lcdui.pointer;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.event.CanvasEvent;

public class KeyRepeater implements Runnable {
	private static final long INTERVAL = 200;

	protected Canvas target;

	protected Thread thread;
	private final Object waiter;
	private boolean isrunning;

	protected boolean enabled;

	private int keyCode;
	private int secondKeyCode;

	public KeyRepeater() {
		waiter = new Object();

		thread = new Thread(this, "MIDletKeyRepeater");
		thread.start();
	}

	public void setTarget(Canvas canvas) {
		if (canvas == null) {
			stop();
		}

		target = canvas;
	}

	public void start(int keyCode, int secondKeyCode) {
		if (target == null) {
			return;
		}

		synchronized (waiter) {
			if (isrunning) {
				return;
			}

			this.keyCode = keyCode;
			this.secondKeyCode = secondKeyCode;

			enabled = true;

			waiter.notifyAll();
		}
	}

	public void stop() {
		enabled = false;
		thread.interrupt();
	}

	@Override
	public void run() {
		while (true) {
			try {
				synchronized (waiter) {
					isrunning = false;
					waiter.wait();

					isrunning = true;
				}

				while (enabled) {
					Thread.sleep(INTERVAL);

					target.postEvent(CanvasEvent.getInstance(target, CanvasEvent.KEY_REPEATED, keyCode));
					if (secondKeyCode != 0) {
						target.postEvent(CanvasEvent.getInstance(target, CanvasEvent.KEY_REPEATED, secondKeyCode));
					}
				}
			} catch (InterruptedException ie) {
				// Don't need to print stacktrace here
			}
		}
	}
}