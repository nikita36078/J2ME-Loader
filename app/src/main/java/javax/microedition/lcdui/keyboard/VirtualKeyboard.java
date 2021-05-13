/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017-2018 Nikita Shakarun
 * Copyright 2021 Yury Kharchenko
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
package javax.microedition.lcdui.keyboard;

import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;

import androidx.annotation.NonNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.graphics.CanvasWrapper;
import javax.microedition.lcdui.overlay.Overlay;
import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.config.ProfileModel;
import ru.playsoftware.j2meloader.config.ProfilesManager;

public class VirtualKeyboard implements Overlay, Runnable {
	private static final String TAG = VirtualKeyboard.class.getName();

	private static final String ARROW_LEFT = "\u2190";
	private static final String ARROW_UP = "\u2191";
	private static final String ARROW_RIGHT = "\u2192";
	private static final String ARROW_DOWN = "\u2193";
	private static final String ARROW_UP_LEFT = "\u2196";
	private static final String ARROW_UP_RIGHT = "\u2197";
	private static final String ARROW_DOWN_LEFT = "\u2199";
	private static final String ARROW_DOWN_RIGHT = "\u2198";

	private static final int LAYOUT_SIGNATURE = 0x564B4C00;
	private static final int LAYOUT_VERSION_1 = 1;
	private static final int LAYOUT_VERSION_2 = 2;
	private static final int LAYOUT_VERSION_3 = 3;
	private static final int LAYOUT_VERSION = LAYOUT_VERSION_3;
	public static final int LAYOUT_EOF = -1;
	public static final int LAYOUT_KEYS = 0;
	public static final int LAYOUT_SCALES = 1;

	private static final int OVAL_SHAPE = 0;
	private static final int RECT_SHAPE = 1;
	public static final int ROUND_RECT_SHAPE = 2;

	private static final int BACKGROUND = 0;
	private static final int FOREGROUND = 1;
	private static final int BACKGROUND_SELECTED = 2;
	private static final int FOREGROUND_SELECTED = 3;
	private static final int OUTLINE = 4;

	private static final float PHONE_KEY_ROWS = 5;
	private static final float PHONE_KEY_SCALE_X = 2.0f;
	private static final float PHONE_KEY_SCALE_Y = 0.75f;
	private static final long[] REPEAT_INTERVALS = {200, 400, 128, 128, 128, 128, 128};
	private static final int KEYBOARD_SIZE = 25;

	private static final int SCREEN = -1;
	private static final int KEY_NUM1 = 0;
	private static final int KEY_NUM2 = 1;
	private static final int KEY_NUM3 = 2;
	private static final int KEY_NUM4 = 3;
	private static final int KEY_NUM5 = 4;
	private static final int KEY_NUM6 = 5;
	private static final int KEY_NUM7 = 6;
	private static final int KEY_NUM8 = 7;
	private static final int KEY_NUM9 = 8;
	private static final int KEY_NUM0 = 9;
	private static final int KEY_STAR = 10;
	private static final int KEY_POUND = 11;
	private static final int KEY_SOFT_LEFT = 12;
	private static final int KEY_SOFT_RIGHT = 13;
	private static final int KEY_DIAL = 14;
	private static final int KEY_CANCEL = 15;
	private static final int KEY_UP_LEFT = 16;
	private static final int KEY_UP = 17;
	private static final int KEY_UP_RIGHT = 18;
	private static final int KEY_LEFT = 19;
	private static final int KEY_RIGHT = 20;
	private static final int KEY_DOWN_LEFT = 21;
	private static final int KEY_DOWN = 22;
	private static final int KEY_DOWN_RIGHT = 23;
	private static final int KEY_FIRE = 24;

	private static final int SCALE_JOYSTICK = 0;
	private static final int SCALE_SOFT_KEYS = 2;
	private static final int SCALE_DIAL_KEYS = 4;
	private static final int SCALE_DIGITS = 6;
	private static final int SCALE_FIRE_KEY = 8;
	private static final float SCALE_SNAP_RADIUS = 0.05f;

	private static final int FEEDBACK_DURATION = 50;

	private final int[] colors = {
			0xD0D0D0,
			0x000080,
			0x000080,
			0xFFFFFF,
			0xFFFFFF
	};
	private final float[] keyScales = {
			1, 1,
			1, 1,
			1, 1,
			1, 1,
			1, 1
	};
	private final int[][] keyScaleGroups = {{
			KEY_UP_LEFT,
			KEY_UP,
			KEY_UP_RIGHT,
			KEY_LEFT,
			KEY_RIGHT,
			KEY_DOWN_LEFT,
			KEY_DOWN,
			KEY_DOWN_RIGHT
	}, {
			KEY_SOFT_LEFT,
			KEY_SOFT_RIGHT
	}, {
			KEY_DIAL,
			KEY_CANCEL,
	}, {
			KEY_NUM1,
			KEY_NUM2,
			KEY_NUM3,
			KEY_NUM4,
			KEY_NUM5,
			KEY_NUM6,
			KEY_NUM7,
			KEY_NUM8,
			KEY_NUM9,
			KEY_NUM0,
			KEY_STAR,
			KEY_POUND
	}, {
			KEY_FIRE
	}};
	private final VirtualKey[] keypad = new VirtualKey[KEYBOARD_SIZE];
	// the average user usually has no more than 10 fingers...
	private final VirtualKey[] associatedKeys = new VirtualKey[10];
	private final int[] snapStack = new int[KEYBOARD_SIZE];

	private final int delay;
	private final int shape;
	private final boolean feedback;
	private final boolean forceOpacity;
	private final Handler handler;
	private final File saveFile;
	private final ProfileModel settings;

	private Canvas target;
	private View overlayView;
	private boolean obscuresVirtualScreen;
	private boolean visible = true;
	private int layoutEditMode = LAYOUT_EOF;
	private int editedIndex;
	private float offsetX;
	private float offsetY;
	private float prevScaleX;
	private float prevScaleY;
	private int layoutVariant;
	private RectF screen;
	private RectF virtualScreen;
	private float keySize =
			Math.max(ContextHolder.getDisplayWidth(), ContextHolder.getDisplayHeight()) / 12.0f;
	private float snapRadius;

