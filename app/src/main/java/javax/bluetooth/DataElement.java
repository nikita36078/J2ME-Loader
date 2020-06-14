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

import java.util.Arrays;
import java.util.Vector;

public class DataElement {
	public static final int NULL = 0;
	public static final int U_INT_1 = 0x8;
	public static final int U_INT_2 = 0x9;
	public static final int U_INT_4 = 0xA;
	public static final int U_INT_8 = 0xB;
	public static final int U_INT_16 = 0xC;
	public static final int INT_1 = 0x10;
	public static final int INT_2 = 0x11;
	public static final int INT_4 = 0x12;
	public static final int INT_8 = 0x13;
	public static final int INT_16 = 0x14;
	public static final int URL = 0x40;
	public static final int UUID = 0x18;
	public static final int BOOL = 0x28;
	public static final int STRING = 0x20;
	public static final int DATSEQ = 0x30;
	public static final int DATALT = 0x38;

	private Object value;
	private int valueType;

	public DataElement(int valueType) {
		switch (valueType) {
			case NULL:
				value = null;
				break;
			case DATALT:
			case DATSEQ:
				value = new Vector();
				break;
			default:
				throw new IllegalArgumentException();
		}

		this.valueType = valueType;
	}

	public DataElement(boolean bool) {
		value = bool ? Boolean.TRUE : Boolean.FALSE;
		valueType = BOOL;
	}

	public DataElement(int valueType, long value) {
		switch (valueType) {
			case U_INT_1:
				if (value < 0 || value > 0xff)
					throw new IllegalArgumentException(value + " not U_INT_1");
				break;
			case U_INT_2:
				if (value < 0 || value > 0xffff)
					throw new IllegalArgumentException(value + " not U_INT_2");
				break;
			case U_INT_4:
				if (value < 0 || value > 0xffffffffL)
					throw new IllegalArgumentException(value + " not U_INT_4");
				break;
			case INT_1:
				if (value < -0x80 || value > 0x7f)
					throw new IllegalArgumentException(value + " not INT_1");
				break;
			case INT_2:
				if (value < -0x8000 || value > 0x7fff)
					throw new IllegalArgumentException(value + " not INT_2");
				break;
			case INT_4:
				if (value < -0x80000000 || value > 0x7fffffff)
					throw new IllegalArgumentException(value + " not INT_4");
				break;
			case INT_8:
				break;
			default:
				throw new IllegalArgumentException();
		}

		this.value = new Long(value);
		this.valueType = valueType;
	}

	public DataElement(int valueType, Object value) {
		if (value == null)
			throw new IllegalArgumentException();

		switch (valueType) {
			case URL:
			case STRING:
				if (!(value instanceof String))
					throw new IllegalArgumentException();
				break;
			case UUID:
				if (!(value instanceof UUID))
					throw new IllegalArgumentException();
				break;
			case U_INT_8:
				if (!(value instanceof byte[]) || ((byte[]) value).length != 8)
					throw new IllegalArgumentException();
				break;
			case U_INT_16:
			case INT_16:
				if (!(value instanceof byte[]) || ((byte[]) value).length != 16)
					throw new IllegalArgumentException();
				break;
			default:
				throw new IllegalArgumentException();
		}

		this.value = value;
		this.valueType = valueType;
	}

	public void addElement(DataElement elem) {
		if (elem == null)
			throw new NullPointerException();

		switch (valueType) {
			case DATALT:
			case DATSEQ:
				((Vector) value).addElement(elem);
				break;
			default:
				throw new ClassCastException();
		}
	}

	public void insertElementAt(DataElement elem, int index) {
		if (elem == null)
			throw new NullPointerException();

		switch (valueType) {
			case DATALT:
			case DATSEQ:
				if (index < 0 || index > ((Vector) value).size())
					throw new IndexOutOfBoundsException();
				((Vector) value).insertElementAt(elem, index);
				break;
			default:
				throw new ClassCastException();
		}
	}

	public int getSize() {
		switch (valueType) {
			case DATALT:
			case DATSEQ:
				return ((Vector) value).size();
			default:
				throw new ClassCastException();
		}
	}

	public boolean removeElement(DataElement elem) {
		if (elem == null)
			throw new NullPointerException();

		switch (valueType) {
			case DATALT:
			case DATSEQ:
				return ((Vector) value).removeElement(elem);
			default:
				throw new ClassCastException();
		}
	}

	public int getDataType() {
		return valueType;
	}

	public long getLong() {
		switch (valueType) {
			case U_INT_1:
			case U_INT_2:
			case U_INT_4:
			case INT_1:
			case INT_2:
			case INT_4:
			case INT_8:
				return ((Long) value).longValue();
			default:
				throw new ClassCastException();
		}
	}

	public boolean getBoolean() {
		if (valueType == BOOL)
			return ((Boolean) value).booleanValue();
		else
			throw new ClassCastException();
	}

	public Object getValue() {
		switch (valueType) {
			case URL:
			case STRING:
				return value;
			case UUID:
				return new UUID(value.toString(), false);
			case U_INT_8:
			case U_INT_16:
			case INT_16:
				return Arrays.copyOf((byte[]) value, ((byte[]) value).length);
			case DATSEQ:
			case DATALT:
				return ((Vector) value).elements();
			default:
				throw new ClassCastException();
		}
	}
}
