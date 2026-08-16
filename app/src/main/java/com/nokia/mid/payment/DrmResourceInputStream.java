package com.nokia.mid.payment;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parses an OMA DRM 2.0 DCF file and exposes its raw encrypted ODDA payload.
 * <p>
 * This class does not decrypt protected content. The content-encryption key is
 * supplied by a separate DRM rights database and is not present in plain text
 * in the DCF container.
 */
public final class DrmResourceInputStream extends FilterInputStream {
	private static final Charset ASCII = StandardCharsets.US_ASCII;

	private final DRMInfo drmInfo;

	public DrmResourceInputStream(InputStream encryptedInput)
			throws IOException {
		this(parse(encryptedInput));
	}

	private DrmResourceInputStream(DRMInfo drmInfo) {
		super(new ByteArrayInputStream(drmInfo.payload));
		this.drmInfo = drmInfo;
		printProperties(System.out);
	}

	public void printProperties(PrintStream out) {
		out.println("File size: " + drmInfo.fileLength + " bytes");
		out.println("Major brand: " + value(drmInfo.majorBrand));
		out.println("DCF version: 0x" + Long.toHexString(drmInfo.dcfVersion));
		out.println("MIME type: " + value(drmInfo.mimeType));
		out.println("Encryption method: " + encryptionName(drmInfo.encryptionMethod)
				+ " (" + drmInfo.encryptionMethod + ")");
		out.println("Padding method: " + paddingName(drmInfo.paddingMethod)
				+ " (" + drmInfo.paddingMethod + ")");
		out.println("Plaintext length: " + lengthValue(drmInfo.plainTextLength));
		out.println("Content ID: " + value(drmInfo.contentId));
		out.println("Rights issuer URL: " + value(drmInfo.rightsIssuerUrl));
		out.println("Textual headers: " + printable(drmInfo.textualHeaders));
		out.println("Group ID: " + value(drmInfo.groupId));
		out.println("Group-key encryption: "
				+ encryptionName(drmInfo.groupKeyEncryptionMethod)
				+ " (" + drmInfo.groupKeyEncryptionMethod + ")");
		out.println("Encrypted group key: " + drmInfo.encryptedGroupKey.length
				+ " bytes, hex=" + toHex(drmInfo.encryptedGroupKey));
		out.println("ODDA payload offset: " + drmInfo.payloadOffset);
		out.println("ODDA encrypted data length: "
				+ drmInfo.encryptedDataLength + " bytes");
		if (drmInfo.plainTextLength >= 0 && drmInfo.encryptedDataLength >= 0) {
			out.println("Encrypted overhead/padding: "
					+ (drmInfo.encryptedDataLength - drmInfo.plainTextLength)
					+ " bytes");
		}
		out.println("Boxes:");
		for (Box box : drmInfo.boxes) {
			out.println(repeat(box.depth) + box.type
					+ " offset=" + box.offset
					+ " size=" + box.size
					+ " header=" + box.headerSize);
		}
	}

	public static String encryptionName(int method) {
		return switch (method) {
			case 0 -> "none";
			case 1 -> "AES-128-CBC";
			case 2 -> "AES-128-CTR";
			default -> method < 0 ? "not present" : "unknown";
		};
	}

	public static String paddingName(int method) {
		return switch (method) {
			case 0 -> "none";
			case 1 -> "RFC 2630 / PKCS#7";
			default -> method < 0 ? "not present" : "unknown";
		};
	}

	private static DRMInfo parse(InputStream encryptedInput)
			throws IOException {
		if (encryptedInput == null) {
			throw new NullPointerException("encryptedInput");
		}

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		try (encryptedInput) {
			int count;
			while ((count = encryptedInput.read(buffer)) >= 0) {
				if (count > 0) {
					output.write(buffer, 0, count);
				}
			}
		}
		return new Parser(output.toByteArray()).parse();
	}

	private static String lengthValue(long value) {
		return value < 0 ? "<not present>" : value + " bytes";
	}

	private static String value(String value) {
		return value == null || value.isEmpty()
				? "<not present>" : value;
	}

	private static String printable(byte[] bytes) {
		if (bytes.length == 0) {
			return "<not present>";
		}
		StringBuilder result = new StringBuilder(bytes.length);
		for (byte aByte : bytes) {
			int c = aByte & 0xff;
			result.append(c >= 32 && c <= 126 ? (char) c : '.');
		}
		return result.toString();
	}

