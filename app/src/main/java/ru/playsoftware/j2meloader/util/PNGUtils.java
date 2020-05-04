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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.ImageLineSetDefault;
import ar.com.hjg.pngj.PngReaderInt;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;

public class PNGUtils {

	public static Bitmap getFixedBitmap(InputStream stream) {
		Bitmap b = null;
		try {
			b = fixPNG(stream);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return b;
	}

	public static Bitmap getFixedBitmap(byte[] imageData, int imageOffset, int imageLength) {
		Bitmap b = null;
		try (ByteArrayInputStream stream = new ByteArrayInputStream(imageData, imageOffset, imageLength)) {
			b = fixPNG(stream);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return b;
	}

	private static Bitmap fixPNG(InputStream stream) throws IOException {
		PngReaderInt reader = new PngReaderInt(stream);
		reader.setCrcCheckDisabled();
		ImageInfo imageInfo = reader.imgInfo;
		int width = imageInfo.cols;
		int height = imageInfo.rows;
		PngChunkTRNS trns = reader.getMetadata().getTRNS();
		PngChunkPLTE plte = reader.getMetadata().getPLTE();
		ImageLineSetDefault<ImageLineInt> lineSet = (ImageLineSetDefault) reader.readRows();
		int[] pix = new int[width * height];
		if (imageInfo.indexed) {
			int[] buf = new int[width];
			for (int i = 0; i < height; i++) {
				ImageLineInt lineInt = lineSet.getImageLine(i);
				int[] r = palette2rgb(lineInt, plte, trns, buf);
				for (int j = 0; j < width; j++) {
					pix[i * width + j] = r[j];
				}
			}
		} else {
			for (int i = 0; i < height; i++) {
				ImageLineInt lineInt = lineSet.getImageLine(i);
				for (int j = 0; j < width; j++) {
					if (imageInfo.alpha) {
						pix[i * width + j] = ImageLineHelper.getPixelARGB8(lineInt, j);
					} else {
						pix[i * width + j] = ImageLineHelper.getPixelRGB8(lineInt, j);
						pix[i * width + j] |= 0xFF << 24;
					}
				}
			}
		}
		reader.end();
		return Bitmap.createBitmap(pix, width, height, Bitmap.Config.ARGB_8888);
	}

	private static int[] palette2rgb(IImageLine line, PngChunkPLTE pal, PngChunkTRNS trns, int[] buf) {
		boolean isalpha = trns != null;
		int channels = isalpha ? 4 : 3;
		ImageLineInt linei = (ImageLineInt) (line instanceof ImageLineInt ? line : null);
		int cols = linei.imgInfo.cols;
		int nsamples = cols;
		if (buf == null || buf.length < nsamples)
			buf = new int[nsamples];
		int nindexesWithAlpha = isalpha ? trns.getPalletteAlpha().length : 0;
		for (int c = 0; c < cols; c++) {
			int index = linei.getScanline()[c];
			buf[c] = pal.getEntry(index);
			if (isalpha) {
				int alpha = index < nindexesWithAlpha ? trns.getPalletteAlpha()[index] : 255;
				buf[c] |= alpha << 24;
			} else {
				buf[c] |= 0xFF << 24;
			}
		}
		return buf;
	}
}
