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

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.FFprobeKit;
import com.arthenica.ffmpegkit.MediaInformation;
import com.arthenica.ffmpegkit.MediaInformationSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.StreamInformation;

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
	private String type;

	public InternalDataSource(InputStream stream, String type) throws IllegalArgumentException, IOException {
		super(null);

		String extension = "." + MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
		this.mediaFile = File.createTempFile("media", extension, ContextHolder.getCacheDir());
		this.type = type;

		final RandomAccessFile raf = new RandomAccessFile(mediaFile, "rw");

		final String name = mediaFile.getName();
		Log.d(TAG, "Starting media pipe: " + name);

		int length = stream.available();
		if (length >= 0) {
			raf.setLength(length);
			Log.d(TAG, "Changing file size to " + length + " bytes: " + name);
		}

		byte[] buf = new byte[0x10000];
		int read;
		try {
			while (true) {
				read = stream.read(buf);
				if (read > 0) {
					raf.write(buf, 0, read);
				} else if (read < 0) {
					break;
				}
			}
			raf.close();
			Log.d(TAG, "Media pipe closed: " + name);
		} catch (IOException e) {
			Log.d(TAG, "Media pipe failure: " + e.toString());
		} finally {
			stream.close();
		}

		try {
			convert();
		} catch (Throwable e) {
			// Thrown on fake Oppo devices
			e.printStackTrace();
		}
	}

	private void convert() {
		MediaInformationSession session = FFprobeKit.getMediaInformation(mediaFile.getPath());
		MediaInformation mediaInformation = session.getMediaInformation();
		if (mediaInformation != null) {
			StreamInformation streamInformation = mediaInformation.getStreams().get(0);
			if (streamInformation.getCodec().contains("adpcm")) {
				String newName = mediaFile.getPath() + ".wav";
				String cmd = "-i " + mediaFile.getPath() + " -acodec pcm_u8 -ar 16000 " + newName;
				FFmpegSession ffmpegSession = FFmpegKit.execute(cmd);
				ReturnCode rc = ffmpegSession.getReturnCode();
				if (rc.isValueSuccess()) {
					Log.i(TAG, "Command execution completed successfully.");
					mediaFile.delete();
					mediaFile = new File(newName);
				} else {
					Log.i(TAG, String.format(
							"Command execution failed with rc=%d and the output below.", rc.getValue()));
				}
			}
		}
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
	}

	@Override
	public void disconnect() {
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
