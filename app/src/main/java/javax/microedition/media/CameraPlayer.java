/*
 * Copyright 2022 Nikita Shakarun
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

package javax.microedition.media;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;

import androidx.annotation.RequiresApi;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.VideoItem;
import javax.microedition.lcdui.graphics.CanvasWrapper;
import javax.microedition.lcdui.overlay.Layer;
import javax.microedition.lcdui.overlay.OverlayView;
import javax.microedition.media.control.GUIControl;
import javax.microedition.media.control.VideoControl;
import javax.microedition.shell.MicroActivity;
import javax.microedition.util.ContextHolder;

public class CameraPlayer extends BasePlayer implements VideoControl, Layer {

	private final CameraController controller;
	private final CameraController.FrameListener frameListener = this::onVideoFrame;
	private final Rect srcRect = new Rect();
	private final RectF dstRect = new RectF();
	private final RectF virtualScreen = new RectF();

	private OverlayView overlayView;
	private Canvas canvas;
	private Bitmap videoFrame;
	private boolean sizeSet;
	private boolean visible = true;
	private boolean fullScreen;
	private int displayX;
	private int displayY;
	private int displayWidth;
	private int displayHeight;

	public CameraPlayer() {
		controller = new CameraController();

		addControl(VideoControl.class.getName(), this);
	}

	@Override
	public Object initDisplayMode(int mode, Object arg) {
		if (mode == GUIControl.USE_GUI_PRIMITIVE) {
			detachDirectVideo();
			return new VideoItem(controller);
		}
		if (mode == VideoControl.USE_DIRECT_VIDEO && arg instanceof Canvas) {
			detachDirectVideo();
			canvas = (Canvas) arg;
			displayX = 0;
			displayY = 0;
			sizeSet = false;
			attachDirectVideo();
			controller.setUp(frameListener);
		}
		return null;
	}

	@Override
	public void setDisplayLocation(int x, int y) {
		displayX = x;
		displayY = y;
	}

	@Override
	public void setDisplaySize(int width, int height) throws MediaException {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("width and height must be > 0");
		}
		displayWidth = width;
		displayHeight = height;
		sizeSet = true;
	}

	@Override
	public void setDisplayFullScreen(boolean fullScreenMode) throws MediaException {
		fullScreen = fullScreenMode;
	}

	@Override
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	@Override
	public int getSourceWidth() {
		return controller.getSourceWidth();
	}

	@Override
	public int getSourceHeight() {
		return controller.getSourceHeight();
	}

	@Override
	public int getDisplayX() {
		return fullScreen ? 0 : displayX;
	}

	@Override
	public int getDisplayY() {
		return fullScreen ? 0 : displayY;
	}

	@Override
	public int getDisplayWidth() {
		Canvas canvas = this.canvas;
		if (canvas != null && (fullScreen || !sizeSet)) {
			return canvas.getWidth();
		}
		return displayWidth;
	}

	@Override
	public int getDisplayHeight() {
		Canvas canvas = this.canvas;
		if (canvas != null && (fullScreen || !sizeSet)) {
			return canvas.getHeight();
		}
		return displayHeight;
	}

	@Override
	public byte[] getSnapshot(String imageType) throws MediaException {
		return controller.getSnapshot(imageType);
	}

	@Override
	public void doClose() {
		detachDirectVideo();
		controller.tearDown();
	}

	// Layer

	@Override
	public void paint(CanvasWrapper g) {
		Bitmap frame = videoFrame;
		Canvas canvas = this.canvas;
		if (frame == null || canvas == null || !visible || !canvas.isShown()) {
			return;
		}
		int canvasWidth = canvas.getWidth();
		int canvasHeight = canvas.getHeight();
		if (canvasWidth <= 0 || canvasHeight <= 0) {
			return;
		}
		boolean fill = fullScreen || !sizeSet;
		int left = Math.max(getDisplayX(), 0);
		int top = Math.max(getDisplayY(), 0);
		int right = Math.min(left + (fill ? canvasWidth : displayWidth), canvasWidth);
		int bottom = Math.min(top + (fill ? canvasHeight : displayHeight), canvasHeight);
		if (left >= right || top >= bottom) {
			return;
		}
		// Crop the frame
		float scale = Math.max((right - left) / (float) frame.getWidth(),
				(bottom - top) / (float) frame.getHeight());
		int cropWidth = Math.round((right - left) / scale);
		int cropHeight = Math.round((bottom - top) / scale);
		int srcLeft = (frame.getWidth() - cropWidth) / 2;
		int srcTop = (frame.getHeight() - cropHeight) / 2;
		srcRect.set(srcLeft, srcTop, srcLeft + cropWidth, srcTop + cropHeight);

		canvas.getVirtualScreenBounds(virtualScreen);
		float scaleX = virtualScreen.width() / canvasWidth;
		float scaleY = virtualScreen.height() / canvasHeight;
		dstRect.set(virtualScreen.left + left * scaleX,
				virtualScreen.top + top * scaleY,
				virtualScreen.left + right * scaleX,
				virtualScreen.top + bottom * scaleY);
		g.drawBitmap(frame, srcRect, dstRect);
	}

	private void onVideoFrame(Bitmap frame) {
		videoFrame = frame;
		Canvas canvas = this.canvas;
		OverlayView view = overlayView;
		if (view != null && canvas != null && visible && canvas.isShown()) {
			view.postInvalidate();
		}
	}

	private void attachDirectVideo() {
		MicroActivity activity = ContextHolder.getActivity();
		if (activity == null) {
			return;
		}
		OverlayView view = activity.binding.overlayView;
		overlayView = view;
		view.post(() -> view.addLayer(this, 0));
	}

	private void detachDirectVideo() {
		OverlayView view = overlayView;
		if (view != null) {
			overlayView = null;
			view.post(() -> view.removeLayer(this));
		}
		canvas = null;
		videoFrame = null;
	}
}
