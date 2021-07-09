/*
 *  Copyright 2021 Yury Kharchenko
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

package javax.microedition.lcdui.keyboard;

import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;

public class DelKeyWorkaround extends BaseInputConnection {

	public DelKeyWorkaround(View target, boolean fullEditor) {
		super(target, fullEditor);
	}

	@Override
	public boolean deleteSurroundingText(int beforeLength, int afterLength) {
		if (afterLength == 0 && beforeLength == 1) {
			sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
			sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
			return true;
		}
		return super.deleteSurroundingText(beforeLength, afterLength);
	}

	@Override
	public boolean sendKeyEvent(KeyEvent event) {
		super.sendKeyEvent(event);
		return true;
	}
}
