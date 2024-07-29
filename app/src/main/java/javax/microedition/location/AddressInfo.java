/*
 * Copyright 2023 Arman Jussupgaliyev
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
package javax.microedition.location;

public class AddressInfo {
	public static final int EXTENSION = 1;
	public static final int STREET = 2;
	public static final int POSTAL_CODE = 3;
	public static final int CITY = 4;
	public static final int COUNTY = 5;
	public static final int STATE = 6;
	public static final int COUNTRY = 7;
	public static final int COUNTRY_CODE = 8;
	public static final int DISTRICT = 9;
	public static final int BUILDING_NAME = 10;
	public static final int BUILDING_FLOOR = 11;
	public static final int BUILDING_ROOM = 12;
	public static final int BUILDING_ZONE = 13;
	public static final int CROSSING1 = 14;
	public static final int CROSSING2 = 15;
	public static final int URL = 16;
	public static final int PHONE_NUMBER = 17;
	static final int NUM_FIELDS = 17;
	private static final int MAX_FIELD_SIZE = 255;
	String[] addressFields = new String[17];

	public AddressInfo() {
	}

	AddressInfo(AddressInfo other) {
		for (int i = 0; i < 17; i++) {
			this.addressFields[i] = other.addressFields[i];
		}
	}

	public String getField(int field) {
		if ((field < 1) || (field > 17)) {
			throw new IllegalArgumentException();
		}
		return this.addressFields[(field - 1)];
	}

	public void setField(int field, String value) {
		if ((field < 1) || (field > 17)) {
			throw new IllegalArgumentException();
		}
		if ((value != null) && (value.length() > 255)) {
			this.addressFields[(field - 1)] = value.substring(0, 255);
		} else {
			this.addressFields[(field - 1)] = value;
		}
	}

	protected AddressInfo clone() {
		return new AddressInfo(this);
	}
}
