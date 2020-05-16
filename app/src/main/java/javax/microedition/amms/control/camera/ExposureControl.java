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

public interface ExposureControl extends Control {
	public int[] getSupportedFStops();

	public int getFStop();

	public void setFStop(int aperture) throws MediaException;

	public int getMinExposureTime();

	public int getMaxExposureTime();

	public int getExposureTime();

	public int setExposureTime(int time) throws MediaException;

	public int[] getSupportedISOs();

	public int getISO();

	public void setISO(int iso) throws MediaException;

	public int[] getSupportedExposureCompensations();

	public int getExposureCompensation();

	public void setExposureCompensation(int ec) throws MediaException;

	public int getExposureValue();

	public String[] getSupportedLightMeterings();

	public void setLightMetering(String metering);

	public String getLightMetering();
}
