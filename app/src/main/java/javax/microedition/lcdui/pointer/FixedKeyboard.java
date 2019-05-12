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

	public FixedKeyboard() {
		super();
		shape = SQUARE_SHAPE;
	}

	@Override
	protected void resetLayout(int variant) {
		setSnap(KEY_SOFT_LEFT, KEY_NUM1, RectSnap.EXT_NORTH);
		setSnap(KEY_FIRE, KEY_NUM2, RectSnap.EXT_NORTH);
		setSnap(KEY_SOFT_RIGHT, KEY_NUM3, RectSnap.EXT_NORTH);

		setSnap(KEY_STAR, KEY_NUM0, RectSnap.EXT_WEST);
		setSnap(KEY_NUM0, SCREEN, RectSnap.INT_SOUTH);
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

		for (int i = KEY_NUM1; i < KEY_DIAL; i++) {
			keypad[i].setVisible(true);
		}
		for (int i = KEY_DIAL; i < KEY_FIRE; i++) {
			keypad[i].setVisible(false);
		}
	}

	@Override
	public void resize(RectF screen, RectF virtualScreen) {
		this.screen = screen;
		this.virtualScreen = virtualScreen;
		float keyWidth = screen.width() / 3;
		float keyHeight = keyWidth / 2.5f;
		for (VirtualKey aKeypad : keypad) {
			aKeypad.resize(keyWidth, keyHeight);
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
