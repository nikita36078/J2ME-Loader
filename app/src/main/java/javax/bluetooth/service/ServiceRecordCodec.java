/*
 * Copyright 2026 Nikita Shakarun
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

package javax.bluetooth.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import javax.bluetooth.DataElement;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

public final class ServiceRecordCodec {
	public static final int VERSION = 1;
	public static final int MAX_RECORD_SIZE = 64 * 1024;

	private static final int MAX_ELEMENTS = 1024;
	private static final int MAX_STRING_LENGTH = 64 * 1024;
	private static final Charset UTF_8 = StandardCharsets.UTF_8;

	private ServiceRecordCodec() {
	}

	public static void write(ServiceRecord record, OutputStream stream) throws IOException {
		DataOutputStream out = new DataOutputStream(stream);
		int[] ids = record.getAttributeIDs();
		int count = 0;
		for (int id : ids) {
			if (id > 0 && id <= 0xFFFF && record.getAttributeValue(id) != null) {
				count++;
			}
		}
		if (count > MAX_ELEMENTS) {
			throw new IOException("Too many service record attributes");
		}
		out.writeInt(count);
		for (int id : ids) {
			if (id <= 0 || id > 0xFFFF) {
				continue;
			}
			DataElement element = record.getAttributeValue(id);
			if (element == null) {
				continue;
			}
			out.writeInt(id);
			writeElement(element, out);
		}
	}

	public static void read(ServiceRecord record, InputStream stream) throws IOException {
		DataInputStream in = new DataInputStream(stream);
		int count = in.readInt();
		if (count < 0 || count > MAX_ELEMENTS) {
			throw new IOException("Invalid service record attribute count: " + count);
		}
		for (int i = 0; i < count; i++) {
			int id = in.readInt();
			DataElement element = readElement(in);
			if (id > 0 && id <= 0xFFFF) {
				record.setAttributeValue(id, element);
			}
		}
	}

	private static void writeElement(DataElement element, DataOutputStream out) throws IOException {
		int type = element.getDataType();
		out.writeByte(type);
		switch (type) {
			case DataElement.NULL:
				break;
			case DataElement.U_INT_1:
			case DataElement.U_INT_2:
			case DataElement.U_INT_4:
			case DataElement.INT_1:
			case DataElement.INT_2:
			case DataElement.INT_4:
			case DataElement.INT_8:
				out.writeLong(element.getLong());
				break;
			case DataElement.U_INT_8:
			case DataElement.U_INT_16:
			case DataElement.INT_16: {
				byte[] bytes = (byte[]) element.getValue();
				out.writeInt(bytes.length);
				out.write(bytes);
				break;
			}
			case DataElement.URL:
			case DataElement.STRING: {
				byte[] bytes = ((String) element.getValue()).getBytes(UTF_8);
				if (bytes.length > MAX_STRING_LENGTH) {
					throw new IOException("Service record string is too long");
				}
				out.writeInt(bytes.length);
				out.write(bytes);
				break;
			}
			case DataElement.UUID: {
				java.util.UUID uuid = ((UUID) element.getValue()).uuid;
				out.writeLong(uuid.getMostSignificantBits());
				out.writeLong(uuid.getLeastSignificantBits());
				break;
			}
			case DataElement.BOOL:
				out.writeBoolean(element.getBoolean());
				break;
			case DataElement.DATSEQ:
			case DataElement.DATALT: {
				int size = element.getSize();
				if (size < 0 || size > MAX_ELEMENTS) {
					throw new IOException("Service record element is too large");
				}
				out.writeInt(size);
				Enumeration<?> enumeration = (Enumeration<?>) element.getValue();
				while (enumeration.hasMoreElements()) {
					writeElement((DataElement) enumeration.nextElement(), out);
				}
				break;
			}
			default:
				throw new IOException("Unsupported service record element type: " + type);
		}
	}

	private static DataElement readElement(DataInputStream in) throws IOException {
		int type = in.readUnsignedByte();
		try {
			switch (type) {
				case DataElement.NULL:
					return new DataElement(DataElement.NULL);
				case DataElement.U_INT_1:
				case DataElement.U_INT_2:
				case DataElement.U_INT_4:
				case DataElement.INT_1:
				case DataElement.INT_2:
				case DataElement.INT_4:
				case DataElement.INT_8:
					return new DataElement(type, in.readLong());
				case DataElement.U_INT_8:
				case DataElement.U_INT_16:
				case DataElement.INT_16: {
					int length = in.readInt();
					if (length < 0 || length > MAX_RECORD_SIZE) {
						throw new IOException("Invalid service record byte array length: " + length);
					}
					byte[] bytes = new byte[length];
					in.readFully(bytes);
					return new DataElement(type, bytes);
				}
				case DataElement.URL:
				case DataElement.STRING: {
					int length = in.readInt();
					if (length < 0 || length > MAX_STRING_LENGTH) {
						throw new IOException("Invalid service record string length: " + length);
					}
					byte[] bytes = new byte[length];
					in.readFully(bytes);
					return new DataElement(type, new String(bytes, UTF_8));
				}
				case DataElement.UUID: {
					long most = in.readLong();
					long least = in.readLong();
					return new DataElement(DataElement.UUID, new UUID(toHex(most, least), false));
				}
				case DataElement.BOOL:
					return new DataElement(in.readBoolean());
				case DataElement.DATSEQ:
				case DataElement.DATALT: {
					int size = in.readInt();
					if (size < 0 || size > MAX_ELEMENTS) {
						throw new IOException("Invalid service record element count: " + size);
					}
					DataElement element = new DataElement(type);
					for (int i = 0; i < size; i++) {
						element.addElement(readElement(in));
					}
					return element;
				}
				default:
					throw new IOException("Unsupported service record element type: " + type);
			}
		} catch (IllegalArgumentException e) {
			throw new IOException("Invalid service record element", e);
		}
	}

	private static String toHex(long most, long least) {
		String mostHex = Long.toHexString(most);
		String leastHex = Long.toHexString(least);
		StringBuilder sb = new StringBuilder(32);
		for (int i = mostHex.length(); i < 16; i++) {
			sb.append('0');
		}
		sb.append(mostHex);
		for (int i = leastHex.length(); i < 16; i++) {
			sb.append('0');
		}
		sb.append(leastHex);
		return sb.toString();
	}
}
