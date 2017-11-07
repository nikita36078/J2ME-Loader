/*
 *  Nokia API for MicroEmulator
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
 *
 *  Contributor(s):
 *    Bartek Teodorczyk <barteo@barteo.net>
 *    Nikita Shakarun
 */

package com.nokia.mid.ui;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

public class DirectGraphicsImp implements DirectGraphics {
	private Graphics graphics;
	private int alphaComponent;

	/**
	 * @param g
	 */
	public DirectGraphicsImp(Graphics g) {
		graphics = g;
	}

	/**
	 * @param img
	 * @param x
	 * @param y
	 * @param anchor
	 * @param manipulation ignored, since manipulations are not supported at the moment
	 */
	public void drawImage(Image img, int x, int y, int anchor, int manipulation) {
		if (img == null) {
			throw new NullPointerException();
		}
		int transform;
		switch (manipulation) {
			case FLIP_HORIZONTAL:
				transform = Sprite.TRANS_MIRROR_ROT180;
				break;
			case FLIP_VERTICAL:
				transform = Sprite.TRANS_MIRROR;
				break;
			case ROTATE_90:
				transform = Sprite.TRANS_ROT90;
				break;
			case ROTATE_180:
				transform = Sprite.TRANS_ROT180;
				break;
			case ROTATE_270:
				transform = Sprite.TRANS_ROT270;
				break;
			default:
				transform = -1;
		}
		if (anchor >= 64 || transform == -1) {
			throw new IllegalArgumentException();
		} else {
			graphics.drawRegion(
					img,
					x + graphics.getTranslateX(), y + graphics.getTranslateY(),
					img.getWidth(), img.getHeight(),
					transform,
					x + graphics.getTranslateX(), y + graphics.getTranslateY(), anchor);
			return;
		}
	}

	/**
	 * @param argb
	 */
	public void setARGBColor(int argb) {
		alphaComponent = (argb >> 24 & 0xff);
		graphics.setColorAlpha(argb);
	}

	/**
	 * @return
	 */
	public int getAlphaComponent() {
		return alphaComponent;
	}

	/**
	 * @return
	 */
	public int getNativePixelFormat() {
		return TYPE_BYTE_1_GRAY;
	}

	/**
	 * @param xPoints
	 * @param xOffset
	 * @param yPoints
	 * @param yOffset
	 * @param nPoints
	 * @param argbColor
	 */
	public void drawPolygon(int xPoints[], int xOffset, int yPoints[], int yOffset, int nPoints, int argbColor) {
		setARGBColor(argbColor);
		graphics.drawPolygon(xPoints, xOffset, yPoints, yOffset, nPoints);
	}

