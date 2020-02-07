package com.mascotcapsule.micro3d.v3.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

public class BitmapUtils {

	public static int getPaletteColor(InputStream inputStream) throws IOException {
		BitInputStream bis = new BitInputStream(inputStream, ByteOrder.LITTLE_ENDIAN);

		// BITMAPFILEHEADER (14 bytes)
		int fileSize;
		int imageDataOffset;
		if (bis.readShort() != 0x4D42)  // "BM"
			throw new RuntimeException("Invalid BMP signature");
		fileSize = bis.readInt();
		bis.readInt();  // Skip reserved
		imageDataOffset = bis.readInt();

		// BITMAPINFOHEADER
		int headerSize = bis.readInt();
		int width;
		int height;
		boolean topToBottom;
		int bitsPerPixel;
		int compression;
		int colorsUsed;
		if (headerSize == 40) {
			int planes;
			int colorsImportant;
			width = bis.readInt();
			height = bis.readInt();
			topToBottom = height < 0;
			height = Math.abs(height);
			planes = bis.readShort();
			bitsPerPixel = bis.readShort();
			compression = bis.readInt();
			bis.readInt();  // imageSize
			bis.readInt();
			bis.readInt();
			colorsUsed = bis.readInt();
			colorsImportant = bis.readInt();

			if (width <= 0)
				throw new RuntimeException("Invalid width: " + width);
			if (height == 0)
				throw new RuntimeException("Invalid height: " + height);
			if (planes != 1)
				throw new RuntimeException("Unsupported planes: " + planes);

			if (bitsPerPixel == 1 || bitsPerPixel == 4 || bitsPerPixel == 8) {
				if (colorsUsed == 0)
					colorsUsed = 1 << bitsPerPixel;
				if (colorsUsed > 1 << bitsPerPixel)
					throw new RuntimeException("Invalid colors used: " + colorsUsed);

			} else if (bitsPerPixel == 24 || bitsPerPixel == 32) {
				if (colorsUsed != 0)
					throw new RuntimeException("Invalid colors used: " + colorsUsed);

			} else
				throw new RuntimeException("Unsupported bits per pixel: " + bitsPerPixel);

			if (compression == 0) {
			} else if (bitsPerPixel == 8 && compression == 1 || bitsPerPixel == 4 && compression == 2) {
				if (topToBottom)
					throw new RuntimeException("Top-to-bottom order not supported for compression = 1 or 2");
			} else
				throw new RuntimeException("Unsupported compression: " + compression);

			if (colorsImportant < 0 || colorsImportant > colorsUsed)
				throw new RuntimeException("Invalid important colors: " + colorsImportant);

		} else
			throw new RuntimeException("Unsupported BMP header format: " + headerSize + " bytes");

		// Some more checks
		if (14 + headerSize + 4 * colorsUsed > imageDataOffset)
			throw new RuntimeException("Invalid image data offset: " + imageDataOffset);
		if (imageDataOffset > fileSize)
			throw new RuntimeException("Invalid file size: " + fileSize);

		// Read the image data
		bis.readBits((imageDataOffset - (14 + headerSize + 4 * colorsUsed)) * 8);
		// Get the first color
		byte[] entry = new byte[4];
		bis.read(entry);
		return 0xFF << 24 | (entry[2] & 0xFF) << 16 | (entry[1] & 0xFF) << 8 | (entry[0] & 0xFF);
	}

}
