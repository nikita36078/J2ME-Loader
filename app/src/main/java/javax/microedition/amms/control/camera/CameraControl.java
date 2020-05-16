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

public interface CameraControl extends Control {
	public final static int ROTATE_LEFT = 2;
	public final static int ROTATE_RIGHT = 3;
	public final static int ROTATE_NONE = 1;
	public final static int UNKNOWN = -1004;

	public int getCameraRotation();

	public void enableShutterFeedback(boolean enable) throws MediaException;

	public boolean isShutterFeedbackEnabled();

	public String[] getSupportedExposureModes();

	public void setExposureMode(String mode);

	public String getExposureMode();

	public int[] getSupportedVideoResolutions();

	public int[] getSupportedStillResolutions();

	public void setVideoResolution(int index);

	public void setStillResolution(int index);

	public int getVideoResolution();

	public int getStillResolution();
}
