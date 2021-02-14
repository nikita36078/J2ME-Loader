/*
 * Copyright 2012 Kulikov Dmitriy
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

package javax.microedition.amms.control;

import javax.microedition.media.Control;
import javax.microedition.media.MediaException;

public interface EffectControl extends Control {
	public static final int SCOPE_LIVE_ONLY = 1;
	public static final int SCOPE_RECORD_ONLY = 2;
	public static final int SCOPE_LIVE_AND_RECORD = 3;

	public void setScope(int scope) throws MediaException;

	public int getScope();

	public String[] getPresetNames();

	public void setPreset(String preset);

	public String getPreset();

	public void setEnabled(boolean enable);

	public boolean isEnabled();

	public void setEnforced(boolean enforced);

	public boolean isEnforced();
}
