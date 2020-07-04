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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;

public class Sound {
	private Player player;

	public Sound(byte[] data) throws IOException {
		if (data == null) {
			throw new NullPointerException("sound data is null!");
		}
		player = Manager.createPlayer(new ByteArrayInputStream(data), "audio/xmf");
		try {
			player.realize();
		} catch (MediaException e) {
			e.printStackTrace();
		}
	}

	Player getPlayer() {
		return player;
	}
}
