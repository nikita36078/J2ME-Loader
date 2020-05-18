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

package javax.microedition.amms.control.tuner;

import javax.microedition.media.Control;
import javax.microedition.media.MediaException;

public interface TunerControl extends Control {
	public static final int MONO = 1;
	public static final int STEREO = 2;
	public static final int AUTO = 3;
	public static final String MODULATION_FM = "fm";
	public static final String MODULATION_AM = "am";

	public int getMinFreq(String modulation);

	public int getMaxFreq(String modulation);

	public int setFrequency(int freq, String modulation);

	public int getFrequency();

	public int seek(int startFreq, String modulation, boolean upwards) throws MediaException;

	public boolean getSquelch();

	public void setSquelch(boolean squelch) throws MediaException;

	public String getModulation();

	public int getSignalStrength() throws MediaException;

	public int getStereoMode();

	public void setStereoMode(int mode);

	public int getNumberOfPresets();

	public void usePreset(int preset);

	public void setPreset(int preset);

	public void setPreset(int preset, int freq, String mod, int stereoMode);

	public int getPresetFrequency(int preset);

	public String getPresetModulation(int preset);

	public int getPresetStereoMode(int preset) throws MediaException;

	public String getPresetName(int preset);

	public void setPresetName(int preset, String name);
}
