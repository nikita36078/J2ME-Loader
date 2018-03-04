package javax.bluetooth;

import javax.microedition.io.Connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.app.Activity;
import javax.microedition.util.ActivityResultListener;
import java.util.Hashtable;
import javax.microedition.util.ContextHolder;
import javax.microedition.shell.MicroActivity;

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
		if (!DiscoveryAgent.adapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			((MicroActivity) ContextHolder.getCurrentActivity()).forceActiveState();
			ContextHolder.getCurrentActivity().startActivityForResult(enableBtIntent, 2);
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
		// Will this work?
		return new DeviceClass(DiscoveryAgent.adapter.getRemoteDevice(DiscoveryAgent.adapter.getAddress()).getBluetoothClass().getDeviceClass());
	}

	public boolean setDiscoverable(int mode) throws BluetoothStateException {
		if ((mode != DiscoveryAgent.GIAC) && (mode != DiscoveryAgent.LIAC) && (mode != DiscoveryAgent.NOT_DISCOVERABLE)
				&& (mode < 0x9E8B00 || mode > 0x9E8B3F)) {
			throw new IllegalArgumentException("Invalid discoverable mode");
		}

		if (lock)
			return true;

		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		lock = true;
		((MicroActivity) ContextHolder.getCurrentActivity()).forceActiveState();
		ContextHolder.getCurrentActivity().startActivityForResult(discoverableIntent, 1);
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

	static String androidToJavaAddress(String addr) {
		return addr.replaceAll(":", "");
	}

	public String getBluetoothAddress() {
		return androidToJavaAddress(DiscoveryAgent.adapter.getAddress());
	}

	public ServiceRecord getRecord(Connection notifier) {
		if (notifier == null) {
			throw new NullPointerException("notifier is null");
		}

		if (notifier instanceof org.microemu.cldc.btspp.Connection) {
			org.microemu.cldc.btspp.Connection conn = (org.microemu.cldc.btspp.Connection) notifier;
			if (conn.socket == null)
				// probably calling this for local device, so socket isn't opened
				return new J2MEServiceRecord(null, conn.connUuid, false);
			else
				return new J2MEServiceRecord(new RemoteDevice(conn.socket.getRemoteDevice()), conn.connUuid, false);
		} else
			throw new IllegalArgumentException("notifier is not BTSPP connection");
	}

	// Not supported on Android due to API limitations
	public void updateRecord(ServiceRecord srvRecord) throws ServiceRegistrationException {
		if (srvRecord == null) {
			throw new NullPointerException("Service Record is null");
		}
	}

}
