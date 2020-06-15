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
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.ImageLineSetDefault;
import ar.com.hjg.pngj.PngReaderInt;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;

public class PNGUtils {

	private static final byte[] PNG_SIGNATURE = new byte[]{-119, 80, 78, 71, 13, 10, 26, 10};

	public static Bitmap getFixedBitmap(InputStream stream) {
		Bitmap b = null;
		try {
			byte[] data = IOUtils.toByteArray(stream);
			b = getFixedBitmap(data, 0, data.length);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return b;
	}

	public static Bitmap getFixedBitmap(byte[] imageData, int imageOffset, int imageLength) {
		Bitmap b = null;
		byte[] signature = Arrays.copyOfRange(imageData, imageOffset, imageOffset + PNG_SIGNATURE.length);
		if (Arrays.equals(signature, PNG_SIGNATURE)) {
			try (ByteArrayInputStream stream = new ByteArrayInputStream(imageData, imageOffset, imageLength)) {
				b = fixPNG(stream);
			} catch (Exception e) {
				e.printStackTrace();
				b = BitmapFactory.decodeByteArray(imageData, imageOffset, imageLength);
			}
		} else {
			b = BitmapFactory.decodeByteArray(imageData, imageOffset, imageLength);
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
		int[] buf = new int[width];
		for (int i = 0; i < height; i++) {
			ImageLineInt lineInt = lineSet.getImageLine(i);
			ImageLineHelper.scaleUp(lineInt);
			int[] r = lineToARGB32(lineInt, plte, trns, buf);
			for (int j = 0; j < width; j++) {
				pix[i * width + j] = r[j];
			}
		}
		reader.end();
		return Bitmap.createBitmap(pix, width, height, Bitmap.Config.ARGB_8888);
	}

	private static int[] lineToARGB32(ImageLineInt line, PngChunkPLTE pal, PngChunkTRNS trns, int[] buf) {
		boolean alphachannel = line.imgInfo.alpha;
		int[] scanline = line.getScanline();
		int cols = line.imgInfo.cols;
		if (buf == null || buf.length < cols)
			buf = new int[cols];
		int index, rgb, alpha, ga, g;
		if (line.imgInfo.indexed) { // palette
			int nindexesWithAlpha = trns != null ? trns.getPalletteAlpha().length : 0;
			for (int c = 0; c < cols; c++) {
				index = scanline[c];
				rgb = pal.getEntry(index);
				alpha = index < nindexesWithAlpha ? trns.getPalletteAlpha()[index] : 255;
				buf[c] = (alpha << 24) | rgb;
			}
		} else if (line.imgInfo.greyscale) { // gray
			ga = trns != null ? trns.getGray() : -1;
			for (int c = 0, c2 = 0; c < cols; c++) {
				g = scanline[c2++];
				alpha = alphachannel ? scanline[c2++] : (g != ga ? 255 : 0);
				buf[c] = (alpha << 24) | g | (g << 8) | (g << 16);
			}
		} else if (line.imgInfo.bitDepth == 16) { // true color
			ga = trns != null ? trns.getRGB888() : -1;
			for (int c = 0, c2 = 0; c < cols; c++) {
				rgb = ((scanline[c2++] & 0xFF00) << 8) | (scanline[c2++] & 0xFF00)
						| ((scanline[c2++] & 0xFF00) >> 8);
				alpha = alphachannel ? ((scanline[c2++] & 0xFF00) >> 8) : (rgb != ga ? 255 : 0);
				buf[c] = (alpha << 24) | rgb;
			}
		} else { // true color
			ga = trns != null ? trns.getRGB888() : -1;
			for (int c = 0, c2 = 0; c < cols; c++) {
				rgb = ((scanline[c2++]) << 16) | ((scanline[c2++]) << 8)
						| (scanline[c2++]);
				alpha = alphachannel ? scanline[c2++] : (rgb != ga ? 255 : 0);
				buf[c] = (alpha << 24) | rgb;
			}
		}
		return buf;
	}
}
