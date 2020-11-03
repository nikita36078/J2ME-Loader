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
import java.util.Hashtable;
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

	private final RecordStoreManager recordStoreManager;
	private final Vector<RecordListener> recordListeners = new Vector<>();
	private final Hashtable<Integer, byte[]> records = new Hashtable<>();

	private int lastRecordId = 0;
	private int size = 0;
	private String recordStoreName;
	private int version = 0;
	private long lastModified = 0;
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
		size = dis.readInt();

	}

	void readRecord(DataInputStream dis) throws IOException {
		int recordId = dis.readInt();
		if (recordId > lastRecordId) {
			lastRecordId = recordId;
		}
		dis.readInt(); // TODO Tag
		int len = dis.readInt();
		byte[] data = new byte[len];
		dis.readFully(data, 0, len);
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
		dos.writeInt(size);
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

	void setOpen(boolean open) {
		this.open = open;
	}

	@Override
	public void closeRecordStore() throws RecordStoreException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}

		if (recordListeners != null) {
			recordListeners.removeAllElements();
		}

		records.clear();

		open = false;
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

		synchronized (this) {
			return version;
		}
	}

	@Override
	public int getNumRecords() throws RecordStoreNotOpenException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}

		return size;
	}

	@Override
	public int getSize() throws RecordStoreNotOpenException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}

		// TODO include size overhead such as the data structures used to hold the state of the record store

		// Preload all records
		enumerateRecords(null, null, false);

		int result = 0;
		Enumeration<Integer> keys = records.keys();
		while (keys.hasMoreElements()) {
			int key = (Integer) keys.nextElement();
			try {
				byte[] data = getRecord(key);
				if (data != null) {
					result += data.length;
				}
			} catch (RecordStoreException e) {
				e.printStackTrace();
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

		synchronized (this) {
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

		// lastRecordId needs to hold correct number, all records have to be preloaded
		enumerateRecords(null, null, false);

		synchronized (this) {
			return lastRecordId + 1;
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

		// lastRecordId needs to hold correct number, all records have to be preloaded
		enumerateRecords(null, null, false);

		byte[] recordData = new byte[numBytes];
		if (data != null) {
			System.arraycopy(data, offset, recordData, 0, numBytes);
		}

		int nextRecordID = getNextRecordID();
		synchronized (this) {
			records.put(nextRecordID, recordData);
			version++;
			lastModified = System.currentTimeMillis();
			lastRecordId++;
			size++;
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

		synchronized (this) {
			// throws InvalidRecordIDException when no record found
			getRecord(recordId);
			records.remove(recordId);
			version++;
			lastModified = System.currentTimeMillis();
			size--;
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

		synchronized (this) {
			byte[] data = records.get(recordId);
			if (data == null) {
				recordStoreManager.loadRecord(this, recordId);
				data = records.get(recordId);
				if (data == null) {
					throw new InvalidRecordIDException();
				}
			}

			return data.length;
		}
	}

	@Override
	public int getRecord(int recordId, byte[] buffer, int offset) throws RecordStoreException {
		int recordSize;
		synchronized (this) {
			recordSize = getRecordSize(recordId);
			System.arraycopy(records.get(recordId), 0, buffer, offset, recordSize);
		}

		fireRecordListener(ExtendedRecordListener.RECORD_READ, recordId);

		return recordSize;
	}

	@Override
	public byte[] getRecord(int recordId) throws RecordStoreException {
		if (!open) {
			throw new RecordStoreNotOpenException();
		}

		byte[] data;

		synchronized (this) {
			data = new byte[getRecordSize(recordId)];
			getRecord(recordId, data, 0);
		}

		return data.length < 1 ? null : data;
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

		synchronized (this) {
			// throws InvalidRecordIDException when no record found
			getRecord(recordId);
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
