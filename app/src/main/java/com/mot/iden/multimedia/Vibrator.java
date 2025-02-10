/*
 * Copyright 2025 Yury Kharchenko
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

package com.mot.iden.multimedia;

import javax.microedition.util.ContextHolder;

public class Vibrator extends Thread {
	public static final int MAX_VIBRATE_TIME = 500;
	public static final int MIN_PAUSE_TIME = 50;

	public static void vibratorOn() {
		ContextHolder.vibrate(MAX_VIBRATE_TIME);
	}

	public static void vibratorOff() {
		ContextHolder.vibrate(0);
	}

	public static void vibrateFor(int timeInMs) throws IllegalArgumentException {
		if (timeInMs <= 0 || timeInMs > MAX_VIBRATE_TIME) {
			throw new IllegalArgumentException();
		}
		ContextHolder.vibrate(timeInMs);
	}

	public static void vibratePeriodicaly(int timeOnInMs, int timeOffInMs)
			throws IllegalArgumentException {
		if (timeOnInMs <= 0 || timeOnInMs > MAX_VIBRATE_TIME || timeOffInMs < MIN_PAUSE_TIME) {
			throw new IllegalArgumentException();
		}
		ContextHolder.vibratePeriodically(timeOnInMs, timeOffInMs);
	}

	public static void vibratePeriodicaly(int timeInMs) throws IllegalArgumentException {
		if (timeInMs < MIN_PAUSE_TIME || timeInMs > MAX_VIBRATE_TIME) {
			throw new IllegalArgumentException();
		}
		ContextHolder.vibratePeriodically(timeInMs, timeInMs);
	}

	public void run() {
	}
}