	public VirtualKeyboard(ProfileModel settings) {
		this.settings = settings;
		this.saveFile = new File(settings.dir + Config.MIDLET_KEY_LAYOUT_FILE);

		this.layoutVariant = settings.vkType;
		this.delay = settings.vkHideDelay;
		this.feedback = settings.vkFeedback;
		this.shape = settings.vkButtonShape;
		this.forceOpacity = settings.vkForceOpacity;

		int vkAlpha = settings.vkAlpha << 24;
		colors[BACKGROUND] = vkAlpha | settings.vkBgColor;
		colors[FOREGROUND] = vkAlpha | settings.vkFgColor;
		colors[BACKGROUND_SELECTED] = vkAlpha | settings.vkBgColorSelected;
		colors[FOREGROUND_SELECTED] = vkAlpha | settings.vkFgColorSelected;
		colors[OUTLINE] = vkAlpha | settings.vkOutlineColor;

		for (int i = KEY_NUM1; i < 9; i++) {
			keypad[i] = new VirtualKey(Canvas.KEY_NUM1 + i, Integer.toString(1 + i));
		}

		keypad[KEY_NUM0] = new VirtualKey(Canvas.KEY_NUM0, "0");
		keypad[KEY_STAR] = new VirtualKey(Canvas.KEY_STAR, "*");
		keypad[KEY_POUND] = new VirtualKey(Canvas.KEY_POUND, "#");

		keypad[KEY_SOFT_LEFT] = new VirtualKey(Canvas.KEY_SOFT_LEFT, "L");
		keypad[KEY_SOFT_RIGHT] = new VirtualKey(Canvas.KEY_SOFT_RIGHT, "R");

		keypad[KEY_DIAL] = new VirtualKey(Canvas.KEY_SEND, "D");
		keypad[KEY_CANCEL] = new VirtualKey(Canvas.KEY_END, "C");

		keypad[KEY_UP_LEFT] = new VirtualKey(Canvas.KEY_UP, Canvas.KEY_LEFT, ARROW_UP_LEFT);
		keypad[KEY_UP] = new VirtualKey(Canvas.KEY_UP, ARROW_UP);
		keypad[KEY_UP_RIGHT] = new VirtualKey(Canvas.KEY_UP, Canvas.KEY_RIGHT, ARROW_UP_RIGHT);

		keypad[KEY_LEFT] = new VirtualKey(Canvas.KEY_LEFT, ARROW_LEFT);
		keypad[KEY_RIGHT] = new VirtualKey(Canvas.KEY_RIGHT, ARROW_RIGHT);

		keypad[KEY_DOWN_LEFT] = new VirtualKey(Canvas.KEY_DOWN, Canvas.KEY_LEFT, ARROW_DOWN_LEFT);
		keypad[KEY_DOWN] = new VirtualKey(Canvas.KEY_DOWN, ARROW_DOWN);
		keypad[KEY_DOWN_RIGHT] = new VirtualKey(Canvas.KEY_DOWN, Canvas.KEY_RIGHT, ARROW_DOWN_RIGHT);

		keypad[KEY_FIRE] = new VirtualKey(Canvas.KEY_FIRE, "F");

		if (layoutVariant == 0) {
			try {
				readLayout();
			} catch (IOException ioe) {
				ioe.printStackTrace();
				layoutVariant = 3;
				resetLayout();
				onLayoutChanged();
			}
		} else {
			resetLayout();
		}

		HandlerThread thread = new HandlerThread("MIDletVirtualKeyboard");
		thread.start();
		handler = new Handler(thread.getLooper());
	}

