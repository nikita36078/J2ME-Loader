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

package com.siemens.mp.ui;

public class Image extends com.siemens.mp.misc.NativeMem {

	public static javax.microedition.lcdui.Image createImageFromBitmap(byte[] imageData, int imageWidth, int imageHeight) {
		return createImageFromBitmap(imageData, null, imageWidth, imageHeight);
	}

	public static javax.microedition.lcdui.Image createImageFromBitmap(byte[] imageData, byte[] alpha, int imageWidth, int imageHeight) {
		if (imageData == null) return null;

		if (imageWidth < 8) imageWidth = 8;

		int c;
		int[] pixres = new int[imageHeight * imageWidth];
		for (int y = 0; y < imageHeight; y++) {
			for (int x = 0; x < imageWidth / 8; x++) {
				for (int b = 0; b < 8; b++) {
					c = doAlpha(imageData, alpha, y * imageWidth / 8 + x, b);
					pixres[x * 8 + 7 - b + y * imageWidth] = c;
				}
			}
		}
		javax.microedition.lcdui.Image image = javax.microedition.lcdui.Image.createRGBImage(pixres, imageWidth, imageHeight, true);
		image.setBlackWhiteAlpha(alpha != null);
		return image;
	}

	public static javax.microedition.lcdui.Image createImageWithoutScaling(String name) throws java.io.IOException {
		return javax.microedition.lcdui.Image.createImage(name);
	}

	public static javax.microedition.lcdui.Image createTransparentImageFromBitmap(byte[] imageData, int imageWidth, int imageHeight) {
		if (imageData == null) return null;

		if (imageWidth < 4) imageWidth = 4;

		int[] pixres = new int[imageHeight * imageWidth];
		for (int y = 0; y < imageHeight; y++) {
			for (int x = 0; x < imageWidth / 4; x++) {
				for (int b = 7; b >= 0; b -= 2) {
					int c = doAlpha(imageData, y * imageWidth / 4 + x, b);
					pixres[x * 4 + 3 - b / 2 + y * imageWidth] = c;
				}
			}
		}
		javax.microedition.lcdui.Image image = javax.microedition.lcdui.Image.createRGBImage(pixres, imageWidth, imageHeight, true);
		image.setBlackWhiteAlpha(true);
		return image;
	}

	public static javax.microedition.lcdui.Image createTransparentImageFromMask(javax.microedition.lcdui.Image image, javax.microedition.lcdui.Image mask) {
		int width = image.getWidth();
		int height = image.getHeight();
		int[] imagePixels = new int[width * height];
		int[] maskPixels = new int[width * height];

		image.getRGB(imagePixels, 0, width, 0, 0, width, height);
		mask.getRGB(maskPixels, 0, width, 0, 0, width, height);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (maskPixels[y * width + x] == 0xFFFFFFFF) {
					imagePixels[y * width + x] = 0;
				}
			}
		}
		return javax.microedition.lcdui.Image.createRGBImage(imagePixels, width, height, true);
	}

	private static boolean isBitSet(byte b, int pos) {
		return ((b & (byte) (1 << pos)) != 0);
	}

	private static int doAlpha(byte[] pix, int pos, int shift) {
		int p;
		int a;
		if (isBitSet(pix[pos], shift))
			p = 0;
		else
			p = 0x00FFFFFF;
		if (isBitSet(pix[pos], shift) || isBitSet(pix[pos], shift - 1))
			a = 0xFF000000;
		else
			a = 0;
		return p | a;
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
}
