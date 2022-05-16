/*
 * Copyright 2020 Nikita Shakarun
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

package com.vodafone.v10.graphics.sprite;

import java.util.ArrayList;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public abstract class SpriteCanvas extends Canvas {
	private static ArrayList<CharacterCommand> commands = new ArrayList<>();
	private Image spriteImage;
	private Graphics graphics;
	private int[] palette;
	private byte[] patternData;
	private int[] pixels;

	public SpriteCanvas(int numPalettes, int numPatterns) {
		super();
		this.palette = new int[numPalettes];
		this.patternData = new byte[numPatterns * 64];
		this.pixels = new int[64];
	}

	public void createFrameBuffer(int fw, int fh) {
		spriteImage = Image.createImage(fw, fh, 0);
		graphics = spriteImage.getGraphics();
	}

	public void disposeFrameBuffer() {
	}

	public void copyArea(int sx, int sy, int fw, int fh, int tx, int ty) {
	}

	public void drawFrameBuffer(int tx, int ty) {
		flushBuffer(spriteImage, tx, ty);
		graphics.getBitmap().eraseColor(0);
	}

	public void setPalette(int index, int palette) {
		this.palette[index] = palette | 0xFF000000;
	}

	public void setPattern(int index, byte[] data) {
		System.arraycopy(data, 0, patternData, index * 64, data.length);
	}

	public static short createCharacterCommand(int offset, boolean transparent, int rotation,
											   boolean isUpsideDown, boolean isRightsideLeft, int patternNo) {
		CharacterCommand command = new CharacterCommand();
		command.offset = offset;
		command.transparent = transparent;
		command.rotation = rotation;
		command.isUpsideDown = isUpsideDown;
		command.isRightsideLeft = isRightsideLeft;
		command.patternNo = patternNo;
		commands.add(command);
		return (short) (commands.size() - 1);
	}

	public void drawSpriteChar(short command, short x, short y) {
		CharacterCommand characterCommand = commands.get(command);
		for (int i = 0; i < 64; i++) {
			int colorId = patternData[characterCommand.patternNo * 64 + i];
			pixels[i] = palette[colorId];
		}
		graphics.drawRGB(pixels, 0, 8, x, y, 8, 8, true);
	}

	private static class CharacterCommand {
		int offset;
		boolean transparent;
		int rotation;
		boolean isUpsideDown;
		boolean isRightsideLeft;
		int patternNo;
	}
}
