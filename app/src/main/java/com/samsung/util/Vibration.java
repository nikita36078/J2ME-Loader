/*
 * J2ME Loader
 * Copyright (C) 2017 Nikita Shakarun
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.samsung.util;

import android.content.Context;
import android.os.Vibrator;

import javax.microedition.util.ContextHolder;

public class Vibration {
	public static void start(int duration, int strength) {
		Vibrator vibrator = (Vibrator) ContextHolder.getContext().getSystemService(Context.VIBRATOR_SERVICE);
		if (vibrator != null) {
			vibrator.vibrate(duration * 1000);
		}
	}

	public static boolean isSupported() {
		if (ContextHolder.getContext().getSystemService(Context.VIBRATOR_SERVICE) != null) {
			return true;
		} else {
			return false;
		}
	}
}
