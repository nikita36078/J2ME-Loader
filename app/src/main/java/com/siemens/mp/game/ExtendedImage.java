/*
 *  Siemens API for MicroEmulator
 *  Copyright (C) 2003 Markus Heberling <markus@heberling.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 */

package com.siemens.mp.game;

import android.graphics.Color;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class ExtendedImage extends com.siemens.mp.misc.NativeMem {
	private final boolean hasAlpha;
	private Image image;

	public ExtendedImage(Image image) {
		if (image == null || image.getWidth() % 8 != 0) {
			throw new IllegalArgumentException("ExtendedImage: width is not divisible by 8");
		}
		this.image = image;
		hasAlpha = image.isBlackWhiteAlpha();
	}

	public void blitToScreen(int x, int y) {
		Displayable current = Display.getDisplay(null).getCurrent();
		if (current instanceof Canvas) {
			((Canvas) current).flushBuffer(image, x, y);
		}
	}

	public void clear(byte color) {
		int c = (color == 0) ? 0x00FFFFFF : 0x0;
		Graphics g = image.getGraphics();
		g.setColor(c);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
	}

	public Image getImage() {
		return image;
	}

	public int getPixel(int x, int y) {
		final int pixel = image.getBitmap().getPixel(x, y);
		if (hasAlpha) {
			if ((pixel & 0xFF000000) != 0xFF000000) return 0;
			return ((pixel & 0xFFFFFF) == 0xFFFFFF) ? 1 : 2;
		}
		return ((pixel & 0xFFFFFF) == 0xFFFFFF) ? 0 : 1;
	}

	public void getPixelBytes(byte[] pixels, int x, int y, int width, int height) {
		int[] colors = new int[width * height];
		image.getBitmap().getPixels(colors, 0, width, x, y, width, height);
		if (hasAlpha) {
			final int dataLen = colors.length / 4;
			for (int i = 0, k = 0; i < dataLen; i++) {
				int data = 0;
				for (int j = 0; j < 4; j++) {
					data <<= 2;
					int color = colors[k++];
					if ((color & 0xFF000000) != 0xFF000000) continue;
					if ((color & 0xFFFFFF) == 0xFFFFFF) data |= 1;
					else data |= 2;
				}
				pixels[i] = (byte) data;
			}
		} else {
			final int dataLen = colors.length / 8;
			for (int i = 0, k = 0; i < dataLen; i++) {
				int data = 0;
				for (int j = 0; j < 8; j++) {
					data <<= 1;
					if ((colors[k++] & 0xFFFFFF) != 0xFFFFFF) {
						data |= 1;
					}
				}
				pixels[i] = (byte) data;
			}
		}
	}

	public void setPixel(int x, int y, byte color) {
		if (!hasAlpha) {
			image.getBitmap().setPixel(x, y, color == 1 ? Color.BLACK : Color.WHITE);
			return;
		}
		if (color == 0) {
			image.getBitmap().setPixel(x, y, 0);
		} else {
			image.getBitmap().setPixel(x, y, color == 1 ? Color.WHITE : Color.BLACK);
		}
	}

	public void setPixels(byte[] pixels, int x, int y, int width, int height) {
		int[] colors = new int[width * height];
		if (hasAlpha) {
			final int dataLen = colors.length / 4;
			for (int i = 0, k = 0; i < dataLen; i++) {
				final int data = pixels[i];
				for (int j = 3; j >= 0; j--) {
					int color = (data >> j) & 0b11;
					if (color == 0) {
						colors[k++] = 0;
					} else {
						colors[k++] = color == 1 ? Color.WHITE : Color.BLACK;
					}
				}
			}
		} else {
			final int dataLen = colors.length / 8;
			for (int i = 0, k = 0; i < dataLen; i++) {
				final int data = pixels[i];
				for (int j = 7; j >= 0; j--) {
					final int color = (data >> j) & 1;
					colors[k++] = color == 1 ? Color.BLACK : Color.WHITE;
				}
			}
		}
		image.getBitmap().setPixels(colors, 0, width, x, y, width, height);
	}
}
