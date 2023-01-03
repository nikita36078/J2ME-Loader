/*
 *  Copyright 2020 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.siemens.mp.color_game;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class GameCanvas extends Canvas {
	public static final int UP_PRESSED = 1 << Canvas.UP;
	public static final int DOWN_PRESSED = 1 << Canvas.DOWN;
	public static final int LEFT_PRESSED = 1 << Canvas.LEFT;
	public static final int RIGHT_PRESSED = 1 << Canvas.RIGHT;
	public static final int FIRE_PRESSED = 1 << Canvas.FIRE;
	public static final int GAME_A_PRESSED = 1 << Canvas.GAME_A;
	public static final int GAME_B_PRESSED = 1 << Canvas.GAME_B;
	public static final int GAME_C_PRESSED = 1 << Canvas.GAME_C;
	public static final int GAME_D_PRESSED = 1 << Canvas.GAME_D;

	private final Image image;
	private int keyState = 0;

	protected GameCanvas(boolean suppressKeyEvents) throws OutOfMemoryError {
		image = Image.createImage(width, maxHeight);
	}

	public void flushGraphics() {
		repaint();
		serviceRepaints();
	}

	public void flushGraphics(int x, int y, int width, int height) {
		repaint(x, y, width, height);
		serviceRepaints();
	}

	protected Graphics getGraphics() {
		return image.getGraphics();
	}

	public int getKeyStates() {
		int keyState = this.keyState;
		this.keyState = 0;
		if (!isShown())
			return 0;
		return keyState;
	}

	protected void keyPressed(int keyCode) {
		switch (getGameAction(keyCode)) {
			case FIRE:
				keyState |= FIRE_PRESSED;
				break;
			case UP:
				keyState |= UP_PRESSED;
				break;
			case DOWN:
				keyState |= DOWN_PRESSED;
				break;
			case LEFT:
				keyState |= LEFT_PRESSED;
				break;
			case RIGHT:
				keyState |= RIGHT_PRESSED;
				break;
			case GAME_A:
				keyState |= GAME_A_PRESSED;
				break;
			case GAME_B:
				keyState |= GAME_B_PRESSED;
				break;
			case GAME_C:
				keyState |= GAME_C_PRESSED;
				break;
			case GAME_D:
				keyState |= GAME_D_PRESSED;
				break;
		}
	}

	protected void keyRepeated(int keyCode) {
		keyPressed(keyCode);
	}

	public void paint(Graphics g) {
		g.drawImage(image, 0, 0, 0);
	}
}
