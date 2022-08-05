/*
 *  Nokia API for MicroEmulator
 *  Copyright (C) 2003 Markus Heberling <markus@heberling.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 */

package com.nokia.mid.ui;

import javax.microedition.util.ContextHolder;

public class DeviceControl {
	public static int getUserInactivityTime() {
		return 0;
	}

	public static void resetUserInactivityTime() {
	}

	public static void startVibra(int frequency, long duration) {
		if (frequency < 0 || frequency > 100) {
			throw new IllegalArgumentException();
		}
		ContextHolder.vibrate(duration < 0 ? 0 : (int) duration);
	}

	public static void stopVibra() {
		ContextHolder.vibrate(0);
	}

	public static void setLights(int num, int level) {
	}

	public static void flashLights(long duration) {
	}
}
