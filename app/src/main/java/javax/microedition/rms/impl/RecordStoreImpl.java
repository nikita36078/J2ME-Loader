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

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordComparator;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordFilter;
import javax.microedition.rms.RecordListener;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotOpenException;

public class RecordStoreImpl extends RecordStore {
	private static final String TAG = RecordStoreImpl.class.getName();

	private static final byte[] fileIdentifier = "MIDRMS".getBytes();
	private static final byte versionMajor = 0x03;
	private static final byte versionMinor = 0x00;

	final HashMap<Integer, byte[]> records = new HashMap<>();

	private final RecordStoreManager recordStoreManager;
	private final Vector<RecordListener> recordListeners = new Vector<>();

	private int lastRecordId = 0;
	private String recordStoreName;
	private int version = 0;
	private long lastModified = 0;
	private int openCount = 0;
	private boolean open;

	RecordStoreImpl(RecordStoreManager recordStoreManager, String recordStoreName) {
		this.recordStoreManager = recordStoreManager;
		if (recordStoreName.length() <= 32) {
			this.recordStoreName = recordStoreName;
		} else {
			this.recordStoreName = recordStoreName.substring(0, 32);
		}
		this.open = false;
	}

	RecordStoreImpl(RecordStoreManager recordStoreManager) {
		this.recordStoreManager = recordStoreManager;
	}

	void readHeader(DataInputStream dis) throws IOException {
		for (byte aFileIdentifier : fileIdentifier) {
			if (dis.read() != aFileIdentifier) {
				throw new IOException();
			}
		}
		dis.readByte(); // Major version number
		dis.readByte(); // Minor version number
		dis.readByte(); // Encrypted flag

		recordStoreName = dis.readUTF();
		lastModified = dis.readLong();
		version = dis.readInt();
		dis.readInt(); // TODO AuthMode
		dis.readByte(); // TODO Writable
		dis.readInt();
		if (dis.available() >= 4)
			lastRecordId = dis.readInt();
	}

	void readRecord(DataInputStream dis) throws IOException {
		int recordId = dis.readInt();
		if (recordId > lastRecordId) {
			lastRecordId = recordId;
		}
		dis.readInt(); // TODO Tag
		byte[] data = new byte[dis.readInt()];
		dis.readFully(data, 0, data.length);
		this.records.put(recordId, data);
	}

	void writeHeader(DataOutputStream dos) throws IOException {
		dos.write(fileIdentifier);
		dos.write(versionMajor);
		dos.write(versionMinor);
		dos.write(0); // Encrypted flag

		dos.writeUTF(recordStoreName);
		dos.writeLong(lastModified);
		dos.writeInt(version);
		dos.writeInt(0); // TODO AuthMode
		dos.writeByte(0); // TODO Writable
		dos.writeInt(records.size());
		dos.writeInt(lastRecordId);
	}

	void writeRecord(DataOutputStream dos, int recordId) throws IOException {
		dos.writeInt(recordId);
		dos.writeInt(0); // TODO Tag
		try {
			byte[] data = getRecord(recordId);
			if (data == null) {
				dos.writeInt(0);
			} else {
				dos.writeInt(data.length);
				dos.write(data);
			}
		} catch (RecordStoreException e) {
			throw new IOException();
		}
	}

	boolean isOpen() {
		return open;
	}

	void setOpen() {
		openCount++;
		this.open = true;
	}

	@Override
	public void closeRecordStore() throws RecordStoreException {
		synchronized (records) {
			if (!open) {
				throw new RecordStoreNotOpenException();
			}

			if (--openCount > 0) {
				return;
			}

			if (recordListeners != null) {
				recordListeners.removeAllElements();
			}

			records.clear();

			open = false;
		}
		Log.d(TAG, "RecordStore " + recordStoreName + " closed");
	}

