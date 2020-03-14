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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.os.Parcelable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;

import javax.microedition.util.ContextHolder;

public class DiscoveryAgent {
	public static final int NOT_DISCOVERABLE = 0;
	public static final int GIAC = 0x9E8B33;
	public static final int LIAC = 0x9E8B00;
	public static final int CACHED = 0x00;
	public static final int PREKNOWN = 0x01;

	private static int maxID = 1;

	static BluetoothAdapter adapter;

	private class Transaction extends BroadcastReceiver {
		public final int transID;
		public final int[] attrs;
		public final UUID[] uuids;
		public final RemoteDevice dev;
		public final DiscoveryListener listener;
		public volatile boolean stop = false;
		public volatile boolean discovering = false;

		private String serviceName = null;
		private boolean btl2cap = false;
		private int id;

		public Transaction(int transID, int[] attrs, UUID[] uuids, RemoteDevice dev, DiscoveryListener listener) {
			this.transID = transID;
			this.attrs = attrs;
			this.uuids = uuids;
			this.dev = dev;
			this.listener = listener;
		}

		public boolean equals(Object obj) {
			if (obj == null || !(obj instanceof Transaction))
				return false;
			return (((Transaction) obj).transID == transID);
		}

		// Android 6.0.1 bug: UUID is reversed
		// see https://issuetracker.google.com/issues/37075233
		private java.util.UUID byteSwappedUuid(java.util.UUID toSwap) {
			ByteBuffer buffer = ByteBuffer.allocate(16);
			buffer.putLong(toSwap.getLeastSignificantBits()).putLong(toSwap.getMostSignificantBits());
			buffer.rewind();
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			return new java.util.UUID(buffer.getLong(), buffer.getLong());
		}

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_UUID.equals(action)) {
				BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (d.equals(dev.dev)) {
					LinkedList<J2MEServiceRecord> records = new LinkedList<J2MEServiceRecord>();
					UUID[] uuidExtra = null;
					UUID SppUuid = new UUID(0x1101);
					UUID NameUuid = new UUID(0x1102);
					// SE phones publish a SPP service UUID instead of requested one
					boolean supportsSPP = false;
					{
						Parcelable[] uuidParcel = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
						if (uuidParcel != null) {
							uuidExtra = new UUID[uuidParcel.length];
							for (int i = 0; i < uuidExtra.length; i++)
								uuidExtra[i] = new UUID(((ParcelUuid) uuidParcel[i]).getUuid());
						}
					}

					for (int i = 0; !stop && (uuidExtra != null) && (i < uuidExtra.length); i++) {
						if (uuidExtra[i].equals(SppUuid))
							supportsSPP = true;
						if (uuidExtra[i].equals(NameUuid)) {
							// Workaround to get service name
							if (!btl2cap && serviceName == null) {
								try {
									BluetoothSocket bluetoothSocket = dev.dev.createInsecureRfcommSocketToServiceRecord(NameUuid.uuid);
									if (!bluetoothSocket.isConnected()) {
										bluetoothSocket.connect();
									}
									InputStream is = bluetoothSocket.getInputStream();
									byte[] resByte = new byte[256];
									btl2cap = is.read() == 1;
									is.read(resByte);
									if (attrs != null && attrs.length > 0) {
										serviceName = new String(resByte).trim();
									}
									bluetoothSocket.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
						for (int j = 0; !stop && j < uuids.length; j++) {
							if (uuidExtra[i].uuid.equals(uuids[j].uuid) || uuidExtra[i].uuid.equals(byteSwappedUuid(uuids[j].uuid))) {
								J2MEServiceRecord record = new J2MEServiceRecord(dev, uuids[j], false, btl2cap);
								records.add(record);
							}
						}
					}
					if (serviceName != null) {
						for (J2MEServiceRecord record : records) {
							record.setServiceName(serviceName);
						}
					}

					if (records.isEmpty()) {
						if (supportsSPP) {
							listener.servicesDiscovered(transID, new J2MEServiceRecord[]
									{new J2MEServiceRecord(dev, new UUID(0x1101), true, false)});
						}
					} else {
						J2MEServiceRecord[] casted = records.toArray(new J2MEServiceRecord[0]);
						listener.servicesDiscovered(transID, casted);
					}
					listener.serviceSearchCompleted(transID, (records.isEmpty() && !supportsSPP) ? DiscoveryListener.SERVICE_SEARCH_NO_RECORDS :
							stop ? DiscoveryListener.SERVICE_SEARCH_TERMINATED : DiscoveryListener.SERVICE_SEARCH_COMPLETED);
					ContextHolder.getAppContext().unregisterReceiver(this);
					synchronized (transList) {
						transList.remove(this);
					}
				}
			}
		}

	}

	private LinkedList<Transaction> transList = new LinkedList<>();
	private HashSet<BluetoothDevice> discoveredList = new HashSet<>();

	DiscoveryAgent() throws BluetoothStateException {
		adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter == null)
			throw new BluetoothStateException();
	}

