/*
 * Copyright 2018 Nikita Shakarun
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

package com.vodafone.util;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.microedition.lcdui.Image;

public class ImageEncoder {
	public static int FORMAT_PNG = 0;
	public static int FORMAT_JPEG = 1;
	private int format;

	public ImageEncoder(int format) {
		this.format = format;
	}

	public static ImageEncoder createEncoder(int format) {
		if (format != FORMAT_PNG && format != FORMAT_JPEG) {
			throw new IllegalArgumentException();
		}
		return new ImageEncoder(format);
	}

	public byte[] encodeOffscreen(Image src, int x, int y, int width, int height) throws IOException {
		Image resultImage = Image.createImage(src, x, y, width, height, 0);
		Bitmap bmp = resultImage.getBitmap();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		Bitmap.CompressFormat compressFormat =
				(format == FORMAT_PNG) ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
		bmp.compress(compressFormat, 100, stream);
		byte[] byteArray = stream.toByteArray();
		stream.close();
		return byteArray;
	}

	public void setJpegOption(int size) {
	}
}
