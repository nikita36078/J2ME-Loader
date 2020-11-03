/*
 * MicroEmulator
 * Copyright (C) 2001-2007 Bartek Teodorczyk <barteo@barteo.net>
 * Copyright (C) 2018 Nikita Shakarun
 * <p>
 * It is licensed under the following two licenses as alternatives:
 * 1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 * 2. Apache License (the "AL") Version 2.0
 * <p>
 * You may not use this file except in compliance with at least one of
 * the above two licenses.
 * <p>
 * You may obtain a copy of the LGPL at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 * <p>
 * You may obtain a copy of the AL at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the LGPL or the AL for the specific language governing permissions and
 * limitations.
 */
package javax.microedition.rms.impl;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

public interface RecordStoreManager {

	void deleteRecord(RecordStoreImpl recordStoreImpl, int recordId) throws RecordStoreException;

	void deleteRecordStore(String recordStoreName) throws RecordStoreException;

	String getName();

	int getSizeAvailable(RecordStoreImpl recordStoreImpl);

	String[] listRecordStores();

	void loadRecord(RecordStoreImpl recordStoreImpl, int recordId) throws RecordStoreException;

	RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary)
			throws RecordStoreException;

	void saveRecord(RecordStoreImpl recordStoreImpl, int recordId) throws RecordStoreException;
}
