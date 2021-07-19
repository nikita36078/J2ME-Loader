/*
 *  Copyright 2021 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package javax.microedition.pim;

public interface PIMItem {
	int BINARY = 0;
	int BOOLEAN = 1;
	int DATE = 2;
	int INT = 3;
	int STRING = 4;
	int STRING_ARRAY = 5;
	int ATTR_NONE = 0;
	int EXTENDED_FIELD_MIN_VALUE = 16777216;
	int EXTENDED_ATTRIBUTE_MIN_VALUE = 16777216;

	PIMList getPIMList();

	void commit() throws PIMException;

	boolean isModified();

	int[] getFields();

	byte[] getBinary(int field, int index);

	void addBinary(int field, int attributes, byte[] value, int offset, int length);

	void setBinary(int field, int index, int attributes, byte[] value, int offset, int length);

	long getDate(int field, int index);

	void addDate(int field, int attributes, long value);

	void setDate(int field, int index, int attributes, long value);

	int getInt(int field, int index);

	void addInt(int field, int attributes, int value);

	void setInt(int field, int index, int attributes, int value);

	String getString(int field, int index);

	void addString(int field, int attributes, String value);

	void setString(int field, int index, int attributes, String value);

	boolean getBoolean(int field, int index);

	void addBoolean(int field, int attributes, boolean value);

	void setBoolean(int field, int index, int attributes, boolean value);

	String[] getStringArray(int field, int index);

	void addStringArray(int field, int attributes, String[] value);

	void setStringArray(int field, int index, int attributes, String[] value);

	int countValues(int field);

	void removeValue(int field, int index);

	int getAttributes(int field, int index);

	void addToCategory(String category) throws PIMException;

	void removeFromCategory(String category);

	String[] getCategories();

	int maxCategories();
}
