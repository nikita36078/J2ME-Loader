/*
 * Copyright (c) 2003 Nokia Corporation and/or its subsidiary(-ies).
 * All rights reserved.
 * This component and the accompanying materials are made available
 * under the terms of "Eclipse Public License v1.0"
 * which accompanies this distribution, and is available
 * at the URL "http://www.eclipse.org/legal/epl-v10.html".
 *
 * Initial Contributors:
 * Nokia Corporation - initial contribution.
 *
 * Contributors:
 *
 * Description:
 *
 */

package javax.microedition.m3g;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.InputConnection;
import javax.microedition.lcdui.Image;
import javax.microedition.util.ContextHolder;

public class Loader {
	// M3G
	static final byte[] M3G_FILE_IDENTIFIER =
			{
					-85, 74, 83, 82, 49, 56, 52, -69, 13, 10, 26, 10
			};
	// PNG
	static final byte[] PNG_FILE_IDENTIFIER =
			{
					-119, 80, 78, 71, 13, 10, 26, 10
			};
	static final int PNG_IHDR = ((73 << 24) + (72 << 16) + (68 << 8) + 82);
	static final int PNG_tRNS = ((116 << 24) + (82 << 16) + (78 << 8) + 83);
	static final int PNG_IDAT = ((73 << 24) + (68 << 16) + (65 << 8) + 84);

	// JPEG
	static final byte[] JPEG_FILE_IDENTIFIER =
			{
					-1, -40
			};
	static final int JPEG_JFIF = ((74 << 24) + (70 << 16) + (73 << 8) + 70);
	// Bytes before colour info in a frame header 'SOFn':
	// length (2 bytes), precision (1 byte), image height & width (4 bytes)
	static final int JPEG_SOFn_DELTA = 7;
	static final int JPEG_INVALID_COLOUR_FORMAT = -1;

	// File identifier types
	private static final int INVALID_HEADER_TYPE = -1;
	private static final int M3G_TYPE = 0;
	private static final int PNG_TYPE = 1;
	private static final int JPEG_TYPE = 2;

	// Misc.
	private static final int MAX_IDENTIFIER_LENGTH = M3G_FILE_IDENTIFIER.length;

	// Initial buffer length for the header
	private static final int AVG_HEADER_SEC_LENGTH = 64;

	// Initial buffer length for the xref section
	private static final int AVG_XREF_SEC_LENGTH = 128;

	// Instance specific
	long handle;

	private Vector iLoadedObjects = new Vector();
	private Vector iFileHistory = new Vector();
	private String iResourceName = null;
	private String iParentResourceName = null;

	private int iTotalFileSize = 0;
	private int iBytesRead = M3G_FILE_IDENTIFIER.length;

	private byte[] iStreamData = null;
	private int iStreamOffset = 0;

	private Interface iInterface;

	//#ifdef RD_JAVA_OMJ
	@Override
	protected void finalize() {
		doFinalize();
	}
//#endif // RD_JAVA_OMJ

	/**
	 * Default ctor
	 */
	private Loader() {
		iInterface = Interface.getInstance();
	}

	/**
	 * Ctor
	 *
	 * @param aFileHistory        File storage
	 * @param aParentResourceName Resource name
	 */
	private Loader(Vector aFileHistory, String aParentResourceName) {
		iParentResourceName = aParentResourceName;
		iFileHistory = aFileHistory;
		iInterface = Interface.getInstance();
	}

	public static Object3D[] load(String name) throws IOException {
		if (name == null) {
			throw new NullPointerException();
		}

		try {
			return (new Loader()).loadFromStream(name);
		} catch (SecurityException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Load error " + e);
		}
	}

	public static Object3D[] load(byte[] data, int offset) throws IOException {
		if (data == null) {
			throw new NullPointerException();
		}

		if (offset < 0 || offset >= data.length) {
			throw new IndexOutOfBoundsException();
		}
		try {
			return (new Loader()).loadFromByteArray(data, offset);
		} catch (SecurityException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Load error " + e);
		}
	}

