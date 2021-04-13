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

import java.util.HashSet;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.pointer.VirtualKeyboard.VirtualKey;

public class KeyRepeater implements Runnable {
	private static final long INTERVAL = 150;
	private static final long INITIAL_INTERVAL = 250;

	private Canvas target;

	private Thread thread;
	private final Object waiter;
	private final HashSet<VirtualKey> keys;

	private boolean enabled;

	public KeyRepeater() {
		keys = new HashSet<>();
		waiter = new Object();

		thread = new Thread(this, "MIDletKeyRepeater");
		thread.start();
	}

	public void setTarget(Canvas canvas) {
		synchronized (keys) {
			releaseAllKeys();
			keys.clear();
			enabled = false;
		}

		target = canvas;
	}

	private void releaseAllKeys() {
		for (VirtualKey key : keys) {
			target.postKeyReleased(key.getKeyCode());
			if (key.getSecondKeyCode() != 0) {
				target.postKeyReleased(key.getSecondKeyCode());
			}
		}
	}

	public void add(VirtualKey key) {
		if (target == null) {
			return;
		}

		synchronized (keys) {
			keys.add(key);
			enabled = true;
		}

		synchronized (waiter) {
			waiter.notifyAll();
		}
	}

	public void remove(VirtualKey key) {
		synchronized (keys) {
			keys.remove(key);

			if (keys.isEmpty()) {
				enabled = false;
			}
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				synchronized (waiter) {
					waiter.wait();
				}

				Thread.sleep(INITIAL_INTERVAL);

				while (enabled) {
					Thread.sleep(INTERVAL);

					synchronized (keys) {
						for (VirtualKey key : keys) {
							target.postKeyRepeated(key.getKeyCode());
							if (key.getSecondKeyCode() != 0) {
								target.postKeyRepeated(key.getSecondKeyCode());
							}
						}
					}
				}
			} catch (InterruptedException ie) {
				// Don't need to print stacktrace here
				System.out.println();
			}
		}
	}
}
