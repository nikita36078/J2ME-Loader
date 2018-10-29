/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017-2018 Nikita Shakarun
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.microedition.media.protocol.DataSource;

public class Manager {
	public static final String TONE_DEVICE_LOCATOR = "device://tone";
	public static final String MIDI_DEVICE_LOCATOR = "device://midi";

	public static Player createPlayer(String locator) throws IOException {
		if (locator.equals(MIDI_DEVICE_LOCATOR)) {
			return new MidiPlayer();
		} else {
			return new BasePlayer();
		}
	}

	public static Player createPlayer(DataSource source) throws IOException {
		return new BasePlayer();
	}

	public static Player createPlayer(final InputStream stream, String type) throws IOException {
		String[] supportedTypes = getSupportedContentTypes(null);
		if (type != null && Arrays.asList(supportedTypes).contains(type.toLowerCase())) {
			return new MicroPlayer(new InternalDataSource(stream, type));
		} else {
			return new BasePlayer();
		}
	}

	public static String[] getSupportedContentTypes(String str) {
		return new String[]{"audio/wav", "audio/x-wav", "audio/midi", "audio/x-midi",
				"audio/mpeg", "audio/aac", "audio/amr", "audio/amr-wb", "audio/mp3",
				"audio/mp4", "video/mpeg", "video/mp4", "video/mpeg4", "video/3gpp"};
	}

	public static String[] getSupportedProtocols(String str) {
		return new String[]{"device", "file", "http"};
	}

	public synchronized static void playTone(int frequency, int time, int volume)
			throws MediaException {
		//TODO method stub
	}
}
