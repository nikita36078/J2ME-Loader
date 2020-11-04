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

public interface SnapshotControl extends Control {
	public String SHOOTING_STOPPED = "SHOOTING_STOPPED";
	public String STORAGE_ERROR = "STORAGE_ERROR";
	public String WAITING_UNFREEZE = "WAITING_UNFREEZE";
	public final static int FREEZE = -2;
	public final static int FREEZE_AND_CONFIRM = -1;

	public void setDirectory(String directory);

	public String getDirectory();

	public void setFilePrefix(String prefix);

	public String getFilePrefix();

	public void setFileSuffix(String suffix);

	public String getFileSuffix();

	public void start(int maxShots) throws SecurityException;

	public void stop();

	public void unfreeze(boolean save);
}
