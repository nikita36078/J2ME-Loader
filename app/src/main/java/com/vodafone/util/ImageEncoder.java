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

	public static ImageEncoder createEncoder(int flags) {
		return new ImageEncoder();
	}

	public byte[] encodeOffscreen(Image image, int x, int y, int width, int height) throws IOException {
		Image resultImage = Image.createImage(image, x, y, width, height, 0);
		Bitmap bmp = resultImage.getBitmap();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
		byte[] byteArray = stream.toByteArray();
		stream.close();
		return byteArray;
	}
}