	@Override
	public String getName() throws RecordStoreNotOpenException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}

		return recordStoreName;
	}

	@Override
	public int getVersion() throws RecordStoreNotOpenException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}

		synchronized (records) {
			return version;
		}
	}

	@Override
	public int getNumRecords() throws RecordStoreNotOpenException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}

		return records.size();
	}

	@Override
	public int getSize() throws RecordStoreNotOpenException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}

		// TODO include size overhead such as the data structures used to hold the state of the record store

		int result = 0;
		synchronized (records) {
			for (byte[] data : records.values()) {
				if (data != null) {
					result += data.length;
				}
			}
		}
		return result;
	}

	@Override
	public int getSizeAvailable() throws RecordStoreNotOpenException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}

		return recordStoreManager.getSizeAvailable(this);
	}

	@Override
	public long getLastModified() throws RecordStoreNotOpenException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}

		synchronized (records) {
			return lastModified;
		}
	}

	@Override
	public void addRecordListener(RecordListener listener) {
		if (!recordListeners.contains(listener)) {
			recordListeners.addElement(listener);
		}
	}

	@Override
	public void removeRecordListener(RecordListener listener) {
		recordListeners.removeElement(listener);
	}

	@Override
	public int getNextRecordID() throws RecordStoreException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}

		synchronized (records) {
			int next = lastRecordId + 1;
			if (next < 0 ) next = 0;
			return next;
		}
	}

	@Override
	public int addRecord(byte[] data, int offset, int numBytes) throws RecordStoreException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}
		if (data == null && numBytes > 0) {
			throw new NullPointerException();
		}
		if (numBytes > recordStoreManager.getSizeAvailable(this)) {
			throw new RecordStoreFullException();
		}

		byte[] recordData = new byte[numBytes];
		if (data != null) {
			System.arraycopy(data, offset, recordData, 0, numBytes);
		}

		int nextRecordID = getNextRecordID();
		synchronized (records) {
			records.put(nextRecordID, recordData);
			version++;
			lastModified = System.currentTimeMillis();
			lastRecordId = nextRecordID;
		}

		recordStoreManager.saveRecord(this, nextRecordID);

		fireRecordListener(ExtendedRecordListener.RECORD_ADD, nextRecordID);

		Log.d(TAG, "Record " + recordStoreName + "." + nextRecordID + " added");
		return nextRecordID;
	}

	@Override
	public void deleteRecord(int recordId) throws RecordStoreException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}

		synchronized (records) {
			if (records.remove(recordId) == null) {
				throw new InvalidRecordIDException();
			}
			version++;
			lastModified = System.currentTimeMillis();
		}

		recordStoreManager.deleteRecord(this, recordId);

		fireRecordListener(ExtendedRecordListener.RECORD_DELETE, recordId);
		Log.d(TAG, "Record " + recordStoreName + "." + recordId + " deleted");
	}

	@Override
	public int getRecordSize(int recordId) throws RecordStoreException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}

		synchronized (records) {
			byte[] data = records.get(recordId);
			if (data == null) {
				throw new InvalidRecordIDException();
			}

			return data.length;
		}
	}

	@Override
	public int getRecord(int recordId, byte[] buffer, int offset) throws RecordStoreException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}
		int recordSize;
		synchronized (records) {
			byte[] data = records.get(recordId);
			if (data == null) {
				throw new InvalidRecordIDException();
			}
			recordSize = data.length;
			System.arraycopy(data, 0, buffer, offset, recordSize);
		}

		return recordSize;
	}

	@Override
	public byte[] getRecord(int recordId) throws RecordStoreException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}

		synchronized (records) {
			byte[] data = records.get(recordId);
			if (data == null) {
				throw new InvalidRecordIDException();
			}
			return data.length < 1 ? null : data.clone();
		}
	}

	@Override
	public void setRecord(int recordId, byte[] newData, int offset, int numBytes)
			throws RecordStoreException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}

		// FIXME fixit
		if (numBytes > recordStoreManager.getSizeAvailable(this)) {
			throw new RecordStoreFullException();
		}

		byte[] recordData = new byte[numBytes];
		System.arraycopy(newData, offset, recordData, 0, numBytes);

		synchronized (records) {
			if (!records.containsKey(recordId)) {
				throw new InvalidRecordIDException();
			}
			records.put(recordId, recordData);
			version++;
			lastModified = System.currentTimeMillis();
		}

		recordStoreManager.saveRecord(this, recordId);

		fireRecordListener(ExtendedRecordListener.RECORD_CHANGE, recordId);
		Log.d(TAG, "Record " + recordStoreName + "." + recordId + " set");
	}

	@Override
	public RecordEnumeration enumerateRecords(RecordFilter filter,
											  RecordComparator comparator,
											  boolean keepUpdated)
			throws RecordStoreNotOpenException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}

		Log.d(TAG, "Enumerate records in " + recordStoreName);
		return new RecordEnumerationImpl(this, filter, comparator, keepUpdated);
	}

	private void fireRecordListener(int type, int recordId) {
		long timestamp = System.currentTimeMillis();

		if (recordListeners != null) {
			for (Enumeration<RecordListener> e = recordListeners.elements(); e.hasMoreElements(); ) {
				RecordListener l = e.nextElement();
				if (l instanceof ExtendedRecordListener) {
					((ExtendedRecordListener) l).recordEvent(type, timestamp, this, recordId);
				} else {
					switch (type) {
						case ExtendedRecordListener.RECORD_ADD:
							l.recordAdded(this, recordId);
							break;
						case ExtendedRecordListener.RECORD_CHANGE:
							l.recordChanged(this, recordId);
							break;
						case ExtendedRecordListener.RECORD_DELETE:
							l.recordDeleted(this, recordId);
					}
				}
			}
		}
	}
}
