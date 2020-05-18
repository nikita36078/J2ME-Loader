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

package javax.microedition.amms.control.camera;

import javax.microedition.media.Control;
import javax.microedition.media.MediaException;

public interface FocusControl extends Control {
	public final static int AUTO = -1000;
	public final static int AUTO_LOCK = -1005;
	public final static int NEXT = -1001;
	public final static int PREVIOUS = -1002;
	public final static int UNKNOWN = -1004;

	public int setFocus(int distance) throws MediaException;

	public int getFocus();

	public int getMinFocus();

	public int getFocusSteps();

	public boolean isManualFocusSupported();

	public boolean isAutoFocusSupported();

	public boolean isMacroSupported();

	public void setMacro(boolean enable) throws MediaException;

	public boolean getMacro();
}
