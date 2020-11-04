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

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;

import java.util.Hashtable;

import javax.microedition.io.Connection;
import javax.microedition.util.ActivityResultListener;
import javax.microedition.util.ContextHolder;

public class LocalDevice implements ActivityResultListener {
	private static LocalDevice dev;
	private DiscoveryAgent agent;
	private static Hashtable<String, String> properties;
	private volatile boolean lock = false;
	private boolean cancelled = false;
	private Object monitor = new Object();

	static {
		properties = new Hashtable<String, String>();
		properties.put("bluetooth.api.version", "1.1");
		properties.put("bluetooth.master.switch", "true");
		properties.put("bluetooth.sd.attr.retrievable.max", "256");
		properties.put("bluetooth.connected.devices.max", "7");
		properties.put("bluetooth.l2cap.receiveMTU.max", "672");
		properties.put("bluetooth.sd.trans.max", "1");
		properties.put("bluetooth.connected.inquiry.scan", "true");
		properties.put("bluetooth.connected.page.scan", "true");
		properties.put("bluetooth.connected.inquiry", "true");
		properties.put("bluetooth.connected.page", "true");
	}

	private LocalDevice() throws BluetoothStateException {
		agent = new DiscoveryAgent();
		ContextHolder.addActivityResultListener(this);
		if (!ContextHolder.requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
			throw new BluetoothStateException();
		}
		if (!DiscoveryAgent.adapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			ContextHolder.getActivity().startActivityForResult(enableBtIntent, 2);
			synchronized (monitor) {
				try {
					monitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (cancelled)
				throw new BluetoothStateException();
			cancelled = false;
		}
	}

	public static LocalDevice getLocalDevice() throws BluetoothStateException {
		if (dev == null)
			dev = new LocalDevice();
		return dev;
	}

	public DiscoveryAgent getDiscoveryAgent() {
		return agent;
	}

	public String getFriendlyName() {
		return DiscoveryAgent.adapter.getName();
	}

	public DeviceClass getDeviceClass() {
		return new DeviceClass();
	}

	public boolean setDiscoverable(int mode) throws BluetoothStateException {
		if ((mode != DiscoveryAgent.GIAC) && (mode != DiscoveryAgent.LIAC) && (mode != DiscoveryAgent.NOT_DISCOVERABLE)
				&& (mode < 0x9E8B00 || mode > 0x9E8B3F)) {
			throw new IllegalArgumentException("Invalid discoverable mode");
		}

		if (lock || mode == DiscoveryAgent.NOT_DISCOVERABLE)
			return true;

		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		lock = true;
		ContextHolder.getActivity().startActivityForResult(discoverableIntent, 1);
		while (lock) {
			synchronized (monitor) {
				try {
					monitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					lock = false;
				}
			}
		}
		return true;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			synchronized (monitor) {
				lock = false;

				monitor.notifyAll();
			}
		} else if (requestCode == 2) {
			synchronized (monitor) {
				if (resultCode != Activity.RESULT_OK)
					cancelled = true;
				monitor.notifyAll();
			}
		}
	}

	public static boolean isPowerOn() {
		return BluetoothAdapter.getDefaultAdapter().isEnabled();
	}

	public int getDiscoverable() {
		int scanMode = DiscoveryAgent.adapter.getScanMode();
		switch (scanMode) {
			case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
				return DiscoveryAgent.LIAC;
			case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
				return DiscoveryAgent.GIAC;
			case BluetoothAdapter.SCAN_MODE_NONE:
			default:
				return DiscoveryAgent.NOT_DISCOVERABLE;
		}
	}

	public static String getProperty(String property) {
		return properties.get(property);
	}

	private static String androidToJavaAddress(String addr) {
		return addr.replaceAll(":", "");
	}

	public String getBluetoothAddress() {
		return androidToJavaAddress(DiscoveryAgent.adapter.getAddress());
	}

	public ServiceRecord getRecord(Connection notifier) {
		if (notifier == null) {
			throw new NullPointerException("notifier is null");
		}
		if (!(notifier instanceof org.microemu.cldc.btspp.Connection || notifier instanceof org.microemu.cldc.btl2cap.Connection))
			throw new java.lang.IllegalArgumentException("not a RFCOMM connection");

		if (notifier instanceof org.microemu.cldc.btspp.Connection) {
			org.microemu.cldc.btspp.Connection conn = (org.microemu.cldc.btspp.Connection) notifier;
			if (conn.socket == null)
				// probably calling this for local device, so socket isn't opened
				return new J2MEServiceRecord(null, conn.connUuid, false, false);
			else
				return new J2MEServiceRecord(new RemoteDevice(conn.socket.getRemoteDevice()), conn.connUuid, false, false);
		} else {
			org.microemu.cldc.btl2cap.Connection conn = (org.microemu.cldc.btl2cap.Connection) notifier;
			if (conn.socket == null)
				// probably calling this for local device, so socket isn't opened
				return new J2MEServiceRecord(null, conn.connUuid, false, true);
			else
				return new J2MEServiceRecord(new RemoteDevice(conn.socket.getRemoteDevice()), conn.connUuid, false, true);
		}
	}

	// Not supported on Android due to API limitations
	public void updateRecord(ServiceRecord srvRecord) throws ServiceRegistrationException {
		if (srvRecord == null) {
			throw new NullPointerException("Service Record is null");
		}
	}
}
