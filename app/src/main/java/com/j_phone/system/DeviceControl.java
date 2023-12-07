/*
 * Copyright 2023 Yury Kharchenko
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

package com.j_phone.system;

import javax.microedition.lcdui.keyboard.VirtualKeyboard;
import javax.microedition.util.ContextHolder;

public class DeviceControl {
	private static final DeviceControl deviceControl = new DeviceControl();

	public static DeviceControl getDefaultDeviceControl() {
		return deviceControl;
	}

	public int getDeviceState(int device) {
		if (device != 3) {
			return 0;
		}
		VirtualKeyboard vk = ContextHolder.getVk();
		if (vk == null) return 0;
		return vk.getKeyStatesVodafone();
	}

	public boolean setDeviceActive(int device, boolean active) {
		return true;
	}
}
