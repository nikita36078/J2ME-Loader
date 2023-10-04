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

package ru.woesss.j2me.mmapi.tsf;

import android.util.Log;

import javax.microedition.media.Player;
import javax.microedition.media.protocol.DataSource;
import javax.microedition.shell.MicroLoader;

import ru.woesss.j2me.mmapi.Plugin;
import ru.woesss.j2me.mmapi.protocol.device.DeviceDataSource;

public class PluginTSF implements Plugin {
	private static final String TAG = "TsfPlugin";

	public PluginTSF() {
		String soundBank = MicroLoader.getSoundBank();
		if (soundBank == null) {
			throw new IllegalStateException("Sound Bank not selected");
		}
		TinySoundFont.init(soundBank);
	}

	@Override
	public Player createPlayer(DataSource dataSource) {
		try {
			return new PlayerTSF(dataSource);
		} catch (Exception e) {
			Log.w(TAG, "createPlayer: ", e);
			return null;
		}
	}

	@Override
	public Player createPlayer(String locator) {
		try {
			return new PlayerTSF(new DeviceDataSource(locator));
		} catch (Exception e) {
			Log.w(TAG, "createPlayer: ", e);
			return null;
		}
	}
}
