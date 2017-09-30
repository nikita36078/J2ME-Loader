/*
 * J2ME Loader
 * Copyright (C) 2017 Nikita Shakarun
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nokia.mid.sound;

public class Sound {
	public static final int FORMAT_TONE = 1;
	public static final int FORMAT_WAV = 5;
	public static final int SOUND_PLAYING = 0;
	public static final int SOUND_STOPPED = 1;
	public static final int SOUND_UNINITIALIZED = 3;

	public Sound(int i, long j) {
	}

	public Sound(byte[] bArr, int i) {
	}

	public static int getConcurrentSoundCount(int i) {
		return 0;
	}

	public static int[] getSupportedFormats() {
		return new int[0];
	}

	public int getGain() {
		return 0;
	}

	public int getState() {
		return 1;
	}

	public void init(int i, long j) {
	}

	public void init(byte[] bArr, int i) {
	}

	public synchronized void play(int i) {
	}

	public void release() {
	}

	public void resume() {
	}

	public void setGain(int i) {
	}

	public void setSoundListener(SoundListener soundListener) {
	}

	public void stop() {
	}
}
