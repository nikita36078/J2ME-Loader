/*
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

package com.samsung.util;

import javax.microedition.util.ContextHolder;

public class Vibration {
	public static void start(int duration, int strength) {
		ContextHolder.vibrate(duration * 1000);
	}

	public static void stop() {
		ContextHolder.vibrate(0);
	}

	public static boolean isSupported() {
		return true;
	}
}
