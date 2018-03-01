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

import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Overlay;
import javax.microedition.lcdui.event.CanvasEvent;
import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.R;

public class VirtualKeyboard implements Overlay, Runnable {

	private static String TAG = VirtualKeyboard.class.getName();
	private static final String ARROW_LEFT = "\u2190";
	private static final String ARROW_UP = "\u2191";
	private static final String ARROW_RIGHT = "\u2192";
	private static final String ARROW_DOWN = "\u2193";

	public interface LayoutListener {
		void layoutChanged(VirtualKeyboard vk);
	}

	private class VirtualKey {

		private RectF rect;
		private int keyCode, secondKeyCode;
		private String label;
		private boolean selected;
		private boolean visible;

		public VirtualKey(int keyCode, String label) {
			this.keyCode = keyCode;
			this.label = label;
			this.visible = true;
			rect = new RectF();
		}

		public VirtualKey(int keyCode, int secondKeyCode, String label) {
			this(keyCode, label);
			this.secondKeyCode = secondKeyCode;
		}

		public int getKeyCode() {
			return keyCode;
		}

		public int getSecondKeyCode() {
			return secondKeyCode;
		}

		public void setSelected(boolean flag) {
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

		public void resize(float size) {
			rect.right = rect.left + size;
			rect.bottom = rect.top + size;
		}

		public boolean contains(float x, float y) {
			return visible && rect.contains(x, y);
		}

		public void paint(Graphics g) {
			if (label != null && visible) {
				g.setColor(colors[selected ? BACKGROUND_SELECTED : BACKGROUND]);
				g.fillArc(rect, 0, 360);

				g.setColor(colors[selected ? FOREGROUND_SELECTED : FOREGROUND]);
				g.setFont(font);
				g.drawString(label, (int) rect.centerX(), (int) rect.centerY(), Graphics.HCENTER | Graphics.VCENTER);

				g.setColor(colors[OUTLINE]);
				g.drawArc(rect, 0, 360);
			}
		}

		public String getLabel() {
			return label;
		}

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
	private static final int SCREEN = -1;

	private static final int KEY_NUM1 = 0,
			KEY_NUM2 = 1,
			KEY_NUM3 = 2,
			KEY_NUM4 = 3,
			KEY_NUM5 = 4,
			KEY_NUM6 = 5,
			KEY_NUM7 = 6,
			KEY_NUM8 = 7,
			KEY_NUM9 = 8,
			KEY_NUM0 = 9,
			KEY_STAR = 10,
			KEY_POUND = 11,
			KEY_SOFT_LEFT = 12,
			KEY_SOFT_RIGHT = 13,
			KEY_DIAL = 14,
			KEY_CANCEL = 15,
			KEY_UP_LEFT = 16,
			KEY_UP = 17,
			KEY_UP_RIGHT = 18,
			KEY_LEFT = 19,
			KEY_RIGHT = 20,
			KEY_DOWN_LEFT = 21,
			KEY_DOWN = 22,
			KEY_DOWN_RIGHT = 23,
			KEY_FIRE = 24;

	private static final int LAYOUT_SIGNATURE = 0x564B4C00;
	private static final int LAYOUT_OLD_VERSION = 1;
	private static final int LAYOUT_VERSION = 2;

	public static final int LAYOUT_EOF = -1;
	public static final int LAYOUT_KEYS = 0,
			LAYOUT_SCALES = 1,
			LAYOUT_COLORS = 2;

	protected Font font;
	private int delay = -1;
	private int overlayAlpha = 64;

	public static final int BACKGROUND = 0;
	public static final int FOREGROUND = 1;
	public static final int BACKGROUND_SELECTED = 2;
	public static final int FOREGROUND_SELECTED = 3;
	public static final int OUTLINE = 4;

	private int[] colors
			= {
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

	private float[] keyScales
			= {
			1,
			1,
			1,
			0.75f,
			1.5f
	};

	private int[][] keyScaleGroups
			= {
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
	protected Context context;

	private Image offscreen;
	private Graphics offgraphics;
	private boolean obscuresVirtualScreen;
	private boolean offscreenChanged;
	private boolean feedback;
	private static final int FEEDBACK_DURATION = 50;

	private boolean visible, hiding, skip;
	private final Object waiter = new Object();
	private Thread hider;

	private int[] snapOrigins;
	private int[] snapModes;
	private PointF[] snapOffsets;
	private boolean[] snapValid;
	private int[] snapStack;

	private int layoutEditMode;
	private int editedIndex;
	private float offsetX, offsetY;
	private float prevScale;
	private int layoutVariant;

	protected RectF screen;
	private RectF virtualScreen;
	private float keySize;
	private float snapRadius;

	private VirtualKey[] keypad;
	private VirtualKey[] associatedKeys;

	private KeyRepeater repeater;
	protected LayoutListener listener;

	public VirtualKeyboard() {
		context = ContextHolder.getContext();
		font = new Font();
		keypad = new VirtualKey[KEYBOARD_SIZE];
		associatedKeys = new VirtualKey[10]; // у среднестатистического пользователя обычно не более 10 пальцев...

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

		keypad[KEY_UP_LEFT] = new VirtualKey(Canvas.KEY_UP, Canvas.KEY_LEFT, ARROW_LEFT + ARROW_UP);
		keypad[KEY_UP] = new VirtualKey(Canvas.KEY_UP, ARROW_UP);
		keypad[KEY_UP_RIGHT] = new VirtualKey(Canvas.KEY_UP, Canvas.KEY_RIGHT, ARROW_UP + ARROW_RIGHT);

		keypad[KEY_LEFT] = new VirtualKey(Canvas.KEY_LEFT, ARROW_LEFT);
		keypad[KEY_RIGHT] = new VirtualKey(Canvas.KEY_RIGHT, ARROW_RIGHT);

		keypad[KEY_DOWN_LEFT] = new VirtualKey(Canvas.KEY_DOWN, Canvas.KEY_LEFT, ARROW_LEFT + ARROW_DOWN);
		keypad[KEY_DOWN] = new VirtualKey(Canvas.KEY_DOWN, ARROW_DOWN);
		keypad[KEY_DOWN_RIGHT] = new VirtualKey(Canvas.KEY_DOWN, Canvas.KEY_RIGHT, ARROW_DOWN + ARROW_RIGHT);

		keypad[KEY_FIRE] = new VirtualKey(Canvas.KEY_FIRE, "F");

		snapOrigins = new int[keypad.length];
		snapModes = new int[keypad.length];
		snapOffsets = new PointF[keypad.length];
		snapValid = new boolean[keypad.length];
		snapStack = new int[keypad.length];

		resetLayout(layoutVariant = 0);
		layoutEditMode = LAYOUT_EOF;
		visible = true;
		offscreenChanged = true;
		hider = new Thread(this);
		hider.start();
		repeater = new KeyRepeater();
	}

	private void resetLayout(int variant) {
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

				for (int i = KEY_NUM1; i < 12; i++) {
					keypad[i].setVisible(true);
				}
				keypad[KEY_DIAL].setVisible(true);
				keypad[KEY_CANCEL].setVisible(true);
				break;
			case 1:
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

				for (int i = KEY_NUM1; i < 12; i++) {
					keypad[i].setVisible(false);
				}
				keypad[KEY_DIAL].setVisible(false);
				keypad[KEY_CANCEL].setVisible(false);
				break;
		}
	}

	public void switchLayout() {
		layoutVariant ^= 1;
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
		repaint();
		listener.layoutChanged(this);
	}

	public void setTarget(Canvas canvas) {
		target = canvas;
		repeater.setTarget(canvas);
	}

	public void setLayoutListener(LayoutListener listener) {
		this.listener = listener;
	}

	private void setSnap(int key, int origin, int mode) {
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

	private void snapKeys() {
		obscuresVirtualScreen = false;
		for (int i = 0; i < keypad.length; i++) {
			snapKey(i, 0);
			if (RectF.intersects(keypad[i].getRect(), virtualScreen)) {
				obscuresVirtualScreen = true;
			}
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

	private void setLayoutEditMode(int mode) {
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

	public void switchLayoutEditMode(int newMode) {
		setLayoutEditMode(newMode);
		switch (newMode) {
			case LAYOUT_KEYS:
				Toast.makeText(context, R.string.layout_edit_mode, Toast.LENGTH_SHORT).show();
				break;
			case LAYOUT_SCALES:
				Toast.makeText(context, R.string.layout_scale_mode, Toast.LENGTH_SHORT).show();
				break;
			case LAYOUT_EOF:
				Toast.makeText(context, R.string.layout_edit_finished, Toast.LENGTH_SHORT).show();
				break;
		}
	}

	private void resizeKey(int key, float size) {
		keypad[key].resize(size);
		snapValid[key] = false;
	}

	private void resizeKeyGroup(int group) {
		float size = keySize * keyScales[group];
		for (int key = 0; key < keyScaleGroups[group].length; key++) {
			resizeKey(keyScaleGroups[group][key], size);
		}
	}

	public void resize(RectF screen, RectF virtualScreen) {
		this.screen = screen;
		this.virtualScreen = virtualScreen;
		int width = Math.round(screen.width());
		int height = Math.round(screen.height());
		if (offscreen == null || offscreen.getWidth() != width || offscreen.getHeight() != height) {
			offscreen = Image.createImage(width, height);
			offgraphics = offscreen.getGraphics();
		}
		snapRadius = keyScales[0];
		for (int i = 1; i < keyScales.length; i++) {
			if (keyScales[i] < snapRadius) {
				snapRadius = keyScales[i];
			}
		}
		keySize = (float) Math.max(width, height) / 12;
		snapRadius = keySize * snapRadius / 4;
		for (int group = 0; group < keyScaleGroups.length; group++) {
			resizeKeyGroup(group);
		}
		snapKeys();
		repaint();
	}

	public void paint(Graphics g) {
		if (visible) {
			if (obscuresVirtualScreen && overlayAlpha <= 250) {
				if (offscreenChanged) {
					offgraphics.clear(0);
					for (VirtualKey aKeypad : keypad) {
						aKeypad.paint(offgraphics);
					}
					offscreenChanged = false;
				}
				g.drawImage(offscreen, 0, 0, -1, -1, false, overlayAlpha);
			} else {
				for (VirtualKey aKeypad : keypad) {
					aKeypad.paint(g);
				}
			}
		}
	}

	private void repaint() {
		offscreenChanged = true;

		/*if (target != null) {
			target.repaint();
		}*/
	}

	/**
	 * Проверить, было ли нами обработано нажатие указателя.
	 * <p>
	 * Считается, что не было, если оно пришлось на виртуальный экран: в этом
	 * случае оно достанется и мидлету. А вот нажатия вне виртуального экрана
	 * мидлету не передаются в целях оптимизации.
	 *
	 * @param x координаты нажатия
	 * @param y координаты нажатия
	 * @return true, если нажатие пришлось на виртуальный экран мидлета
	 */
	private boolean checkPointerHandled(float x, float y) {
		return !virtualScreen.contains(x, y);
	}

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
						target.postEvent(CanvasEvent.getInstance(target, CanvasEvent.KEY_PRESSED, aKeypad.getKeyCode()));
						if (aKeypad.getSecondKeyCode() != 0) {
							target.postEvent(CanvasEvent.getInstance(target, CanvasEvent.KEY_PRESSED, aKeypad.getSecondKeyCode()));
						}
						repeater.start(aKeypad.getKeyCode(), aKeypad.getSecondKeyCode());
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
					repeater.stop();
					target.postEvent(CanvasEvent.getInstance(target, CanvasEvent.KEY_RELEASED, associatedKeys[pointer].getKeyCode()));
					if (associatedKeys[pointer].getSecondKeyCode() != 0) {
						target.postEvent(CanvasEvent.getInstance(target, CanvasEvent.KEY_RELEASED, associatedKeys[pointer].getSecondKeyCode()));
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
				repeater.stop();
				target.postEvent(CanvasEvent.getInstance(target, CanvasEvent.KEY_RELEASED, associatedKeys[pointer].getKeyCode()));
				if (associatedKeys[pointer].getSecondKeyCode() != 0) {
					target.postEvent(CanvasEvent.getInstance(target, CanvasEvent.KEY_RELEASED, associatedKeys[pointer].getSecondKeyCode()));
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

	public void show() {
		synchronized (waiter) {
			if (hiding) {
				hider.interrupt();
			}
		}
		visible = true;
		repaint();
	}

	public void hide() {
		if (delay >= 0 && obscuresVirtualScreen) {
			synchronized (waiter) {
				waiter.notifyAll();
			}
		}
	}

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

	public boolean keyPressed(int keyCode) {
		for (VirtualKey aKeypad : keypad) {
			if (aKeypad.getKeyCode() == keyCode && aKeypad.getSecondKeyCode() == 0) {
				aKeypad.setSelected(true);
			}
		}
		return false;
	}

	public boolean keyRepeated(int keyCode) {
		return false;
	}

	public boolean keyReleased(int keyCode) {
		for (VirtualKey aKeypad : keypad) {
			if (aKeypad.getKeyCode() == keyCode && aKeypad.getSecondKeyCode() == 0) {
				aKeypad.setSelected(false);
			}
		}
		return false;
	}

	private void vibrate() {
		if (feedback) Display.getDisplay(null).vibrate(FEEDBACK_DURATION);
	}

	public Font getFont() {
		return font;
	}

	public void setFont(Font font) {
		this.font = font;
	}

	public void setHideDelay(int delay) {
		this.delay = delay;
	}

	public void setOverlayAlpha(int overlayAlpha) {
		this.overlayAlpha = overlayAlpha;
	}

	public void setColor(int color, int value) {
		colors[color] = value;
	}

	public void setHasHapticFeedback(boolean feedback) {
		this.feedback = feedback;
	}
}
