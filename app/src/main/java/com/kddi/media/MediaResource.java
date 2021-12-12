/*
 * Copyright 2021 ohayoyogi
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

package com.kddi.media;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.microedition.media.MMFConverter;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;

public class MediaResource {

	private Player player;
	private String type;

	public Player _getPlayer() {
		// workaround
		return this.player;
	}

	public MediaResource(String url) {
		this.player = null;
	}

	public MediaResource(byte[] resource, String disposition) {
		this.player = null;
		this.type = disposition;
		String mime;
		switch (disposition) {
			case "dev4anm":
			case "devmfan":
			case "devm39z":
			case "devm53z":
				mime = "audio/midi";
				try {
					MMFConverter converter = new MMFConverter();
					byte[] midiDat = converter.convertToMDI(resource);
					try (ByteArrayInputStream bis = new ByteArrayInputStream(midiDat)) {
						player = Manager.createPlayer(bis, mime);
						player.realize();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (MediaException e) {
						e.printStackTrace();
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
		}
	}

	public MediaPlayerBox[] getPlayer() {
		return new MediaPlayerBox[]{};
	}

	public String getType() {
		return this.type;
	}

	public void dispose() {
		if (this.player != null) {
			this.player.close();
			this.player = null;
		}
	}

	@Override
	public String toString() {
		return super.toString();
	}
}
