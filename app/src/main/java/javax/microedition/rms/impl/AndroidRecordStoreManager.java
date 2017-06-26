/**
 * MicroEmulator
 * Copyright (C) 2008 Bartek Teodorczyk <barteo@barteo.net>
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
 *
 * @version $Id$
 */

package javax.microedition.rms.impl;

import android.content.Context;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;
import javax.microedition.shell.MyClassLoader;
import javax.microedition.util.ContextHolder;

public class AndroidRecordStoreManager implements RecordStoreManager {

	private final static String RECORD_STORE_HEADER_SUFFIX = ".rsh";

	private final static String RECORD_STORE_RECORD_SUFFIX = ".rsr";

	private final static Object NULL_STORE = new Object();

	private Context activity;

	private String TAG = "RecordStore";

	private ConcurrentHashMap<String, Object> recordStores = null;

	private ExtendedRecordListener recordListener = null;

	public AndroidRecordStoreManager() {
	}

	public String getName() {
		return "Android record store";
	}

	private synchronized void initializeIfNecessary() {
		this.activity = ContextHolder.getContext();
		if (recordStores == null) {
			recordStores = new ConcurrentHashMap<String, Object>();
			String[] list = new File(activity.getFilesDir() + MyClassLoader.getName()).list();
			if (list != null && list.length > 0) {
				for (int i = 0; i < list.length; i++) {
					if (list[i].endsWith(RECORD_STORE_HEADER_SUFFIX)) {
						recordStores.put(
								list[i].substring(0, list[i].length() - RECORD_STORE_HEADER_SUFFIX.length()),
								NULL_STORE);
					}
				}
			}
		}
	}

	public void deleteRecordStore(final String recordStoreName)
			throws RecordStoreNotFoundException, RecordStoreException {
		initializeIfNecessary();

		Object value = recordStores.get(recordStoreName);
		if (value == null) {
			throw new RecordStoreNotFoundException(recordStoreName);
		}
		if (value instanceof RecordStoreImpl && ((RecordStoreImpl) value).isOpen()) {
			throw new RecordStoreException();
		}

		RecordStoreImpl recordStoreImpl;
		try {
			DataInputStream dis = new DataInputStream(ContextHolder.openFileInput(getHeaderFileName(recordStoreName)));
			recordStoreImpl = new RecordStoreImpl(this);
			recordStoreImpl.readHeader(dis);
			dis.close();
		} catch (IOException e) {
			Log.e(TAG, "RecordStore.deleteRecordStore: ERROR reading " + getHeaderFileName(recordStoreName), e);
			throw new RecordStoreException();
		}

		recordStoreImpl.setOpen(true);
		RecordEnumeration re = recordStoreImpl.enumerateRecords(null, null, false);
		while (re.hasNextElement()) {
			ContextHolder.deleteFile(getRecordFileName(recordStoreName, re.nextRecordId()));
		}
		recordStoreImpl.setOpen(false);
		ContextHolder.deleteFile(getHeaderFileName(recordStoreName));

		recordStores.remove(recordStoreName);

		fireRecordStoreListener(ExtendedRecordListener.RECORDSTORE_DELETE, recordStoreName);
	}

