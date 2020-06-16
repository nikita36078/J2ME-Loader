/*
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

package com.nokia.mid.sound;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;

public class Sound {
	public static final int FORMAT_TONE = 1;
	public static final int FORMAT_WAV = 5;
	public static final int SOUND_PLAYING = 0;
	public static final int SOUND_STOPPED = 1;
	public static final int SOUND_UNINITIALIZED = 3;

	private Player player;
	private int state;
	private SoundListener soundListener;

	public Sound(int freq, long duration) {
		init(freq, duration);
	}

	public Sound(byte[] data, int type) {
		init(data, type);
	}

	public static int getConcurrentSoundCount(int type) {
		return 1;
	}

	public static int[] getSupportedFormats() {
		return new int[]{FORMAT_TONE, FORMAT_WAV};
	}

	public int getGain() {
		return -1;
	}

	public int getState() {
		return state;
	}

	public void init(int freq, long duration) {
		try {
			player = Manager.createPlayer(Manager.TONE_DEVICE_LOCATOR);
			state = SOUND_STOPPED;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void init(byte[] data, int type) {
		try {
			player = Manager.createPlayer(new ByteArrayInputStream(data), "audio/midi");
			state = SOUND_STOPPED;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void play(int loop) {
		try {
			if (loop == 0) {
				loop = -1;
			}
			if (player.getState() == Player.STARTED) {
				player.stop();
			}
			player.setLoopCount(loop);
			player.start();
			postEvent(SOUND_PLAYING);
		} catch (MediaException e) {
			e.printStackTrace();
		}
	}

	public void release() {
		player.close();
		postEvent(SOUND_UNINITIALIZED);
	}

	public void resume() {
		try {
			player.start();
			postEvent(SOUND_PLAYING);
		} catch (MediaException e) {
			e.printStackTrace();
		}
	}

	public void setGain(int i) {
	}

	public void setSoundListener(SoundListener soundListener) {
		this.soundListener = soundListener;
	}

	public void stop() {
		try {
			player.stop();
			postEvent(SOUND_STOPPED);
		} catch (MediaException e) {
			e.printStackTrace();
		}
	}

	private void postEvent(int state) {
		this.state = state;
		if (soundListener != null) {
			soundListener.soundStateChanged(this, state);
		}
	}
}
