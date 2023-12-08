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

import android.graphics.Bitmap;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

public class DirectGraphicsImp implements DirectGraphics {
	private static final String TAG = DirectGraphicsImp.class.getName();
	private final Graphics graphics;
	private static final String KEY_FORMAT = "com.nokia.mid.ui.DirectGraphics.PIXEL_FORMAT";
	private static final int PIXEL_FORMAT = Integer.getInteger(KEY_FORMAT, TYPE_USHORT_565_RGB);

	private static final int[][] MANIPULATION2TRANSFORM = new int[][]{
			// rotate:                0,                        90,                        180,                        270
			{Sprite.TRANS_NONE         , Sprite.TRANS_ROT270       , Sprite.TRANS_ROT180       , Sprite.TRANS_ROT90        }, // flip none
			{Sprite.TRANS_MIRROR       , Sprite.TRANS_MIRROR_ROT90 , Sprite.TRANS_MIRROR_ROT180, Sprite.TRANS_MIRROR_ROT270}, // flip horizontal
			{Sprite.TRANS_MIRROR_ROT180, Sprite.TRANS_MIRROR_ROT270, Sprite.TRANS_MIRROR       , Sprite.TRANS_MIRROR_ROT90 }, // flip vertical
			{Sprite.TRANS_ROT180       , Sprite.TRANS_ROT90        , Sprite.TRANS_NONE         , Sprite.TRANS_ROT270       }, // flip both
	};

	private int alphaComponent;

	public DirectGraphicsImp(Graphics g) {
		graphics = g;
	}

	private static int getPixel(byte[] pixels, byte[] alpha, int idx, int shift) {
		int p = (pixels[idx] >> shift & 1 ^ 1) * 0x00FFFFFF;
		if (alpha == null) {
			return p;
		}
		return (alpha[idx] >> shift & 1) * 0xFF000000 | p;
	}

	private static int getTransformation(int manipulation) {
		int flip = manipulation >>> 13;
		if (flip > 3) {
			throw new IllegalArgumentException();
		}
		int rotation = (manipulation & 0x1FFF);
		int rotIdx = rotation / 90;
		if (rotation - rotIdx * 90 != 0 || rotIdx > 3) {
			throw new IllegalArgumentException();
		}
		return MANIPULATION2TRANSFORM[flip][rotIdx];
	}

	private static void setPixel(byte[] pixels, byte[] alpha, int idx, int shift, int color) {
		int a = color >>> 31;
		int r = color >> 16 & 0xff;
		int g = color >> 8 & 0xff;
		int b = color & 0xff;
		int pixel = (0x4CB2 * r + 0x9691 * g + 0x1D3E * b >> 23 ^ 1) & a;
		if (pixel == 1) {
			pixels[idx] |= 1 << shift;
		} else {
			pixels[idx] &= ~(1 << shift);
		}
		if (alpha != null) {
			if (a == 1) {
				alpha[idx] |= 1 << shift;
			} else {
				alpha[idx] &= ~(1 << shift);
			}
		}
	}

	@Override
	public void drawImage(Image img, int x, int y, int anchor, int manipulation) {
		if (img == null) {
			throw new NullPointerException();
		} else if ((anchor & -64) != 0) {
			throw new IllegalArgumentException();
		}
		int transform = getTransformation(manipulation);
		graphics.drawRegion(img, 0, 0, img.getWidth(), img.getHeight(), transform, x, y, anchor);
	}

