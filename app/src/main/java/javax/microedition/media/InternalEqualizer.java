/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017 Nikita Shakarun
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

import javax.microedition.amms.control.audioeffect.EqualizerControl;

public class InternalEqualizer implements EqualizerControl {

	@Override
	public void setScope(int scope) throws MediaException {
	}

	@Override
	public int getScope() {
		return 0;
	}

	@Override
	public String[] getPresetNames() {
		return new String[0];
	}

	@Override
	public void setPreset(String preset) {
	}

	@Override
	public String getPreset() {
		return null;
	}

	@Override
	public void setEnabled(boolean enable) {
	}

	@Override
	public boolean isEnabled() {
		return false;
	}

	@Override
	public void setEnforced(boolean enforced) {
	}

	@Override
	public boolean isEnforced() {
		return false;
	}

	@Override
	public int getNumberOfBands() {
		return 0;
	}

	@Override
	public int getBand(int frequency) {
		return 0;
	}

	@Override
	public int getCenterFreq(int band) {
		return 0;
	}

	@Override
	public int getMinBandLevel() {
		return 0;
	}

	@Override
	public int getMaxBandLevel() {
		return 0;
	}

	@Override
	public void setBandLevel(int level, int band) {
	}

	@Override
	public int getBandLevel(int band) {
		return 0;
	}
}