/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017-2020 Nikita Shakarun
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

package javax.microedition.media;

import android.util.Log;

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

import ru.woesss.j2me.mmapi.FileCacheDataSource;

class InternalDataSource extends FileCacheDataSource {
	private static final String TAG = InternalDataSource.class.getName();

	InternalDataSource(InputStream stream, String type) throws IllegalArgumentException, IOException {
		super(type);

		final String name = mediaFile.getName();
		Log.d(TAG, "Starting media pipe: " + name);

		try (RandomAccessFile raf = new RandomAccessFile(mediaFile, "rw")) {
			int length = stream.available();
			if (length >= 0) {
				raf.setLength(length);
				Log.d(TAG, "Changing file size to " + length + " bytes: " + name);
			}
			byte[] buf = new byte[4096];
			int read;
			while ((read = stream.read(buf)) != -1) {
				raf.write(buf, 0, read);
			}
		} catch (IOException e) {
			Log.d(TAG, "Media pipe failure: " + e);
			throw e;
		}
		Log.d(TAG, "Media pipe closed: " + name);

		convert();
	}

	private void convert() {
		try {
			String path = mediaFile.getPath();
			MediaInformationSession session = FFprobeKit.getMediaInformation(path);
			MediaInformation mediaInformation = session.getMediaInformation();
			if (mediaInformation != null) {
				StreamInformation streamInformation = mediaInformation.getStreams().get(0);
				if (streamInformation.getCodec().contains("adpcm")) {
					int dot = path.lastIndexOf('.');
					String newName = (dot == -1 ? path : path.substring(0, dot)) + ".wav";
					String cmd = "-i " + path + " -acodec pcm_u8 -ar 16000 " + newName;
					FFmpegSession ffmpegSession = FFmpegKit.execute(cmd);
					ReturnCode rc = ffmpegSession.getReturnCode();
					if (rc.isValueSuccess()) {
						Log.i(TAG, "Command execution completed successfully.");
						if (mediaFile.delete()) {
							Log.w(TAG, "convert: error delete file=" + mediaFile);
						}
						mediaFile = new File(newName);
						mediaFile.deleteOnExit();
					} else {
						Log.i(TAG, String.format(
								"Command execution failed with rc=%d and the output below.", rc.getValue()));
					}
				}
			}
		} catch (Throwable t) {
			Log.e(TAG, "FFmpeg error", t);
		}
	}
}