	@Override
	public void drawPixels(byte[] pixels,
						   byte[] transparencyMask,
						   int offset,
						   int scanlength,
						   int x,
						   int y,
						   int width,
						   int height,
						   int manipulation,
						   int format) {
		if (pixels == null) {
			throw new NullPointerException();
		} else if (width < 0 || height < 0 || scanlength < width) {
			throw new IllegalArgumentException();
		} else if (offset < 0) {
			throw new ArrayIndexOutOfBoundsException();
		} else if (width == 0 || height == 0) {
			return;
		}

		int transform = getTransformation(manipulation);
		int[] colors = new int[height * width];

		switch (format) {
			case TYPE_BYTE_1_GRAY: {
				int space = scanlength - width;
				for (int yi = 0, di = 0; yi < height; yi++, offset += space) {
					for (int xi = 0; xi < width; xi++, offset++) {
						int shift = 7 - (offset & 7);
						colors[di++] = getPixel(pixels, transparencyMask, offset >> 3, shift);
					}
				}
				break;
			}
			case TYPE_BYTE_1_GRAY_VERTICAL: {
				int ods = offset / scanlength;
				int oms = offset % scanlength;
				int shift = ods & 7;
				for (int yi = 0, di = 0; yi < height; yi++) {
					int idx = ((ods + yi) >> 3) * scanlength + oms;
					for (int xi = 0; xi < width; xi++) {
						colors[di++] = getPixel(pixels, transparencyMask, idx++, shift);
					}
					shift = (shift + 1) & 7;
				}
				break;
			}
			case TYPE_BYTE_2_GRAY:
			case TYPE_BYTE_4_GRAY:
			case TYPE_BYTE_8_GRAY:
			case TYPE_BYTE_332_RGB:
				throw new IllegalArgumentException("Illegal format: " + format);
			default:
				throw new IllegalArgumentException("Unsupported format: " + format);
		}

		Image image = Image.createRGBImage(colors, width, height, transparencyMask != null);
		graphics.drawRegion(image, 0, 0, width, height, transform, x, y, 0);
	}

	@Override
	public void drawPixels(int[] pixels,
						   boolean transparency,
						   int offset,
						   int scanlength,
						   int x,
						   int y,
						   int width,
						   int height,
						   int manipulation,
						   int format) {
		if (pixels == null) {
			throw new NullPointerException();
		} else if (format != TYPE_INT_888_RGB && format != TYPE_INT_8888_ARGB) {
			throw new IllegalArgumentException("Illegal format: " + format);
		} else if (width < 0 || height < 0 || scanlength < width) {
			throw new IllegalArgumentException();
		} else if (offset < 0) {
			throw new ArrayIndexOutOfBoundsException();
		} else if (width == 0 || height == 0) {
			return;
		}

		int transform = getTransformation(manipulation);
		int[] colors = new int[height * width];

		int space = scanlength - width;
		for (int yi = 0, di = 0; yi < height; yi++, offset += space) {
			for (int xi = 0; xi < width; xi++) {
				colors[di++] = pixels[offset++];
			}
		}
		Image image = Image.createRGBImage(colors, width, height, format != TYPE_INT_888_RGB && transparency);
		graphics.drawRegion(image, 0, 0, width, height, transform, x, y, 0);
	}

	@Override
	public void drawPixels(short[] pixels,
						   boolean transparency,
						   int offset,
						   int scanlength,
						   int x,
						   int y,
						   int width,
						   int height,
						   int manipulation,
						   int format) {
		if (pixels == null) {
			throw new NullPointerException();
		} else if (width < 0 || height < 0 || scanlength < width) {
			throw new IllegalArgumentException();
		} else if (offset < 0) {
			throw new ArrayIndexOutOfBoundsException();
		} else if (width == 0 || height == 0) {
			return;
		}

		int transform = getTransformation(manipulation);
		int[] colors = new int[height * width];

		switch (format) {
			case TYPE_USHORT_4444_ARGB: {
				int space = scanlength - width;
				for (int yi = 0, di = 0; yi < height; yi++, offset += space) {
					for (int xi = 0; xi < width; xi++) {
						short s = pixels[offset++];
						int a = (s & 0xF000) << 12;
						int r = (s & 0x0F00) << 8;
						int g = (s & 0x00F0) << 4;
						int b = (s & 0x000F);
						int argb = a | r | g | b;
						colors[di++] = argb | argb << 4;
					}
				}
				break;
			}
			case TYPE_USHORT_444_RGB: {
				int space = scanlength - width;
				for (int yi = 0, di = 0; yi < height; yi++, offset += space) {
					for (int xi = 0; xi < width; xi++) {
						short s = pixels[offset++];
						int rgb = (s & 0x0F00) << 8 | (s & 0x00F0) << 4 | (s & 0x000F);
						colors[di++] = 0xFF000000 | rgb | rgb << 4;
					}
				}
				break;
			}
			case TYPE_USHORT_565_RGB: {
				int space = scanlength - width;
				for (int yi = 0, di = 0; yi < height; yi++, offset += space) {
					for (int xi = 0; xi < width; xi++) {
						short s = pixels[offset++];
						int r = (s & 0xF800) << 8 | (s & 0xE000) << 3;
						int g = (s & 0x07E0) << 5 | (s & 0x0600) >> 1;
						int b = (s & 0x001F) << 3 | (s & 0x001C) >> 2;
						colors[di++] = 0xFF000000 | r | g | b;
					}
				}
				break;
			}
			case TYPE_USHORT_555_RGB:
			case TYPE_USHORT_1555_ARGB:
				throw new IllegalArgumentException("Unsupported format: " + format);
			default:
				throw new IllegalArgumentException("Illegal format: " + format);
		}
		Image image = Image.createRGBImage(colors, width, height, true);
		graphics.drawRegion(image, 0, 0, width, height, transform, x, y, 0);
	}

