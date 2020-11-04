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

package javax.microedition.media;

import android.media.MediaPlayer;

import java.io.IOException;

public class AndroidPlayer extends MediaPlayer {
	private boolean loaded;
	private String path;
	private float leftVolume, rightVolume;
	private int timePos;
	private boolean looping;

	public AndroidPlayer() {
		super();
		this.leftVolume = 1.0f;
		this.rightVolume = 1.0f;
	}

	@Override
	public void setDataSource(String path) throws IOException, IllegalArgumentException, IllegalStateException, SecurityException {
		this.path = path;
	}

	@Override
	public void seekTo(int msec) throws IllegalStateException {
		if (loaded) {
			super.seekTo(msec);
		}
		this.timePos = msec;
	}

	@Override
	public int getCurrentPosition() {
		if (loaded) {
			return super.getCurrentPosition();
		} else {
			return timePos;
		}
	}

	@Override
	public int getDuration() {
		if (loaded) {
			return super.getDuration();
		} else {
			return 0;
		}
	}

	@Override
	public void setLooping(boolean looping) {
		if (loaded) {
			super.setLooping(looping);
		}
		this.looping = looping;
	}

	@Override
	public void setVolume(float leftVolume, float rightVolume) {
		if (loaded) {
			super.setVolume(leftVolume, rightVolume);
		}
		this.leftVolume = leftVolume;
		this.rightVolume = rightVolume;
	}

	@Override
	public void start() throws IllegalStateException {
		load();
		timePos = 0;
		super.start();
	}

	@Override
	public void reset() {
		if (loaded) {
			super.reset();
			loaded = false;
		}
	}

	private void load() {
		if (!loaded) {
			try {
				super.setDataSource(path);
				super.prepare();
				super.setVolume(leftVolume, rightVolume);
				super.setLooping(looping);
				super.seekTo(timePos);
			} catch (IOException e) {
				e.printStackTrace();
			}
			loaded = true;
		}
	}
}
