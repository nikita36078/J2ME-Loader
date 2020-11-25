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
package javax.microedition.lcdui.pointer;

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.graphics.CanvasWrapper;
import javax.microedition.lcdui.overlay.Overlay;
import javax.microedition.util.ContextHolder;

import androidx.annotation.NonNull;

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

	public interface LayoutListener {
		void layoutChanged(VirtualKeyboard vk);
	}

	protected class VirtualKey {

		private RectF rect;
		private int keyCode, secondKeyCode;
		private final String label;
		private boolean selected;
		private boolean visible;
		private boolean opaque = true;
		private int corners = 0;

		VirtualKey(int keyCode, String label) {
			this.keyCode = keyCode;
			this.label = label;
			this.visible = true;
			rect = new RectF();
		}

		VirtualKey(int keyCode, int secondKeyCode, String label) {
			this(keyCode, label);
			this.secondKeyCode = secondKeyCode;
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

		public void setVisible(boolean flag) {
			visible = flag;
		}

		public boolean isVisible() {
			return visible;
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
			if (opaque) {
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
			final int prime = 31;
			int result = 1;
			result = prime * result + keyCode;
			result = prime * result + secondKeyCode;
			return result;
		}
	}

	private static final int KEYBOARD_SIZE = 25;
	static final int SCREEN = -1;

	static final int KEY_NUM1 = 0;
	static final int KEY_NUM2 = 1;
	static final int KEY_NUM3 = 2;
	static final int KEY_NUM4 = 3;
	static final int KEY_NUM5 = 4;
	static final int KEY_NUM6 = 5;
	static final int KEY_NUM7 = 6;
	static final int KEY_NUM8 = 7;
	static final int KEY_NUM9 = 8;
	static final int KEY_NUM0 = 9;
	static final int KEY_STAR = 10;
	static final int KEY_POUND = 11;
	static final int KEY_SOFT_LEFT = 12;
	static final int KEY_SOFT_RIGHT = 13;
	static final int KEY_DIAL = 14;
	static final int KEY_CANCEL = 15;
	static final int KEY_UP_LEFT = 16;
	static final int KEY_UP = 17;
	static final int KEY_UP_RIGHT = 18;
	static final int KEY_LEFT = 19;
	static final int KEY_RIGHT = 20;
	static final int KEY_DOWN_LEFT = 21;
	static final int KEY_DOWN = 22;
	static final int KEY_DOWN_RIGHT = 23;
	static final int KEY_FIRE = 24;

	private static final int LAYOUT_SIGNATURE = 0x564B4C00;
	private static final int LAYOUT_OLD_VERSION = 1;
	private static final int LAYOUT_VERSION = 2;

	private static final int NUM_VARIANTS = 4;

	public static final int LAYOUT_EOF = -1;
	public static final int LAYOUT_KEYS = 0;
	public static final int LAYOUT_SCALES = 1;
	public static final int LAYOUT_COLORS = 2;

	private int delay = -1;
	protected int shape;

	public static final int CUSTOMIZABLE_TYPE = 0;
	public static final int PHONE_DIGITS_TYPE = 1;
	public static final int PHONE_ARROWS_TYPE = 2;

	public static final int OVAL_SHAPE = 0;
	public static final int RECT_SHAPE = 1;
	public static final int ROUND_RECT_SHAPE = 2;

	public static final int BACKGROUND = 0;
	public static final int FOREGROUND = 1;
	public static final int BACKGROUND_SELECTED = 2;
	public static final int FOREGROUND_SELECTED = 3;
	public static final int OUTLINE = 4;

	private int[] colors = {
			0xD0D0D0,
			0x000080,
			0x000080,
			0xFFFFFF,
			0xFFFFFF
	};

	private static final int SCALE_JOYSTICK = 0;
	private static final int SCALE_SOFT_KEYS = 1;
	private static final int SCALE_DIAL_KEYS = 2;
	private static final int SCALE_DIGITS = 3;
	private static final int SCALE_FIRE_KEY = 4;

	private static final float SCALE_SNAP_RADIUS = 0.05f;

	private float[] keyScales = {
			1,
			1,
			1,
			0.75f,
			1.5f
	};

	private int[][] keyScaleGroups = {
			{
					KEY_UP_LEFT,
					KEY_UP,
					KEY_UP_RIGHT,
					KEY_LEFT,
					KEY_RIGHT,
					KEY_DOWN_LEFT,
					KEY_DOWN,
					KEY_DOWN_RIGHT
			},
			{
					KEY_SOFT_LEFT,
					KEY_SOFT_RIGHT
			},
			{
					KEY_DIAL,
					KEY_CANCEL,
			},
			{
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
			},
			{
					KEY_FIRE
			}
	};

	protected Canvas target;

	private View overlayView;
	private boolean obscuresVirtualScreen;
	private boolean feedback;
	private boolean forceOpacity;
	private static final int FEEDBACK_DURATION = 50;

	private boolean visible, hiding, skip;
	private final Object waiter = new Object();
	private Thread hider;

	private int[] snapOrigins;
	private int[] snapModes;
	private PointF[] snapOffsets;
	protected boolean[] snapValid;
	private int[] snapStack;

	private int layoutEditMode;
	private int editedIndex;
	private float offsetX, offsetY;
	private float prevScale;

	protected RectF screen;
	protected RectF virtualScreen;
	private float keySize;
	private float snapRadius;

	protected VirtualKey[] keypad;
	private VirtualKey[] associatedKeys;

	private KeyRepeater repeater;
	protected LayoutListener listener;

	public VirtualKeyboard() {
		this(0);
	}

	public VirtualKeyboard(int variant) {
		keypad = new VirtualKey[KEYBOARD_SIZE];
		associatedKeys = new VirtualKey[10]; // the average user usually has no more than 10 fingers...

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

		snapOrigins = new int[keypad.length];
		snapModes = new int[keypad.length];
		snapOffsets = new PointF[keypad.length];
		snapValid = new boolean[keypad.length];
		snapStack = new int[keypad.length];

		resetLayout(variant);
		layoutEditMode = LAYOUT_EOF;
		visible = true;
		hider = new Thread(this, "MIDletVirtualKeyboard");
		hider.start();
		repeater = new KeyRepeater();
	}

	protected void resetLayout(int variant) {
		switch (variant) {
			case 0:
				keyScales[SCALE_JOYSTICK] = 1;
				keyScales[SCALE_SOFT_KEYS] = 1;
				keyScales[SCALE_DIAL_KEYS] = 1;
				keyScales[SCALE_DIGITS] = 1;
				keyScales[SCALE_FIRE_KEY] = 1;

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
			case 1:
				keyScales[SCALE_JOYSTICK] = 1;
				keyScales[SCALE_SOFT_KEYS] = 1;
				keyScales[SCALE_DIAL_KEYS] = 1;
				keyScales[SCALE_DIGITS] = 1;
				keyScales[SCALE_FIRE_KEY] = 1;

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
			case 2:
				keyScales[SCALE_JOYSTICK] = 1;
				keyScales[SCALE_SOFT_KEYS] = 1;
				keyScales[SCALE_DIAL_KEYS] = 1;
				keyScales[SCALE_DIGITS] = 1;
				keyScales[SCALE_FIRE_KEY] = 1;

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
			case 3:
				keyScales[SCALE_JOYSTICK] = 1;
				keyScales[SCALE_SOFT_KEYS] = 1;
				keyScales[SCALE_DIAL_KEYS] = 1;
				keyScales[SCALE_DIGITS] = 1;
				keyScales[SCALE_FIRE_KEY] = 1;

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
		}
	}

	protected int getLayoutNum() {
		return NUM_VARIANTS;
	}

	public String[] getLayoutNames() {
		int num = getLayoutNum();
		String[] names = new String[num];
		for (int i = 0; i < num; i++) {
			names[i] = String.valueOf(i + 1);
		}
		return names;
	}

	public void setLayout(int layoutVariant) {
		resetLayout(layoutVariant);
		for (int group = 0; group < keyScaleGroups.length; group++) {
			resizeKeyGroup(group);
		}
		snapKeys();
		repaint();
		listener.layoutChanged(this);
	}

	public void writeLayout(DataOutputStream dos) throws IOException {
		dos.writeInt(LAYOUT_SIGNATURE);
		dos.writeInt(LAYOUT_VERSION);
		dos.writeInt(LAYOUT_KEYS);
		dos.writeInt(keypad.length * 20 + 4);
		dos.writeInt(keypad.length);
		for (int i = 0; i < keypad.length; i++) {
			dos.writeInt(keypad[i].hashCode());
			dos.writeBoolean(keypad[i].isVisible());
			dos.writeInt(snapOrigins[i]);
			dos.writeInt(snapModes[i]);
			dos.writeFloat(snapOffsets[i].x);
			dos.writeFloat(snapOffsets[i].y);
		}
		dos.writeInt(LAYOUT_SCALES);
		dos.writeInt(keyScales.length * 4 + 4);
		dos.writeInt(keyScales.length);
		for (float keyScale : keyScales) {
			dos.writeFloat(keyScale);
		}
		dos.writeInt(LAYOUT_COLORS);
		dos.writeInt(colors.length * 4 + 4);
		dos.writeInt(colors.length);
		for (int color : colors) {
			dos.writeInt(color);
		}
		dos.writeInt(LAYOUT_EOF);
		dos.writeInt(0);
	}

	public void readLayout(DataInputStream dis) throws IOException {
		if (dis.readInt() != LAYOUT_SIGNATURE) {
			throw new IOException("file signature not found");
		}
		int version = dis.readInt();
		if (version != LAYOUT_VERSION && version != LAYOUT_OLD_VERSION) {
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
						for (int key = 0; key < keypad.length; key++) {
							if (keypad[key].hashCode() == hash) {
								if (version == LAYOUT_VERSION) {
									keypad[key].setVisible(dis.readBoolean());
								}
								snapOrigins[key] = dis.readInt();
								snapModes[key] = dis.readInt();
								snapOffsets[key].x = dis.readFloat();
								snapOffsets[key].y = dis.readFloat();
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
					if (count == keyScales.length) {
						for (int i = 0; i < count; i++) {
							keyScales[i] = dis.readFloat();
						}
					} else {
						dis.skip(count * 4);
					}
					break;
				case LAYOUT_COLORS:
					count = dis.readInt();
					if (count == colors.length) {
						for (int i = 0; i < count; i++) {
							colors[i] = dis.readInt();
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

	public String[] getKeyNames() {
		String[] names = new String[KEYBOARD_SIZE];
		for (int i = 0; i < KEYBOARD_SIZE; i++) {
			names[i] = keypad[i].getLabel();
		}
		return names;
	}

	public boolean[] getKeyVisibility() {
		boolean[] states = new boolean[KEYBOARD_SIZE];
		for (int i = 0; i < KEYBOARD_SIZE; i++) {
			states[i] = !keypad[i].isVisible();
		}
		return states;
	}

	public void setKeyVisibility(int id, boolean hidden) {
		keypad[id].setVisible(!hidden);
		snapKeys();
		repaint();
		listener.layoutChanged(this);
	}

	@Override
	public void setTarget(Canvas canvas) {
		if (canvas != null) {
			target = canvas;
		}
		repeater.setTarget(canvas);
		highlightGroup(-1);
	}

	public void setLayoutListener(LayoutListener listener) {
		this.listener = listener;
	}

	protected void setSnap(int key, int origin, int mode) {
		snapOrigins[key] = origin;
		snapModes[key] = mode;
		snapOffsets[key] = new PointF();
		snapValid[key] = false;
	}

	private boolean findSnap(int target, int origin) {
		snapModes[target] = RectSnap.getSnap(keypad[target].getRect(), keypad[origin].getRect(), snapRadius, RectSnap.COARSE_MASK, true);
		if (snapModes[target] != RectSnap.NO_SNAP) {
			snapOrigins[target] = origin;
			snapOffsets[target].set(0, 0);
			for (VirtualKey aKeypad : keypad) {
				origin = snapOrigins[origin];
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
		if (snapOrigins[key] == SCREEN) {
			RectSnap.snap(keypad[key].getRect(), screen, snapModes[key], snapOffsets[key]);
			snapValid[key] = true;
		} else {
			if (!snapValid[snapOrigins[key]]) {
				snapKey(snapOrigins[key], level + 1);
			}
			RectSnap.snap(keypad[key].getRect(), keypad[snapOrigins[key]].getRect(), snapModes[key], snapOffsets[key]);
			snapValid[key] = true;
		}
	}

	protected void snapKeys() {
		obscuresVirtualScreen = false;
		for (int i = 0; i < keypad.length; i++) {
			snapKey(i, 0);
			VirtualKey key = keypad[i];
			if (key.isVisible() && RectF.intersects(key.getRect(), virtualScreen)) {
				obscuresVirtualScreen = true;
				key.opaque = false;
			} else {
				key.opaque = true;
			}
			key.corners = (int) (Math.min(key.getRect().width(), key.getRect().height()) * 0.25F);
		}
		for (VirtualKey key : keypad) {
			key.opaque &= !obscuresVirtualScreen || forceOpacity;
		}
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

	public void setLayoutEditMode(int mode) {
		if ((layoutEditMode != LAYOUT_EOF) && (mode == LAYOUT_EOF) && listener != null) {
			listener.layoutChanged(this);
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

	private void resizeKey(int key, float size) {
		keypad[key].resize(size, size);
		snapValid[key] = false;
	}

	private void resizeKeyGroup(int group) {
		float size = keySize * keyScales[group];
		for (int key = 0; key < keyScaleGroups[group].length; key++) {
			resizeKey(keyScaleGroups[group][key], size);
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
		if (skip) {
			return checkPointerHandled(x, y);
		}

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
						repeater.add(aKeypad);
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
				prevScale = keyScales[editedIndex];
				break;
		}
		return checkPointerHandled(x, y);
	}

	@Override
	public boolean pointerDragged(int pointer, float x, float y) {
		if (skip) {
			return checkPointerHandled(x, y);
		}
		switch (layoutEditMode) {
			case LAYOUT_EOF:
				if (pointer > associatedKeys.length) {
					return checkPointerHandled(x, y);
				}
				if (associatedKeys[pointer] == null) {
					pointerPressed(pointer, x, y);
				} else if (!associatedKeys[pointer].contains(x, y)) {
					repeater.remove(associatedKeys[pointer]);
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
					RectF rect = keypad[editedIndex].getRect();
					rect.offsetTo(x - offsetX, y - offsetY);
					snapModes[editedIndex] = RectSnap.NO_SNAP;
					for (int i = 0; i < keypad.length; i++) {
						if (i != editedIndex && findSnap(editedIndex, i)) {
							break;
						}
					}
					if (snapModes[editedIndex] == RectSnap.NO_SNAP) {
						snapModes[editedIndex] = RectSnap.getSnap(rect, screen, snapOffsets[editedIndex]);
						snapOrigins[editedIndex] = SCREEN;
						if (Math.abs(snapOffsets[editedIndex].x) <= snapRadius) {
							snapOffsets[editedIndex].x = 0;
						}
						if (Math.abs(snapOffsets[editedIndex].y) <= snapRadius) {
							snapOffsets[editedIndex].y = 0;
						}
					}
					snapKey(editedIndex, 0);
					snapKeys();
					repaint();
				}
				break;
			case LAYOUT_SCALES:
				float dx = x - offsetX;
				float dy = offsetY - y;
				float delta;
				if (Math.abs(dx) > Math.abs(dy)) {
					delta = dx;
				} else {
					delta = dy;
				}
				float scale = prevScale + delta / Math.max(screen.width(), screen.height());
				if (Math.abs(1 - scale) <= SCALE_SNAP_RADIUS) {
					scale = 1;
				} else {
					for (int i = 0; i < keyScales.length; i++) {
						if (i != editedIndex && Math.abs(keyScales[i] - scale) <= SCALE_SNAP_RADIUS) {
							scale = keyScales[i];
							break;
						}
					}
				}
				keyScales[editedIndex] = scale;
				resizeKeyGroup(editedIndex);
				snapKeys();
				repaint();
				break;
		}
		return checkPointerHandled(x, y);
	}

	@Override
	public boolean pointerReleased(int pointer, float x, float y) {
		if (skip) {
			skip = false;
			return checkPointerHandled(x, y);
		}
		if (layoutEditMode == LAYOUT_EOF) {
			if (pointer > associatedKeys.length) {
				return checkPointerHandled(x, y);
			}
			if (associatedKeys[pointer] != null) {
				repeater.remove(associatedKeys[pointer]);
				target.postKeyReleased(associatedKeys[pointer].getKeyCode());
				if (associatedKeys[pointer].getSecondKeyCode() != 0) {
					target.postKeyReleased(associatedKeys[pointer].getSecondKeyCode());
				}
				associatedKeys[pointer].setSelected(false);
				associatedKeys[pointer] = null;
				repaint();
			}
		} else if (layoutEditMode == LAYOUT_KEYS) {
			editedIndex = -1;
		}
		return checkPointerHandled(x, y);
	}

	@Override
	public void show() {
		synchronized (waiter) {
			if (hiding) {
				hider.interrupt();
			}
		}
		visible = true;
		repaint();
	}

	@Override
	public void hide() {
		if (delay > 0 && obscuresVirtualScreen) {
			synchronized (waiter) {
				waiter.notifyAll();
			}
		}
	}

	@Override
	public void run() {
		try {
			while (true) {
				synchronized (waiter) {
					hiding = false;
					waiter.notifyAll();
					waiter.wait();
					hiding = true;
				}
				try {
					if (delay > 0) {
						Thread.sleep(delay);
					}
					visible = false;
					skip = true;
					repaint();
				} catch (InterruptedException ie) {
					ie.printStackTrace();
				}
			}
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
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
		if (feedback) {
			ContextHolder.vibrate(FEEDBACK_DURATION);
		}
	}

	public void setHideDelay(int delay) {
		this.delay = delay;
	}

	public void setColor(int color, int value) {
		colors[color] = value;
	}

	public void setHasHapticFeedback(boolean feedback) {
		this.feedback = feedback;
	}

	public void setButtonShape(int shape) {
		this.shape = shape;
	}

	public void setForceOpacity(boolean forceOpacity) {
		this.forceOpacity = forceOpacity;
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
}
