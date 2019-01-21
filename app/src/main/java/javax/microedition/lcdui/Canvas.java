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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.os.Build;
import android.support.v4.util.SparseArrayCompat;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.lcdui.event.CanvasEvent;
import javax.microedition.lcdui.event.Event;
import javax.microedition.lcdui.event.EventFilter;
import javax.microedition.lcdui.event.EventQueue;
import javax.microedition.util.ContextHolder;

public abstract class Canvas extends Displayable {
	public static final int KEY_POUND = 35;
	public static final int KEY_STAR = 42;
	public static final int KEY_NUM0 = 48;
	public static final int KEY_NUM1 = 49;
	public static final int KEY_NUM2 = 50;
	public static final int KEY_NUM3 = 51;
	public static final int KEY_NUM4 = 52;
	public static final int KEY_NUM5 = 53;
	public static final int KEY_NUM6 = 54;
	public static final int KEY_NUM7 = 55;
	public static final int KEY_NUM8 = 56;
	public static final int KEY_NUM9 = 57;

	public static final int KEY_UP = -1;
	public static final int KEY_DOWN = -2;
	public static final int KEY_LEFT = -3;
	public static final int KEY_RIGHT = -4;
	public static final int KEY_FIRE = -5;
	public static final int KEY_SOFT_LEFT = -6;
	public static final int KEY_SOFT_RIGHT = -7;
	public static final int KEY_CLEAR = -8;
	public static final int KEY_SEND = -10;
	public static final int KEY_END = -11;

	public static final int UP = 1;
	public static final int LEFT = 2;
	public static final int RIGHT = 5;
	public static final int DOWN = 6;
	public static final int FIRE = 8;
	public static final int GAME_A = 9;
	public static final int GAME_B = 10;
	public static final int GAME_C = 11;
	public static final int GAME_D = 12;

	private static SparseIntArray androidToMIDP;
	private static SparseIntArray keyCodeToGameAction;
	private static SparseIntArray gameActionToKeyCode;
	private static SparseArrayCompat<String> keyCodeToKeyName;

	static {
		keyCodeToGameAction = new SparseIntArray();
		gameActionToKeyCode = new SparseIntArray();
		keyCodeToKeyName = new SparseArrayCompat<>();

		mapKeyCode(KEY_NUM0, 0, "0");
		mapKeyCode(KEY_NUM1, 0, "1");
		mapKeyCode(KEY_NUM2, UP, "2");
		mapKeyCode(KEY_NUM3, 0, "3");
		mapKeyCode(KEY_NUM4, LEFT, "4");
		mapKeyCode(KEY_NUM5, FIRE, "5");
		mapKeyCode(KEY_NUM6, RIGHT, "6");
		mapKeyCode(KEY_NUM7, GAME_A, "7");
		mapKeyCode(KEY_NUM8, DOWN, "8");
		mapKeyCode(KEY_NUM9, GAME_B, "9");
		mapKeyCode(KEY_STAR, GAME_C, "ASTERISK");
		mapKeyCode(KEY_POUND, GAME_D, "POUND");
		mapKeyCode(KEY_UP, UP, "UP");
		mapKeyCode(KEY_DOWN, DOWN, "DOWN");
		mapKeyCode(KEY_LEFT, LEFT, "LEFT");
		mapKeyCode(KEY_RIGHT, RIGHT, "RIGHT");
		mapKeyCode(KEY_FIRE, FIRE, "SELECT");
		mapKeyCode(KEY_SOFT_LEFT, 0, "SOFT1");
		mapKeyCode(KEY_SOFT_RIGHT, 0, "SOFT2");
		mapKeyCode(KEY_CLEAR, 0, "CLEAR");
		mapKeyCode(KEY_SEND, 0, "SEND");
		mapKeyCode(KEY_END, 0, "END");

		mapGameAction(UP, KEY_UP, "UP");
		mapGameAction(LEFT, KEY_LEFT, "LEFT");
		mapGameAction(RIGHT, KEY_RIGHT, "RIGHT");
		mapGameAction(DOWN, KEY_DOWN, "DOWN");
		mapGameAction(FIRE, KEY_FIRE, "SELECT");
		mapGameAction(GAME_A, KEY_NUM7, "7");
		mapGameAction(GAME_B, KEY_NUM9, "9");
		mapGameAction(GAME_C, KEY_STAR, "ASTERISK");
		mapGameAction(GAME_D, KEY_POUND, "POUND");
	}