	public RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary)
			throws RecordStoreException {
		initializeIfNecessary();

		RecordStoreImpl recordStoreImpl;
		try {
			DataInputStream dis = new DataInputStream(
					ContextHolder.openFileInput(getHeaderFileName(recordStoreName)));
			recordStoreImpl = new RecordStoreImpl(this);
			recordStoreImpl.readHeader(dis);
			recordStoreImpl.setOpen(true);
			dis.close();
		} catch (FileNotFoundException e) {
			if (!createIfNecessary) {
				throw new RecordStoreNotFoundException(recordStoreName);
			}
			recordStoreImpl = new RecordStoreImpl(this, recordStoreName);
			recordStoreImpl.setOpen(true);
			saveToDisk(recordStoreImpl, -1);
		} catch (IOException e) {
			throw new RecordStoreException();
		}
		if (recordListener != null) {
			recordStoreImpl.addRecordListener(recordListener);
		}

		recordStores.put(recordStoreName, recordStoreImpl);

		fireRecordStoreListener(ExtendedRecordListener.RECORDSTORE_OPEN, recordStoreName);

		return recordStoreImpl;
	}

	public String[] listRecordStores() {
		initializeIfNecessary();

		String[] result = recordStores.keySet().toArray(new String[0]);

		if (result.length > 0) {
			return result;
		} else {
			return null;
		}
	}

	public void deleteRecord(RecordStoreImpl recordStoreImpl, int recordId)
			throws RecordStoreNotOpenException, RecordStoreException {
		deleteFromDisk(recordStoreImpl, recordId);
	}

	public void loadRecord(RecordStoreImpl recordStoreImpl, int recordId)
			throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
		try {
			DataInputStream dis = new DataInputStream(
					ContextHolder.openFileInput(getRecordFileName(recordStoreImpl.getName(), recordId)));
			recordStoreImpl.readRecord(dis);
			dis.close();
		} catch (FileNotFoundException e) {
			throw new InvalidRecordIDException();
		} catch (IOException e) {
			Log.e(TAG, "RecordStore.loadFromDisk: ERROR reading " + getRecordFileName(recordStoreImpl.getName(), recordId), e);
		}
	}


	public void saveRecord(RecordStoreImpl recordStoreImpl, int recordId)
			throws RecordStoreNotOpenException, RecordStoreException {
		saveToDisk(recordStoreImpl, recordId);
	}

	public void init() {
	}

	public void deleteStores() {
		String[] stores = listRecordStores();
		for (int i = 0; i < stores.length; i++) {
			String store = stores[i];
			try {
				deleteRecordStore(store);
			} catch (RecordStoreException e) {
				Log.d(TAG, "deleteRecordStore", e);
			}
		}
	}

	private synchronized void deleteFromDisk(RecordStoreImpl recordStore, int recordId)
			throws RecordStoreException {
		try {
			DataOutputStream dos = new DataOutputStream(
					ContextHolder.openFileOutput(getHeaderFileName(recordStore.getName()), Context.MODE_PRIVATE));
			recordStore.writeHeader(dos);
			dos.close();
		} catch (IOException e) {
			Log.e(TAG, "RecordStore.saveToDisk: ERROR writting object to " + getHeaderFileName(recordStore.getName()), e);
			throw new RecordStoreException(e.getMessage());
		}

		ContextHolder.deleteFile(getRecordFileName(recordStore.getName(), recordId));
	}

	/**
	 * @param recordId -1 for storing only header
	 */
	private synchronized void saveToDisk(RecordStoreImpl recordStore, int recordId)
			throws RecordStoreException {
		try {
			DataOutputStream dos = new DataOutputStream(
					ContextHolder.openFileOutput(getHeaderFileName(recordStore.getName()), Context.MODE_PRIVATE));
			recordStore.writeHeader(dos);
			dos.close();
		} catch (IOException e) {
			Log.e(TAG, "RecordStore.saveToDisk: ERROR writting object to " + getHeaderFileName(recordStore.getName()), e);
			throw new RecordStoreException(e.getMessage());
		}

		if (recordId != -1) {
			try {
				DataOutputStream dos = new DataOutputStream(
						ContextHolder.openFileOutput(getRecordFileName(recordStore.getName(), recordId), Context.MODE_PRIVATE));
				recordStore.writeRecord(dos, recordId);
				dos.close();
			} catch (IOException e) {
				Log.e(TAG, "RecordStore.saveToDisk: ERROR writting object to " + getRecordFileName(recordStore.getName(), recordId), e);
				throw new RecordStoreException(e.getMessage());
			}
		}
	}

	public int getSizeAvailable(RecordStoreImpl recordStoreImpl) {
		// FIXME should return free space on device
		return 1024 * 1024;
	}

	public void setRecordListener(ExtendedRecordListener recordListener) {
		this.recordListener = recordListener;
	}

	public void fireRecordStoreListener(int type, String recordStoreName) {
		if (recordListener != null) {
			recordListener.recordStoreEvent(type, System.currentTimeMillis(), recordStoreName);
		}
	}

	private String getHeaderFileName(String recordStoreName) {
		return recordStoreName + RECORD_STORE_HEADER_SUFFIX;
	}

	private String getRecordFileName(String recordStoreName, int recordId) {
		return recordStoreName + "." + recordId + RECORD_STORE_RECORD_SUFFIX;
	}

}