	/**
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @param x3
	 * @param y3
	 * @param argbColor
	 */
	public void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor) {
		drawPolygon(new int[]{x1, x2, x3}, 0, new int[]{y1, y2, y3}, 0, 3, argbColor);
	}

	/**
	 * @param xPoints
	 * @param xOffset
	 * @param yPoints
	 * @param yOffset
	 * @param nPoints
	 * @param argbColor
	 */
	public void fillPolygon(int xPoints[], int xOffset, int yPoints[], int yOffset, int nPoints, int argbColor) {
		setARGBColor(argbColor);
		graphics.fillPolygon(xPoints, xOffset, yPoints, yOffset, nPoints);
	}

	/**
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @param x3
	 * @param y3
	 * @param argbColor
	 */
	public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor) {
		fillPolygon(new int[]{x1, x2, x3}, 0, new int[]{y1, y2, y3}, 0, 3, argbColor);
	}

	//manipulations are not supported!

	/**
	 * @param pix
	 * @param alpha
	 * @param off
	 * @param scanlen
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param manipulation ignored, since manipulations are not supported at the moment
	 * @param format
	 */
	public void drawPixels(byte[] pix, byte[] alpha, int off, int scanlen, int x, int y, int width, int height, int manipulation, int format) {
		System.out.println("public void drawPixels(byte[] pix, byte[] alpha, int off, int scanlen, int x, int y, int width, int height, int manipulation, int format)");
		if (pix == null) {
			throw new NullPointerException();
		}
		if (width < 0 || height < 0) {
			throw new IllegalArgumentException();
		}

		Graphics g = graphics;
		int c;

		if (format == TYPE_BYTE_1_GRAY) {

			int b = 7;

			for (int yj = 0; yj < height; yj++) {
				int line = off + yj * scanlen;
				int ypos = yj * width;
				for (int xj = 0; xj < width; xj++) {
					c = doAlpha(pix, alpha, (line + xj) / 8, b);
					if ((c >> 24 & 0xff) != 0)//alpha
					{
						if (g.getColor() != c) g.setColor(c);
						g.drawLine(xj + x, yj + y, xj + x, yj + y);
					}
					b--;
					if (b < 0) b = 7;
				}
			}
		} else if (format == TYPE_BYTE_1_GRAY_VERTICAL) {
			int ods = off / scanlen;
			int oms = off % scanlen;
			int b = 0;
			for (int yj = 0; yj < height; yj++) {
				int ypos = yj * width;
				int tmp = (ods + yj) / 8 * scanlen + oms;
				for (int xj = 0; xj < width; xj++) {
					c = doAlpha(pix, alpha, tmp + xj, b);
					if (g.getColor() != c) g.setColor(c);
					if ((c >> 24 & 0xff) != 0) //alpha
						g.drawLine(xj + x, yj + y, xj + x, yj + y);
				}
				b++;
				if (b > 7) b = 0;
			}
		} else
			throw new IllegalArgumentException();
	}

	/**
	 * Only TYPE_USHORT_4444_ARGB format supported
	 *
	 * @param pix
	 * @param trans
	 * @param off
	 * @param scanlen
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param manipulation
	 * @param format
	 */
	public void drawPixels(short pix[], boolean trans, int off, int scanlen, int x, int y, int width, int height, int manipulation, int format) {
		if (format != TYPE_USHORT_4444_ARGB) {
			throw new IllegalArgumentException("Illegal format: " + format);
		}

		Graphics g = graphics;

		for (int iy = 0; iy < height; iy++) {
			for (int ix = 0; ix < width; ix++) {
				int c = toARGB(pix[off + ix + iy * scanlen], TYPE_USHORT_4444_ARGB);
				if (!isTransparent(c)) {
					g.setColor(c);
					g.drawLine(x + ix, y + iy, x + ix, y + iy);
				}
			}
		}
	}

	/**
	 * Not supported
	 *
	 * @param pix
	 * @param trans
	 * @param off
	 * @param scanlen
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param manipulation
	 * @param format
	 */
	public void drawPixels(int pix[], boolean trans, int off, int scanlen, int x, int y, int width, int height, int manipulation, int format) {
		System.out.println("public void drawPixels(int pix[], boolean trans, int off, int scanlen, int x, int y, int width, int height, int manipulation, int format)");
		throw new IllegalArgumentException();
	}

	/**
	 * Not supported
	 *
	 * @param pix
	 * @param alpha
	 * @param offset
	 * @param scanlen
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param format
	 */
	public void getPixels(byte pix[], byte alpha[], int offset, int scanlen, int x, int y, int width, int height, int format) {
		System.out.println("public void getPixels(byte pix[], byte alpha[], int offset, int scanlen, int x, int y, int width, int height, int format)");
		throw new IllegalArgumentException();
	}

	/**
	 * Only TYPE_USHORT_4444_ARGB format supported
	 *
	 * @param pix
	 * @param offset
	 * @param scanlen
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param format
	 */
	public void getPixels(short pix[], int offset, int scanlen, int x, int y, int width, int height, int format) {
		System.out.println("public void getPixels(short pix[], int offset, int scanlen, int x, int y, int width, int height, int format)");
		throw new IllegalArgumentException();
	}

	/**
	 * Not supported
	 *
	 * @param pix
	 * @param offset
	 * @param scanlen
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param format
	 */
	public void getPixels(int pix[], int offset, int scanlen, int x, int y, int width, int height, int format) {
		System.out.println("public void getPixels(int pix[], int offset, int scanlen, int x, int y, int width, int height, int format");
		throw new IllegalArgumentException();
	}

	private static int doAlpha(byte[] pix, byte[] alpha, int pos, int shift) {
		int p;
		int a;
		if (isBitSet(pix[pos], shift))
			p = 0;
		else
			p = 0x00FFFFFF;
		if (alpha == null || isBitSet(alpha[pos], shift))
			a = 0xFF000000;
		else
			a = 0;
		return p | a;
	}

	private static boolean isBitSet(byte b, int pos) {
		return ((b & (byte) (1 << pos)) != 0);
	}

	private static int toARGB(int s, int type) {
		switch (type) {
			case TYPE_USHORT_4444_ARGB: {
				int a = ((s) & 0xF000) >>> 12;
				int r = ((s) & 0x0F00) >>> 8;
				int g = ((s) & 0x00F0) >>> 4;
				int b = ((s) & 0x000F);

				s = ((a * 15) << 24) | ((r * 15) << 16) | ((g * 15) << 8) | (b * 15);
				break;
			}
			case TYPE_USHORT_444_RGB: {
				int r = ((s) & 0x0F00) >>> 8;
				int g = ((s) & 0x00F0) >>> 4;
				int b = ((s) & 0x000F);

				s = ((r * 15) << 16) | ((g * 15) << 8) | (b * 15);
				break;
			}
		}
		return s;
	}

	private static boolean isTransparent(int s) {
		return (s & 0xFF000000) == 0;
	}
}