	public RemoteDevice[] retrieveDevices(int option) {
		Set<BluetoothDevice> set;
		if (option == CACHED) {
			set = discoveredList;
		} else if (option == PREKNOWN) {
			set = adapter.getBondedDevices();
		} else {
			throw new IllegalArgumentException();
		}
		RemoteDevice[] devices = new RemoteDevice[set.size()];
		int i = 0;
		for (BluetoothDevice device : set) devices[i++] = new RemoteDevice(device);
		return devices;
	}

	public boolean startInquiry(int accessCode, final DiscoveryListener listener) throws BluetoothStateException {
		if (listener == null) {
			throw new NullPointerException("DiscoveryListener is null");
		}
		if ((accessCode != LIAC) && (accessCode != GIAC) && ((accessCode < 0x9E8B00) || (accessCode > 0x9E8B3F))) {
			throw new IllegalArgumentException("Invalid accessCode " + accessCode);
		}

		if (adapter.isDiscovering())
			return false;

		synchronized (transList) {
			if (!transList.isEmpty())
				return false;
		}

		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

		// MTK do not send ACTION_DISCOVERY_FINISHED
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(15000);
					if (adapter.isDiscovering())
						adapter.cancelDiscovery();
				} catch (InterruptedException e) {
				}
			}
		}).start();

		ContextHolder.getAppContext().registerReceiver(new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (BluetoothDevice.ACTION_FOUND.equals(action)) {
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					if (discoveredList.add(device)) {
						RemoteDevice dev = new RemoteDevice(device);
						DeviceClass cod = new DeviceClass();
						listener.deviceDiscovered(dev, cod);
					}
				} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
					listener.inquiryCompleted(DiscoveryListener.INQUIRY_COMPLETED);
					synchronized (transList) {
						if (!transList.isEmpty()) {
							for (Transaction t : transList) {
								if (!t.discovering) {
									t.dev.dev.fetchUuidsWithSdp();
									t.discovering = true;
								}
							}
						}
					}
					ContextHolder.getAppContext().unregisterReceiver(this);
				}
			}
		}, filter);

		discoveredList.clear();
		return adapter.startDiscovery();
	}

	public boolean cancelInquiry(DiscoveryListener listener) {
		if (listener == null) {
			throw new NullPointerException("DiscoveryListener is null");
		}
		boolean ret = adapter.cancelDiscovery();
		listener.inquiryCompleted(DiscoveryListener.INQUIRY_TERMINATED);
		return ret;
	}

	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice btDev, DiscoveryListener listener)
			throws BluetoothStateException {
		if (uuidSet == null) {
			throw new NullPointerException("uuidSet is null");
		}
		if (uuidSet.length == 0) {
			// The same as on Motorola, Nokia and SE Phones
			throw new IllegalArgumentException("uuidSet is empty");
		}
		for (int u1 = 0; u1 < uuidSet.length; u1++) {
			for (int u2 = u1 + 1; u2 < uuidSet.length; u2++) {
				if (uuidSet[u1].equals(uuidSet[u2])) {
					throw new IllegalArgumentException("uuidSet has duplicate values " + uuidSet[u1].toString());
				}
			}
		}
		if (btDev == null) {
			throw new NullPointerException("RemoteDevice is null");
		}
		if (listener == null) {
			throw new NullPointerException("DiscoveryListener is null");
		}
		for (int i = 0; attrSet != null && i < attrSet.length; i++) {
			if (attrSet[i] < 0x0000 || attrSet[i] > 0xffff) {
				throw new IllegalArgumentException("attrSet[" + i + "] not in range");
			}
		}

		final Transaction curTrans = new Transaction(maxID, attrSet, uuidSet, btDev, listener);
		transList.add(curTrans);
		ContextHolder.getAppContext().registerReceiver(curTrans, new IntentFilter(BluetoothDevice.ACTION_UUID));

		if (!adapter.isDiscovering()) {
			synchronized (transList) {
				for (Transaction t : transList) {
					if (!t.discovering) {
						t.dev.dev.fetchUuidsWithSdp();
						t.discovering = true;
					}
				}
			}
		}
		return maxID++;
	}

	public boolean cancelServiceSearch(int transID) {
		synchronized (transList) {
			ListIterator<Transaction> iter = transList.listIterator();
			while (iter.hasNext()) {
				Transaction trans = iter.next();
				if (trans.transID == transID) {
					trans.stop = true;
					break;
				}
			}
		}
		return true;
	}

	// TODO
	public String selectService(UUID uuid, int security, boolean master) throws BluetoothStateException {
		return null;
	}

}
