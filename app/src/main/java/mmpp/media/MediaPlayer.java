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

package mmpp.media;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;
import javax.microedition.util.ContextHolder;

public class MediaPlayer {
	private Player player;
	private boolean loop;
	private String currentVolume = String.valueOf(MAX_VOLUME);
	private static final int MAX_VOLUME = 5;

	public void setMediaLocation(String location) {
		try {
			InputStream is = ContextHolder.getResourceAsStream(null, location);
			player = Manager.createPlayer(is, "audio/midi");
			player.realize();
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (MediaException e) {
			e.printStackTrace();
		}
	}

	public void setMediaSource(byte[] buffer, int offset, int length) {
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(buffer, offset, length);
			player = Manager.createPlayer(bis, "audio/midi");
			player.realize();
			bis.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (MediaException e) {
			e.printStackTrace();
		}
	}

	public void setVolumeLevel(String level) {
		int setLevel = Integer.valueOf(level) * 100 / MAX_VOLUME;
		VolumeControl volumeControl = (VolumeControl) player.getControl("VolumeControl");
		volumeControl.setLevel(setLevel);
		currentVolume = level;
	}

	public String getVolumeLevel() {
		return currentVolume;
	}

	public void start() {
		try {
			player.start();
		} catch (MediaException e) {
			e.printStackTrace();
		}
	}

	public void pause() {
		try {
			player.stop();
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

	public void setPlayBackLoop(boolean val) {
		loop = val;
		int loopCount = val ? -1 : 1;
		player.setLoopCount(loopCount);
	}

	public boolean getPlayBackLoop() {
		return loop;
	}
}
