/*
 * Copyright 2022 Nikita Shakarun
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

package javax.microedition.media;

import android.os.Build;

import androidx.annotation.RequiresApi;

import javax.microedition.lcdui.VideoItem;
import javax.microedition.media.control.GUIControl;
import javax.microedition.media.control.VideoControl;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraPlayer extends BasePlayer implements VideoControl {

	private final CameraController controller;

	public CameraPlayer() {
		controller = new CameraController();

		addControl(VideoControl.class.getName(), this);
	}

	@Override
	public Object initDisplayMode(int mode, Object arg) {
		if (mode == GUIControl.USE_GUI_PRIMITIVE) {
			return new VideoItem(controller);
		}
		return null;
	}

	@Override
	public void setDisplayLocation(int x, int y) {

	}

	@Override
	public void setDisplaySize(int width, int height) throws MediaException {

	}

	@Override
	public void setDisplayFullScreen(boolean fullScreenMode) throws MediaException {

	}

	@Override
	public void setVisible(boolean visible) {

	}

	@Override
	public int getSourceWidth() {
		return 0;
	}

	@Override
	public int getSourceHeight() {
		return 0;
	}

	@Override
	public int getDisplayX() {
		return 0;
	}

	@Override
	public int getDisplayY() {
		return 0;
	}

	@Override
	public int getDisplayWidth() {
		return 0;
	}

	@Override
	public int getDisplayHeight() {
		return 0;
	}

	@Override
	public byte[] getSnapshot(String imageType) throws MediaException {
		return controller.getSnapshot();
	}
}
