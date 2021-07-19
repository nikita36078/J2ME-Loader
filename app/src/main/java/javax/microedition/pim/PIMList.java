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

import java.util.Enumeration;

public interface PIMList {
	String UNCATEGORIZED = null;

	String getName();

	void close() throws PIMException;

	Enumeration items() throws PIMException;

	Enumeration items(PIMItem matchingItem) throws PIMException;

	Enumeration items(String matchingValue) throws PIMException;

	Enumeration itemsByCategory(String category) throws PIMException;

	String[] getCategories() throws PIMException;

	boolean isCategory(String category) throws PIMException;

	void addCategory(String category) throws PIMException;

	void deleteCategory(String category, boolean deleteUnassignedItems) throws PIMException;

	void renameCategory(String currentCategory, String newCategory) throws PIMException;

	int maxCategories();

	boolean isSupportedField(int field);

	int[] getSupportedFields();

	boolean isSupportedAttribute(int field, int attribute);

	int[] getSupportedAttributes(int field);

	boolean isSupportedArrayElement(int stringArrayField, int arrayElement);

	int[] getSupportedArrayElements(int stringArrayField);

	int getFieldDataType(int field);

	String getFieldLabel(int field);

	String getAttributeLabel(int attribute);

	String getArrayElementLabel(int stringArrayField, int arrayElement);

	int maxValues(int field);

	int stringArraySize(int stringArrayField);
}
