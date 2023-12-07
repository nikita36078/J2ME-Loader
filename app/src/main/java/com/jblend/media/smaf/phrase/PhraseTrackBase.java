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

package com.jblend.media.smaf.phrase;

abstract class PhraseTrackBase {
	public static final int NO_DATA = 1;
	public static final int READY = 2;
	public static final int PLAYING = 3;
	public static final int PAUSED = 5;
	public static final int DEFAULT_VOLUME = 100;
	public static final int DEFAULT_PANPOT = 64;

	PhraseTrackBase(int id) {}

	public void removePhrase() {}

	public void play() {}

	public void play(int loop) {}

	public void stop() {}

	public void pause() {}

	public void resume() {}

	public int getState() {
		return PLAYING;
	}

	public void setVolume(int value) {}

	public int getVolume() {
		return 0;
	}

	public void setPanpot(int value) {}

	public int getPanpot() {
		return 0;
	}

	public void mute(boolean mute) {}

	public boolean isMute() {
		return true;
	}

	public int getID() {
		return 0;
	}

	public void setEventListener(PhraseTrackListener l) {}
}
