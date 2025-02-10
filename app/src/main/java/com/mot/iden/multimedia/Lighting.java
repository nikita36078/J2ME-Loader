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

public class Lighting {
	public static final int LIGHT_CALL_INDICATOR = 4;
	public static final int LIGHT_DISPLAY = 1;
	public static final int LIGHT_KEYPAD = 2;
	public static final int LIGHT_STATE_BLUE = 4;
	public static final int LIGHT_STATE_CYAN = 5;
	public static final int LIGHT_STATE_GREEN = 1;
	public static final int LIGHT_STATE_MAGENTA = 6;
	public static final int LIGHT_STATE_OFF = 0;
	public static final int LIGHT_STATE_ON = 255;
	public static final int LIGHT_STATE_RED = 2;
	public static final int LIGHT_STATE_WHITE = 7;
	public static final int LIGHT_STATE_YELLOW = 3;
	public static final int LIGHT_STATUS = 3;
	public static final int STATUS_AMBER = 3;
	public static final int STATUS_GREEN = 1;
	public static final int STATUS_OFF = 0;
	public static final int STATUS_RED = 2;

	public static void setLighting(int light, int state) throws IllegalStateException {
	}

	public static void backlightOn() {
	}

	public static void backlightOff() {
	}

	public static void keypadLightOn() {
	}

	public static void keypadLightOff() {
	}

	public static void setStatusLight(int color) {
	}

	public static int getPhotoSensorLevel() {
		return -100;
	}

	public static void javaOverRideLighting(boolean state) {
	}
}
