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

package com.sprintpcs.media;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;

public class Clip {
	private String contentType;
	private int priority;
	private int vibration;
	private byte[] data;
	private String str;

	public Clip(String str, String contentType, int priority, int vibration) throws IOException {
		this.str = str;
		this.contentType = contentType;
		this.priority = priority;
		this.vibration = vibration;
	}

	public Clip(byte[] data, String contentType, int priority, int vibration) throws IOException {
		this.data = data;
		this.contentType = contentType;
		this.priority = priority;
		this.vibration = vibration;
	}

	public String getContentType() {
		return contentType;
	}

	public String getStateString() {
		return "Inactive";
	}

	protected Player getPlayer() {
		Player player = null;
		try {
			if (data != null) {
				player = Manager.createPlayer(new ByteArrayInputStream(data), contentType);
			} else {
				player = Manager.createPlayer(str);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (MediaException e) {
			throw new RuntimeException(e);
		}
		return player;
	}

	protected int getPriority() {
		return priority;
	}

	protected int getVibration() {
		return vibration;
	}
}