	private static String toHex(byte[] bytes) {
		if (bytes.length == 0) {
			return "<not present>";
		}
		StringBuilder result = new StringBuilder(bytes.length * 2);
		for (byte aByte : bytes) {
			result.append(String.format("%02x", aByte & 0xff));
		}
		return result.toString();
	}

	private static String repeat(int count) {
		StringBuilder result =
				new StringBuilder("  ".length() * count);
		for (int i = 0; i < count; i++) {
			result.append("  ");
		}
		return result.toString();
	}

	private static final class DRMInfo {
		long fileLength;
		final List<Box> boxes = new ArrayList<>();
		String majorBrand;
		long dcfVersion;
		String mimeType;
		int encryptionMethod = -1;
		int paddingMethod = -1;
		long plainTextLength = -1;
		String contentId;
		String rightsIssuerUrl;
		byte[] textualHeaders = new byte[0];
		String groupId;
		int groupKeyEncryptionMethod = -1;
		byte[] encryptedGroupKey = new byte[0];
		long encryptedDataLength = -1;
		long payloadOffset = -1;
		byte[] payload;
	}

	private static final class Parser {
		private final byte[] data;
		private final DRMInfo parsed = new DRMInfo();

		Parser(byte[] data) {
			this.data = data;
			parsed.fileLength = data.length;
		}

		DRMInfo parse() throws IOException {
			require(data.length >= 20,
					"File is too small for an OMA2 DCF header");

			Box ftyp = readBox(0, 0, data.length);
			require("ftyp".equals(ftyp.type),
					"Expected ftyp box at offset 0");
			require(ftyp.size >= 16, "Invalid ftyp box");
			parsed.majorBrand = readString(ftyp.dataOffset, 4);
			parsed.dcfVersion = readUInt32(ftyp.dataOffset + 4);

			long offset = ftyp.end();
			boolean foundContainer = false;
			while (offset < data.length) {
				Box box = readBox(offset, 0, data.length);
				if ("odrm".equals(box.type)) {
					parseOdrm(box);
					foundContainer = true;
				}
				offset = box.end();
			}

			require(foundContainer, "No odrm container found");
			require(parsed.payloadOffset >= 0,
					"No odda content object found");
			require(parsed.encryptedDataLength >= 0,
					"Invalid odda data length");
			require(parsed.encryptedDataLength <= Integer.MAX_VALUE,
					"ODDA payload is too large");
			require(parsed.payloadOffset + parsed.encryptedDataLength
					<= data.length, "ODDA payload extends beyond the file");

			int start = (int) parsed.payloadOffset;
			int end = start + (int) parsed.encryptedDataLength;
			parsed.payload = Arrays.copyOfRange(data, start, end);
			return parsed;
		}

		private void parseOdrm(Box odrm) throws IOException {
			long offset = odrm.dataOffset + 4;
			while (offset < odrm.end()) {
				Box child = readBox(offset, odrm.depth + 1, odrm.end());
				if ("odhe".equals(child.type)) {
					parseOdhe(child);
				} else if ("odda".equals(child.type)) {
					parseOdda(child);
				}
				offset = child.end();
			}
		}

		private void parseOdhe(Box odhe) throws IOException {
			long offset = odhe.dataOffset + 4;
			require(offset < odhe.end(), "Truncated odhe box");

			int mimeLength = readUInt8(offset++);
			require(offset + mimeLength <= odhe.end(),
					"Invalid MIME type length");
			parsed.mimeType = readString(offset, mimeLength);
			offset += mimeLength;

			while (offset < odhe.end()) {
				Box child = readBox(offset, odhe.depth + 1, odhe.end());
				if ("ohdr".equals(child.type)) {
					parseOhdr(child);
				}
				offset = child.end();
			}
		}

