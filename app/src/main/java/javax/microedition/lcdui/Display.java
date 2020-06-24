/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017-2018 Nikita Shakarun
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

package javax.microedition.lcdui;

import android.content.Context;
import android.os.Vibrator;

import javax.microedition.lcdui.event.RunnableEvent;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.shell.MicroActivity;
import javax.microedition.util.ContextHolder;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class Display {
	public static final int LIST_ELEMENT = 1;
	public static final int CHOICE_GROUP_ELEMENT = 2;
	public static final int ALERT = 3;

	public static final int COLOR_BACKGROUND = 0;
	public static final int COLOR_FOREGROUND = 1;
	public static final int COLOR_HIGHLIGHTED_BACKGROUND = 2;
	public static final int COLOR_HIGHLIGHTED_FOREGROUND = 3;
	public static final int COLOR_BORDER = 4;
	public static final int COLOR_HIGHLIGHTED_BORDER = 5;

	private static final int[] COLORS =
			{
					0xFFD0D0D0,
					0xFF000080,
					0xFF000080,
					0xFFFFFFFF,
					0xFFFFFFFF,
					0xFF000080
			};

	private static Display instance;

	private MIDlet context;
	private Displayable current;
	private AppCompatActivity activity;

	private static Vibrator vibrator;

	public static Display getDisplay(MIDlet midlet) {
		if (instance == null && midlet != null) {
			instance = new Display(midlet);
		}
		return instance;
	}

	private Display(MIDlet midlet) {
		context = midlet;
		activity = ContextHolder.getCurrentActivity();
	}

	public static void initDisplay() {
		instance = null;
	}

	public void setCurrent(Displayable disp) {
		if (disp == null || disp == current) {
			return;
		}
		if (disp instanceof Alert) {
			Alert alert = (Alert) disp;
			alert.setNextDisplayable(current);
			showAlert(alert);
		} else {
			changeCurrent(disp);
			showCurrent();
		}
	}

	public void setCurrent(final Alert alert, Displayable disp) {
		if (disp == null) {
			throw new NullPointerException();
		} else if (disp instanceof Alert) {
			throw new IllegalArgumentException();
		}
		alert.setNextDisplayable(disp);
		showAlert(alert);
	}

	private void showAlert(Alert alert) {
		activity.runOnUiThread(() -> {
			AlertDialog alertDialog = alert.prepareDialog();
			alertDialog.show();
			if (alert.finiteTimeout()) {
				(new Thread(() -> {
					try {
						Thread.sleep(alert.getTimeout());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					alertDialog.dismiss();
				}, "MIDletAlertThread")).start();
			}
		});
	}

	private void changeCurrent(Displayable disp) {
		if (current instanceof Canvas) {
			((Canvas) current).setOverlay(null);
		}
		if (disp instanceof Canvas) {
			((Canvas) disp).setOverlay(ContextHolder.getVk());
		}
		current = disp;
	}

	private void showCurrent() {
		((MicroActivity) activity).setCurrent(current);
	}

	public void activityResumed() {
		try {
			context.startApp();
		} catch (MIDletStateChangeException e) {
			e.printStackTrace();
		} catch (IllegalThreadStateException e) {
			e.printStackTrace();
		}
	}

	public void activityStopped() {
		try {
			context.pauseApp();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	public Displayable getCurrent() {
		return current;
	}

	public void callSerially(Runnable r) {
		if (current != null) {
			current.postEvent(RunnableEvent.getInstance(r));
		} else {
			(new Thread(r, "MIDletDisplayThread")).start();
		}
	}

	public boolean flashBacklight(int duration) {
		return false;
	}

	public boolean vibrate(int duration) {
		if (vibrator == null) {
			vibrator = (Vibrator) ContextHolder.getContext().getSystemService(Context.VIBRATOR_SERVICE);
		}
		if (!vibrator.hasVibrator()) {
			return false;
		}
		if (duration > 0) {
			vibrator.vibrate(duration);
		} else if (duration < 0) {
			throw new IllegalStateException();
		} else {
			vibrator.cancel();
		}
		return true;
	}

	public void setCurrentItem(Item item) {
		if (item.hasOwnerForm()) {
			setCurrent(item.getOwnerForm());
		}
	}

	public int numAlphaLevels() {
		return 256;
	}

	public int numColors() {
		return Integer.MAX_VALUE;
	}

	public int getBestImageHeight(int imageType) {
		return 0;
	}

	public int getBestImageWidth(int imageType) {
		return 0;
	}

	public int getBorderStyle(boolean highlighted) {
		return highlighted ? Graphics.SOLID : Graphics.DOTTED;
	}

	public int getColor(int colorSpecifier) {
		return COLORS[colorSpecifier];
	}

	public boolean isColor() {
		return true;
	}
}
