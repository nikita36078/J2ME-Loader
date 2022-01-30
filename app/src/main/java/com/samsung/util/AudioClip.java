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

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;
import javax.microedition.shell.AppClassLoader;

public class AudioClip {
	private static final String TAG = AudioClip.class.getName();
	public static final int TYPE_MMF = 1;
	public static final int TYPE_MP3 = 2;
	public static final int TYPE_MIDI = 3;

	private static final String[] FORMATS = {"audio/mmf", "audio/mp3", "audio/midi"};

	private Player player;

	public AudioClip(int type, String filename) throws IOException {
		if (type < 1 || type > 3) {
			// SnowballFight uses track number here
			Log.e(TAG, "Invalid AudioClipType: " + type);
		}
		if (filename == null) {
			throw new NullPointerException();
		}
		InputStream stream = AppClassLoader.getResourceAsStream(null, filename);
		player = Manager.createPlayer(stream, "audio/midi");
	}

	public AudioClip(int type, byte[] audioData, int audioOffset, int audioLength) {
		if (type < 1 || type > 3) {
			// SnowballFight uses track number here
			Log.e(TAG, "Invalid AudioClipType: " + type);
		}
		if (audioData == null) {
			throw new NullPointerException();
		}
		if (audioOffset < 0 || audioLength < 0 || audioOffset + audioLength > audioData.length) {
			throw new ArrayIndexOutOfBoundsException();
		}
		ByteArrayInputStream stream = new ByteArrayInputStream(audioData, audioOffset, audioLength);
		try {
			player = Manager.createPlayer(stream, "audio/midi");
		} catch (IOException e) {
			Log.e(TAG, "AudioClip: ", e);
		}
	}

	public static boolean isSupported() {
		return true;
	}

	public void pause() {
		try {
			player.stop();
		} catch (MediaException e) {
			Log.e(TAG, "pause: ", e);
		}
	}

	public void play(int loop, int volume) {
		if (loop < 0 || loop > 255 || volume < 0 || volume > 5) {
			throw new IllegalArgumentException();
		}
		try {
			if (loop == 0) {
				loop = -1;
			}
			if (player.getState() == Player.STARTED) {
				player.stop();
			}
			player.setLoopCount(loop);
			((VolumeControl) player).setLevel(volume * 20);
			player.start();
		} catch (MediaException e) {
			Log.e(TAG, "play: ", e);
		}
	}

	public void resume() {
		try {
			player.start();
		} catch (MediaException e) {
			Log.e(TAG, "resume: ", e);
		}
	}

	public void stop() {
		try {
			player.stop();
		} catch (MediaException e) {
			Log.e(TAG, "stop: ", e);
		}
	}
}
