/*
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

package javax.microedition.lcdui.game;

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

	private Image image;
	private int key;
	private int repeatedKey;
	private boolean suppressCommands;

	public GameCanvas(boolean suppressCommands) {
		super();
		this.suppressCommands = suppressCommands;
		image = Image.createImage(width, height);
		image.getBitmap().setHasAlpha(false);
	}

	@Override
	public void paint(Graphics g) {
		g.drawImage(image, 0, 0, Graphics.LEFT | Graphics.TOP);
	}

	private int convertGameKeyCode(int keyCode) {
		switch (keyCode) {
			case KEY_LEFT:
			case KEY_NUM4:
				return LEFT_PRESSED;
			case KEY_UP:
			case KEY_NUM2:
				return UP_PRESSED;
			case KEY_RIGHT:
			case KEY_NUM6:
				return RIGHT_PRESSED;
			case KEY_DOWN:
			case KEY_NUM8:
				return DOWN_PRESSED;
			case KEY_FIRE:
			case KEY_NUM5:
				return FIRE_PRESSED;
			case KEY_NUM7:
				return GAME_A_PRESSED;
			case KEY_NUM9:
				return GAME_B_PRESSED;
			case KEY_STAR:
				return GAME_C_PRESSED;
			case KEY_POUND:
				return GAME_D_PRESSED;
			default:
				return 0;
		}
	}

	public void gameKeyPressed(int keyCode) {
		if (!(suppressCommands && checkGameAction(keyCode))) {
			keyPressed(keyCode);
		}
		key |= convertGameKeyCode(keyCode);
	}

	public void gameKeyReleased(int keyCode) {
		if (!(suppressCommands && checkGameAction(keyCode))) {
			keyReleased(keyCode);
		}
		repeatedKey &= ~convertGameKeyCode(keyCode);
	}

	public void gameKeyRepeated(int keyCode) {
		if (!(suppressCommands && checkGameAction(keyCode))) {
			keyRepeated(keyCode);
		}
		repeatedKey |= convertGameKeyCode(keyCode);
	}

	private boolean checkGameAction(int keyCode) {
		try {
			getGameAction(keyCode);
			return true;
		} catch (IllegalArgumentException iae) {
			return false;
		}
	}

	public int getKeyStates() {
		int temp = key;
		temp |= repeatedKey;
		key = repeatedKey;
		return temp;
	}

	public Graphics getGraphics() {
		return image.getGraphics();
	}

	public void flushGraphics() {
		flushGraphics(0, 0, width, height);
	}

	public void flushGraphics(int x, int y, int width, int height) {
		flushBuffer(image);
	}
}
