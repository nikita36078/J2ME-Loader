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

public class TiledBackground extends GraphicObject {
	private Image[] pixels;
	private byte[] map;
	private int widthInTiles;
	private int heightInTiles;
	private int posx;
	private int posy;

	public TiledBackground(byte[] tilePixels, byte[] tileMask, byte[] map, int widthInTiles, int heightInTiles) {
		this(
				com.siemens.mp.ui.Image.createImageFromBitmap(tilePixels, 8, tilePixels.length),
				com.siemens.mp.ui.Image.createImageFromBitmap(tileMask, 8, tilePixels.length),
				map,
				widthInTiles,
				heightInTiles
		);
	}

	public TiledBackground(ExtendedImage tilePixels, ExtendedImage tileMask, byte[] map, int widthInTiles, int heightInTiles) {
		this(tilePixels.getImage(), tileMask.getImage(), map, widthInTiles, heightInTiles);
	}

	public TiledBackground(Image tilePixels, Image tileMask, byte[] map, int widthInTiles, int heightInTiles) {
		this.map = map;
		this.heightInTiles = heightInTiles;
		this.widthInTiles = widthInTiles;

		pixels = new Image[tilePixels.getHeight() / 8 + 3];
		pixels[0] = Image.createImage(8, 8, 0); // transparent
		pixels[1] = Image.createImage(8, 8); // white
		pixels[2] = Image.createImage(8, 8); // black
		pixels[2].getGraphics().fillRect(0, 0, 8, 8);

		if (tileMask != null) {
			tilePixels = com.siemens.mp.ui.Image.createTransparentImageFromMask(tilePixels, tileMask);
		}

		for (int i = 0; i < this.pixels.length - 3; i++) {
			Image img = Image.createImage(8, 8, 0);

			img.getGraphics().drawImage(tilePixels, 0, -i * 8, 0);
			pixels[i + 3] = img;
		}
	}

	public void setPositionInMap(int x, int y) {
		posx = x;
		posy = y;
	}

	protected void paint(Graphics g) {
		for (int y = posy / 8; y < heightInTiles; y++) {
			for (int x = posx / 8; x < widthInTiles; x++) {
				g.drawImage(pixels[map[y * widthInTiles + x] & 0xFF], -posx + x * 8, -posy + y * 8, 0);
			}
		}
	}
}