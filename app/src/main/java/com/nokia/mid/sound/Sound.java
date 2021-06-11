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
import javax.microedition.media.tone.MidiToneConstants;
import javax.microedition.media.tone.ToneManager;

public class Sound {
	public static final int FORMAT_TONE = 1;
	public static final int FORMAT_WAV = 5;
	public static final int SOUND_PLAYING = 0;
	public static final int SOUND_STOPPED = 1;
	public static final int SOUND_UNINITIALIZED = 3;

	private static final short[] FREQ_TABLE = {
			0, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 23, 24, 26, 27, 29,
			30, 32, 34, 36, 38, 41, 43, 45, 48, 51, 54, 57, 60, 64, 68, 72, 76, 81, 85, 90, 96,
			101, 107, 114, 120, 128, 135, 143, 152, 161, 170, 180, 191, 202, 214, 227, 240, 255,
			270, 286, 303, 321, 340, 360, 381, 404, 428, 453, 480, 509, 539, 571, 605, 641, 679,
			719, 762, 807, 855, 906, 960, 1017, 1078, 1142, 1210, 1282, 1358, 1438, 1524, 1614,
			1710, 1812, 1920, 2034, 2155, 2283, 2419, 2563, 2715, 2876, 3047, 3228, 3420, 3624,
			3839, 4067, 4309, 4565, 4837, 5125, 5429, 5752, 6094, 6456, 6840, 7247, 7678, 8134,
			8618, 9130, 9673, 10249, 10858, 11504, 12188, 12912
	};
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
		if (duration <= 0) {
			throw new IllegalArgumentException("duration = " + duration);
		}
		if (freq < 0 || freq > FREQ_TABLE[FREQ_TABLE.length - 1]) {
			throw new IllegalArgumentException("freq = " + freq);
		}

		try {
			if (player != null) {
				player.close();
			}
			int note = convertFreqToNote(freq);
			player = ToneManager.createPlayer(note, (int) duration, MidiToneConstants.TONE_MAX_VOLUME);
			state = SOUND_STOPPED;
		} catch (MediaException e) {
			e.printStackTrace();
		}
	}

	public void init(byte[] data, int type) {
		try {
			String mime;
			switch (type) {
				case FORMAT_TONE:
					ToneVerifier.fix(data);
					mime = "audio/midi";
					break;
				case FORMAT_WAV:
					mime = "audio/wav";
					break;
				default:
					throw new IllegalArgumentException();
			}
			if (player != null) {
				player.close();
			}
			player = Manager.createPlayer(new ByteArrayInputStream(data), mime);
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

	public static int convertFreqToNote(int freq) {
		int low = 0;
		int high = FREQ_TABLE.length - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			int midVal = FREQ_TABLE[mid];

			if (midVal < freq) {
				low = mid + 1;
			} else if (midVal > freq) {
				high = mid - 1;
			} else {
				return mid;
			}
		}
		if ((freq - FREQ_TABLE[low - 1]) < (FREQ_TABLE[low] - freq)) {
			return low - 1;
		} else {
			return low;
		}
	}
}
