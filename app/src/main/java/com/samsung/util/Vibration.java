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
