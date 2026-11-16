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

package ru.playsoftware.j2meloader.config;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import javax.microedition.lcdui.graphics.CanvasWrapper;
import javax.microedition.lcdui.keyboard.VirtualKeyboard;

@SuppressLint("ViewConstructor")
public class KeyboardLayoutView extends View {
	private final VirtualKeyboard vk;
	private final ProfileModel settings;
	private final RectF screen = new RectF();
	private final RectF virtualScreen = new RectF();
	private final Paint paint = new Paint();
	private final CanvasWrapper graphics = new CanvasWrapper(false);
	private Runnable modifiedListener;

	public KeyboardLayoutView(Context context, VirtualKeyboard vk, ProfileModel settings) {
		super(context);
		this.vk = vk;
		this.settings = settings;
		vk.setView(this);
	}

	public void setOnModifiedListener(Runnable listener) {
		modifiedListener = listener;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		updateScreen(w, h);
	}

	private void updateScreen(int w, int h) {
		screen.set(0, 0, w, h);
		computeVirtualScreen(w, h);
		vk.resize(screen, virtualScreen.left, virtualScreen.top,
				virtualScreen.right, virtualScreen.bottom);
	}

	public void refresh() {
		if (getWidth() > 0 && getHeight() > 0) {
			updateScreen(getWidth(), getHeight());
		}
		invalidate();
	}

	private void computeVirtualScreen(int w, int h) {
		int virtualWidth = settings.screenWidth;
		int virtualHeight = settings.screenHeight;
		if (virtualWidth <= 0 || virtualHeight <= 0) {
			virtualScreen.set(0, 0, w, h);
			return;
		}

		float width = virtualWidth;
		float height = virtualHeight;
		if (settings.screenScaleType != 0) {
			float scaleX = w / width;
			float scaleY = h / height;
			float scale = settings.screenScaleType == 2 ? 1 : Math.min(scaleX, scaleY);
			width = settings.screenScaleType == 2 ? w : width * scale;
			height = settings.screenScaleType == 2 ? h : height * scale;
		}

		float ratio = settings.screenScaleRatio <= 0 || settings.screenScaleRatio > 100
				? 1 : settings.screenScaleRatio / 100f;
		width *= ratio;
		height *= ratio;

		float left;
		float top;
		switch (settings.screenGravity) {
			case 0: // left
				left = 0;
				top = (h - height) / 2;
				break;
			case 2: // center
				left = (w - width) / 2;
				top = (h - height) / 2;
				break;
			case 3: // right
				left = w - width;
				top = (h - height) / 2;
				break;
			case 4: // bottom
				left = (w - width) / 2;
				top = h - height;
				break;
			case 1: // top
			default:
				left = (w - width) / 2;
				top = 0;
				break;
		}
		virtualScreen.set(left, top, left + width, top + height);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(0xFF000000 | settings.screenBackgroundColor);

		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.BLACK);
		canvas.drawRect(virtualScreen, paint);

		graphics.bind(canvas);
		vk.paint(graphics);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int index = event.getActionIndex();
		int pointer = event.getPointerId(index);
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				vk.pointerPressed(pointer, event.getX(index), event.getY(index));
				break;
			case MotionEvent.ACTION_MOVE:
				for (int i = 0; i < event.getPointerCount(); i++) {
					vk.pointerDragged(event.getPointerId(i), event.getX(i), event.getY(i));
				}
				if (modifiedListener != null) {
					modifiedListener.run();
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				vk.pointerReleased(pointer, event.getX(index), event.getY(index));
				break;
			case MotionEvent.ACTION_CANCEL:
				vk.cancel();
				break;
		}
		invalidate();
		return true;
	}
}
