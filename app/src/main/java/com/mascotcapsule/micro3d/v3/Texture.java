/*
 * Copyright 2020 Yury Kharchenko
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

package com.mascotcapsule.micro3d.v3;

import static android.opengl.GLES20.*;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import javax.microedition.shell.AppClassLoader;

@SuppressWarnings("unused, WeakerAccess")
public class Texture {
	private static final int BMP_FILE_HEADER_SIZE = 14;
	private static final int BMP_VERSION_3 = 40;
	private static final int BMP_VERSION_CORE = 12;
	private static int sLastId;

	private final FloatBuffer colorKey =
			ByteBuffer.allocateDirect(3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

	boolean isSphere;

	private Bitmap image;
	private int mTexId = -2;
	int width;
	int height;

	public Texture(byte[] b, boolean isForModel) {
		if (b == null) {
			throw new NullPointerException();
		}
		isSphere = !isForModel;
		prepare(b);
		image = BitmapFactory.decodeByteArray(b, 0, b.length);
		if (image == null) {
			fix(b);
			image = BitmapFactory.decodeByteArray(b, 0, b.length);
		}
		if (image == null) {
			throw new RuntimeException("Image data error");
		}
		width = image.getWidth();
		height = image.getHeight();
	}

	public Texture(String name, boolean isForModel) throws IOException {
		this(getData(name), isForModel);
	}

	public final void dispose() {
//		synchronized (Render.getRender()) {
//			Render.getRender().bindEglContext();
//			if (glIsTexture(mTexId)) {
//				glDeleteTextures(1, new int[]{mTexId}, 0);
//				mTexId = -1;
//			}
//			Render.getRender().releaseEglContext();
//		}
	}

	int getId() {
		if (mTexId == -1) throw new IllegalStateException("Already disposed!!!");
		if (glIsTexture(mTexId)) {
			return mTexId;
		}
		mTexId = loadTexture(image);
		return mTexId;
	}

	private synchronized static int loadTexture(Bitmap bitmap) {
		final int[] textureIds = new int[1];
		synchronized (Texture.class) {
			while (textureIds[0] <= sLastId) {
				glGenTextures(1, textureIds, 0);
			}
		}
		if (textureIds[0] == 0) {
			Render.checkGlError("glGenTextures");
			return 0;
		}

		if (bitmap == null) {
			glDeleteTextures(1, textureIds, 0);
			return 0;
		}

		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, textureIds[0]);

		boolean filter = Boolean.getBoolean("micro3d.v3.texture.filter");
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter ? GL_LINEAR : GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter ? GL_LINEAR : GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

		GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);

		glBindTexture(GL_TEXTURE_2D, 0);

		int textureId = textureIds[0];
		sLastId = textureId;
		return textureId;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			dispose();
		} finally {
			super.finalize();
		}
	}

	private static byte[] getData(String name) throws IOException {
		if (name == null) {
			throw new NullPointerException();
		}
		byte[] b = AppClassLoader.getResourceAsBytes(name);
		if (b == null) throw new IOException();
		return b;
	}

	private void prepare(byte[] bytes) {
		if (bytes[0] != 'B' || bytes[1] != 'M') {
			throw new RuntimeException("Not a BMP!");
		}
		int bInfoOffset = BMP_FILE_HEADER_SIZE;
		int bInfoSize = bytes[bInfoOffset++] & 0xFF | (bytes[bInfoOffset++] & 0xFF) << 8
				| (bytes[bInfoOffset++] & 0xFF) << 16 | (bytes[bInfoOffset] & 0xFF) << 24;

		if (bInfoSize < BMP_VERSION_CORE || bInfoSize > BMP_VERSION_3) {
			throw new RuntimeException("Unsupported BMP version = " + bInfoSize);
		}
		int bpp;
		int paletteSize;
		if (bInfoSize == BMP_VERSION_CORE) {
			bpp = bytes[24] | bytes[25] << 8;
			paletteSize = 256;
		} else {
			bpp = bytes[28] | bytes[29] << 8;
			paletteSize = bytes[0x2e] & 0xFF | (bytes[0x2f] & 0xFF) << 8
					| (bytes[0x30] & 0xFF) << 16 | (bytes[0x31] & 0xFF) << 24;
			if (paletteSize == 0) {
				paletteSize = 256;
			}
			int usedPaletteSize = bytes[0x32] & 0xFF | (bytes[0x33] & 0xFF) << 8
					| (bytes[0x34] & 0xFF) << 16 | (bytes[0x35] & 0xFF) << 24;
			if (usedPaletteSize > 0 && usedPaletteSize < paletteSize) {
				paletteSize = usedPaletteSize;
			}
		}
		if (bpp != 8) { // supports only 8-bit per pixel format
			throw new RuntimeException("Unsupported BMP format: bpp = " + bpp);
		}
		int paletteOffset = bInfoSize + BMP_FILE_HEADER_SIZE;
		// get first color in palette
		int b = bytes[paletteOffset++] & 0xff;
		int g = bytes[paletteOffset++] & 0xff;
		int r = bytes[paletteOffset++] & 0xff;
		paletteOffset++;

		int[] palette = new int[paletteSize - 1];
		for (int i = 0; i < palette.length; i++) {
			palette[i] = bytes[paletteOffset++] & 0xFF | (bytes[paletteOffset++] & 0xFF) << 8
					| (bytes[paletteOffset++] & 0xFF) << 16;
			paletteOffset++;
		}
		Arrays.sort(palette);
		int color0 = b | g << 8 | r << 16;
		int color = color0;
		int m = 0;
		int s = 1;
		while (true) {
			int i = Arrays.binarySearch(palette, color);
			if (i < 0) {
				break;
			}

			switch (m) {
				case 0:
					m++;
					if (b + s <= 0xff) {
						color = color0 + s;
						break;
					}
				case 1:
					m++;
					if (b - s >= 0) {
						color = color0 - s;
						break;
					}
				case 2:
					m++;
					if (g + s <= 0xff) {
						color = color0 + (s << 8);
						break;
					}
				case 3:
					m++;
					if (g - s >= 0) {
						color = color0 - (s << 8);
						break;
					}
				case 4:
					m++;
					if (r + s <= 0xff) {
						color = color0 + (s << 16);
						break;
					}
				case 5:
					m = 0;
					if (r + s <= 0xff) {
						color = color0 + (s << 16);
					}
					s++;
			}
		}
		paletteOffset = bInfoSize + BMP_FILE_HEADER_SIZE;
		// get first color in palette
		bytes[paletteOffset++] = (byte) color;
		bytes[paletteOffset++] = (byte) (color >> 8);
		bytes[paletteOffset  ] = (byte) (color >> 16);
		colorKey.put((color >> 16 & 0xff) / 255.0f)
				.put((color >> 8 & 0xff) / 255.0f)
				.put((color & 0xff) / 255.0f);
	}

	private boolean containsColor(byte[] bytes, int start, int len, byte b, byte g, byte r) {
		for (int i = start; i < start + len; ) {
			if (bytes[i++] != b) {
				i += 3;
			} else if (bytes[i++] != g) {
				i += 2;
			} else if (bytes[i++] != r) {
				i += 1;
			} else {
				return true;
			}
		}
		return false;
	}

	private void fix(byte[] b) {
		int bInfoOffset = BMP_FILE_HEADER_SIZE;
		int bInfoSize = b[bInfoOffset++] & 0xFF | (b[bInfoOffset++] & 0xFF) << 8
				| (b[bInfoOffset++] & 0xFF) << 16 | (b[bInfoOffset] & 0xFF) << 24;
		int paletteOffset = bInfoSize + BMP_FILE_HEADER_SIZE;
		// check pixel data offset field,
		int pixelDataOffset = (b[10] & 0xff) | (b[11] & 0xff) << 8 |
				(b[12] & 0xff) << 16 | (b[13] << 24);
		int pdo = paletteOffset + 256 * 4; // default offset for 8-bit BMP
		if (pixelDataOffset != pdo) {
			b[10] = (byte) ((pdo      ) & 0xff);
			b[11] = (byte) ((pdo >>  8) & 0xff);
			b[12] = (byte) ((pdo >> 16) & 0xff);
			b[13] = (byte) ((pdo >> 24) & 0xff);
		}
	}

	FloatBuffer getColorKey() {
		colorKey.rewind();
		return colorKey;
	}
}
