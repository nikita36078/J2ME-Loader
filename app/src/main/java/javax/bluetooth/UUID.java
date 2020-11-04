/*
 * Copyright 2018 cerg2010cerg2010
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

package javax.bluetooth;

// wrapper around Java UUID
public class UUID {
	public java.util.UUID uuid;
	private static final String SHORT_UUID_BASE = "00001000800000805F9B34FB";

	UUID(java.util.UUID uuid) {
		this.uuid = uuid;
	}

	public UUID(long uuidValue) {
		this(Long.toHexString(uuidValue), true);
		if (uuidValue < 0 || uuidValue > 0xffffffffL) {
			throw new IllegalArgumentException("uuidValue is not in the range [0, 2^32 -1]");
		}
	}

	public UUID(String uuidValue, boolean shortUUID) {
		if (uuidValue == null) {
			throw new NullPointerException("UUID value is null");
		}

		int length = uuidValue.length();

		if (length == 0)
			throw new IllegalArgumentException("UUID value is empty");

		if (shortUUID) {
			if (length > 8)
				throw new IllegalArgumentException("UUID value is too long");
			uuid = java.util.UUID.fromString(JSRToJavaString("00000000".substring(length) + uuidValue + SHORT_UUID_BASE));
		} else {
			if (length > 32)
				throw new IllegalArgumentException("UUID value is too long");
			uuid = java.util.UUID.fromString(JSRToJavaString("00000000000000000000000000000000".substring(length) + uuidValue));
		}
	}

	private static String JSRToJavaString(String str) {
		StringBuilder sb = new StringBuilder(str);
		sb.insert(8, '-');
		sb.insert(13, '-');
		sb.insert(18, '-');
		sb.insert(23, '-');
		return sb.toString();
	}

	public String toString() {
		return uuid.toString().replaceAll("-", "").toUpperCase().replaceFirst("^0+(?!$)", "");
	}

	public boolean equals(Object value) {
		if (value == null || !(value instanceof UUID)) {
			return false;
		}
		return uuid.equals(((UUID) value).uuid);
	}

	public int hashCode() {
		return uuid.hashCode();
	}
}