		private void parseOhdr(Box ohdr) throws IOException {
			long offset = ohdr.dataOffset + 4;
			require(offset + 16 <= ohdr.end(),
					"Truncated ohdr fixed fields");

			parsed.encryptionMethod = readUInt8(offset++);
			parsed.paddingMethod = readUInt8(offset++);
			parsed.plainTextLength = readUInt64(offset);
			offset += 8;

			int contentIdLength = readUInt16(offset);
			int issuerLength = readUInt16(offset + 2);
			int textualLength = readUInt16(offset + 4);
			offset += 6;

			require(offset + contentIdLength + issuerLength + textualLength
					<= ohdr.end(), "OHDR strings extend beyond the box");
			parsed.contentId = readString(offset, contentIdLength);
			offset += contentIdLength;
			parsed.rightsIssuerUrl = readString(offset, issuerLength);
			offset += issuerLength;
			parsed.textualHeaders = readBytes(offset, textualLength);
			offset += textualLength;

			while (offset < ohdr.end()) {
				Box child = readBox(offset, ohdr.depth + 1, ohdr.end());
				if ("grpi".equals(child.type)) {
					parseGrpi(child);
				}
				offset = child.end();
			}
		}

		private void parseGrpi(Box grpi) throws IOException {
			long offset = grpi.dataOffset + 4;
			require(offset + 5 <= grpi.end(), "Truncated grpi fields");

			int groupIdLength = readUInt16(offset);
			parsed.groupKeyEncryptionMethod = readUInt8(offset + 2);
			int groupKeyLength = readUInt16(offset + 3);
			offset += 5;

			require(offset + groupIdLength + groupKeyLength <= grpi.end(),
					"GRPI data extends beyond the box");
			parsed.groupId = readString(offset, groupIdLength);
			offset += groupIdLength;
			parsed.encryptedGroupKey = readBytes(offset, groupKeyLength);
		}

		private void parseOdda(Box odda) throws IOException {
			long offset = odda.dataOffset + 4;
			require(offset + 8 <= odda.end(),
					"Truncated odda data length");
			parsed.encryptedDataLength = readUInt64(offset);
			parsed.payloadOffset = offset + 8;
			require(parsed.payloadOffset + parsed.encryptedDataLength
					<= odda.end(), "ODDA data length exceeds its box");
		}

		private Box readBox(long offset, int depth, long parentEnd)
				throws IOException {
			require(offset >= 0 && offset + 8 <= parentEnd,
					"Truncated box at " + offset);
			long size32 = readUInt32(offset);
			String type = readString(offset + 4, 4);
			long size;
			int headerSize = 8;

			if (size32 == 1) {
				require(offset + 16 <= parentEnd,
						"Truncated extended box at " + offset);
				size = readUInt64(offset + 8);
				headerSize = 16;
			} else if (size32 == 0) {
				size = parentEnd - offset;
			} else {
				size = size32;
			}

			require(size >= headerSize,
					"Invalid " + type + " box size at " + offset);
			require(size <= parentEnd - offset,
					type + " box exceeds its parent");

			Box box = new Box(type, offset, size, headerSize, depth);
			parsed.boxes.add(box);
			return box;
		}

		private int readUInt8(long offset) throws IOException {
			require(offset >= 0 && offset < data.length,
					"Unexpected EOF at " + offset);
			return data[(int) offset] & 0xff;
		}

		private int readUInt16(long offset) throws IOException {
			return (readUInt8(offset) << 8) | readUInt8(offset + 1);
		}

		private long readUInt32(long offset) throws IOException {
			return ((long) readUInt8(offset) << 24)
					| ((long) readUInt8(offset + 1) << 16)
					| ((long) readUInt8(offset + 2) << 8)
					| readUInt8(offset + 3);
		}

		private long readUInt64(long offset) throws IOException {
			long high = readUInt32(offset);
			long low = readUInt32(offset + 4);
			require(high <= 0x7fffffffL,
					"64-bit value is too large for Java long");
			return (high << 32) | low;
		}

		private String readString(long offset, int length)
				throws IOException {
			return new String(readBytes(offset, length), ASCII);
		}

		private byte[] readBytes(long offset, int length)
				throws IOException {
			require(length >= 0, "Negative byte count");
			require(offset >= 0 && offset + length <= data.length,
					"Unexpected EOF at " + offset);
			return Arrays.copyOfRange(data, (int) offset, (int) offset + length);
		}

		private void require(boolean condition, String message)
				throws IOException {
			if (!condition) {
				throw new IOException(message);
			}
		}
	}

	private static final class Box {
		final String type;
		final long offset;
		final long size;
		final int headerSize;
		final int depth;
		final long dataOffset;

		Box(String type, long offset, long size, int headerSize, int depth) {
			this.type = type;
			this.offset = offset;
			this.size = size;
			this.headerSize = headerSize;
			this.depth = depth;
			this.dataOffset = offset + headerSize;
		}

		long end() {
			return offset + size;
		}
	}
}
