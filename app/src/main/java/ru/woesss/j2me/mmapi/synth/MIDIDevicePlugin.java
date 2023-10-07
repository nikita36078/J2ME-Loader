/*
 * Copyright 2023 Yury Kharchenko
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

package ru.woesss.j2me.mmapi.synth;

import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.protocol.DataSource;

import ru.woesss.j2me.mmapi.synth.eas.LibEAS;

public class MIDIDevicePlugin extends SynthPlugin {
	public MIDIDevicePlugin() {
		super(new LibEAS());
	}

	@Override
	public Player createPlayer(DataSource dataSource) {
		return null;
	}

	@Override
	public Player createPlayer(String locator) {
		if (Manager.MIDI_DEVICE_LOCATOR.equals(locator)) {
			return super.createPlayer(locator);
		} else {
			return null;
		}
	}
}
