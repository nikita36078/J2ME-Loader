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
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.lcdui.event.CanvasEvent;
import javax.microedition.lcdui.event.Event;
import javax.microedition.lcdui.event.EventFilter;
import javax.microedition.lcdui.event.EventQueue;
import javax.microedition.lcdui.graphics.CanvasView;
import javax.microedition.lcdui.graphics.CanvasWrapper;
import javax.microedition.lcdui.graphics.GlesView;
import javax.microedition.lcdui.graphics.ShaderProgram;
import javax.microedition.lcdui.keyboard.KeyMapper;
import javax.microedition.lcdui.keyboard.VirtualKeyboard;
import javax.microedition.lcdui.overlay.FpsCounter;
import javax.microedition.lcdui.overlay.Overlay;
import javax.microedition.lcdui.overlay.OverlayView;
import javax.microedition.shell.MicroActivity;
import javax.microedition.util.ContextHolder;

import androidx.annotation.NonNull;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.config.ShaderInfo;

import static android.opengl.GLES20.*;
import static android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class Canvas extends Displayable {
	private static final String TAG = Canvas.class.getName();

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

	private static final float FULLSCREEN_HEIGHT_RATIO = 0.85f;

	private static boolean filter;
	private static boolean touchInput;
	private static int graphicsMode;
	private static ShaderInfo shaderFilter;
	private static boolean parallelRedraw;
	private static boolean forceFullscreen;
	private static boolean showFps;
	private static int backgroundColor;
	private static int scaleRatio;
	private static int fpsLimit;

	private final Object paintSync = new Object();
	private final PaintEvent paintEvent = new PaintEvent();
	protected int width, height;
	protected int maxHeight;
	private LinearLayout layout;
	private SurfaceView innerView;
	private Surface surface;
	private final CanvasWrapper canvasWrapper = new CanvasWrapper(filter);
	private GLRenderer renderer;
	private int displayWidth;
	private int displayHeight;
	private boolean fullscreen = forceFullscreen;
	private boolean visible;
	private boolean sizeChangedCalled;
	private Image offscreen;
	private Image offscreenCopy;
	private int onX, onY, onWidth, onHeight;
	private final RectF virtualScreen = new RectF(0, 0, displayWidth, displayHeight);
	private long lastFrameTime = System.currentTimeMillis();
	private Handler uiHandler;
	private Overlay overlay;
	private FpsCounter fpsCounter;
	private static int scaleType;
	private static int screenGravity;

	public Canvas() {
		if (graphicsMode == 1) {
			renderer = new GLRenderer();
		}
		if (parallelRedraw) {
			uiHandler = new Handler(Looper.getMainLooper(), msg -> repaintScreen());
		}
		displayWidth = ContextHolder.getDisplayWidth();
		displayHeight = ContextHolder.getDisplayHeight();
		updateSize();
	}

	public static void setShaderFilter(ShaderInfo shader) {
		Canvas.shaderFilter = shader;
	}

	public static void setScale(int screenGravity, int scaleType, int scaleRatio) {
		Canvas.screenGravity = screenGravity;
		Canvas.scaleType = scaleType;
		Canvas.scaleRatio = scaleRatio;
	}

	public static void setBackgroundColor(int color) {
		backgroundColor = color | 0xFF000000;
	}

	public static void setFilterBitmap(boolean filter) {
		Canvas.filter = filter;
	}

	public static void setHasTouchInput(boolean touchInput) {
		Canvas.touchInput = touchInput;
	}

	public static void setGraphicsMode(int mode, boolean parallel) {
		Canvas.graphicsMode = mode;
		Canvas.parallelRedraw = (mode == 0 || mode == 3) && parallel;
	}

	public static void setForceFullscreen(boolean forceFullscreen) {
		Canvas.forceFullscreen = forceFullscreen;
	}

	public static void setShowFps(boolean showFps) {
		Canvas.showFps = showFps;
	}

	public static void setLimitFps(int fpsLimit) {
		Canvas.fpsLimit = fpsLimit;
	}

	public int getKeyCode(int gameAction) {
		int res = KeyMapper.getKeyCode(gameAction);
		if (res != Integer.MAX_VALUE) {
			return res;
		} else {
			throw new IllegalArgumentException("unknown game action " + gameAction);
		}
	}

	public int getGameAction(int keyCode) {
		int res = KeyMapper.getGameAction(keyCode);
		if (res != Integer.MAX_VALUE) {
			return res;
		} else {
			throw new IllegalArgumentException("unknown keycode " + keyCode);
		}
	}

	public String getKeyName(int keyCode) {
		String res = KeyMapper.getKeyName(keyCode);
		if (res != null) {
			return res;
		} else {
			throw new IllegalArgumentException("unknown keycode " + keyCode);
		}
	}

	public void postKeyPressed(int keyCode) {
		Display.postEvent(CanvasEvent.getInstance(this, CanvasEvent.KEY_PRESSED, KeyMapper.convertKeyCode(keyCode)));
	}

	public void postKeyReleased(int keyCode) {
		Display.postEvent(CanvasEvent.getInstance(this, CanvasEvent.KEY_RELEASED, KeyMapper.convertKeyCode(keyCode)));
	}

	public void postKeyRepeated(int keyCode) {
		Display.postEvent(CanvasEvent.getInstance(this, CanvasEvent.KEY_REPEATED, KeyMapper.convertKeyCode(keyCode)));
	}

	public void callShowNotify() {
		visible = true;
		showNotify();
	}

	public void callHideNotify() {
		hideNotify();
		visible = false;
	}

	public void onDraw(android.graphics.Canvas canvas) {
		if (graphicsMode != 2) return; // Fix for Android Pie
		CanvasWrapper g = canvasWrapper;
		g.bind(canvas);
		g.clear(backgroundColor);
		offscreenCopy.getBitmap().prepareToDraw();
		g.drawImage(offscreenCopy, virtualScreen);
		if (fpsCounter != null) {
			fpsCounter.increment();
		}
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

	public Bitmap getScreenShot() {
		Bitmap bitmap = Bitmap.createBitmap(onWidth, onHeight, Bitmap.Config.ARGB_8888);
		canvasWrapper.bind(new android.graphics.Canvas(bitmap));
		RectF screen = new RectF(0, 0, onWidth, onHeight);
		canvasWrapper.drawImage(offscreenCopy, screen);
		return bitmap;
	}

	private boolean checkSizeChanged() {
		int tmpWidth = width;
		int tmpHeight = height;
		updateSize();
		return width != tmpWidth || height != tmpHeight;
	}

	/**
	 * Update the size and position of the virtual screen relative to the real one.
	 */
	public void updateSize() {
		/*
		 * We turn the sizes of the virtual screen into the sizes of the visible canvas.
		 *
		 * At the same time, we take into account that one or both virtual sizes can be less
		 * than zero, which means auto-selection of this size so that the resulting canvas
		 * has the same aspect ratio as the actual screen of the device.
		 */
		int scaledDisplayHeight;
		VirtualKeyboard vk = ContextHolder.getVk();
		boolean isPhoneSkin = vk != null && vk.isPhone();

		// if phone keyboard layout is active, then scale down the virtual screen
		if (isPhoneSkin) {
			scaledDisplayHeight = (int) (displayHeight - vk.getPhoneKeyboardHeight() - 1);
		} else {
			scaledDisplayHeight = displayHeight;
		}
		if (virtualWidth > 0) {
			if (virtualHeight > 0) {
				/*
				 * the width and height of the canvas are strictly set
				 */
				width = virtualWidth;
				height = virtualHeight;
			} else {
				/*
				 * only the canvas width is set
				 * height is selected by the ratio of the real screen
				 */
				width = virtualWidth;
				height = scaledDisplayHeight * virtualWidth / displayWidth;
			}
		} else {
			if (virtualHeight > 0) {
				/*
				 * only the canvas height is set
				 * width is selected by the ratio of the real screen
				 */
				width = displayWidth * virtualHeight / scaledDisplayHeight;
				height = virtualHeight;
			} else {
				/*
				 * nothing is set - screen-sized canvas
				 */
				width = displayWidth;
				height = scaledDisplayHeight;
			}
		}

		/*
		 * calculate the maximum height
		 */
		maxHeight = height;
		/*
		 * calculate the current height
		 */
		if (!fullscreen) {
			height = (int) (height * FULLSCREEN_HEIGHT_RATIO);
		}

		/*
		 * We turn the size of the canvas into the size of the image
		 * that will be displayed on the screen of the device.
		 */
		switch (scaleType) {
			case 0:
				// without scaling
				onWidth = width;
				onHeight = height;
				break;
			case 1:
				// try to fit in width
				onWidth = displayWidth;
				onHeight = height * displayWidth / width;
				if (onHeight > scaledDisplayHeight) {
					// if height is too big, then fit in height
					onHeight = scaledDisplayHeight;
					onWidth = width * scaledDisplayHeight / height;
				}
				break;
			case 2:
				// scaling without preserving the aspect ratio:
				// just stretch the picture to full screen
				onWidth = displayWidth;
				onHeight = scaledDisplayHeight;
				break;
		}

		onWidth = onWidth * scaleRatio / 100;
		onHeight = onHeight * scaleRatio / 100;

		int screenGravity = isPhoneSkin ? 1 : Canvas.screenGravity;
		switch (screenGravity) {
			case 0: // left
				onX = 0;
				onY = (displayHeight - onHeight) / 2;
				break;
			case 1: // top
				onX = (displayWidth - onWidth) / 2;
				onY = 0;
				break;
			case 2: // center
				onX = (displayWidth - onWidth) / 2;
				onY = (displayHeight - onHeight) / 2;
				break;
			case 3: // right
				onX = displayWidth - onWidth;
				onY = (displayHeight - onHeight) / 2;
				break;
			case 4: // bottom
				onX = (displayWidth - onWidth) / 2;
				onY = displayHeight - onHeight;
				break;
		}

		RectF screen = new RectF(0, 0, displayWidth, displayHeight);
		virtualScreen.set(onX, onY, onX + onWidth, onY + onHeight);

		if (offscreen == null) {
			offscreen = Image.createTransparentImage(width, maxHeight);
			offscreenCopy = Image.createTransparentImage(width, maxHeight);
		}
		if (offscreen.getWidth() != width || offscreen.getHeight() != height) {
			offscreen.setSize(width, height);
			offscreenCopy.setSize(width, height);
		}
		offscreen.getSingleGraphics().reset();
		offscreenCopy.getSingleGraphics().reset();
		if (overlay != null) {
			overlay.resize(screen, virtualScreen);
		}

		if (graphicsMode == 1) {
			float gl = 2.0f * virtualScreen.left / displayWidth - 1.0f;
			float gt = 1.0f - 2.0f * virtualScreen.top / displayHeight;
			float gr = 2.0f * virtualScreen.right / displayWidth - 1.0f;
			float gb = 1.0f - 2.0f * virtualScreen.bottom / displayHeight;
			float th = (float) height / offscreen.getBitmap().getHeight();
			float tw = (float) width / offscreen.getBitmap().getWidth();
			renderer.updateSize(gl, gt, gr, gb, th, tw);
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

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public View getDisplayableView() {
		if (layout == null) {
			layout = (LinearLayout) super.getDisplayableView();
			MicroActivity activity = getParentActivity();
			if (graphicsMode == 1) {
				GlesView glesView = new GlesView(activity);
				glesView.setRenderer(renderer);
				glesView.setRenderMode(RENDERMODE_WHEN_DIRTY);
				renderer.setView(glesView);
				innerView = glesView;
			} else {
				CanvasView canvasView = new CanvasView(this, activity);
				if (graphicsMode == 2) {
					canvasView.setWillNotDraw(false);
				}
				canvasView.getHolder().setFormat(PixelFormat.RGBA_8888);
				innerView = canvasView;
			}
			ViewCallbacks callback = new ViewCallbacks(innerView);
			innerView.getHolder().addCallback(callback);
			innerView.setOnTouchListener(callback);
			innerView.setOnKeyListener(callback);
			innerView.setFocusableInTouchMode(true);
			layout.addView(innerView);
		}
		return layout;
	}

	@Override
	public void clearDisplayableView() {
		synchronized (paintSync) {
			super.clearDisplayableView();
			layout = null;
			innerView = null;
		}
	}

	public void setFullScreenMode(boolean flag) {
		synchronized (paintSync) {
			if (fullscreen != flag) {
				fullscreen = flag;
				updateSize();
				Display.postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.SIZE_CHANGED,
						width, height));
			}
		}
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

	protected abstract void paint(Graphics g);

	public final void repaint() {
		repaint(0, 0, width, height);
	}

	public final void repaint(int x, int y, int width, int height) {
		limitFps();
		Display.postEvent(paintEvent);
	}

	// GameCanvas
	public void flushBuffer(Image image, int x, int y, int width, int height) {
		limitFps();
		synchronized (paintSync) {
			offscreenCopy.getSingleGraphics().flush(image, x, y, width, height);
			if (graphicsMode == 1) {
				if (innerView != null) {
					renderer.requestRender();
				}
				return;
			} else if (graphicsMode == 2) {
				if (innerView != null) {
					innerView.postInvalidate();
				}
				return;
			}
			if (!parallelRedraw) {
				repaintScreen();
			} else if (!uiHandler.hasMessages(0)) {
				uiHandler.sendEmptyMessage(0);
			}
		}
	}

	// ExtendedImage
	public void flushBuffer(Image image, int x, int y) {
		limitFps();
		synchronized (paintSync) {
			image.copyTo(offscreenCopy, x, y);
			if (graphicsMode == 1) {
				if (innerView != null) {
					renderer.requestRender();
				}
				return;
			} else if (graphicsMode == 2) {
				if (innerView != null) {
					innerView.postInvalidate();
				}
				return;
			}
			if (!parallelRedraw) {
				repaintScreen();
			} else if (!uiHandler.hasMessages(0)) {
				uiHandler.sendEmptyMessage(0);
			}
		}
	}

	private void limitFps() {
		if (fpsLimit <= 0) return;
		try {
			long millis = (1000 / fpsLimit) - (System.currentTimeMillis() - lastFrameTime);
			if (millis > 0) Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		lastFrameTime = System.currentTimeMillis();
	}

	@SuppressLint("NewApi")
	private boolean repaintScreen() {
		if (surface == null || !surface.isValid()) {
			return true;
		}
		try {
			android.graphics.Canvas canvas = graphicsMode == 3 ?
					surface.lockHardwareCanvas() : surface.lockCanvas(null);
			if (canvas == null) {
				return true;
			}
			CanvasWrapper g = this.canvasWrapper;
			g.bind(canvas);
			g.clear(backgroundColor);
			g.drawImage(offscreenCopy, virtualScreen);
			surface.unlockCanvasAndPost(canvas);
			if (fpsCounter != null) {
				fpsCounter.increment();
			}
			if (parallelRedraw) uiHandler.removeMessages(0);
		} catch (Exception e) {
			Log.w(TAG, "repaintScreen: " + e);
		}
		return true;
	}

	/**
	 * After calling this method, an immediate redraw is guaranteed to occur,
	 * and the calling thread is blocked until it is completed.
	 */
	public final void serviceRepaints() {
		EventQueue queue = Display.getEventQueue();

		/*
		 * blocking order:
		 *
		 * 1 - queue.this
		 * 2 - queue.queue
		 *
		 * accordingly, inside the EventQueue, the order must be the same,
		 * otherwise mutual blocking of two threads is possible (everything will hang)
		 */

		//noinspection SynchronizationOnLocalVariableOrMethodParameter
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

				if (Thread.holdsLock(paintSync)) { // Avoid deadlock
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

	protected void showNotify() {
	}

	protected void hideNotify() {
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

	private class GLRenderer implements GLSurfaceView.Renderer {
		private final FloatBuffer vbo = ByteBuffer.allocateDirect(8 * 2 * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		private GLSurfaceView mView;
		private final int[] bgTextureId = new int[1];
		private ShaderProgram program;
		private boolean isStarted;

		@Override
		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			program = new ShaderProgram(shaderFilter);
			int c = Canvas.backgroundColor;
			glClearColor((c >> 16 & 0xff) / 255.0f, (c >> 8 & 0xff) / 255.0f, (c & 0xff) / 255.0f, 1.0f);
			glDisable(GL_BLEND);
			glDisable(GL_DEPTH_TEST);
			glDepthMask(false);
			initTex();
			Bitmap bitmap = offscreenCopy.getBitmap();
			program.loadVbo(vbo, bitmap.getWidth(), bitmap.getHeight());
			if (shaderFilter != null && shaderFilter.values != null) {
				glUniform4fv(program.uSetting, 1, shaderFilter.values, 0);
			}
			isStarted = true;
		}

		@Override
		public void onSurfaceChanged(GL10 gl, int width, int height) {
			glViewport(0, 0, width, height);
			glUniform2f(program.uPixelDelta, 1.0f / width, 1.0f / height);
		}

		@Override
		public void onDrawFrame(GL10 gl) {
			glClear(GL_COLOR_BUFFER_BIT);
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, offscreenCopy.getBitmap(), 0);
			glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
			if (fpsCounter != null) {
				fpsCounter.increment();
			}
		}

		private void initTex() {
			glGenTextures(1, bgTextureId, 0);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, bgTextureId[0]);
			glTexParameteri(GLES20.GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter ? GL_LINEAR : GL_NEAREST);
			glTexParameteri(GLES20.GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter ? GL_LINEAR : GL_NEAREST);
			glTexParameteri(GLES20.GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GLES20.GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

			// юнит текстуры
			glUniform1i(program.uTextureUnit, 0);
		}

		public void updateSize(float gl, float gt, float gr, float gb, float th, float tw) {
			FloatBuffer vertex_bg = vbo;
			vertex_bg.rewind();
			vertex_bg.put(gl).put(gt).put(0.0f).put(0.0f);// lt
			vertex_bg.put(gl).put(gb).put(0.0f).put(  th);// lb
			vertex_bg.put(gr).put(gt).put(  tw).put(0.0f);// rt
			vertex_bg.put(gr).put(gb).put(  tw).put(  th);// rb
			if (isStarted) {
				mView.queueEvent(() -> {
					Bitmap bitmap = offscreenCopy.getBitmap();
					program.loadVbo(vbo, bitmap.getWidth(), bitmap.getHeight());
				});
			}
		}

		public void requestRender() {
			mView.requestRender();
		}

		public void setView(GLSurfaceView mView) {
			this.mView = mView;
		}

		public void stop() {
			isStarted = false;
			mView.onPause();
		}

		public void start() {
			mView.onResume();
		}
	}

	private class PaintEvent extends Event implements EventFilter {

		private int enqueued = 0;

		@Override
		public void process() {
			synchronized (paintSync) {
				if (surface == null || !surface.isValid() || !visible) {
					return;
				}
				Graphics g = offscreen.getSingleGraphics();
				g.reset();
				try {
					paint(g);
				} catch (Throwable t) {
					t.printStackTrace();
				}
				offscreen.copyTo(offscreenCopy);
				if (graphicsMode == 1) {
					if (innerView != null) {
						renderer.requestRender();
					}
				} else if (graphicsMode == 2) {
					if (innerView != null) {
						innerView.postInvalidate();
					}
				} else if (!parallelRedraw) {
					repaintScreen();
				} else if (!uiHandler.hasMessages(0)) {
					uiHandler.sendEmptyMessage(0);
				}
			}
		}

		@Override
		public void recycle() {
		}

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
			return event != this;
		}

		@Override
		public boolean accept(Event event) {
			return event == this;
		}
	}

	private class ViewCallbacks implements View.OnTouchListener, SurfaceHolder.Callback, View.OnKeyListener {

		private final View mView;
		OverlayView overlayView;
		private final FrameLayout rootView;

		public ViewCallbacks(View view) {
			mView = view;
			rootView = ((Activity) view.getContext()).findViewById(R.id.midletFrame);
			overlayView = rootView.findViewById(R.id.vOverlay);
		}

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			switch (event.getAction()) {
				case KeyEvent.ACTION_DOWN:
					return onKeyDown(keyCode, event);
				case KeyEvent.ACTION_UP:
					return onKeyUp(keyCode, event);
			}
			return false;
		}

		public boolean onKeyDown(int keyCode, KeyEvent event) {
			keyCode = KeyMapper.convertAndroidKeyCode(keyCode);
			if (keyCode == Integer.MAX_VALUE) {
				return false;
			}
			if (event.getRepeatCount() == 0) {
				if (overlay == null || !overlay.keyPressed(keyCode)) {
					postKeyPressed(keyCode);
				}
			} else {
				if (overlay == null || !overlay.keyRepeated(keyCode)) {
					postKeyRepeated(keyCode);
				}
			}
			return true;
		}

		public boolean onKeyUp(int keyCode, KeyEvent event) {
			keyCode = KeyMapper.convertAndroidKeyCode(keyCode);
			if (keyCode == Integer.MAX_VALUE) {
				return false;
			}
			if (overlay == null || !overlay.keyReleased(keyCode)) {
				postKeyReleased(keyCode);
			}
			return true;
		}

		@Override
		@SuppressLint("ClickableViewAccessibility")
		public boolean onTouch(View v, MotionEvent event) {
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
						Display.postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.POINTER_PRESSED, id,
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
								Display.postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.POINTER_DRAGGED, id,
										convertPointerX(event.getHistoricalX(p, h)), convertPointerY(event.getHistoricalY(p, h))));
							}
						}
					}
					for (int p = 0; p < pointerCount; p++) {
						id = event.getPointerId(p);
						if ((overlay == null || !overlay.pointerDragged(id, event.getX(p), event.getY(p)))
								&& touchInput && id == 0) {
							Display.postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.POINTER_DRAGGED, id,
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
						Display.postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.POINTER_RELEASED, id,
								convertPointerX(event.getX()), convertPointerY(event.getY())));
					}
					break;
				case MotionEvent.ACTION_CANCEL:
					if (overlay != null) {
						overlay.cancel();
					}
					break;
				default:
					return false;
			}
			return true;
		}

		@Override
		public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int newWidth, int newHeight) {
			Rect offsetViewBounds = new Rect(0, 0, newWidth, newHeight);
			// calculates the relative coordinates to the parent
			rootView.offsetDescendantRectToMyCoords(mView, offsetViewBounds);
			synchronized (paintSync) {
				overlayView.setTargetBounds(offsetViewBounds);
				displayWidth = newWidth;
				displayHeight = newHeight;
				if (checkSizeChanged() || !sizeChangedCalled) {
					Display.postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.SIZE_CHANGED,
							width, height));
					sizeChangedCalled = true;
				}
			}
			Display.postEvent(paintEvent);
		}

		@Override
		public void surfaceCreated(@NonNull SurfaceHolder holder) {
			if (renderer != null) {
				renderer.start();
			}
			synchronized (paintSync) {
				surface = holder.getSurface();
				Display.postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.SHOW_NOTIFY));
			}
			if (showFps) {
				fpsCounter = new FpsCounter(overlayView);
				overlayView.addLayer(fpsCounter);
			}
			overlayView.setVisibility(true);
		}

		@Override
		public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
			if (renderer != null) {
				renderer.stop();
			}
			synchronized (paintSync) {
				surface = null;
				Display.postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.HIDE_NOTIFY));
				if (fpsCounter != null) {
					fpsCounter.stop();
					overlayView.removeLayer(fpsCounter);
					fpsCounter = null;
				}
			}
			overlayView.setVisibility(false);
		}

	}
}
