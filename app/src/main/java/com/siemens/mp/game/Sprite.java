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

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Sprite extends GraphicObject {
	private Image[] pixels;
	private int x;
	private int y;
	private int frame;
	private int collx, colly, collw, collh;

	public Sprite(byte[] pixels, int pixel_offset, int width, int height, byte[] mask, int mask_offset, int numFrames) {
		this(
				com.siemens.mp.ui.Image.createImageFromBitmap(pixels, mask, width, height * numFrames),
				com.siemens.mp.ui.Image.createImageFromBitmap(mask, width, height * numFrames),
				numFrames
		);
	}

	public Sprite(ExtendedImage pixels, ExtendedImage mask, int numFrames) {
		this(pixels.getImage(), mask.getImage(), numFrames);
	}

	public Sprite(Image pixels, Image mask, int numFrames) {
		this.pixels = new Image[numFrames];

		if (mask != null) {
			pixels = com.siemens.mp.lcdui.Image.createTransparentImageFromMask(pixels, mask);
		}

		for (int i = 0; i < numFrames; i++) {
			Image img = Image.createTransparentImage(pixels.getWidth(), pixels.getHeight() / numFrames);

			img.getGraphics().drawImage(pixels, 0, -i * pixels.getHeight() / numFrames, 0);
			this.pixels[i] = img;
		}
		collx = 0;
		colly = 0;
		collw = this.pixels[0].getWidth();
		collh = this.pixels[0].getHeight();
	}

	public int getFrame() {
		return frame;
	}

	public int getXPosition() {
		return x;
	}

	public int getYPosition() {
		return y;
	}

	public boolean isCollidingWith(Sprite other) {
		int left = x + collx;
		int right = x + collx + collw;
		int top = y + colly;
		int bottom = y + colly + collh;
		int otherLeft = other.x + other.collx;
		int otherRight = other.x + other.collx + other.collw;
		int otherTop = other.y + other.colly;
		int otherBottom = other.y + other.colly + other.collh;
		return left < otherRight && otherLeft < right && top < otherBottom && otherTop < bottom;
	}

	public boolean isCollidingWithPos(int xpos, int ypos) {
		int left = x + collx;
		int right = x + collx + collw;
		int top = y + colly;
		int bottom = y + colly + collh;
		return (xpos >= left) && (xpos < right) && (ypos >= top) && (ypos < bottom);
	}

	public void setCollisionRectangle(int x, int y, int width, int height) {
		collx = x;
		colly = y;
		collw = width;
		collh = height;
	}

	public void setFrame(int framenumber) {
		frame = framenumber;
	}

	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	protected void paint(Graphics g) {
		g.drawImage(pixels[frame], x, y, 0);
	}
}
