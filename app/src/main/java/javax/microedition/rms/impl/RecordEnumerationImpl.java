/*
 *  MicroEmulator
 *  Copyright (C) 2001-2005 Bartek Teodorczyk <barteo@barteo.net>
 *  Copyright (C) 2018 Nikita Shakarun
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 */
package javax.microedition.rms.impl;

import java.util.Collections;
import java.util.Vector;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordComparator;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordFilter;
import javax.microedition.rms.RecordListener;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotOpenException;

public class RecordEnumerationImpl implements RecordEnumeration {
	private final RecordStoreImpl recordStoreImpl;
	private final RecordFilter filter;
	private final RecordComparator comparator;
	private boolean keepUpdated;

	private final Vector<EnumerationRecord> enumerationRecords = new Vector<>();
	private int currentRecord;

	private final RecordListener recordListener = new RecordListener() {

		@Override
		public void recordAdded(RecordStore recordStore, int recordId) {
			rebuild();
		}

		@Override
		public void recordChanged(RecordStore recordStore, int recordId) {
			rebuild();
		}

		@Override
		public void recordDeleted(RecordStore recordStore, int recordId) {
			rebuild();
		}

	};

	public RecordEnumerationImpl(RecordStoreImpl recordStoreImpl, RecordFilter filter,
								 RecordComparator comparator, boolean keepUpdated) {
		this.recordStoreImpl = recordStoreImpl;
		this.filter = filter;
		this.comparator = comparator;
		this.keepUpdated = keepUpdated;

		rebuild();

		if (keepUpdated) {
			recordStoreImpl.addRecordListener(recordListener);
		}
	}

	@Override
	public int numRecords() {
		return enumerationRecords.size();
	}

	@Override
	public byte[] nextRecord() throws RecordStoreException {
		if (!recordStoreImpl.isOpen()) {
			throw new RecordStoreNotOpenException();
		}

		if (currentRecord >= numRecords()) {
			throw new InvalidRecordIDException();
		}

		byte[] result = enumerationRecords.elementAt(currentRecord).value;
		currentRecord++;

		return result;
	}

	@Override
	public int nextRecordId() throws InvalidRecordIDException {
		if (currentRecord >= numRecords()) {
			throw new InvalidRecordIDException();
		}

		int result = enumerationRecords.elementAt(currentRecord).recordId;
		currentRecord++;

		return result;
	}

	@Override
	public byte[] previousRecord() throws RecordStoreException {
		if (!recordStoreImpl.isOpen()) {
			throw new RecordStoreNotOpenException();
		}
		if (currentRecord < 0) {
			throw new InvalidRecordIDException();
		}

		currentRecord--;

		return enumerationRecords.elementAt(currentRecord).value;
	}

	@Override
	public int previousRecordId() throws InvalidRecordIDException {
		if (currentRecord < 0) {
			throw new InvalidRecordIDException();
		}

		currentRecord--;

		return enumerationRecords.elementAt(currentRecord).recordId;
	}

	@Override
	public boolean hasNextElement() {
		return currentRecord != numRecords();
	}

	@Override
	public boolean hasPreviousElement() {
		return currentRecord != 0;
	}

	@Override
	public void reset() {
		currentRecord = 0;
	}

	@Override
	public void rebuild() {
		enumerationRecords.removeAllElements();
		//
		// filter
		//
		synchronized (recordStoreImpl) {
			try {
				int recordId = 1;
				int i = 0;
				while (i < recordStoreImpl.getNumRecords()) {
					try {
						byte[] data = recordStoreImpl.getRecord(recordId);
						i++;
						if (filter != null && !filter.matches(data)) {
							recordId++;
							continue;
						}
						enumerationRecords.add(new EnumerationRecord(recordId, data));
					} catch (InvalidRecordIDException e) {
						e.printStackTrace();
					}
					recordId++;
				}
			} catch (RecordStoreException e) {
				e.printStackTrace();
			}
		}

		//
		// sort
		//
		if (comparator != null) {
			Collections.sort(enumerationRecords, (lhs, rhs) -> {
				int compare = comparator.compare(lhs.value, rhs.value);
				if (compare == RecordComparator.EQUIVALENT)
					return 0;
				else if (compare == RecordComparator.FOLLOWS)
					return 1;
				else
					return -1;
			});
		}
	}

	@Override
	public void keepUpdated(boolean keepUpdated) {
		if (keepUpdated) {
			if (!this.keepUpdated) {
				rebuild();
				recordStoreImpl.addRecordListener(recordListener);
			}
		} else {
			recordStoreImpl.removeRecordListener(recordListener);
		}

		this.keepUpdated = keepUpdated;
	}

	@Override
	public boolean isKeptUpdated() {
		return keepUpdated;
	}

	@Override
	public void destroy() {
	}

	static class EnumerationRecord {
		final int recordId;
		final byte[] value;

		EnumerationRecord(int recordId, byte[] value) {
			this.recordId = recordId;
			this.value = value;
		}
	}
}