	private void onLayoutChanged() {
		if (settings.vkType != layoutVariant) {
			settings.vkType = layoutVariant;
			ProfilesManager.saveConfig(settings);
		}
		if (layoutVariant > 0) {
			return;
		}
		try {
			saveLayout();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void resetLayout() {
		switch (layoutVariant) {
			case 0: // custom
				return;
			case 1: // phone
				keyScales[SCALE_JOYSTICK] = PHONE_KEY_SCALE_X;
				keyScales[SCALE_JOYSTICK + 1] = PHONE_KEY_SCALE_Y;
				keyScales[SCALE_SOFT_KEYS] = PHONE_KEY_SCALE_X;
				keyScales[SCALE_SOFT_KEYS + 1] = PHONE_KEY_SCALE_Y;
				keyScales[SCALE_DIAL_KEYS] = PHONE_KEY_SCALE_X;
				keyScales[SCALE_DIAL_KEYS + 1] = PHONE_KEY_SCALE_Y;
				keyScales[SCALE_DIGITS] = PHONE_KEY_SCALE_X;
				keyScales[SCALE_DIGITS + 1] = PHONE_KEY_SCALE_Y;
				keyScales[SCALE_FIRE_KEY] = PHONE_KEY_SCALE_X;
				keyScales[SCALE_FIRE_KEY + 1] = PHONE_KEY_SCALE_Y;

				setSnap(KEY_NUM0, SCREEN, RectSnap.INT_SOUTH);
				setSnap(KEY_STAR, KEY_NUM0, RectSnap.EXT_WEST);
				setSnap(KEY_POUND, KEY_NUM0, RectSnap.EXT_EAST);
				setSnap(KEY_NUM7, KEY_STAR, RectSnap.EXT_NORTH);
				setSnap(KEY_NUM8, KEY_NUM7, RectSnap.EXT_EAST);
				setSnap(KEY_NUM9, KEY_NUM8, RectSnap.EXT_EAST);
				setSnap(KEY_NUM4, KEY_NUM7, RectSnap.EXT_NORTH);
				setSnap(KEY_NUM5, KEY_NUM4, RectSnap.EXT_EAST);
				setSnap(KEY_NUM6, KEY_NUM5, RectSnap.EXT_EAST);
				setSnap(KEY_NUM1, KEY_NUM4, RectSnap.EXT_NORTH);
				setSnap(KEY_NUM2, KEY_NUM1, RectSnap.EXT_EAST);
				setSnap(KEY_NUM3, KEY_NUM2, RectSnap.EXT_EAST);

				setSnap(KEY_SOFT_LEFT, KEY_NUM1, RectSnap.EXT_NORTH);
				setSnap(KEY_FIRE, KEY_NUM2, RectSnap.EXT_NORTH);
				setSnap(KEY_SOFT_RIGHT, KEY_NUM3, RectSnap.EXT_NORTH);

				for (int i = KEY_NUM1; i < KEY_DIAL; i++) {
					keypad[i].setVisible(true);
				}
				for (int i = KEY_DIAL; i < KEY_FIRE; i++) {
					keypad[i].setVisible(false);
				}
				keypad[KEY_FIRE].setVisible(true);
				break;
			case 2: // phone (arrows)
				keyScales[SCALE_JOYSTICK] = PHONE_KEY_SCALE_X;
				keyScales[SCALE_JOYSTICK + 1] = PHONE_KEY_SCALE_Y;
				keyScales[SCALE_SOFT_KEYS] = PHONE_KEY_SCALE_X;
				keyScales[SCALE_SOFT_KEYS + 1] = PHONE_KEY_SCALE_Y;
				keyScales[SCALE_DIAL_KEYS] = PHONE_KEY_SCALE_X;
				keyScales[SCALE_DIAL_KEYS + 1] = PHONE_KEY_SCALE_Y;
				keyScales[SCALE_DIGITS] = PHONE_KEY_SCALE_X;
				keyScales[SCALE_DIGITS + 1] = PHONE_KEY_SCALE_Y;
				keyScales[SCALE_FIRE_KEY] = PHONE_KEY_SCALE_X;
				keyScales[SCALE_FIRE_KEY + 1] = PHONE_KEY_SCALE_Y;

				setSnap(KEY_NUM0, SCREEN, RectSnap.INT_SOUTH);
				setSnap(KEY_STAR, KEY_NUM0, RectSnap.EXT_WEST);
				setSnap(KEY_POUND, KEY_NUM0, RectSnap.EXT_EAST);
				setSnap(KEY_NUM7, KEY_STAR, RectSnap.EXT_NORTH);
				setSnap(KEY_DOWN, KEY_NUM7, RectSnap.EXT_EAST);
				setSnap(KEY_NUM9, KEY_DOWN, RectSnap.EXT_EAST);
				setSnap(KEY_LEFT, KEY_NUM7, RectSnap.EXT_NORTH);
				setSnap(KEY_NUM5, KEY_LEFT, RectSnap.EXT_EAST);
				setSnap(KEY_RIGHT, KEY_NUM5, RectSnap.EXT_EAST);
				setSnap(KEY_NUM1, KEY_LEFT, RectSnap.EXT_NORTH);
				setSnap(KEY_UP, KEY_NUM1, RectSnap.EXT_EAST);
				setSnap(KEY_NUM3, KEY_UP, RectSnap.EXT_EAST);

				setSnap(KEY_SOFT_LEFT, KEY_NUM1, RectSnap.EXT_NORTH);
				setSnap(KEY_FIRE, KEY_UP, RectSnap.EXT_NORTH);
				setSnap(KEY_SOFT_RIGHT, KEY_NUM3, RectSnap.EXT_NORTH);

				for (int i = KEY_NUM1; i < KEY_NUM9;) {
					keypad[i++].setVisible(true);
					keypad[i++].setVisible(false);
				}
				keypad[KEY_NUM9].setVisible(true);
				keypad[KEY_NUM0].setVisible(true);
				keypad[KEY_STAR].setVisible(true);
				keypad[KEY_POUND].setVisible(true);
				keypad[KEY_SOFT_LEFT].setVisible(true);
				keypad[KEY_SOFT_RIGHT].setVisible(true);
				keypad[KEY_DIAL].setVisible(false);
				keypad[KEY_CANCEL].setVisible(false);
				keypad[KEY_UP_LEFT].setVisible(false);
				keypad[KEY_UP].setVisible(true);
				keypad[KEY_UP_RIGHT].setVisible(false);
				keypad[KEY_LEFT].setVisible(true);
				keypad[KEY_RIGHT].setVisible(true);
				keypad[KEY_DOWN_LEFT].setVisible(false);
				keypad[KEY_DOWN].setVisible(true);
				keypad[KEY_DOWN_RIGHT].setVisible(false);
				keypad[KEY_FIRE].setVisible(true);
				break;
			case 3: // numbers + arrays
			default:
				Arrays.fill(keyScales, 1);

				setSnap(KEY_DOWN_RIGHT, SCREEN, RectSnap.INT_SOUTHEAST);
				setSnap(KEY_DOWN, KEY_DOWN_RIGHT, RectSnap.EXT_WEST);
				setSnap(KEY_DOWN_LEFT, KEY_DOWN, RectSnap.EXT_WEST);
				setSnap(KEY_LEFT, KEY_DOWN_LEFT, RectSnap.EXT_NORTH);
				setSnap(KEY_RIGHT, KEY_DOWN_RIGHT, RectSnap.EXT_NORTH);
				setSnap(KEY_UP_RIGHT, KEY_RIGHT, RectSnap.EXT_NORTH);
				setSnap(KEY_UP, KEY_UP_RIGHT, RectSnap.EXT_WEST);
				setSnap(KEY_UP_LEFT, KEY_UP, RectSnap.EXT_WEST);
				setSnap(KEY_FIRE, KEY_DOWN_RIGHT, RectSnap.EXT_NORTHWEST);
				setSnap(KEY_SOFT_LEFT, KEY_UP_LEFT, RectSnap.EXT_NORTH);
				setSnap(KEY_SOFT_RIGHT, KEY_UP_RIGHT, RectSnap.EXT_NORTH);

				setSnap(KEY_STAR, SCREEN, RectSnap.INT_SOUTHWEST);
				setSnap(KEY_NUM0, KEY_STAR, RectSnap.EXT_EAST);
				setSnap(KEY_POUND, KEY_NUM0, RectSnap.EXT_EAST);
				setSnap(KEY_NUM7, KEY_STAR, RectSnap.EXT_NORTH);
				setSnap(KEY_NUM8, KEY_NUM7, RectSnap.EXT_EAST);
				setSnap(KEY_NUM9, KEY_NUM8, RectSnap.EXT_EAST);
				setSnap(KEY_NUM4, KEY_NUM7, RectSnap.EXT_NORTH);
				setSnap(KEY_NUM5, KEY_NUM4, RectSnap.EXT_EAST);
				setSnap(KEY_NUM6, KEY_NUM5, RectSnap.EXT_EAST);
				setSnap(KEY_NUM1, KEY_NUM4, RectSnap.EXT_NORTH);
				setSnap(KEY_NUM2, KEY_NUM1, RectSnap.EXT_EAST);
				setSnap(KEY_NUM3, KEY_NUM2, RectSnap.EXT_EAST);
				setSnap(KEY_DIAL, KEY_NUM1, RectSnap.EXT_NORTH);
				setSnap(KEY_CANCEL, KEY_NUM3, RectSnap.EXT_NORTH);

				for (int i = KEY_NUM1; i < KEYBOARD_SIZE; i++) {
					keypad[i].setVisible(true);
				}
				keypad[KEY_DIAL].setVisible(false);
				keypad[KEY_CANCEL].setVisible(false);
				break;
			case 4: // arrays + numbers
				Arrays.fill(keyScales, 1);

				setSnap(KEY_DOWN_LEFT, SCREEN, RectSnap.INT_SOUTHWEST);
				setSnap(KEY_DOWN, KEY_DOWN_LEFT, RectSnap.EXT_EAST);
				setSnap(KEY_DOWN_RIGHT, KEY_DOWN, RectSnap.EXT_EAST);
				setSnap(KEY_LEFT, KEY_DOWN_LEFT, RectSnap.EXT_NORTH);
				setSnap(KEY_RIGHT, KEY_DOWN_RIGHT, RectSnap.EXT_NORTH);
				setSnap(KEY_UP_RIGHT, KEY_RIGHT, RectSnap.EXT_NORTH);
				setSnap(KEY_UP, KEY_UP_RIGHT, RectSnap.EXT_WEST);
				setSnap(KEY_UP_LEFT, KEY_UP, RectSnap.EXT_WEST);
				setSnap(KEY_FIRE, KEY_DOWN_RIGHT, RectSnap.EXT_NORTHWEST);
				setSnap(KEY_SOFT_LEFT, KEY_UP_LEFT, RectSnap.EXT_NORTH);
				setSnap(KEY_SOFT_RIGHT, KEY_UP_RIGHT, RectSnap.EXT_NORTH);

				setSnap(KEY_POUND, SCREEN, RectSnap.INT_SOUTHEAST);
				setSnap(KEY_NUM0, KEY_POUND, RectSnap.EXT_WEST);
				setSnap(KEY_STAR, KEY_NUM0, RectSnap.EXT_WEST);
				setSnap(KEY_NUM7, KEY_STAR, RectSnap.EXT_NORTH);
				setSnap(KEY_NUM8, KEY_NUM7, RectSnap.EXT_EAST);
				setSnap(KEY_NUM9, KEY_NUM8, RectSnap.EXT_EAST);
				setSnap(KEY_NUM4, KEY_NUM7, RectSnap.EXT_NORTH);
				setSnap(KEY_NUM5, KEY_NUM4, RectSnap.EXT_EAST);
				setSnap(KEY_NUM6, KEY_NUM5, RectSnap.EXT_EAST);
				setSnap(KEY_NUM1, KEY_NUM4, RectSnap.EXT_NORTH);
				setSnap(KEY_NUM2, KEY_NUM1, RectSnap.EXT_EAST);
				setSnap(KEY_NUM3, KEY_NUM2, RectSnap.EXT_EAST);
				setSnap(KEY_DIAL, KEY_NUM1, RectSnap.EXT_NORTH);
				setSnap(KEY_CANCEL, KEY_NUM3, RectSnap.EXT_NORTH);

				for (int i = KEY_NUM1; i < KEYBOARD_SIZE; i++) {
					keypad[i].setVisible(true);
				}
				keypad[KEY_DIAL].setVisible(false);
				keypad[KEY_CANCEL].setVisible(false);
				break;
			case 5: // numbers
				Arrays.fill(keyScales, 1);

				setSnap(KEY_SOFT_LEFT, KEY_NUM1, RectSnap.EXT_WEST);
				setSnap(KEY_SOFT_RIGHT, KEY_NUM3, RectSnap.EXT_EAST);

				setSnap(KEY_STAR, KEY_NUM0, RectSnap.EXT_WEST);
				setSnap(KEY_NUM0, SCREEN, RectSnap.INT_SOUTH);
				setSnap(KEY_POUND, KEY_NUM0, RectSnap.EXT_EAST);
				setSnap(KEY_NUM7, KEY_STAR, RectSnap.EXT_NORTH);
				setSnap(KEY_NUM8, KEY_NUM7, RectSnap.EXT_EAST);
				setSnap(KEY_NUM9, KEY_NUM8, RectSnap.EXT_EAST);
				setSnap(KEY_NUM4, KEY_NUM7, RectSnap.EXT_NORTH);
				setSnap(KEY_NUM5, KEY_NUM4, RectSnap.EXT_EAST);
				setSnap(KEY_NUM6, KEY_NUM5, RectSnap.EXT_EAST);
				setSnap(KEY_NUM1, KEY_NUM4, RectSnap.EXT_NORTH);
				setSnap(KEY_NUM2, KEY_NUM1, RectSnap.EXT_EAST);
				setSnap(KEY_NUM3, KEY_NUM2, RectSnap.EXT_EAST);

				for (int i = KEY_NUM1; i < KEY_DIAL; i++) {
					keypad[i].setVisible(true);
				}
				for (int i = KEY_DIAL; i < KEYBOARD_SIZE; i++) {
					keypad[i].setVisible(false);
				}
				break;
			case 6: // arrays
				Arrays.fill(keyScales, 1);

				setSnap(KEY_DOWN, SCREEN, RectSnap.INT_SOUTH);
				setSnap(KEY_DOWN_RIGHT, KEY_DOWN, RectSnap.EXT_EAST);
				setSnap(KEY_DOWN_LEFT, KEY_DOWN, RectSnap.EXT_WEST);
				setSnap(KEY_LEFT, KEY_DOWN_LEFT, RectSnap.EXT_NORTH);
				setSnap(KEY_RIGHT, KEY_DOWN_RIGHT, RectSnap.EXT_NORTH);
				setSnap(KEY_UP_RIGHT, KEY_RIGHT, RectSnap.EXT_NORTH);
				setSnap(KEY_UP, KEY_UP_RIGHT, RectSnap.EXT_WEST);
				setSnap(KEY_UP_LEFT, KEY_UP, RectSnap.EXT_WEST);
				setSnap(KEY_FIRE, KEY_DOWN_RIGHT, RectSnap.EXT_NORTHWEST);
				setSnap(KEY_SOFT_LEFT, KEY_UP_LEFT, RectSnap.EXT_WEST);
				setSnap(KEY_SOFT_RIGHT, KEY_UP_RIGHT, RectSnap.EXT_EAST);

				for (int i = KEY_NUM1; i < KEY_SOFT_LEFT; i++) {
					keypad[i].setVisible(false);
				}
				for (int i = KEY_SOFT_LEFT; i < KEYBOARD_SIZE; i++) {
					keypad[i].setVisible(true);
				}
				keypad[KEY_DIAL].setVisible(false);
				keypad[KEY_CANCEL].setVisible(false);
				break;
		}
	}

	public int getLayout() {
		return layoutVariant;
	}

	public float getPhoneKeyboardHeight() {
		return PHONE_KEY_ROWS * keySize * PHONE_KEY_SCALE_Y;
	}

	public void changeLayout(int variant) {
		if (layoutVariant == variant) {
			return;
		}
		if (variant == 0) {
			try {
				readLayout();
				layoutVariant = variant;
			} catch (IOException ioe) {
				ioe.printStackTrace();
				resetLayout();
				try {
					saveLayout();
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		} else {
			layoutVariant = variant;
			resetLayout();
		}
		if (settings.vkType != variant) {
			settings.vkType = variant;
			ProfilesManager.saveConfig(settings);
		}
		for (int group = 0; group < keyScaleGroups.length; group++) {
			resizeKeyGroup(group);
		}
		snapKeys();
		repaint();
		if (target != null && target.isShown()) {
			target.updateSize();
		}
	}

	public void saveLayout() throws IOException {
		try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(saveFile))) {
			dos.writeInt(LAYOUT_SIGNATURE);
			dos.writeInt(LAYOUT_VERSION);
			dos.writeInt(LAYOUT_KEYS);
			dos.writeInt(keypad.length * 20 + 4);
			dos.writeInt(keypad.length);
			for (VirtualKey key : keypad) {
				dos.writeInt(key.hashCode());
				dos.writeBoolean(key.isVisible());
				dos.writeInt(key.snapOrigin);
				dos.writeInt(key.snapMode);
				PointF snapOffset = key.snapOffset;
				dos.writeFloat(snapOffset.x);
				dos.writeFloat(snapOffset.y);
			}
			dos.writeInt(LAYOUT_SCALES);
			dos.writeInt(keyScales.length * 4 + 4);
			dos.writeInt(keyScales.length);
			for (float keyScale : keyScales) {
				dos.writeFloat(keyScale);
			}
			dos.writeInt(LAYOUT_EOF);
			dos.writeInt(0);
		}
	}

	public void readLayout() throws IOException {
		try (DataInputStream dis = new DataInputStream(new FileInputStream(saveFile))) {
			if (dis.readInt() != LAYOUT_SIGNATURE) {
				throw new IOException("file signature not found");
			}
			int version = dis.readInt();
			if (version < LAYOUT_VERSION_1 || version > LAYOUT_VERSION) {
				throw new IOException("incompatible file version");
			}
			while (true) {
				int block = dis.readInt();
				int length = dis.readInt();
				if (block == LAYOUT_EOF) {
					break;
				}
				int count;
				switch (block) {
					case LAYOUT_KEYS:
						count = dis.readInt();
						int hash;
						boolean found;
						for (int i = 0; i < count; i++) {
							hash = dis.readInt();
							found = false;
							for (VirtualKey key : keypad) {
								if (key.hashCode() == hash) {
									if (version >= LAYOUT_VERSION_2) {
										key.setVisible(dis.readBoolean());
									}
									key.snapOrigin = dis.readInt();
									key.snapMode = dis.readInt();
									key.snapOffset.x = dis.readFloat();
									key.snapOffset.y = dis.readFloat();
									found = true;
									break;
								}
							}
							if (!found) {
								dis.skip(16);
							}
						}
						break;
					case LAYOUT_SCALES:
						count = dis.readInt();
						if (version >= LAYOUT_VERSION_3) {
							for (int i = 0; i < count; i++) {
								keyScales[i] = dis.readFloat();
							}
						} else if (count * 2 == keyScales.length) {
							for (int i = 0; i < keyScales.length; i++) {
								float v = dis.readFloat();
								keyScales[i++] = v;
								keyScales[i] = v;
							}
						} else {
							dis.skip(count * 4);
						}
						break;
					default:
						dis.skip(length);
						break;
				}
			}
		}
	}

	public String[] getKeyNames() {
		String[] names = new String[KEYBOARD_SIZE];
		for (int i = 0; i < KEYBOARD_SIZE; i++) {
			names[i] = keypad[i].getLabel();
		}
		return names;
	}

	public boolean[] getKeysVisibility() {
		boolean[] states = new boolean[KEYBOARD_SIZE];
		for (int i = 0; i < KEYBOARD_SIZE; i++) {
			states[i] = !keypad[i].isVisible();
		}
		return states;
	}

	public void setKeysVisibility(SparseBooleanArray states) {
		for (int i = 0; i < states.size(); i++) {
			keypad[states.keyAt(i)].visible = !states.valueAt(i);
		}
		layoutVariant = 0;
		onLayoutChanged();
		repaint();
	}

	@Override
	public void setTarget(Canvas canvas) {
		target = canvas;
		highlightGroup(-1);
	}

	protected void setSnap(int key, int origin, int mode) {
		keypad[key].snapOrigin = origin;
		keypad[key].snapMode = mode;
		keypad[key].snapOffset.set(0, 0);
		keypad[key].snapValid = false;
	}

	private boolean findSnap(int target, int origin) {
		VirtualKey tk = keypad[target];
		VirtualKey ok = keypad[origin];
		tk.snapMode = RectSnap.getSnap(tk.getRect(), ok.getRect(), snapRadius, RectSnap.COARSE_MASK, true);
		if (tk.snapMode != RectSnap.NO_SNAP) {
			tk.snapOrigin = origin;
			tk.snapOffset.set(0, 0);
			for (int i = 0; i < keypad.length; i++) {
				origin = keypad[origin].snapOrigin;
				if (origin == SCREEN) {
					return true;
				}
			}
		}
		return false;
	}

	private void snapKey(int key, int level) {
		if (level >= snapStack.length) {
			Log.d(TAG, "Snap loop detected: ");
			for (int i = 1; i < snapStack.length; i++) {
				System.out.print(snapStack[i]);
				System.out.print(", ");
			}
			Log.d(TAG, String.valueOf(key));
			return;
		}
		snapStack[level] = key;
		VirtualKey vKey = keypad[key];
		if (vKey.snapOrigin == SCREEN) {
			RectSnap.snap(vKey.getRect(), screen, vKey.snapMode, vKey.snapOffset);
		} else {
			if (!keypad[vKey.snapOrigin].snapValid) {
				snapKey(vKey.snapOrigin, level + 1);
			}
			RectSnap.snap(vKey.getRect(), keypad[vKey.snapOrigin].getRect(),
					vKey.snapMode, vKey.snapOffset);
		}
		vKey.snapValid = true;
	}

	protected void snapKeys() {
		obscuresVirtualScreen = false;
		boolean isPhone = isPhone();
		for (int i = 0; i < keypad.length; i++) {
			snapKey(i, 0);
			VirtualKey key = keypad[i];
			RectF rect = key.getRect();
			key.corners = (int) (Math.min(rect.width(), rect.height()) * 0.25F);
			if (!isPhone && RectF.intersects(rect, virtualScreen)) {
				obscuresVirtualScreen = true;
				key.opaque = false;
			} else {
				key.opaque = true;
			}
		}
		boolean opaque = !obscuresVirtualScreen || this.forceOpacity;
		for (VirtualKey key : keypad) {
			key.opaque &= opaque;
		}
	}

	public boolean isPhone() {
		return layoutVariant == 1 || layoutVariant == 2;
	}

	private void highlightGroup(int group) {
		for (VirtualKey aKeypad : keypad) {
			aKeypad.setSelected(false);
		}
		if (group >= 0) {
			for (int key = 0; key < keyScaleGroups[group].length; key++) {
				keypad[keyScaleGroups[group][key]].setSelected(true);
			}
		}
	}

	public int getLayoutEditMode() {
		return layoutEditMode;
	}

	public void setLayoutEditMode(int mode) {
		if (layoutEditMode != LAYOUT_EOF && mode == LAYOUT_EOF) {
			highlightGroup(-1);
			layoutVariant = 0;
			onLayoutChanged();
			for (int group = 0; group < keyScaleGroups.length; group++) {
				resizeKeyGroup(group);
			}
			snapKeys();
			if (target != null && target.isShown()) {
				target.updateSize();
			}
		}
		layoutEditMode = mode;
		switch (mode) {
			case LAYOUT_SCALES:
				editedIndex = 0;
				highlightGroup(0);
				break;
			default:
				highlightGroup(-1);
				break;
		}
		show();
	}

	private void resizeKey(int key, float w, float h) {
		VirtualKey vKey = keypad[key];
		vKey.resize(w, h);
		vKey.snapValid = false;
	}

	private void resizeKeyGroup(int group) {
		float sizeX = (float) Math.ceil(keySize * keyScales[group * 2]);
		float sizeY = (float) Math.ceil(keySize * keyScales[group * 2 + 1]);
		for (int key = 0; key < keyScaleGroups[group].length; key++) {
			resizeKey(keyScaleGroups[group][key], sizeX, sizeY);
		}
	}

	@Override
	public void resize(RectF screen, RectF virtualScreen) {
		this.screen = screen;
		this.virtualScreen = virtualScreen;
		float width = screen.width();
		float height = screen.height();
		boolean landscape = width > height;
		float maxSize = Math.max(screen.width(), screen.height());
		float minSize = Math.min(screen.width(), screen.height());
		boolean nonWide = maxSize / minSize < 2;
		snapRadius = keyScales[0];
		for (int i = 1; i < keyScales.length; i++) {
			if (keyScales[i] < snapRadius) {
				snapRadius = keyScales[i];
			}
		}
		if (nonWide || landscape) {
			keySize = maxSize / 12F;
		} else {
			keySize = minSize / 6.5F;
		}
		snapRadius = keySize * snapRadius / 4;
		for (int group = 0; group < keyScaleGroups.length; group++) {
			resizeKeyGroup(group);
		}
		snapKeys();
		repaint();
		if (delay > 0 && obscuresVirtualScreen) {
			for (VirtualKey key : associatedKeys) {
				if (key != null) {
					return;
				}
			}
			handler.postDelayed(this, delay);
		}
	}

	@Override
	public void paint(CanvasWrapper g) {
		if (visible) {
			for (VirtualKey key : keypad) {
				if (key.visible) {
					key.paint(g);
				}
			}
		}
	}

	protected void repaint() {
		overlayView.postInvalidate();
	}

	/**
	 * Check if we have processed the pointer touch.
	 * <p>
	 * The pointer touch is not processed if it is on the virtual screen:
	 * in this case, it will be handled by the midlet.
	 * But clicking outside the virtual screen is not transmitted
	 * to the midlet for optimization purposes.
	 *
	 * @param x the touch coordinates
	 * @param y the touch coordinates
	 * @return true, if the touch point is on the virtual screen
	 */
	private boolean checkPointerHandled(float x, float y) {
		return !virtualScreen.contains(x, y);
	}

	@Override
	public boolean pointerPressed(int pointer, float x, float y) {
		switch (layoutEditMode) {
			case LAYOUT_EOF:
				if (pointer > associatedKeys.length) {
					return checkPointerHandled(x, y);
				}
				for (VirtualKey aKeypad : keypad) {
					if (aKeypad.contains(x, y)) {
						vibrate();
						associatedKeys[pointer] = aKeypad;
						aKeypad.setSelected(true);
						target.postKeyPressed(aKeypad.getKeyCode());
						if (aKeypad.getSecondKeyCode() != 0) {
							target.postKeyPressed(aKeypad.getSecondKeyCode());
						}
						handler.postDelayed(aKeypad, 400);
						repaint();
						break;
					}
				}
				break;
			case LAYOUT_KEYS:
				editedIndex = -1;
				for (int i = 0; i < keypad.length; i++) {
					if (keypad[i].contains(x, y)) {
						editedIndex = i;
						RectF rect = keypad[i].getRect();
						offsetX = x - rect.left;
						offsetY = y - rect.top;
						break;
					}
				}
				break;
			case LAYOUT_SCALES:
				int index = -1;
				for (int group = 0; group < keyScaleGroups.length && index < 0; group++) {
					for (int key = 0; key < keyScaleGroups[group].length && index < 0; key++) {
						if (keypad[keyScaleGroups[group][key]].contains(x, y)) {
							index = group;
						}
					}
				}
				if (index >= 0) {
					editedIndex = index;
					highlightGroup(index);
					repaint();
				}
				offsetX = x;
				offsetY = y;
				prevScaleX = keyScales[editedIndex * 2];
				prevScaleY = keyScales[editedIndex * 2 + 1];
				break;
		}
		return checkPointerHandled(x, y);
	}

	@Override
	public boolean pointerDragged(int pointer, float x, float y) {
		switch (layoutEditMode) {
			case LAYOUT_EOF:
				if (pointer > associatedKeys.length) {
					return checkPointerHandled(x, y);
				}
				if (associatedKeys[pointer] == null) {
					pointerPressed(pointer, x, y);
				} else if (!associatedKeys[pointer].contains(x, y)) {
					handler.removeCallbacks(associatedKeys[pointer]);
					target.postKeyReleased(associatedKeys[pointer].getKeyCode());
					if (associatedKeys[pointer].getSecondKeyCode() != 0) {
						target.postKeyReleased(associatedKeys[pointer].getSecondKeyCode());
					}
					associatedKeys[pointer].setSelected(false);
					associatedKeys[pointer] = null;
					repaint();
					pointerPressed(pointer, x, y);
				}
				break;
			case LAYOUT_KEYS:
				if (editedIndex >= 0) {
					VirtualKey key = keypad[editedIndex];
					RectF rect = key.getRect();
					rect.offsetTo(x - offsetX, y - offsetY);
					key.snapMode = RectSnap.NO_SNAP;
					for (int i = 0; i < keypad.length; i++) {
						if (i != editedIndex && findSnap(editedIndex, i)) {
							break;
						}
					}
					if (key.snapMode == RectSnap.NO_SNAP) {
						key.snapMode = RectSnap.getSnap(rect, screen, key.snapOffset);
						key.snapOrigin = SCREEN;
						if (Math.abs(key.snapOffset.x) <= snapRadius) {
							key.snapOffset.x = 0;
						}
						if (Math.abs(key.snapOffset.y) <= snapRadius) {
							key.snapOffset.y = 0;
						}
					}
					snapKey(editedIndex, 0);
					repaint();
				}
				break;
			case LAYOUT_SCALES:
				float dx = x - offsetX;
				float dy = offsetY - y;
				float scale;
				int index = this.editedIndex * 2;
				if (Math.abs(dx) > Math.abs(dy)) {
					scale = prevScaleX + dx / Math.min(screen.centerX(), screen.centerY());
				} else {
					scale = prevScaleY + dy / Math.min(screen.centerX(), screen.centerY());
					index++;
				}
				if (Math.abs(1 - scale) <= SCALE_SNAP_RADIUS) {
					scale = 1;
				} else {
					for (int i = index % 2; i < keyScales.length; i += 2) {
						if (i != index && Math.abs(keyScales[i] - scale) <= SCALE_SNAP_RADIUS) {
							scale = keyScales[i];
							break;
						}
					}
				}
				keyScales[index] = scale;
				resizeKeyGroup(this.editedIndex);
				snapKeys();
				repaint();
				break;
		}
		return checkPointerHandled(x, y);
	}

	@Override
	public boolean pointerReleased(int pointer, float x, float y) {
		if (layoutEditMode == LAYOUT_EOF) {
			if (pointer > associatedKeys.length) {
				return checkPointerHandled(x, y);
			}
			if (associatedKeys[pointer] != null) {
				handler.removeCallbacks(associatedKeys[pointer]);
				target.postKeyReleased(associatedKeys[pointer].getKeyCode());
				if (associatedKeys[pointer].getSecondKeyCode() != 0) {
					target.postKeyReleased(associatedKeys[pointer].getSecondKeyCode());
				}
				associatedKeys[pointer].setSelected(false);
				associatedKeys[pointer] = null;
				repaint();
			}
		} else if (layoutEditMode == LAYOUT_KEYS) {
			for (int key = 0; key < keypad.length; key++) {
				VirtualKey vKey = keypad[key];
				if (vKey.snapOrigin == editedIndex) {
					vKey.snapMode = RectSnap.NO_SNAP;
					for (int i = 0; i < KEYBOARD_SIZE; i++) {
						if (i != key && findSnap(key, i)) {
							break;
						}
					}
					if (vKey.snapMode == RectSnap.NO_SNAP) {
						vKey.snapMode = RectSnap.getSnap(vKey.rect, screen, vKey.snapOffset);
						vKey.snapOrigin = SCREEN;
						if (Math.abs(vKey.snapOffset.x) <= snapRadius) {
							vKey.snapOffset.x = 0;
						}
						if (Math.abs(vKey.snapOffset.y) <= snapRadius) {
							vKey.snapOffset.y = 0;
						}
					}
					snapKey(key, 0);
				}
			}
			snapKeys();
			editedIndex = -1;
		}
		return checkPointerHandled(x, y);
	}

	@Override
	public void show() {
		handler.removeCallbacks(this);
		if (!visible) {
			visible = true;
			repaint();
		}
	}

	@Override
	public void hide() {
		if (delay > 0 && obscuresVirtualScreen) {
			handler.postDelayed(this, delay);
		}
	}

	@Override
	public void cancel() {
		for (VirtualKey key : keypad) {
			key.selected = false;
			handler.removeCallbacks(key);
		}
	}

	@Override
	public void run() {
		visible = false;
		repaint();
	}

	@Override
	public boolean keyPressed(int keyCode) {
		for (VirtualKey aKeypad : keypad) {
			if (aKeypad.getKeyCode() == keyCode && aKeypad.getSecondKeyCode() == 0) {
				aKeypad.setSelected(true);
				repaint();
				break;
			}
		}
		return false;
	}

	@Override
	public boolean keyRepeated(int keyCode) {
		return false;
	}

	@Override
	public boolean keyReleased(int keyCode) {
		for (VirtualKey aKeypad : keypad) {
			if (aKeypad.getKeyCode() == keyCode && aKeypad.getSecondKeyCode() == 0) {
				aKeypad.setSelected(false);
				repaint();
				break;
			}
		}
		return false;
	}

	private void vibrate() {
		if (feedback) ContextHolder.vibrateKey(FEEDBACK_DURATION);
	}

	public void setView(View view) {
		overlayView = view;
	}

	public int getKeyStatesVodafone() {
		int keyStates = 0;
		for (int i = 0; i < keypad.length; i++) {
			VirtualKey key = keypad[i];
			if (key.selected) {
				keyStates |= getKeyBit(i);
			}
		}
		return keyStates;
	}

	private int getKeyBit(int vKey) {
		switch (vKey) {
			case KEY_NUM0      : return 1;       // 0 0 key         KEY_NUM0       = 9;
			case KEY_NUM1      : return 1 <<  1; // 1 1 key         KEY_NUM1       = 0;
			case KEY_NUM2      : return 1 <<  2; // 2 2 key         KEY_NUM2       = 1;
			case KEY_NUM3      : return 1 <<  3; // 3 3 key         KEY_NUM3       = 2;
			case KEY_NUM4      : return 1 <<  4; // 4 4 key         KEY_NUM4       = 3;
			case KEY_NUM5      : return 1 <<  5; // 5 5 key         KEY_NUM5       = 4;
			case KEY_NUM6      : return 1 <<  6; // 6 6 key         KEY_NUM6       = 5;
			case KEY_NUM7      : return 1 <<  7; // 7 7key          KEY_NUM7       = 6;
			case KEY_NUM8      : return 1 <<  8; // 8 8 key         KEY_NUM8       = 7;
			case KEY_NUM9      : return 1 <<  9; // 9 9 key         KEY_NUM9       = 8;
			case KEY_STAR      : return 1 << 10; // 10 * key        KEY_STAR       = 10;
			case KEY_POUND     : return 1 << 11; // 11 # key        KEY_POUND      = 11;
			case KEY_UP        : return 1 << 12; // 12 Up key       KEY_UP         = 17;
			case KEY_LEFT      : return 1 << 13; // 13 Left key     KEY_LEFT       = 19;
			case KEY_RIGHT     : return 1 << 14; // 14 Right key    KEY_RIGHT      = 20;
			case KEY_DOWN      : return 1 << 15; // 15 Down key     KEY_DOWN       = 22;
			case KEY_FIRE      : return 1 << 16; // 16 Select key   KEY_FIRE       = 24;
			case KEY_SOFT_LEFT : return 1 << 17; // 17 Softkey 1    KEY_SOFT_LEFT  = 12;
			case KEY_SOFT_RIGHT: return 1 << 18; // 18 Softkey 2    KEY_SOFT_RIGHT = 13;
			// TODO: 05.08.2020 Softkey3 mapped to KEY_CANCEL
			case KEY_CANCEL    : return 1 << 19; // 19 Softkey 3    KEY_CANCEL     = 15;
			case KEY_UP_RIGHT  : return 1 << 20; // 20 Upper Right  KEY_UP_RIGHT   = 18;
			case KEY_UP_LEFT   : return 1 << 21; // 21 Upper Left   KEY_UP_LEFT    = 16;
			case KEY_DOWN_RIGHT: return 1 << 22; // 22 Lower Right  KEY_DOWN_RIGHT = 23;
			case KEY_DOWN_LEFT : return 1 << 23; // 23 Lower Left   KEY_DOWN_LEFT  = 21;
		}
		return 0;
	}

	protected class VirtualKey implements Runnable {
		private final String label;
		private final int keyCode;
		private final int secondKeyCode;
		private final int hashCode;
		private final RectF rect = new RectF();
		private final PointF snapOffset = new PointF();
		private int snapOrigin;
		private int snapMode;
		private boolean snapValid;
		private boolean selected;
		private boolean visible;
		private boolean opaque = true;
		private int corners;
		private int repeatCount;

		VirtualKey(int keyCode, String label) {
			this(keyCode, 0, label);
		}

		VirtualKey(int keyCode, int secondKeyCode, String label) {
			this.keyCode = keyCode;
			this.label = label;
			this.visible = true;
			this.secondKeyCode = secondKeyCode;
			hashCode = 31 * (31 + this.keyCode) + this.secondKeyCode;
		}

		int getKeyCode() {
			return keyCode;
		}

		int getSecondKeyCode() {
			return secondKeyCode;
		}

		void setSelected(boolean flag) {
			selected = flag;
		}

		public boolean isVisible() {
			return visible;
		}

		public void setVisible(boolean flag) {
			visible = flag;
		}

		public RectF getRect() {
			return rect;
		}

		void resize(float width, float height) {
			rect.right = rect.left + width;
			rect.bottom = rect.top + height;
		}

		public boolean contains(float x, float y) {
			return visible && rect.contains(x, y);
		}

		public void paint(CanvasWrapper g) {
			int bgColor;
			int fgColor;
			if (selected) {
				bgColor = colors[BACKGROUND_SELECTED];
				fgColor = colors[FOREGROUND_SELECTED];
			} else {
				bgColor = colors[BACKGROUND];
				fgColor = colors[FOREGROUND];
			}
			int olColor = colors[OUTLINE];
			if (opaque || layoutEditMode != LAYOUT_EOF) {
				bgColor |= 0xFF000000;
				fgColor |= 0xFF000000;
				olColor |= 0xFF000000;
			}
			g.setFillColor(bgColor);
			g.setTextColor(fgColor);
			g.setDrawColor(olColor);

			switch (shape) {
				case ROUND_RECT_SHAPE:
					g.fillRoundRect(rect, corners, corners);
					g.drawRoundRect(rect, corners, corners);
					break;
				case RECT_SHAPE:
					g.fillRect(rect);
					g.drawRect(rect);
					break;
				case OVAL_SHAPE:
					g.fillArc(rect, 0, 360);
					g.drawArc(rect, 0, 360);
					break;
			}
			g.drawString(label, rect.centerX(), rect.centerY());
		}

		public String getLabel() {
			return label;
		}

		@NonNull
		public String toString() {
			return "[" + label + ": " + rect.left + ", " + rect.top + ", " + rect.right + ", " + rect.bottom + "]";
		}

		public int hashCode() {
			return hashCode;
		}

		@Override
		public void run() {
			if (target == null) {
				selected = false;
				repeatCount = 0;
				return;
			}
			if (selected) {
				target.postKeyRepeated(getKeyCode());
				if (getSecondKeyCode() != 0) {
					target.postKeyRepeated(getSecondKeyCode());
				}
				handler.postDelayed(this, repeatCount > 6 ? 80 : REPEAT_INTERVALS[repeatCount++]);
			} else {
				repeatCount = 0;
			}
		}
	}
}