	private static void mapKeyCode(int midpKeyCode, int gameAction, String keyName) {
		keyCodeToGameAction.put(midpKeyCode, gameAction);
		keyCodeToKeyName.put(midpKeyCode, keyName);
	}

	private static void mapGameAction(int gameAction, int keyCode, String keyName) {
		gameActionToKeyCode.put(gameAction, keyCode);
		keyCodeToKeyName.put(keyCode, keyName);
	}

	private static int convertAndroidKeyCode(int keyCode) {
		return androidToMIDP.get(keyCode, Integer.MAX_VALUE);
	}

	public int getKeyCode(int gameAction) {
		int res = gameActionToKeyCode.get(gameAction, Integer.MAX_VALUE);
		if (res != Integer.MAX_VALUE) {
			return res;
		} else {
			throw new IllegalArgumentException("unknown game action " + gameAction);
		}
	}

	public int getGameAction(int keyCode) {
		int res = keyCodeToGameAction.get(keyCode, Integer.MAX_VALUE);
		if (res != Integer.MAX_VALUE) {
			return res;
		} else {
			throw new IllegalArgumentException("unknown keycode " + keyCode);
		}
	}

	public String getKeyName(int keyCode) {
		String res = keyCodeToKeyName.get(keyCode);
		if (res != null) {
			return res;
		} else {
			throw new IllegalArgumentException("unknown keycode " + keyCode);
		}
	}

	private class InnerView extends SurfaceView implements SurfaceHolder.Callback {

		private Graphics mGraphics;

