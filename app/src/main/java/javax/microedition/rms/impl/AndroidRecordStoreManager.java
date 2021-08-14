/*
 * MicroEmulator
 * Copyright (C) 2008 Bartek Teodorczyk <barteo@barteo.net>
 * Copyright (C) 2017-2018 Nikita Shakarun
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

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.shell.AppClassLoader;
import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.util.FileUtils;

public class AndroidRecordStoreManager implements RecordStoreManager {
	private static final String TAG = "RecordStore";

	private final static String RECORD_STORE_HEADER_SUFFIX = ".rsh";

	private final static String RECORD_STORE_RECORD_SUFFIX = ".rsr";

	private final static Object NULL_STORE = new Object();


	private Map<String, Object> recordStores = null;

	@Override
	public String getName() {
		return "Android record store";
	}

	private synchronized void initializeIfNecessary() {
		if (recordStores == null) {
			recordStores = new ConcurrentHashMap<>();
			String[] list = new File(AppClassLoader.getDataDir()).list();
			if (list != null) {
				for (String fileName : list) {
					if (fileName.endsWith(RECORD_STORE_HEADER_SUFFIX)) {
						recordStores.put(fileName.substring(0,
								fileName.length() - RECORD_STORE_HEADER_SUFFIX.length()), NULL_STORE);
					}
				}
			}
		}
	}

	@Override
	public void deleteRecordStore(String recordStoreName) throws RecordStoreException {
		initializeIfNecessary();

		recordStoreName = recordStoreName.replaceAll(FileUtils.ILLEGAL_FILENAME_CHARS, "");
		Object value = recordStores.get(recordStoreName);
		if (value == null) {
			throw new RecordStoreNotFoundException(recordStoreName);
		}
		if (value instanceof RecordStoreImpl && ((RecordStoreImpl) value).isOpen()) {
			throw new RecordStoreException();
		}

		File dataDir = new File(AppClassLoader.getDataDir());
		String prefix = recordStoreName + ".";
		String[] files = dataDir.list();
		if (files != null) {
			for (String name : files) {
				int dot = name.indexOf('.', prefix.length() + 1);
				if ((dot == -1 || dot == name.lastIndexOf('.')) && name.startsWith(prefix)) {
					//noinspection ResultOfMethodCallIgnored
					new File(dataDir, name).delete();
				}
			}
		}

		recordStores.remove(recordStoreName);
		Log.d(TAG, "RecordStore " + recordStoreName + " deleted");
	}

	@Override
	public RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary)
			throws RecordStoreException {
		initializeIfNecessary();
		recordStoreName = recordStoreName.replaceAll(FileUtils.ILLEGAL_FILENAME_CHARS, "");

		Object value = recordStores.get(recordStoreName);
		if (value instanceof RecordStoreImpl && ((RecordStoreImpl) value).isOpen()) {
			((RecordStoreImpl) value).setOpen();
			return (RecordStoreImpl) value;
		}

		RecordStoreImpl recordStoreImpl;
		String headerName = getHeaderFileName(recordStoreName);
		File headerFile = new File(AppClassLoader.getDataDir(), headerName);
		try (DataInputStream dis = new DataInputStream(new FileInputStream(headerFile))) {
			recordStoreImpl = new RecordStoreImpl(this);
			recordStoreImpl.readHeader(dis);
			recordStoreImpl.setOpen();
		} catch (FileNotFoundException e) {
			if (!createIfNecessary) {
				throw new RecordStoreNotFoundException(recordStoreName);
			}
			recordStoreImpl = new RecordStoreImpl(this, recordStoreName);
			recordStoreImpl.setOpen();
			saveToDisk(recordStoreImpl, -1);
		} catch (IOException e) {
			Log.w(TAG, "openRecordStore: broken header " + headerFile, e);
			recordStoreImpl = new RecordStoreImpl(this, recordStoreName);
			recordStoreImpl.setOpen();
			saveToDisk(recordStoreImpl, -1);
		}

		recordStores.put(recordStoreName, recordStoreImpl);
		synchronized (recordStoreImpl.records) {
			File dataDir = new File(AppClassLoader.getDataDir());
			String prefix = recordStoreName + ".";
			String[] files = dataDir.list();
			if (files != null) {
				for (String name : files) {
					if (name.startsWith(prefix) && name.endsWith(RECORD_STORE_RECORD_SUFFIX)) {
						File file = new File(dataDir, name);
						try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
							recordStoreImpl.readRecord(dis);
						} catch (IOException e) {
							Log.w(TAG, "loadFromDisk: broken record " + file, e);
							int pLen = prefix.length();
							int sLen = RECORD_STORE_RECORD_SUFFIX.length();
							int nLen = name.length();
							if (pLen + sLen < nLen) {
								try {
									int recordId = Integer.parseInt(name.substring(pLen, nLen - sLen));
									recordStoreImpl.records.put(recordId, new byte[0]);
								} catch (NumberFormatException numberFormatException) {
									Log.w(TAG, "loadFromDisk: ERROR stubbing broken record " + file);
								}
							}
						}
					}
				}
			}

		}

		Log.d(TAG, "RecordStore " + recordStoreName + " opened");
		return recordStoreImpl;
	}

	@Override
	public String[] listRecordStores() {
		initializeIfNecessary();

		String[] result = recordStores.keySet().toArray(new String[0]);

		if (result.length > 0) {
			return result;
		} else {
			return null;
		}
	}

	@Override
	public void deleteRecord(RecordStoreImpl recordStoreImpl, int recordId)
			throws RecordStoreException {
		deleteFromDisk(recordStoreImpl, recordId);
	}

	@Override
	public void loadRecord(RecordStoreImpl recordStoreImpl, int recordId)
			throws RecordStoreException {
		String recordName = getRecordFileName(recordStoreImpl.getName(), recordId);
		try (DataInputStream dis = new DataInputStream(ContextHolder.openFileInput(recordName))) {
			recordStoreImpl.readRecord(dis);
		} catch (FileNotFoundException e) {
			throw new InvalidRecordIDException();
		} catch (IOException e) {
			Log.e(TAG, "RecordStore.loadFromDisk: ERROR reading " + recordName, e);
		}
	}

	@Override
	public void saveRecord(RecordStoreImpl recordStoreImpl, int recordId)
			throws RecordStoreException {
		saveToDisk(recordStoreImpl, recordId);
	}

	private synchronized void deleteFromDisk(RecordStoreImpl recordStore, int recordId)
			throws RecordStoreException {
		String headerName = getHeaderFileName(recordStore.getName());
		try (DataOutputStream dos = new DataOutputStream(ContextHolder.openFileOutput(headerName))) {
			recordStore.writeHeader(dos);
		} catch (IOException e) {
			Log.e(TAG, "RecordStore.saveToDisk: ERROR writing object to " + headerName, e);
			throw new RecordStoreException(e.getMessage());
		}

		ContextHolder.deleteFile(getRecordFileName(recordStore.getName(), recordId));
	}

	/**
	 * @param recordId -1 for storing only header
	 */
	private synchronized void saveToDisk(RecordStoreImpl recordStore, int recordId)
			throws RecordStoreException {
		String headerName = getHeaderFileName(recordStore.getName());
		try (DataOutputStream dos = new DataOutputStream(ContextHolder.openFileOutput(headerName))) {
			recordStore.writeHeader(dos);
		} catch (IOException e) {
			Log.e(TAG, "RecordStore.saveToDisk: ERROR writing object to " + headerName, e);
			throw new RecordStoreException(e.getMessage());
		}

		if (recordId != -1) {
			String recordName = getRecordFileName(recordStore.getName(), recordId);
			try (DataOutputStream dos = new DataOutputStream(ContextHolder.openFileOutput(recordName))) {
				recordStore.writeRecord(dos, recordId);
			} catch (IOException e) {
				Log.e(TAG, "RecordStore.saveToDisk: ERROR writing object to " + recordName, e);
				throw new RecordStoreException(e.getMessage());
			}
		}
	}

	@Override
	public int getSizeAvailable(RecordStoreImpl recordStoreImpl) {
		// TODO should return free space on device
		return 1024 * 1024;
	}

	private String getHeaderFileName(String recordStoreName) {
		return recordStoreName + RECORD_STORE_HEADER_SUFFIX;
	}

	private String getRecordFileName(String recordStoreName, int recordId) {
		return recordStoreName + "." + recordId + RECORD_STORE_RECORD_SUFFIX;
	}
}
