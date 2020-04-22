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

package ru.playsoftware.j2meloader.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.CRC32;

public class PNGUtils {

	private static final byte[] PNG_SIGNATURE = new byte[]{-119, 80, 78, 71, 13, 10, 26, 10};
	private static final byte[] END_TYPE = new byte[]{'I', 'E', 'N', 'D'};

	public static Bitmap getFixedBitmap(InputStream stream) {
		Bitmap b = null;
		try {
			byte[] data = IOUtils.toByteArray(stream);
			b = getFixedBitmap(data, 0, data.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return b;
	}

	public static Bitmap getFixedBitmap(byte[] imageData, int imageOffset, int imageLength) {
		Bitmap b = BitmapFactory.decodeByteArray(imageData, imageOffset, imageLength);
		if (b == null) {
			try (ByteArrayInputStream stream = new ByteArrayInputStream(imageData, imageOffset, imageLength)) {
				byte[] data = fixPNG(stream);
				b = BitmapFactory.decodeByteArray(data, 0, data.length);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return b;
	}

	private static byte[] fixPNG(InputStream stream) throws IOException {
		DataInputStream inputStream = new DataInputStream(stream);
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream outputStream = new DataOutputStream(byteStream);
		byte[] signature = new byte[8];
		inputStream.read(signature);
		if (!Arrays.equals(signature, PNG_SIGNATURE)) {
			throw new IOException("Not a PNG file");
		}
		outputStream.write(signature);
		boolean end = false;
		byte[] type = new byte[4];
		CRC32 crc32 = new CRC32();
		while (!end) {
			int length = inputStream.readInt();
			inputStream.read(type);
			byte[] data = new byte[length];
			inputStream.read(data);
			int crc = inputStream.readInt();

			crc32.reset();
			crc32.update(type);
			crc32.update(data);
			int calculatedCrc = (int) crc32.getValue();

			outputStream.writeInt(length);
			outputStream.write(type);
			outputStream.write(data);
			outputStream.writeInt(calculatedCrc);
			end = Arrays.equals(type, END_TYPE);
		}
		return byteStream.toByteArray();
	}
}
