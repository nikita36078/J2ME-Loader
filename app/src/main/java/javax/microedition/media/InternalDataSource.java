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

import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import javax.microedition.media.protocol.DataSource;
import javax.microedition.media.protocol.SourceStream;
import javax.microedition.util.ContextHolder;

public class InternalDataSource extends DataSource {
	private static final String TAG = InternalDataSource.class.getName();

	private File mediaFile;
	private InputStream stream;
	private String type;

	public InternalDataSource(InputStream stream, String type) throws IllegalArgumentException, IOException {
		super(null);
		if (type != null) {
			type = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
			if (type != null) {
				type = "." + type;
			}
		}
		if (stream == null) {
			throw new IllegalArgumentException();
		}

		this.mediaFile = File.createTempFile("media", type, ContextHolder.getCacheDir());
		this.stream = stream;
		this.type = type;
	}

	@Override
	public String getLocator() {
		return mediaFile.getAbsolutePath();
	}

	@Override
	public String getContentType() {
		return type;
	}

	@Override
	public void connect() throws IOException {
		final RandomAccessFile raf = new RandomAccessFile(mediaFile, "rw");

		final String name = mediaFile.getName();
		Log.d(TAG, "Starting media pipe: " + name);

		int length = stream.available();
		if (length >= 0) {
			raf.setLength(length);
			Log.d(TAG, "Changing file size to " + length + " bytes: " + name);
		}

		final Object sync = new Object();

		byte[] buf = new byte[0x10000];
		int read;
		try {
			while (true) {
				read = stream.read(buf);
				if (read > 0) {
					synchronized (sync) {
						raf.write(buf, 0, read);
					}
				} else if (read < 0) {
					break;
				}
			}
			raf.close();
			Log.d(TAG, "Media pipe closed: " + name);
		} catch (IOException e) {
			Log.d(TAG, "Media pipe failure: " + e.toString());
		}
	}

	@Override
	public void disconnect() {
		try {
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (mediaFile.delete()) {
			Log.d(TAG, "Temp file deleted: " + mediaFile.getAbsolutePath());
		}
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
