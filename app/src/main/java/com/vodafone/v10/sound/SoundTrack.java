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

package com.vodafone.v10.sound;

import javax.microedition.media.MediaException;
import javax.microedition.media.Player;

public class SoundTrack {
	public static final int NO_DATA = 0;
	public static final int PAUSED = 3;
	public static final int PLAYING = 2;
	public static final int READY = 1;
	private static final int MAX_VOLUME = 127;
	private Sound snd;
	private Player player;
	private int state;
	private int loopCount;
	private int volume = MAX_VOLUME;
	private SoundTrackListener listener;

	public Sound getSound() {
		return snd;
	}

	public void setSound(Sound p) {
		this.snd = p;
		this.player = p.getPlayer();
	}

	public int getState() {
		return state;
	}

	public void setVolume(int value) {
		this.volume = value;
		if (volume < 0) {
			volume = 0;
		}
		if (volume > MAX_VOLUME) {
			volume = MAX_VOLUME;
		}
	}

	public void stop() {
		try {
			player.stop();
			state = READY;
		} catch (MediaException e) {
			e.printStackTrace();
		}
	}

	public void play(int loop) {
		try {
			if (state != PLAYING) {
				loopCount = loop;
				if (loop == 0) {
					loop = -1;
				}
				player.setLoopCount(loop);
				player.start();
				state = PLAYING;
			}
		} catch (MediaException e) {
			e.printStackTrace();
		}
	}

	public void removeSound() {
		this.snd = null;
	}

	public void setEventListener(SoundTrackListener l) {
		this.listener = l;
	}

	private void postEvent(int event) {
		if (listener != null) {
			listener.eventOccurred(event);
		}
	}
}
