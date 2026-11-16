/*
 *  Copyright 2026 Nikita Shakarun
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package javax.microedition.util;

import android.content.Context;
import android.os.Vibrator;
import android.view.InputDevice;

public class VibrationController {
	private Vibrator systemVibrator;

	public VibrationController(Context context) {
		this.systemVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	}

	public boolean vibrate(int duration) {
		for (int id : InputDevice.getDeviceIds()) {
			Vibrator vibrator = getVibrator(id);
			if (vibrator == null || !vibrator.hasVibrator()) {
				continue;
			}
			handleVibration(vibrator, duration);
			return true;
		}
		if (systemVibrator == null || !systemVibrator.hasVibrator()) {
			return false;
		}
		handleVibration(systemVibrator, duration);
		return true;
	}

	private void handleVibration(Vibrator vibrator, int duration) {
		if (duration > 0) {
			vibrator.vibrate(duration);
		} else if (duration < 0) {
			throw new IllegalStateException();
		} else {
			vibrator.cancel();
		}
	}

	public void vibratePeriodically(int durationOn, int durationOff) {
		long[] pattern = {0, durationOn, Math.max(durationOff, 0)};
		for (int id : InputDevice.getDeviceIds()) {
			Vibrator vibrator = getVibrator(id);
			if (vibrator == null || !vibrator.hasVibrator()) {
				continue;
			}
			vibrator.vibrate(pattern, 1);
			return;
		}
		if (systemVibrator == null || !systemVibrator.hasVibrator()) {
			return;
		}
		systemVibrator.vibrate(pattern, 1);
	}

	@SuppressWarnings("deprecation")
	private static Vibrator getVibrator(int id) {
		// The deprecated method uses all device motors
		InputDevice device = InputDevice.getDevice(id);
		if (device == null ||
				(device.getSources() & InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD) {
			return null;
		}
		return device.getVibrator();
	}
}