	@Override
	public void drawPolygon(int[] xPoints,
							int xOffset,
							int[] yPoints,
							int yOffset,
							int nPoints,
							int argbColor) {
		setARGBColor(argbColor);
		graphics.drawPolygon(xPoints, xOffset, yPoints, yOffset, nPoints);
	}

	@Override
	public void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor) {
		drawPolygon(new int[]{x1, x2, x3}, 0, new int[]{y1, y2, y3}, 0, 3, argbColor);
	}

	@Override
	public void fillPolygon(int[] xPoints,
							int xOffset,
							int[] yPoints,
							int yOffset,
							int nPoints,
							int argbColor) {
		setARGBColor(argbColor);
		graphics.fillPolygon(xPoints, xOffset, yPoints, yOffset, nPoints);
	}

	@Override
	public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor) {
		fillPolygon(new int[]{x1, x2, x3}, 0, new int[]{y1, y2, y3}, 0, 3, argbColor);
	}

	@Override
	public int getAlphaComponent() {
		return alphaComponent;
	}

	@Override
	public int getNativePixelFormat() {
		return PIXEL_FORMAT;
	}

	@Override
	public void getPixels(byte[] pixels,
						  byte[] transparencyMask,
						  int offset,
						  int scanlength,
						  int x,
						  int y,
						  int width,
						  int height,
						  int format) {
		if (pixels == null) {
			throw new NullPointerException();
		} else if (x < 0 || y < 0 || width < 0 || height < 0 || scanlength < width) {
			throw new IllegalArgumentException();
		} else if (offset < 0) {
			throw new ArrayIndexOutOfBoundsException();
		} else if (width == 0 || height == 0) {
			return;
		}

		switch (format) {
			case TYPE_BYTE_1_GRAY: {
				int bits = offset + width + (height - 1) * scanlength;
				if (bits > pixels.length << 3 || transparencyMask != null && bits > transparencyMask.length << 3) {
					throw new ArrayIndexOutOfBoundsException();
				}
				int[] colors = new int[width * height];
				getPixels(colors, 0, width, x, y, width, height);
				int space = scanlength - width;
				for (int yi = 0, si = 0; yi < height; yi++, offset += space) {
					for (int xi = 0; xi < width; xi++, offset++) {
						setPixel(pixels, transparencyMask, offset >> 3, 7 - (offset & 7), colors[si++]);
					}
				}
				break;
			}
			case TYPE_BYTE_1_GRAY_VERTICAL: {
				int ods = offset / scanlength;
				int oms = offset % scanlength;
				int shift = ods & 7;
				int maxIndex = ((ods + height - 1) >> 3) * scanlength + oms;
				if (maxIndex >= pixels.length) {
					throw new ArrayIndexOutOfBoundsException();
				} else if (transparencyMask != null && maxIndex >= transparencyMask.length) {
					throw new ArrayIndexOutOfBoundsException();
				}
				int[] colors = new int[width * height];
				getPixels(colors, 0, width, x, y, width, height);
				for (int yi = 0, si = 0; yi < height; yi++) {
					int idx = ((ods + yi) >> 3) * scanlength + oms;
					for (int xi = 0; xi < width; xi++) {
						setPixel(pixels, transparencyMask, idx++, shift, colors[si++]);
					}
					shift = (shift + 1) & 7;
				}
				break;
			}
			case TYPE_BYTE_2_GRAY:
			case TYPE_BYTE_4_GRAY:
			case TYPE_BYTE_8_GRAY:
			case TYPE_BYTE_332_RGB:
				throw new IllegalArgumentException("Unsupported format: " + format);
			default:
				throw new IllegalArgumentException("Illegal format: " + format);
		}

	}

	@Override
	public void getPixels(int[] pixels,
						  int offset,
						  int scanlength,
						  int x,
						  int y,
						  int width,
						  int height,
						  int format) {
		if (pixels == null) {
			throw new NullPointerException();
		} else if (x < 0 || y < 0 || width < 0 || height < 0 || scanlength < width) {
			throw new IllegalArgumentException();
		} else if (offset < 0 || offset + width + (height - 1) > pixels.length) {
			throw new IllegalArgumentException();
		} else if (format != TYPE_INT_888_RGB && format != TYPE_INT_8888_ARGB) {
			throw new IllegalArgumentException("Illegal format: " + format);
		} else if (width == 0 || height == 0) {
			return;
		}

		getPixels(pixels, offset, scanlength, x, y, width, height);
		if (format == TYPE_INT_888_RGB) {
			int space = scanlength - width;
			for (int yi = 0; yi < height; yi++, offset += space) {
				for (int xi = 0; xi < width; xi++) {
					pixels[offset++] &= 0xFFFFFF;
				}
			}
		}
	}

	@Override
	public void getPixels(short[] pixels,
						  int offset,
						  int scanlength,
						  int x,
						  int y,
						  int width,
						  int height,
						  int format) {
		if (pixels == null) {
			throw new NullPointerException();
		} else if (x < 0 || y < 0 || width < 0 || height < 0 || scanlength < width) {
			throw new IllegalArgumentException();
		} else if (offset < 0 || offset + width + (height - 1) > pixels.length) {
			throw new IllegalArgumentException();
		} else if (width == 0 || height == 0) {
			return;
		}

		int[] colors = new int[width * height];
		getPixels(colors, 0, width, x, y, width, height);
		switch (format) {
			case TYPE_USHORT_4444_ARGB: {
				int space = scanlength - width;
				for (int yi = 0, si = 0; yi < height; yi++, offset += space) {
					for (int xi = 0; xi < width; xi++, si++) {
						int a = colors[si] >> 16 & 0xF000;
						int r = colors[si] >> 12 & 0x0F00;
						int g = colors[si] >> 8 & 0x00F0;
						int b = colors[si] >> 4 & 0x000F;
						pixels[offset++] = (short) (a | r | g | b);
					}
				}
				break;
			}
			case TYPE_USHORT_444_RGB: {
				int space = scanlength - width;
				for (int yi = 0, si = 0; yi < height; yi++, offset += space) {
					for (int xi = 0; xi < width; xi++, si++) {
						int r = colors[si] >> 12 & 0x0F00;
						int g = colors[si] >> 8 & 0x00F0;
						int b = colors[si] >> 4 & 0x000F;
						pixels[offset++] = (short) (r | g | b);
					}
				}
				break;
			}
			case TYPE_USHORT_565_RGB: {
				int space = scanlength - width;
				for (int yi = 0, si = 0; yi < height; yi++, offset += space) {
					for (int xi = 0; xi < width; xi++, si++) {
						int r = colors[si] >> 8 & 0xF800;
						int g = colors[si] >> 5 & 0x07E0;
						int b = colors[si] >> 3 & 0x001F;
						pixels[offset++] = (short) (r | g | b);
					}
				}
				break;
			}
			case TYPE_USHORT_555_RGB:
			case TYPE_USHORT_1555_ARGB:
				throw new IllegalArgumentException("Unsupported format: " + format);
			default:
				throw new IllegalArgumentException("Illegal format: " + format);
		}
	}

	@Override
	public void setARGBColor(int argb) {
		alphaComponent = (argb >> 24 & 0xff);
		graphics.setColorAlpha(argb);
	}

	private void getPixels(int[] pixels,
						   int offset,
						   int stride,
						   int x,
						   int y,
						   int width,
						   int height) {
		x += graphics.getTranslateX();
		y += graphics.getTranslateY();
		Bitmap image = graphics.getBitmap();
		int w = Math.min(width, image.getWidth() - x);
		int h = Math.min(height, image.getHeight() - y);
		image.getPixels(pixels, offset, stride, x, y, w, h);
	}
}
