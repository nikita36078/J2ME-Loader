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

package com.motorola.midi;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.util.ContextHolder;

public class MidiPlayer {
	public static final int PLAY_CONTINUOUS = 0;
	public static final int PLAY_ONCE = 1;

	private static Player player;

	public static void play(String filename, int mode) {
		String location = filename.substring(filename.indexOf('/'));
		if (player != null) {
			player.close();
		}
		try {
			InputStream is = ContextHolder.getResourceAsStream(null, location);
			player = Manager.createPlayer(is, "audio/midi");
			if (mode == PLAY_CONTINUOUS) {
				player.setLoopCount(-1);
			}
			player.start();
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (MediaException e) {
			e.printStackTrace();
		}
	}

	public static void stop() {
		if (player != null) {
			player.close();
		}
	}

	public static void playEffect(int note, int key_pressure, int channel_vol, int instrument, int channel) {
	}
}
