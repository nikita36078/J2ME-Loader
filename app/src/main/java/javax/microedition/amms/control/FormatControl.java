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

package javax.microedition.amms.control;

import javax.microedition.media.Control;
import javax.microedition.media.MediaException;

public interface FormatControl extends Control {
	public final static int METADATA_NOT_SUPPORTED = 0;
	public final static int METADATA_SUPPORTED_FIXED_KEYS = 1;
	public final static int METADATA_SUPPORTED_FREE_KEYS = 2;
	public final static String PARAM_BITRATE = "bitrate";
	public final static String PARAM_BITRATE_TYPE = "bitrate type";
	public final static String PARAM_SAMPLERATE = "sample rate";
	public final static String PARAM_FRAMERATE = "frame rate";
	public final static String PARAM_QUALITY = "quality";
	public final static String PARAM_VERSION_TYPE = "version type";

	public String[] getSupportedFormats();

	public String[] getSupportedStrParameters();

	public String[] getSupportedIntParameters();

	public String[] getSupportedStrParameterValues(String parameter);

	public int[] getSupportedIntParameterRange(String parameter);

	public void setFormat(String format);

	public String getFormat();

	public int setParameter(String parameter, int value);

	public void setParameter(String parameter, String value);

	public String getStrParameterValue(String parameter);

	public int getIntParameterValue(String parameter);

	public int getEstimatedBitRate() throws MediaException;

	public void setMetadata(String key, String value) throws MediaException;

	public String[] getSupportedMetadataKeys();

	public int getMetadataSupportMode();

	public void setMetadataOverride(boolean override);

	public boolean getMetadataOverride();
}