	/**
	 * @see javax.microedition.m3g.Loader#load(String)
	 */
	private Object3D[] loadFromStream(String aName) throws IOException {
		if (aName == null) {
			throw new NullPointerException();
		}

		if (inFileHistory(aName)) {
			throw new IOException("Reference loop detected.");
		}
		iResourceName = aName;
		iFileHistory.addElement(aName);
		PeekInputStream stream = new PeekInputStream(
				getInputStream(aName), MAX_IDENTIFIER_LENGTH);
		// png, jpeg or m3g
		int type = getIdentifierType(stream);
		stream.rewind();
		iStreamData = null;
		iStreamOffset = 0;

		Object3D[] objects;
		try {
			objects = doLoad(stream, type);
		} finally {
			try {
				stream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// Finally, remove file from history
		iFileHistory.removeElement(aName);
		return objects;
	}

	/**
	 * @see javax.microedition.m3g.Loader#load(byte[], int)
	 */
	private Object3D[] loadFromByteArray(byte[] aData, int aOffset) throws IOException {
		if (aData == null) {
			throw new NullPointerException("Resource byte array is null.");
		}
		int type = getIdentifierType(aData, aOffset);
		ByteArrayInputStream stream =
				new ByteArrayInputStream(aData, aOffset, aData.length - aOffset);
		iStreamData = aData;
		iStreamOffset = aOffset;
		iResourceName = "ByteArray";

		Object3D[] objects;
		try {
			objects = doLoad(stream, type);
		} finally {
			try {
				stream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return objects;
	}

	/**
	 * Dispatcher
	 *
	 * @param aStream Source stream
	 * @param aType   Resource type
	 */
	private Object3D[] doLoad(InputStream aStream, int aType) throws IOException {
		switch (aType) {
			case M3G_TYPE:
				return loadM3G(aStream);
			case PNG_TYPE:
				return loadPNG(aStream);
			case JPEG_TYPE:
				return loadJPEG(aStream);
		}
		throw new IOException("File not recognized.");
	}

	/**
	 * PNG resource loader
	 *
	 * @param aStream Resource stream
	 * @return An array of newly created Object3D instances
	 */
	private Object3D[] loadPNG(InputStream aStream) throws IOException {
		int format = Image2D.RGB;
		DataInputStream in = new DataInputStream(aStream);

		// Scan chuncs that have effect on Image2D format
		in.skip(PNG_FILE_IDENTIFIER.length);

		try {
			while (true) {
				int length = in.readInt();
				int type = in.readInt();
				// IHDR
				if (type == PNG_IHDR) {
					in.skip(9);
					int colourType = in.readUnsignedByte();
					length -= 10;

					switch (colourType) {
						case 0:
							format = Image2D.LUMINANCE;
							break;
						case 2:
							format = Image2D.RGB;
							break;
						case 3:
							format = Image2D.RGB;
							break;
						case 4:
							format = Image2D.LUMINANCE_ALPHA;
							break;
						case 6:
							format = Image2D.RGBA;
							break;
					}
				}
				// tRNS
				if (type == PNG_tRNS) {
					switch (format) {
						case Image2D.LUMINANCE:
							format = Image2D.LUMINANCE_ALPHA;
							break;
						case Image2D.RGB:
							format = Image2D.RGBA;
							break;
					}
				}
				// IDAT
				if (type == PNG_IDAT) {
					break;
				}

				in.skip(length + 4);
			}
		}
		// EOF
		catch (Exception e) {
			e.printStackTrace();
		}
		// Close the data stream
		try {
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buildImage2D(format);
	}

	/**
	 * JPEG (with the same detailed definitions about the JPEG image format as defined in the
	 * JSR 118 MIDP 2.1 specification for LCDUI) MUST be supported by compliant
	 * implementations as a 2D bitmap image format for the Image2D class using the
	 * javax.microedition.m3g.Loader class, and for M3G content files referencing bitmap images.
	 * For colour JPEG images, the pixel format of the returned Image2D object MUST be
	 * Image2D.RGB and for monochrome JPEG images, the pixel format MUST be
	 * Image2D.LUMINANCE.
	 * <p>
	 * JPEG marker: A two-byte code in which the first byte is 0xFF and the second
	 * byte is a value between 1 and 0xFE.
	 * <p>
	 * A JFIF file uses APP0 (0xe0) marker segments and constrains certain parameters in the frame.
	 * <p>
	 * A frame header:
	 * - 0xff, 'SOFn'
	 * - length (2 bytes, Hi-Lo)
	 * - data precision (1 byte)
	 * - image height (2 bytes, Hi-Lo)
	 * - image width (2 bytes, Hi-Lo)
	 * - number of components (1 byte): 1 = grey scaled, 3 = color YCbCr or YIQ, 4 = color CMYK)
	 *
	 * @param aStream Resource stream
	 * @return An array of newly created Object3D instances
	 */
	private Object3D[] loadJPEG(InputStream aStream) throws IOException {
		int format = JPEG_INVALID_COLOUR_FORMAT;
		DataInputStream in = new DataInputStream(aStream);
		// Skip file identifier
		in.skip(JPEG_FILE_IDENTIFIER.length);
		try {
			int marker;
			do {
				// Find marker
				while (in.readUnsignedByte() != 0xff) ;
				do {
					marker = in.readUnsignedByte();
				}
				while (marker == 0xff);

				// Parse marker
				switch (marker) {
					// 'SOFn' (Start Of Frame n)
					case 0xC0:
					case 0xC1:
					case 0xC2:
					case 0xC3:
					case 0xC5:
					case 0xC6:
					case 0xC7:
					case 0xC9:
					case 0xCA:
					case 0xCB:
					case 0xCD:
					case 0xCE:
					case 0xCF:
						// Skip length(2), precicion(1), width(2), height(2)
						in.skip(JPEG_SOFn_DELTA);
						switch (in.readUnsignedByte()) {
							case 1:
								format = Image2D.LUMINANCE;
								break;
							case 3:
								format = Image2D.RGB;
								break;
							default:
								throw new IOException("Unknown JPG format.");
						}
						break;
					// APP0 (0xe0) marker segments and constrains certain parameters in the frame.
					case 0xe0:
						int length = in.readUnsignedShort();
						if (JPEG_JFIF != in.readInt()) {
							throw new IOException("Not a valid JPG file.");
						}
						in.skip(length - 4 - 2);
						break;
					default:
						// Skip variable data
						in.skip(in.readUnsignedShort() - 2);
						break;
				}
			}
			while (format == JPEG_INVALID_COLOUR_FORMAT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Close the data stream
		try {
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buildImage2D(format);
	}

	/**
	 * Image2D builder
	 *
	 * @param aColourFormat Colour format
	 * @return An array of newly created Object3D instances
	 */
	private Object3D[] buildImage2D(int aColourFormat) throws IOException {
		InputStream stream;
		if (iStreamData == null) {
			stream = getInputStream(iResourceName);
		} else {
			stream = new ByteArrayInputStream(iStreamData, iStreamOffset, iStreamData.length - iStreamOffset);
		}
		// Create an image object
		Image2D i2d;
		try {
			i2d = new Image2D(aColourFormat, Image.createImage(stream));
		} finally {
			try {
				stream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new Object3D[]{i2d};
	}


	/**
	 * M3G resource loader
	 *
	 * @param aStream Resource stream
	 * @return An array of newly created Object3D instances
	 */
	private Object3D[] loadM3G(InputStream aStream) throws IOException {
		aStream.skip(M3G_FILE_IDENTIFIER.length);
		if (aStream instanceof PeekInputStream)
			((PeekInputStream) aStream).increasePeekBuffer(AVG_HEADER_SEC_LENGTH);

		// Read header
		int compressionScheme = readByte(aStream);
		int totalSectionLength = readUInt32(aStream);
		if (aStream instanceof PeekInputStream && totalSectionLength > AVG_HEADER_SEC_LENGTH)
			((PeekInputStream) aStream).increasePeekBuffer(totalSectionLength - AVG_HEADER_SEC_LENGTH);
		int uncompressedLength = readUInt32(aStream);

		int objectType = readByte(aStream);
		int length = readUInt32(aStream);

		byte vMajor = (byte) readByte(aStream);
		byte vMinor = (byte) readByte(aStream);
		boolean externalLinks = readBoolean(aStream);
		iTotalFileSize = readUInt32(aStream);
		int approximateContentSize = readUInt32(aStream);
		String authoringField = readString(aStream);

		int checksum = readUInt32(aStream);

		/* Create and register a new native Loader */
		handle = _ctor(Interface.getHandle());
		Interface.register(this);

		if (externalLinks) {
			if (aStream instanceof PeekInputStream)
				((PeekInputStream) aStream).increasePeekBuffer(AVG_XREF_SEC_LENGTH);
			loadExternalRefs(aStream);
			if (iLoadedObjects.size() > 0)   // Load and set external references
			{
				long[] xRef = new long[iLoadedObjects.size()];
				for (int i = 0; i < xRef.length; i++)
					xRef[i] = ((Object3D) iLoadedObjects.elementAt(i)).handle;
				_setExternalReferences(handle, xRef);
			} else {
				throw new IOException("No external sections [" + iResourceName + "].");
			}
		}

		// Reset stream
		if (aStream instanceof PeekInputStream)
			((PeekInputStream) aStream).rewind();
		else if (aStream.markSupported())
			aStream.reset(); // Reset is supported in ByteArrayInputStreams

		int read = 0;
		int size = aStream.available();

		if (size == 0) {
			size = 2048;    // start with some size
		}

		while (read < iTotalFileSize) {
			if (read + size > iTotalFileSize) {
				size = iTotalFileSize - read;
			}
			// Use native loader to load objects
			byte[] data = new byte[size];
			if (aStream.read(data) == -1) {
				break;
			}
			read += size;

			size = _decodeData(handle, 0, data);
			if (size > 0 && aStream.available() > size) {
				size = aStream.available();
			}
		}
		if (size != 0 || read != iTotalFileSize) {
			throw new IOException("Invalid file length [" + iResourceName + "].");
		}

		Object3D[] objects = null;
		int num = _getLoadedObjects(handle, null);
		if (num > 0) {
			long[] obj = new long[num];
			_getLoadedObjects(handle, obj);
			objects = new Object3D[num];
			for (int i = 0; i < objects.length; i++) {
				objects[i] = Interface.getObjectInstance(obj[i]);
			}
			setUserObjects();
		}
		return objects;
	}

	/**
	 *
	 */
	private void setUserObjects() throws IOException {
		int numObjects = _getObjectsWithUserParameters(handle, null);
		long[] obj = null;
		if (numObjects > 0) {
			obj = new long[numObjects];
			_getObjectsWithUserParameters(handle, obj);
		}
		for (int i = 0; i < numObjects; i++) {
			int num = _getNumUserParameters(handle, i);
			if (num > 0) {
				Hashtable hash = new Hashtable();
				for (int j = 0; j < num; j++) {
					int len = _getUserParameter(handle, i, j, null);
					byte[] data = new byte[len];
					int id = _getUserParameter(handle, i, j, data);
					if (hash.put(new Integer(id), data) != null)
						throw new IOException("Duplicate id in user data [" + iResourceName + "].");
				}
				Object3D object = Interface.getObjectInstance(obj[i]);
				object.setUserObject(hash);
			}
		}
	}

	/**
	 * Load external resources
	 */
	private void loadExternalRefs(InputStream aStream) throws IOException {
		// Check for the end of the aStream or file
		int firstByte = readByte(aStream);
		if (firstByte == -1 || (iTotalFileSize != 0 && iBytesRead >= iTotalFileSize)) {
			return;
		}

		int compressionScheme = firstByte;

		int totalSectionLength = readUInt32(aStream);
		iBytesRead += totalSectionLength;
		if (aStream instanceof PeekInputStream && totalSectionLength > AVG_XREF_SEC_LENGTH)
			((PeekInputStream) aStream).increasePeekBuffer(totalSectionLength - AVG_XREF_SEC_LENGTH);
		int uncompressedLength = readUInt32(aStream);
		int expectedCount = totalSectionLength;

		// Decompress data if necessary
		CountedInputStream uncompressedStream;
		if (compressionScheme == 0) {
			uncompressedStream = new CountedInputStream(aStream);
			if (uncompressedLength != totalSectionLength - 13) {
				throw new IOException("Section length mismatch [" + iResourceName + "].");
			}
		} else if (compressionScheme == 1) {
			if (uncompressedLength == 0 && totalSectionLength - 13 == 0) {
				uncompressedStream = new CountedInputStream(null);
			} else {
				if (uncompressedLength <= 0 || totalSectionLength - 13 <= 0) {
					throw new IOException("Section length mismatch [" + iResourceName + "].");
				}
				byte[] compressed = new byte[totalSectionLength - 13];
				aStream.read(compressed);

				byte[] uncompressed = new byte[uncompressedLength];

				// zlib decompression
				if (!_inflate(compressed, uncompressed)) {
					throw new IOException("Decompression error.");
				}
				uncompressedStream = new CountedInputStream(
						new ByteArrayInputStream(uncompressed));
			}
		} else {
			throw new IOException("Unrecognized compression scheme [" + iResourceName + "].");
		}

		// load all objects in this section
		uncompressedStream.resetCounter();

		while (uncompressedStream.getCounter() < uncompressedLength) {
			iLoadedObjects.addElement(loadObject(uncompressedStream));
		}

		if (uncompressedStream.getCounter() != uncompressedLength) {
			throw new IOException("Section length mismatch [" + iResourceName + "].");
		}

		// read checksum
		int checksum = readUInt32(aStream);
	}

	private Object3D loadObject(CountedInputStream aStream) throws IOException {
		int objectType = readByte(aStream);
		int length = readUInt32(aStream);

		int expectedCount = aStream.getCounter() + length;
		Object3D newObject;

		if (objectType == 255) {
			String xref = readString(aStream);
			newObject = (new Loader(iFileHistory, iResourceName)).loadFromStream(xref)[0];
		} else {
			throw new IOException("Invalid external section [" + iResourceName + "].");
		}

		if (expectedCount != aStream.getCounter()) {
			throw new IOException("Object length mismatch [" + iResourceName + "].");
		}

		return newObject;
	}

	/**
	 * Read a byte integer from a stream
	 */
	private static final int readByte(InputStream aStream) throws IOException {
		return aStream.read();
	}

	/**
	 * Read a boolean from a stream
	 */
	private static boolean readBoolean(InputStream aStream) throws IOException {
		int b = aStream.read();
		if (b == 0) {
			return false;
		}
		if (b != 1) {
			throw new IOException("Malformed boolean.");
		}
		return true;
	}

	/**
	 * Read a unsigned integer from a stream
	 */
	private static final int readUInt32(InputStream aStream) throws IOException {
		return aStream.read()
				+ (aStream.read() << 8)
				+ (aStream.read() << 16)
				+ (aStream.read() << 24);
	}

	/**
	 * Read a string from a stream
	 */
	private static String readString(InputStream aStream) throws IOException {
		StringBuilder result = new StringBuilder();
		for (int c = aStream.read(); c != 0; c = aStream.read()) {
			if ((c & 0x80) == 0)   // 0xxxxxxx => 1 byte
			{
				result.append((char) (c & 0x00FF));
			} else if ((c & 0xE0) == 0xC0)   // 110xxxxx => 2 bytes
			{
				int c2 = aStream.read();
				if ((c2 & 0xC0) != 0x80)   // second byte is not 10yyyyyy
				{
					throw new IOException("Invalid UTF-8 string.");
				} else   // 110xxxxx 10yyyyyy
				{
					result.append((char) (((c & 0x1F) << 6) | (c2 & 0x3F)));
				}
			} else if ((c & 0xF0) == 0xE0)   // 1110 xxxx => 3 bytes
			{
				int c2 = aStream.read();
				int c3 = aStream.read();
				if (((c2 & 0xC0) != 0x80) || // second byte is not 10yyyyyy
						((c3 & 0xC0) != 0x80))   // third byte is not 10zzzzzz
				{
					throw new IOException("Invalid UTF-8 string.");
				} else   // 1110xxxx 10yyyyyy 10zzzzzz
				{
					result.append((char) (((c & 0x0F) << 12) |
							((c2 & 0x3F) << 6) |
							(c3 & 0x3F)));
				}
			} else   // none of above
			{
				throw new IOException("Invalid UTF-8 string.");
			}
		}

		return result.toString();
	}

	/**
	 * Solve an identifier of the given data
	 *
	 * @param aStream Stream
	 * @return solved identifier.
	 */
	private int getIdentifierType(InputStream aStream) throws IOException {
		byte[] data = new byte[MAX_IDENTIFIER_LENGTH];
		aStream.read(data);
		return getIdentifierType(data, 0);
	}

	/**
	 * Solve an identifier of the given data
	 *
	 * @param aData   Data
	 * @param aOffset Data offset
	 * @return solved identifier.
	 */
	private static int getIdentifierType(byte[] aData, int aOffset) {
		// Try the JPEG/JFIF identifier
		if (parseIdentifier(aData, aOffset, JPEG_FILE_IDENTIFIER)) {
			return JPEG_TYPE;
		}
		// Try the PNG identifier
		else if (parseIdentifier(aData, aOffset, PNG_FILE_IDENTIFIER)) {
			return PNG_TYPE;
		}
		// Try the M3G identifier
		else if (parseIdentifier(aData, aOffset, M3G_FILE_IDENTIFIER)) {
			return M3G_TYPE;
		}
		return INVALID_HEADER_TYPE;
	}

	/**
	 * Parse identifier from a data
	 *
	 * @param aData       Source data
	 * @param aOffset     Source data offset
	 * @param aIdentifier Identifier
	 * @return true if the data contains the given identifier
	 */
	private static boolean parseIdentifier(byte[] aData, int aOffset, byte[] aIdentifier) {
		if ((aData.length - aOffset) < aIdentifier.length) {
			return false;
		}
		for (int index = 0; index < aIdentifier.length; index++) {
			if (aData[index + aOffset] != aIdentifier[index]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * File name storage for preventing multiple referencing
	 *
	 * @param name File name
	 * @return true if the storage contains the given file name
	 */
	private boolean inFileHistory(String name) {
		for (int i = 0; i < iFileHistory.size(); i++)
			if ((iFileHistory.elementAt(i)).equals(name)) {
				return true;
			}
		return false;
	}

	/*
	 * InputStream-related helper functions
	 */

	/**
	 * Open a HTTP stream and check its MIME type
	 *
	 * @param name Resource name
	 * @return a http stream and checks the MIME type
	 */
	private InputStream getHttpInputStream(String name) throws IOException {
		InputConnection ic = (InputConnection) Connector.open(name);
		// Content-Type is available for http and https connections
		if (ic instanceof HttpConnection) {
			HttpConnection hc = (HttpConnection) ic;
			// Check MIME type
			String type = hc.getHeaderField("Content-Type");
			if (type != null &&
					!type.equals("application/m3g") &&
					!type.equals("image/png") &&
					!type.equals("image/jpeg")) {
				throw new IOException("Wrong MIME type: " + type + ".");
			}
		}

		InputStream is;
		try {
			is = ic.openInputStream();
		} finally {
			try {
				ic.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return is;
	}

	// returns a stream built from the specified file or URI
	private InputStream getInputStream(String name) throws IOException {
		if (name.indexOf(':') != -1)   // absolute URI reference
		{
			return getHttpInputStream(name);
		}

		if (name.charAt(0) == '/' || iParentResourceName == null)   // absolute file reference
		{
			return (ContextHolder.getResourceAsStream(null, name));
		}

		String uri = iParentResourceName.substring(0, iParentResourceName.lastIndexOf('/') + 1) + name;

		if (uri.charAt(0) == '/') {
			return (ContextHolder.getResourceAsStream(null, uri));
		} else {
			return getHttpInputStream(uri);
		}
	}

	class PeekInputStream extends InputStream {
		private int[] iPeekBuffer;
		private InputStream iStream;
		private int iBuffered;
		private int iCounter;

		PeekInputStream(InputStream aStream, int aLength) {
			iStream = aStream;
			iPeekBuffer = new int[aLength];
		}

		@Override
		public int read() throws IOException {
			if (iCounter < iBuffered) {
				return iPeekBuffer[iCounter++];
			}

			int nv = iStream.read();

			if (iBuffered < iPeekBuffer.length) {
				iPeekBuffer[iBuffered] = nv;
				iBuffered++;
			}

			iCounter++;
			return nv;
		}

		public void increasePeekBuffer(int aLength) {
			int[] temp = new int[iPeekBuffer.length + aLength];
			System.arraycopy(iPeekBuffer, 0, temp, 0, iBuffered);
			iPeekBuffer = temp;
		}

		@Override
		public int available() throws IOException {
			if (iCounter < iBuffered) {
				return iBuffered - iCounter + iStream.available();
			}
			return iStream.available();
		}

		@Override
		public void close() {
			try {
				iStream.close();
			} catch (IOException ioe) {
				// Intentionally left empty
			}
		}

		public void rewind() throws IOException {
			if (iCounter > iBuffered) {
				throw new IOException("Peek buffer overrun.");
			}
			iCounter = 0;
		}
	}

	class CountedInputStream extends InputStream {
		private InputStream iStream;
		private int iCounter;

		public CountedInputStream(InputStream aStream) {
			iStream = aStream;
			resetCounter();
		}

		@Override
		public int read() throws IOException {
			iCounter++;
			return iStream.read();
		}

		public void resetCounter() {
			iCounter = 0;
		}

		public int getCounter() {
			return iCounter;
		}

		@Override
		public void close() {
			try {
				iStream.close();
			} catch (IOException ioe) {
				// Intentionally left empty
			}
		}

		@Override
		public int available() throws IOException {
			return iStream.available();
		}
	}

	//#ifdef RD_JAVA_OMJ
	private void doFinalize() {
		registeredFinalize();
	}
//#endif // RD_JAVA_OMJ

	// Finalization method for Symbian
	private void registeredFinalize() {
		if (handle != 0) {
			Platform.finalizeObject(handle, iInterface);
			Interface.deregister(this, iInterface);
			iInterface = null;
			handle = 0;
		}
	}

	// zlib decompression
	private native static boolean _inflate(byte[] data, byte[] buffer);

	// native loader
	private native static long _ctor(long handle);

	private native static int _decodeData(long handle, int offset, byte[] data);

	private native static void _setExternalReferences(long handle, long[] references);

	private native static int _getLoadedObjects(long handle, long[] objects);

	private native static int _getObjectsWithUserParameters(long handle, long[] objects);

	private native static int _getNumUserParameters(long handle, int obj);

	private native static int _getUserParameter(long handle, int obj, int index, byte[] data);
}
