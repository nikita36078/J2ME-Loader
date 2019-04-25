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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.LruCache;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.game.Sprite;
import javax.microedition.util.ContextHolder;

public class Image {

	private static final int CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() >> 2); // 1/4 heap max
	private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(CACHE_SIZE) {
		@Override
		protected int sizeOf(String key, Bitmap value) {
			return value.getByteCount();
		}
	};

	private Bitmap bitmap;
	private Canvas canvas;

	public Image(Bitmap bitmap) {
		if (bitmap == null) {
			throw new NullPointerException();
		}

		this.bitmap = bitmap;
	}

	public static Image createImage(int width, int height, boolean hasAlpha, Image reuse) {
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bitmap.setHasAlpha(hasAlpha);
		if (reuse == null) {
			return new Image(bitmap);
		}
		reuse.getCanvas().setBitmap(bitmap);
		reuse.copyPixels(reuse);
		reuse.bitmap = bitmap;
		return new Image(bitmap);
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public Canvas getCanvas() {
		if (canvas == null) {
			canvas = new Canvas(bitmap);
		}

		return canvas;
	}

	public static Image createImage(int width, int height) {
		return new Image(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888));
	}

	public static Image createImage(String resname) throws IOException {
		synchronized (CACHE) {
			Bitmap b = CACHE.get(resname);
			if (b != null) {
				return new Image(b);
			}
			InputStream stream = ContextHolder.getResourceAsStream(null, resname);
			if (stream == null) {
				throw new IOException("Can't read image: " + resname);
			}
			b = BitmapFactory.decodeStream(stream);
			if (b == null) {
				throw new IOException("Can't decode image: " + resname);
			}
			CACHE.put(resname, b);
			return new Image(b);
		}
	}

	public static Image createImage(InputStream stream) {
		return new Image(BitmapFactory.decodeStream(stream));
	}

	public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
		return new Image(BitmapFactory.decodeByteArray(imageData, imageOffset, imageLength));
	}

	public static Image createImage(Image image, int x, int y, int width, int height, int transform) {
		return new Image(Bitmap.createBitmap(image.bitmap, x, y, width, height, Sprite.transformMatrix(transform, width / 2f, height / 2f), false));
	}

	public static Image createImage(Image image) {
		return new Image(Bitmap.createBitmap(image.bitmap));
	}

	public static Image createRGBImage(int[] rgb, int width, int height, boolean processAlpha) {
		if (!processAlpha) {
			final int length = width * height;
			for (int i = 0; i < length; i++) {
				rgb[i] |= 0xFF << 24;
			}
		}
		return new Image(Bitmap.createBitmap(rgb, width, height, Bitmap.Config.ARGB_8888));
	}

	public Graphics getGraphics() {
		Graphics graphics = new Graphics();
		graphics.setCanvas(new Canvas(bitmap), bitmap);
		return graphics;
	}

	public boolean isMutable() {
		return bitmap.isMutable();
	}

	public int getWidth() {
		return bitmap.getWidth();
	}

	public int getHeight() {
		return bitmap.getHeight();
	}

	public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) {
		bitmap.getPixels(rgbData, offset, scanlength, x, y, width, height);
	}

	void copyPixels(Image dst) {
		dst.getCanvas().drawBitmap(bitmap, 0, 0, null);
	}
}
