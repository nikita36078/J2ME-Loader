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
import android.graphics.RectF;
import android.os.Build;
import android.support.v4.util.SparseArrayCompat;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

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
		return androidToMIDP.get(keyCode, -(keyCode << 8));
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
		public InnerView(Context context) {
			super(context);
			getHolder().addCallback(this);
			getHolder().setFormat(android.graphics.PixelFormat.RGBA_8888);
			setFocusableInTouchMode(true);
		}

		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			keyCode = convertAndroidKeyCode(keyCode);
			if (event.getRepeatCount() == 0) {
				if (overlay == null || !overlay.keyPressed(keyCode)) {
					postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.KEY_PRESSED, keyCode));
				}
			} else {
				if (overlay == null || !overlay.keyRepeated(keyCode)) {
					postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.KEY_REPEATED, keyCode));
				}
			}
			return super.onKeyDown(keyCode, event);
		}

		@Override
		public boolean onKeyUp(int keyCode, KeyEvent event) {
			keyCode = convertAndroidKeyCode(keyCode);
			if (overlay == null || !overlay.keyReleased(keyCode)) {
				postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.KEY_RELEASED, keyCode));
			}
			return super.onKeyUp(keyCode, event);
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
					if ((overlay == null || !overlay.pointerPressed(id, event.getX(index), event.getY(index))) && touchInput) {
						postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.POINTER_PRESSED, index,
								convertPointerX(event.getX()), convertPointerY(event.getY())));
					}
					break;
				case MotionEvent.ACTION_MOVE:
					int pointerCount = event.getPointerCount();
					int historySize = event.getHistorySize();
					for (int h = 0; h < historySize; h++) {
						for (int p = 0; p < pointerCount; p++) {
							id = event.getPointerId(p);
							if ((overlay == null || !overlay.pointerDragged(id, event.getHistoricalX(p, h), event.getHistoricalY(p, h))) && touchInput) {
								postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.POINTER_DRAGGED, p,
										convertPointerX(event.getHistoricalX(p, h)), convertPointerY(event.getHistoricalY(p, h))));
							}
						}
					}
					for (int p = 0; p < pointerCount; p++) {
						id = event.getPointerId(p);
						if ((overlay == null || !overlay.pointerDragged(id, event.getX(p), event.getY(p))) && touchInput) {
							postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.POINTER_DRAGGED, p,
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
					if ((overlay == null || !overlay.pointerReleased(id, event.getX(index), event.getY(index))) && touchInput) {
						postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.POINTER_RELEASED, index,
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
			surfaceCreated = true;
			synchronized (paintsync) {
				postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.SHOW_NOTIFY));
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			surfaceCreated = false;
			synchronized (paintsync) {
				postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.HIDE_NOTIFY));
			}
		}
	}

	private class PaintEvent extends Event implements EventFilter {
		@Override
		public void process() {
			synchronized (paintsync) {
				if (holder == null || !holder.getSurface().isValid() || !surfaceCreated) {
					return;
				}
				graphics.setCanvas(offscreen.getCanvas(), offscreen.getBitmap());
				graphics.resetTranslation();
				graphics.resetClip();
				try {
					paint(graphics);
				} catch (Throwable t) {
					t.printStackTrace();
					graphics.resetTranslation();
					graphics.resetClip();
				}
				graphics.setCanvas(lockCanvas(), null);
				if (graphics.hasCanvas()) {
					graphics.clear(backgroundColor);
					graphics.drawImage(offscreen, onX, onY, onWidth, onHeight, filter, 255);
					if (overlay != null) {
						overlay.paint(graphics);
					}
					unlockCanvasAndPost(graphics.getCanvas());
				}
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
		 * В очереди должно быть не более двух перерисовок.
		 * <p>
		 * Одна не обеспечит плавности, а если делать больше двух,
		 * то как определить, насколько именно больше двух их нужно сделать?
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
	private boolean surfaceCreated = false;

	private InnerView view;
	private SurfaceHolder holder;
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
	private static int backgroundColor;
	private static int scaleRatio;

	private Image offscreen;
	private int onX, onY, onWidth, onHeight;

	private Overlay overlay;

	public Canvas() {
		displayWidth = ContextHolder.getDisplayWidth();
		displayHeight = ContextHolder.getDisplayHeight();
		Log.d("Canvas", "Constructor. w=" + displayWidth + " h=" + displayHeight);

		updateSize();
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

	/**
	 * Обновить размер и положение виртуального экрана относительно реального.
	 */
	private void updateSize() {
		/*
		 * Превращаем размеры виртуального экрана
		 * в размеры видимого для мидлета холста.
		 *
		 * При этом учитываем, что один или оба виртуальных размера могут быть
		 * меньше нуля, что означает автоподбор этого размера так,
		 * чтобы получившийся холст имел то же соотношение сторон,
		 * что и реальный экран устройства.
		 */
		if (virtualWidth < 0) {
			if (virtualHeight < 0) {
				/*
				 * не задано ничего - холст размером в экран
				 */
				width = displayWidth;
				height = displayHeight;
			} else {
				/*
				 * задана только высота холста
				 * ширина подбирается по соотношению сторон реального экрана
				 */
				width = displayWidth * virtualHeight / displayHeight;
				height = virtualHeight;
			}
		} else if (virtualHeight < 0) {
			/*
			 * задана только ширина холста
			 * высота подбирается по соотношению сторон реального экрана
			 */
			width = virtualWidth;
			height = displayHeight * virtualWidth / displayWidth;
		} else {
			/*
			 * ширина и высота холста жестко заданы
			 */
			width = virtualWidth;
			height = virtualHeight;
		}
		/*
		 * Превращаем размеры холста в размер картинки,
		 * которая будет отображаться на экране устройсва.
		 */
		if (scaleToFit) {
			if (keepAspectRatio) {
				/*
				 * пробуем вписать по ширине
				 */
				onWidth = displayWidth;
				onHeight = height * displayWidth / width;
				if (onHeight > displayHeight) {
					/*
					 * если при этом не влезли по высоте,
					 * то вписываем по высоте
					 */
					onHeight = displayHeight;
					onWidth = width * displayHeight / height;
				}
			} else {
				/*
				 * масштабирование без сохранения соотношения сторон:
				 * просто растягиваем картинку на весь экран
				 */
				onWidth = displayWidth;
				onHeight = displayHeight;
			}
		} else {
			/*
			 * без масштабирования
			 */
			onWidth = width;
			onHeight = height;
		}

		onWidth = onWidth * scaleRatio / 100;
		onHeight = onHeight * scaleRatio / 100;

		if (displayWidth >= displayHeight) {
			/*
			 * Если мы держим экран горизонтально,
			 * то скорее всего мы держим его за левый и правый края.
			 * Размещаем экран мидлета в центре.
			 */
			onX = (displayWidth - onWidth) / 2;
			onY = (displayHeight - onHeight) / 2;
		} else {
			/*
			 * Если мы держим экран вертикально,
			 * то скорее всего мы держим его за нижний край.
			 * Сдвигаем экран мидлета к верхнему краю.
			 */
			onX = (displayWidth - onWidth) / 2;
			onY = 0;
		}

		RectF screen = new RectF(0, 0, displayWidth, displayHeight);
		RectF virtualScreen = new RectF(onX, onY, onX + onWidth, onY + onHeight);

		if (offscreen == null || offscreen.getWidth() != width || offscreen.getHeight() != height) {
			offscreen = Image.createImage(width, height);
		}
		if (overlay != null) {
			overlay.resize(screen, virtualScreen);
		}
	}

	/**
	 * Привести экранные координаты указателя к виртуальным.
	 *
	 * @param x координата указателя на реальном экране
	 * @return соответствующая координата указателя на виртуальном экране
	 */
	private float convertPointerX(float x) {
		return (x - onX) * virtualWidth / onWidth;
	}

	/**
	 * Привести экранные координаты указателя к виртуальным.
	 *
	 * @param y координата указателя на реальном экране
	 * @return соответствующая координата указателя на виртуальном экране
	 */
	private float convertPointerY(float y) {
		return (y - onY) * virtualHeight / onHeight;
	}

	@Override
	public View getDisplayableView() {
		if (view == null) {
			view = new InnerView(getParentActivity());
			holder = view.getHolder();
		}
		return view;
	}

	@Override
	public void clearDisplayableView() {
		synchronized (paintsync) {
			view = null;
			holder = null;
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

	public void repaint() {
		repaint(0, 0, width, height);
	}

	public void repaint(int x, int y, int width, int height) {
		postEvent(paintEvent);
	}

	// GameCanvas
	protected void flushBuffer(Image image) {
		synchronized (paintsync) {
			if (holder == null || !holder.getSurface().isValid() || !surfaceCreated) {
				return;
			}
			graphics.setCanvas(lockCanvas(), null);
			if (graphics.hasCanvas()) {
				graphics.clear(backgroundColor);
				graphics.drawImage(image, onX, onY, onWidth, onHeight, filter, 255);
				if (overlay != null) {
					overlay.paint(graphics);
				}
				unlockCanvasAndPost(graphics.getCanvas());
			}
		}
	}

	private android.graphics.Canvas lockCanvas() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && hardwareAcceleration) {
			return holder.getSurface().lockHardwareCanvas();
		} else {
			return holder.lockCanvas();
		}
	}

	private void unlockCanvasAndPost(android.graphics.Canvas canvas) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && hardwareAcceleration) {
			holder.getSurface().unlockCanvasAndPost(canvas);
		} else {
			holder.unlockCanvasAndPost(canvas);
		}
	}

	/**
	 * После вызова этого метода гарантированно произойдет немедленная перерисовка,
	 * причем вызывающий поток блокируется до ее завершения.
	 */
	public void serviceRepaints() {
		EventQueue queue = getEventQueue();

		/*
		 * порядок блокировки:
		 *
		 * 1 - queue.this
		 * 2 - queue.queue
		 *
		 * соответственно, внутри EventQueue порядок должен быть такой же,
		 * иначе возможна взаимная блокировка двух потоков (все повиснет)
		 */

		synchronized (queue) {
			/*
			 * Такая синхронизация фактически приостанавливает обработку событий
			 * непосредственно перед изменением значения currentEvent()
			 *
			 * Тогда остается всего два варианта:
			 */

			if (queue.currentEvent() == paintEvent) {
				/*
				 * если там сейчас обрабатывается repaint(),
				 * то здесь нужно просто подождать его завершения
				 */

				try {
					queue.wait();
				} catch (InterruptedException ie) {
					ie.printStackTrace();
				}
			} else if (queue.removeEvents(paintEvent)) {
				/*
				 * если же сейчас там обрабатывается что-то другое (не repaint),
				 * но repaint в очереди все же был (и был оттуда удален),
				 * то его нужно синхронно вызвать отсюда
				 */

				paintEvent.run();
			}
		}
	}

	public void showNotify() {
	}

	public void hideNotify() {
	}

	public void sizeChanged(int w, int h) {
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
