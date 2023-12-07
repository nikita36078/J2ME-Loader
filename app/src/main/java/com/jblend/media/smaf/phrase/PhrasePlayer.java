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

public class PhrasePlayer {
	private static final PhrasePlayer phrasePlayer = new PhrasePlayer();

	protected int trackCount;
	protected int audioTrackCount;

	public static PhrasePlayer getPlayer() {
		return phrasePlayer;
	}

	public void disposePlayer() {}

	public PhraseTrack getTrack() {
		return new PhraseTrack(trackCount++);
	}

	public AudioPhraseTrack getAudioTrack() {
		return new AudioPhraseTrack(audioTrackCount++);
	}

	public int getTrackCount() {
		return 16;
	}

	public int getAudioTrackCount() {
		return 16;
	}

	public PhraseTrack getTrack(int track) {
		return null;
	}

	public AudioPhraseTrack getAudioTrack(int track) {
		return null;
	}

	public void disposeTrack(PhraseTrack t) {}

	public void disposeAudioTrack(AudioPhraseTrack t) {}

	public void kill() {}

	public void pause() {}

	public void resume() {}
}