		public InnerView(Context context) {
			super(context);
			getHolder().addCallback(this);
			getHolder().setFormat(PixelFormat.RGBA_8888);
			setFocusableInTouchMode(true);
			if (hwaOldEnabled) {
				setWillNotDraw(false);
				mGraphics = new Graphics();
				mGraphics.setFont(new Font());
			}
		}

		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			keyCode = convertAndroidKeyCode(keyCode);
			if (keyCode == Integer.MAX_VALUE) {
				return false;
			}
			if (event.getRepeatCount() == 0) {
				if (overlay == null || !overlay.keyPressed(keyCode)) {
					postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.KEY_PRESSED, keyCode));
				}
			} else {
				if (overlay == null || !overlay.keyRepeated(keyCode)) {
					postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.KEY_REPEATED, keyCode));
				}
			}
			return true;
		}

		@Override
		public boolean onKeyUp(int keyCode, KeyEvent event) {
			keyCode = convertAndroidKeyCode(keyCode);
			if (keyCode == Integer.MAX_VALUE) {
				return false;
			}
			if (overlay == null || !overlay.keyReleased(keyCode)) {
				postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.KEY_RELEASED, keyCode));
			}
			return true;
		}

		@Override
		@SuppressLint("ClickableViewAccessibility")
		public boolean onTouchEvent(MotionEvent event) {
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					if (overlay != null) {
						overlay.show();
					}
				case MotionEvent.ACTION_POINTER_DOWN:
					int index = event.getActionIndex();
					int id = event.getPointerId(index);
					if ((overlay == null || !overlay.pointerPressed(id, event.getX(index), event.getY(index)))
							&& touchInput && id == 0) {
						postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.POINTER_PRESSED, id,
								convertPointerX(event.getX()), convertPointerY(event.getY())));
					}
					break;
				case MotionEvent.ACTION_MOVE:
					int pointerCount = event.getPointerCount();
					int historySize = event.getHistorySize();
					for (int h = 0; h < historySize; h++) {
						for (int p = 0; p < pointerCount; p++) {
							id = event.getPointerId(p);
							if ((overlay == null || !overlay.pointerDragged(id, event.getHistoricalX(p, h), event.getHistoricalY(p, h)))
									&& touchInput && id == 0) {
								postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.POINTER_DRAGGED, id,
										convertPointerX(event.getHistoricalX(p, h)), convertPointerY(event.getHistoricalY(p, h))));
							}
						}
					}
					for (int p = 0; p < pointerCount; p++) {
						id = event.getPointerId(p);
						if ((overlay == null || !overlay.pointerDragged(id, event.getX(p), event.getY(p)))
								&& touchInput && id == 0) {
							postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.POINTER_DRAGGED, id,
									convertPointerX(event.getX(p)), convertPointerY(event.getY(p))));
						}
					}
					break;
				case MotionEvent.ACTION_UP:
					if (overlay != null) {
						overlay.hide();
					}
				case MotionEvent.ACTION_POINTER_UP:
					index = event.getActionIndex();
					id = event.getPointerId(index);
					if ((overlay == null || !overlay.pointerReleased(id, event.getX(index), event.getY(index)))
							&& touchInput && id == 0) {
						postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.POINTER_RELEASED, id,
								convertPointerX(event.getX()), convertPointerY(event.getY())));
					}
					break;
				default:
					return super.onTouchEvent(event);
			}
			return true;
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int newwidth, int newheight) {
			synchronized (paintsync) {
				displayWidth = newwidth;
				displayHeight = newheight;
				updateSize();
				postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.SIZE_CHANGED, width, height));
			}
			postEvent(paintEvent);
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			surface = holder.getSurface();
			synchronized (paintsync) {
				postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.SHOW_NOTIFY));
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			surface = null;
			synchronized (paintsync) {
				postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.HIDE_NOTIFY));
			}
		}

		@Override
		protected void onDraw(android.graphics.Canvas canvas) {
			Graphics g = mGraphics;
			g.setSurfaceCanvas(canvas);
			g.clear(backgroundColor);
			g.drawImage(offscreenCopy, onX, onY, onWidth, onHeight, filter, 255);
			if (overlay != null) {
				overlay.paint(g);
			}
			if (showFps) {
				drawFps(g);
				totalFrameCount++;
			}
		}
	}

	private class PaintEvent extends Event implements EventFilter {

		private Graphics mGraphics = new Graphics();

		@Override
		public void process() {
			synchronized (paintsync) {
				Graphics g = this.mGraphics;
				g.setCanvas(offscreen.getCanvas(), offscreen.getBitmap());
				g.reset();
				try {
					paint(g);
				} catch (Throwable t) {
					t.printStackTrace();
				}
				offscreen.copyPixels(offscreenCopy);
				repaintScreen();
			}
		}

		@Override
		public void recycle() {
		}

		private int enqueued = 0;

		@Override
		public void enterQueue() {
			enqueued++;
		}

		@Override
		public void leaveQueue() {
			enqueued--;
		}

		/**
		 * The queue should contain no more than two repaint events
		 * <p>
		 * One won't be smooth enough, and if you add more than two,
		 * then how to determine exactly how many of them need to be added?
		 */
		@Override
		public boolean placeableAfter(Event event) {
			return enqueued < 2;
		}

		@Override
		public boolean accept(Event event) {
			return event == this;
		}
	}

	private final Object paintsync = new Object();

	private PaintEvent paintEvent = new PaintEvent();

	private LinearLayout layout;
	private InnerView innerView;
	private Surface surface;
	private Graphics graphics = new Graphics();

	protected int width, height;

	private int displayWidth;
	private int displayHeight;

	private static int virtualWidth;
	private static int virtualHeight;

	private static boolean scaleToFit;
	private static boolean keepAspectRatio;
	private static boolean filter;
	private static boolean touchInput;
	private static boolean hardwareAcceleration;
	private static boolean hwaOldEnabled;
	private static boolean showFps;
	private static int backgroundColor;
	private static int scaleRatio;

	private Image offscreen;
	private Image offscreenCopy;
	private int onX, onY, onWidth, onHeight;
	private int totalFrameCount = 0;
	private String prevFrameCount = "0";

	private Overlay overlay;

	public Canvas() {
		displayWidth = ContextHolder.getDisplayWidth();
		displayHeight = ContextHolder.getDisplayHeight();
		Log.d("Canvas", "Constructor. w=" + displayWidth + " h=" + displayHeight);
		if (showFps) startFpsCounter();
		graphics.setFont(new Font());
		updateSize();
	}

	private void startFpsCounter() {
		TimerTask updateFPS = new TimerTask() {
			public void run() {
				prevFrameCount = String.valueOf(totalFrameCount);
				totalFrameCount = 0;
			}
		};
		Timer t = new Timer();
		t.scheduleAtFixedRate(updateFPS, 0, 1000);
	}

	private void drawFps(Graphics g) {
		String fps = prevFrameCount;
		Font font = g.getFont();
		g.setColorAlpha(0x90000000);
		g.fillRect(0, 0, font.stringWidth(fps), font.getHeight());
		g.setColor(0, 255, 0);
		g.drawString(fps, 0, 0, 0);
	}

	public static void setVirtualSize(int virtualWidth, int virtualHeight, boolean scaleToFit, boolean keepAspectRatio, int scaleRatio) {
		Canvas.virtualWidth = virtualWidth;
		Canvas.virtualHeight = virtualHeight;

		Canvas.scaleToFit = scaleToFit;
		Canvas.keepAspectRatio = keepAspectRatio;
		Canvas.scaleRatio = scaleRatio;
	}

	public static void setBackgroundColor(int color) {
		backgroundColor = color | 0xFF000000;
	}

	public static void setFilterBitmap(boolean filter) {
		Canvas.filter = filter;
	}

	public static void setKeyMapping(SparseIntArray intArray) {
		Canvas.androidToMIDP = intArray;
	}

	public static void setHasTouchInput(boolean touchInput) {
		Canvas.touchInput = touchInput;
	}

	public static void setHardwareAcceleration(boolean hardwareAcceleration) {
		Canvas.hardwareAcceleration = hardwareAcceleration;
		Canvas.hwaOldEnabled = hardwareAcceleration && Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
	}

	public static void setShowFps(boolean showFps) {
		Canvas.showFps = showFps;
	}

	public void setOverlay(Overlay ov) {
		if (overlay != null) {
			overlay.setTarget(null);
		}
		if (ov != null) {
			ov.setTarget(this);
		}
		overlay = ov;
	}

	public Image getOffscreenCopy() {
		Image image = Image.createImage(onWidth, onHeight);
		Graphics g = image.getGraphics();
		g.drawImage(offscreenCopy, 0, 0, onWidth, onHeight, filter, 255);
		return image;
	}

	/**
	 * Update the size and position of the virtual screen relative to the real one.
	 */
	private void updateSize() {
		/*
		 * We turn the sizes of the virtual screen into the sizes of the visible canvas.
		 *
		 * At the same time, we take into account that one or both virtual sizes can be less
		 * than zero, which means auto-selection of this size so that the resulting canvas
		 * has the same aspect ratio as the actual screen of the device.
		 */
		if (virtualWidth < 0) {
			if (virtualHeight < 0) {
				/*
				 * nothing is set - screen-sized canvas
				 */
				width = displayWidth;
				height = displayHeight;
			} else {
				/*
				 * only the canvas height is set
				 * width is selected by the ratio of the real screen
				 */
				width = displayWidth * virtualHeight / displayHeight;
				height = virtualHeight;
			}
		} else if (virtualHeight < 0) {
			/*
			 * only the canvas width is set
			 * height is selected by the ratio of the real screen
			 */
			width = virtualWidth;
			height = displayHeight * virtualWidth / displayWidth;
		} else {
			/*
			 * the width and height of the canvas are strictly set
			 */
			width = virtualWidth;
			height = virtualHeight;
		}
		/*
		 * We turn the size of the canvas into the size of the image
		 * that will be displayed on the screen of the device.
		 */
		if (scaleToFit) {
			if (keepAspectRatio) {
				/*
				 * try to fit in width
				 */
				onWidth = displayWidth;
				onHeight = height * displayWidth / width;
				if (onHeight > displayHeight) {
					/*
					 * if height is too big,
					 * then fit in height
					 */
					onHeight = displayHeight;
					onWidth = width * displayHeight / height;
				}
			} else {
				/*
				 * scaling without preserving the aspect ratio:
				 * just stretch the picture to full screen
				 */
				onWidth = displayWidth;
				onHeight = displayHeight;
			}
		} else {
			/*
			 * without scaling
			 */
			onWidth = width;
			onHeight = height;
		}

		onWidth = onWidth * scaleRatio / 100;
		onHeight = onHeight * scaleRatio / 100;

		if (displayWidth >= displayHeight) {
			/*
			 * If we hold the screen horizontally, then most likely we hold it by the left and right edges.
			 * Place the midlet screen in the center.
			 */
			onX = (displayWidth - onWidth) / 2;
			onY = (displayHeight - onHeight) / 2;
		} else {
			/*
			 * If we hold the screen vertically, then most likely we hold it by the bottom edge.
			 * Shift the midlet screen to the top.
			 */
			onX = (displayWidth - onWidth) / 2;
			onY = 0;
		}

		RectF screen = new RectF(0, 0, displayWidth, displayHeight);
		RectF virtualScreen = new RectF(onX, onY, onX + onWidth, onY + onHeight);

		if (offscreen == null || offscreen.getWidth() != width || offscreen.getHeight() != height) {
			offscreen = Image.createImage(width, height, false, offscreen);
			offscreenCopy = Image.createImage(width, height, false, offscreenCopy);
		}
		if (overlay != null) {
			overlay.resize(screen, virtualScreen);
		}
	}

	/**
	 * Convert the screen coordinates of the pointer into the virtual ones.
	 *
	 * @param x the pointer coordinate on the real screen
	 * @return the corresponding pointer coordinate on the virtual screen
	 */
	private float convertPointerX(float x) {
		return (x - onX) * width / onWidth;
	}

	/**
	 * Convert the screen coordinates of the pointer into the virtual ones.
	 *
	 * @param y the pointer coordinate on the real screen
	 * @return the corresponding pointer coordinate on the virtual screen
	 */
	private float convertPointerY(float y) {
		return (y - onY) * height / onHeight;
	}

	@Override
	public View getDisplayableView() {
		if (layout == null) {
			layout = (LinearLayout) super.getDisplayableView();
			innerView = new InnerView(getParentActivity());
			layout.addView(innerView);
		}
		return layout;
	}

	@Override
	public void clearDisplayableView() {
		synchronized (paintsync) {
			super.clearDisplayableView();
			layout = null;
			innerView = null;
		}
	}

	public void setFullScreenMode(boolean flag) {
	}

	public boolean hasPointerEvents() {
		return touchInput;
	}

	public boolean hasPointerMotionEvents() {
		return touchInput;
	}

	public boolean hasRepeatEvents() {
		return true;
	}

	public boolean isDoubleBuffered() {
		return true;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	public abstract void paint(Graphics g);

	public final void repaint() {
		repaint(0, 0, width, height);
	}

	public final void repaint(int x, int y, int width, int height) {
		postEvent(paintEvent);
	}

	// GameCanvas
	public void flushBuffer(Image image) {
		synchronized (paintsync) {
			image.copyPixels(offscreenCopy);
			repaintScreen();
		}
	}

	@SuppressLint("NewApi")
	private void repaintScreen() {
		if (hwaOldEnabled) {
			if (innerView != null) {
				innerView.postInvalidate();
			}
			return;
		}
		Surface surface = this.surface;
		if (surface == null || !surface.isValid()) {
			return;
		}
		try {
			android.graphics.Canvas canvas = hardwareAcceleration ?
					surface.lockHardwareCanvas() : surface.lockCanvas(null);
			if (canvas == null) {
				return;
			}
			Graphics g = this.graphics;
			g.setSurfaceCanvas(canvas);
			g.clear(backgroundColor);
			g.drawImage(offscreenCopy, onX, onY, onWidth, onHeight, filter, 255);
			if (overlay != null) {
				overlay.paint(g);
			}
			if (showFps) {
				drawFps(g);
				totalFrameCount++;
			}
			surface.unlockCanvasAndPost(canvas);
		} catch (Exception e) {
			Log.w("Canvas", "repaintScreen: " + e.getMessage());
		}
	}

	/**
	 * After calling this method, an immediate redraw is guaranteed to occur,
	 * and the calling thread is blocked until it is completed.
	 */
	public final void serviceRepaints() {
		EventQueue queue = getEventQueue();

		/*
		 * blocking order:
		 *
		 * 1 - queue.this
		 * 2 - queue.queue
		 *
		 * accordingly, inside the EventQueue, the order must be the same,
		 * otherwise mutual blocking of two threads is possible (everything will hang)
		 */

		synchronized (queue) {
			/*
			 * This synchronization actually stops the events processing
			 * just before changing the value of currentEvent()
			 *
			 * Then there are only two options:
			 */

			if (queue.currentEvent() == paintEvent) {
				/*
				 * if repaint() is being processed there now,
				 * then you just need to wait for it to finish
				 */

				if (Thread.holdsLock(paintsync)) { // Avoid deadlock
					return;
				}

				try {
					queue.wait();
				} catch (InterruptedException ie) {
					ie.printStackTrace();
				}
			} else if (queue.removeEvents(paintEvent)) {
				/*
				 * if now something else is being processed there (not repaint),
				 * but the repaint was in the queue (and was removed from there),
				 * then it needs to be synchronously called from here
				 */

				paintEvent.run();
			}
		}
	}

	public void showNotify() {
	}

	public void hideNotify() {
	}

	public void keyPressed(int keyCode) {
	}

	public void keyRepeated(int keyCode) {
	}

	public void keyReleased(int keyCode) {
	}

	public void pointerPressed(int pointer, float x, float y) {
		if (pointer == 0) {
			pointerPressed(Math.round(x), Math.round(y));
		}
	}

	public void pointerDragged(int pointer, float x, float y) {
		if (pointer == 0) {
			pointerDragged(Math.round(x), Math.round(y));
		}
	}

	public void pointerReleased(int pointer, float x, float y) {
		if (pointer == 0) {
			pointerReleased(Math.round(x), Math.round(y));
		}
	}

	public void pointerPressed(int x, int y) {
	}

	public void pointerDragged(int x, int y) {
	}

	public void pointerReleased(int x, int y) {
	}
}
