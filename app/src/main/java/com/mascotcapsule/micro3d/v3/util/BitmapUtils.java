package com.mascotcapsule.micro3d.v3.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

public class BitmapUtils {

	public static int getPaletteColor(InputStream inputStream) throws IOException {
		BitInputStream bis = new BitInputStream(inputStream, ByteOrder.LITTLE_ENDIAN);
		if (bis.readShort() != 0x4D42)  // "BM"
			throw new RuntimeException("Invalid BMP signature");
		bis.skipBytes(8);
		int imageDataOffset = bis.readInt();
		int headerSize = bis.readInt();
		int bitsPerPixel;
		int colorsUsed;
		if (headerSize == 40) {
			bis.skipBytes(10);
			bitsPerPixel = bis.readShort();
			bis.skipBytes(16);
			colorsUsed = bis.readInt();
			bis.skipBytes(4);

			if (bitsPerPixel == 1 || bitsPerPixel == 4 || bitsPerPixel == 8) {
				if (colorsUsed == 0) colorsUsed = 1 << bitsPerPixel;
			}
		} else {
			throw new RuntimeException("Unsupported BMP header format: " + headerSize + " bytes");
		}

		// Read the image data
		bis.skipBytes((imageDataOffset - (14 + headerSize + 4 * colorsUsed)));
		// Get the first color
		int color = (0xFF << 24) | bis.readInt();
		return color;
	}
}
