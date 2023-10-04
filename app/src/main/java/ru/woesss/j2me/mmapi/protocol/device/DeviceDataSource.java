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

package ru.woesss.j2me.mmapi.protocol.device;

import java.io.IOException;

import javax.microedition.media.Control;
import javax.microedition.media.Manager;
import javax.microedition.media.protocol.DataSource;
import javax.microedition.media.protocol.SourceStream;

public class DeviceDataSource extends DataSource {

	private final String type;

	public DeviceDataSource(String locator) {
		super(locator);
		switch (locator) {
			case Manager.MIDI_DEVICE_LOCATOR:
				type = "audio/midi";
				break;
			case Manager.TONE_DEVICE_LOCATOR:
				type = "audio/x-tone-seq";
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public String getContentType() {
		return type;
	}

	@Override
	public void connect() throws IOException {

	}

	@Override
	public void disconnect() {

	}

	@Override
	public void start() throws IOException {

	}

	@Override
	public void stop() throws IOException {

	}

	@Override
	public SourceStream[] getStreams() {
		return new SourceStream[0];
	}

	@Override
	public Control[] getControls() {
		return new Control[0];
	}

	@Override
	public Control getControl(String control) {
		return null;
	}
}
