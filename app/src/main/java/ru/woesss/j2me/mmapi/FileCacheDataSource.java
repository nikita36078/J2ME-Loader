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

package ru.woesss.j2me.mmapi;

import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.IOException;

import javax.microedition.media.Control;
import javax.microedition.media.protocol.DataSource;
import javax.microedition.media.protocol.SourceStream;
import javax.microedition.util.ContextHolder;

public class FileCacheDataSource extends DataSource {
	private static final String TAG = FileCacheDataSource.class.getSimpleName();

	private final String type;
	protected File mediaFile;

	/**
     * @param type source mime type
     * @throws IOException if I/O problem occurs when creating cache file
     */
	public FileCacheDataSource(String type) throws IOException {
		this(type, null);
	}

	/**
	 * @param type source mime type
	 * @param ext  extension for cache file, if {@code null} generate from type
	 * @throws IOException if the I/O problem occurs when creating the cache file
	 */
	public FileCacheDataSource(String type, String ext) throws IOException {
		super(null);
		mediaFile = createCacheFile(type, ext);
		this.type = type;
	}

	private static File createCacheFile(String type, String ext) throws IOException {
		if (ext == null) {
			ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
			if (ext == null) {
				ext = "tmp";
			}
		}
		File file = File.createTempFile("media", "." + ext, ContextHolder.getCacheDir());
		file.deleteOnExit();
		return file;
	}

	@Override
	public String getContentType() {
		return type;
	}

	@Override
	public String getLocator() {
		return mediaFile.getAbsolutePath();
	}

	@Override
	public void connect() throws IOException {
	}

	@Override
	public void disconnect() {
		if (mediaFile.delete()) {
			Log.d(TAG, "Temp file deleted: " + getLocator());
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
