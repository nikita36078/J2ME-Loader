/*
 * Copyright 2020 Nikita Shakarun
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

package com.vodafone.v10.system.device;

import javax.microedition.util.ContextHolder;

public class DeviceControl {
	public static final int BACK_LIGHT = 5;
	public static final int BATTERY = 1;
	public static final int EIGHT_DIRECTIONS = 6;
	public static final int FIELD_INTENSITY = 2;
	public static final int KEY_STATE = 3;
	public static final int VIBRATION = 4;
	private static DeviceControl instance;

	public static final DeviceControl getDefaultDeviceControl() {
		if (instance == null) {
			instance = new DeviceControl();
		}
		return instance;
	}

	public int getDeviceState(int deviceNo) {
		switch (deviceNo) {
			case BATTERY:
			case FIELD_INTENSITY:
				return 100;
			case KEY_STATE:
				return getKeyState();
			default:
				throw new IllegalStateException();
		}
	}

	private int getKeyState() {
		return 0;
	}

	public boolean setDeviceActive(int deviceNo, boolean active) {
		switch (deviceNo) {
			case BACK_LIGHT:
			case EIGHT_DIRECTIONS:
				break;
			case VIBRATION:
				int duration = active ? 2000 : 0;
				ContextHolder.vibrate(duration);
				break;
			default:
				throw new IllegalStateException();
		}
		return true;
	}
}
