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

import android.Manifest;
import android.os.Build;
import android.webkit.MimeTypeMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.microedition.io.Connector;
import javax.microedition.media.protocol.DataSource;
import javax.microedition.media.protocol.SourceStream;
import javax.microedition.media.tone.ToneManager;
import javax.microedition.util.ContextHolder;

public class Manager {
	public static final String TONE_DEVICE_LOCATOR = "device://tone";
	public static final String MIDI_DEVICE_LOCATOR = "device://midi";

	private static final String FILE_LOCATOR = "file://";
	private static final String CAPTURE_AUDIO_LOCATOR = "capture://audio";
	private static final String CAPTURE_VIDEO_LOCATOR = "capture://video";
	private static final String CAPTURE_IMAGE_LOCATOR = "capture://image";
	private static final TimeBase DEFAULT_TIMEBASE = () -> System.nanoTime() / 1000L;

	public static Player createPlayer(String locator) throws IOException {
		if (locator == null) {
			throw new IllegalArgumentException();
		}
		if (locator.equals(MIDI_DEVICE_LOCATOR)) {
			return new MidiPlayer();
		} else if (locator.equals(TONE_DEVICE_LOCATOR)) {
			return new TonePlayer();
		} else if (locator.startsWith(FILE_LOCATOR)) {
			InputStream stream = Connector.openInputStream(locator);
			String extension = locator.substring(locator.lastIndexOf('.') + 1);
			String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			return createPlayer(stream, type);
		} else if (locator.startsWith(CAPTURE_AUDIO_LOCATOR) &&
				ContextHolder.requestPermission(Manifest.permission.RECORD_AUDIO)) {
			return new RecordPlayer();
		} else if ((locator.startsWith(CAPTURE_IMAGE_LOCATOR) || locator.startsWith(CAPTURE_VIDEO_LOCATOR)) &&
				ContextHolder.requestPermission(Manifest.permission.CAMERA) &&
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return new CameraPlayer();
		} else {
			return new BasePlayer();
		}
	}

	public static Player createPlayer(DataSource source) throws IOException, MediaException {
		if (source == null) {
			throw new IllegalArgumentException();
		}
		String type = source.getContentType();
		String[] supportedTypes = getSupportedContentTypes(null);
		if (type != null && Arrays.asList(supportedTypes).contains(type.toLowerCase())) {
			source.connect();
			SourceStream[] sourceStreams = source.getStreams();
			if (sourceStreams == null || sourceStreams.length == 0) {
				throw new MediaException();
			}
			SourceStream sourceStream = sourceStreams[0];
			InputStream stream = new InternalSourceStream(sourceStream);
			return new MicroPlayer(new InternalDataSource(stream, type));
		} else {
			return new BasePlayer();
		}
	}

	public static Player createPlayer(final InputStream stream, String type) throws IOException {
		if (stream == null) {
			throw new IllegalArgumentException();
		}
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
				"audio/mp4", "audio/mmf", "audio/x-tone-seq"};
	}

	public static String[] getSupportedProtocols(String str) {
		return new String[]{"device", "file", "http"};
	}

	public static TimeBase getSystemTimeBase() {
		return DEFAULT_TIMEBASE;
	}

	public synchronized static void playTone(int note, int duration, int volume)
			throws MediaException {
		ToneManager.play(note, duration, volume);
	}
}
