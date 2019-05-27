/*
 * Copyright 2019 Nikita Shakarun
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

package javax.microedition.lcdui.pointer;

import android.graphics.RectF;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class FixedKeyboard extends VirtualKeyboard {

	public final static float KEY_WIDTH_RATIO = 3;
	public final static float KEY_HEIGHT_RATIO = 2.7f;

	public FixedKeyboard(int variant) {
		super(variant);
		shape = SQUARE_SHAPE;
	}

	@Override
	protected void resetLayout(int variant) {
		switch (variant) {
			case 0:
				setSnap(KEY_NUM0, SCREEN, RectSnap.INT_SOUTH);
				setSnap(KEY_STAR, KEY_NUM0, RectSnap.EXT_WEST);
				setSnap(KEY_POUND, KEY_NUM0, RectSnap.EXT_EAST);
				setSnap(KEY_NUM7, KEY_STAR, RectSnap.EXT_NORTH);
				setSnap(KEY_NUM8, KEY_NUM7, RectSnap.EXT_EAST);
				setSnap(KEY_NUM9, KEY_NUM8, RectSnap.EXT_EAST);
				setSnap(KEY_NUM4, KEY_NUM7, RectSnap.EXT_NORTH);
				setSnap(KEY_NUM5, KEY_NUM4, RectSnap.EXT_EAST);
				setSnap(KEY_NUM6, KEY_NUM5, RectSnap.EXT_EAST);
				setSnap(KEY_NUM1, KEY_NUM4, RectSnap.EXT_NORTH);
				setSnap(KEY_NUM2, KEY_NUM1, RectSnap.EXT_EAST);
				setSnap(KEY_NUM3, KEY_NUM2, RectSnap.EXT_EAST);

				setSnap(KEY_SOFT_LEFT, KEY_NUM1, RectSnap.EXT_NORTH);
				setSnap(KEY_FIRE, KEY_NUM2, RectSnap.EXT_NORTH);
				setSnap(KEY_SOFT_RIGHT, KEY_NUM3, RectSnap.EXT_NORTH);

				for (int i = KEY_NUM1; i < KEY_DIAL; i++) {
					keypad[i].setVisible(true);
				}
				for (int i = KEY_DIAL; i < KEY_FIRE; i++) {
					keypad[i].setVisible(false);
				}
				break;
			case 1:
				setSnap(KEY_NUM0, SCREEN, RectSnap.INT_SOUTH);
				setSnap(KEY_STAR, KEY_NUM0, RectSnap.EXT_WEST);
				setSnap(KEY_POUND, KEY_NUM0, RectSnap.EXT_EAST);
				setSnap(KEY_NUM7, KEY_STAR, RectSnap.EXT_NORTH);
				setSnap(KEY_DOWN, KEY_NUM7, RectSnap.EXT_EAST);
				setSnap(KEY_NUM9, KEY_DOWN, RectSnap.EXT_EAST);
				setSnap(KEY_LEFT, KEY_NUM7, RectSnap.EXT_NORTH);
				setSnap(KEY_NUM5, KEY_LEFT, RectSnap.EXT_EAST);
				setSnap(KEY_RIGHT, KEY_NUM5, RectSnap.EXT_EAST);
				setSnap(KEY_NUM1, KEY_LEFT, RectSnap.EXT_NORTH);
				setSnap(KEY_UP, KEY_NUM1, RectSnap.EXT_EAST);
				setSnap(KEY_NUM3, KEY_UP, RectSnap.EXT_EAST);

				setSnap(KEY_SOFT_LEFT, KEY_NUM1, RectSnap.EXT_NORTH);
				setSnap(KEY_FIRE, KEY_UP, RectSnap.EXT_NORTH);
				setSnap(KEY_SOFT_RIGHT, KEY_NUM3, RectSnap.EXT_NORTH);

				for (int i = KEY_NUM1; i < KEY_NUM9; i += 2) {
					keypad[i].setVisible(true);
					keypad[i + 1].setVisible(false);
				}
				for (int i = KEY_NUM9; i < KEY_DIAL; i++) {
					keypad[i].setVisible(true);
				}
				keypad[KEY_DIAL].setVisible(false);
				keypad[KEY_CANCEL].setVisible(false);
				keypad[KEY_UP_LEFT].setVisible(false);
				keypad[KEY_UP].setVisible(true);
				keypad[KEY_UP_RIGHT].setVisible(false);
				keypad[KEY_LEFT].setVisible(true);
				keypad[KEY_RIGHT].setVisible(true);
				keypad[KEY_DOWN_LEFT].setVisible(false);
				keypad[KEY_DOWN].setVisible(true);
				keypad[KEY_DOWN_RIGHT].setVisible(false);
				keypad[KEY_FIRE].setVisible(true);
				break;
		}
	}

	@Override
	public void switchLayout() {
		layoutVariant ^= 1;
		resetLayout(layoutVariant);
		snapKeys();
		repaint();
	}

	@Override
	public void resize(RectF screen, RectF virtualScreen) {
		this.screen = screen;
		this.virtualScreen = virtualScreen;
		float keyWidth = screen.width() / KEY_WIDTH_RATIO;
		float keyHeight = keyWidth / KEY_HEIGHT_RATIO;
		for (int i = 0; i < keypad.length; i++) {
			keypad[i].resize(keyWidth, keyHeight);
			snapValid[i] = false;
		}
		snapKeys();
		repaint();
	}

	@Override
	public void readLayout(DataInputStream dis) throws IOException {
	}

	@Override
	public void writeLayout(DataOutputStream dos) throws IOException {
	}

	@Override
	public void setButtonShape(int shape) {
	}
}
