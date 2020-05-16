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

public interface FlashControl extends Control {
	public final static int OFF = 1;
	public final static int AUTO = 2;
	public final static int AUTO_WITH_REDEYEREDUCE = 3;
	public final static int FORCE = 4;
	public final static int FORCE_WITH_REDEYEREDUCE = 5;
	public final static int FILLIN = 6;

	public int[] getSupportedModes();

	public void setMode(int mode);

	public int getMode();

	public boolean isFlashReady();
}
