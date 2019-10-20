/*
 * Copyright 2017-2018 Nikita Shakarun
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

package com.samsung.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.util.ContextHolder;

public class AudioClip {
	public static final int TYPE_MIDI = 3;
	public static final int TYPE_MMF = 1;
	public static final int TYPE_MP3 = 2;

	private Player player;

	public AudioClip(int type, java.lang.String filename) throws IOException {
		try {
			InputStream stream = ContextHolder.getResourceAsStream(null, filename);
			player = Manager.createPlayer(stream, "audio/midi");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public AudioClip(int type, byte[] audioData, int audioOffset, int audioLength) {
		try {
			player = Manager.createPlayer(new ByteArrayInputStream(audioData), "audio/mmf");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean isSupported() {
		return true;
	}

	public void pause() {
		try {
			player.stop();
		} catch (MediaException e) {
			e.printStackTrace();
		}
	}

	public void play(int loop, int volume) {
		try {
			if (loop == 0) {
				loop = -1;
			}
			if (player.getState() == Player.STARTED) {
				player.stop();
			}
			player.setLoopCount(loop);
			player.start();
		} catch (MediaException e) {
			e.printStackTrace();
		}
	}

	public void resume() {
		try {
			player.start();
		} catch (MediaException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		player.close();
	}
}
