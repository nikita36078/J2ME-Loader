package javax.bluetooth;

import android.bluetooth.BluetoothClass;

public class DeviceClass {
	private static final int SERVICE_MASK = 0xffe000;
	private static final int MAJOR_MASK = 0x001f00;
	private static final int MINOR_MASK = 0x0000fc;

	int record;
	BluetoothClass cls = null;

	public DeviceClass(int record) {
		if ((record & 0xff000000) != 0)
			throw new IllegalArgumentException();
		this.record = record;
	}

	DeviceClass(BluetoothClass cls) {
		this.cls = cls;
		record = cls.getDeviceClass();

		for (int i = 0; i < 11; i++) {
			int service = 1 << (13 + i);
			record |= (cls.hasService(service)) ? service : 0;
		}
	}

	public int getServiceClasses() {
		return record & SERVICE_MASK;
	}

	public int getMajorDeviceClass() {
		return record & MAJOR_MASK;
	}

	public int getMinorDeviceClass() {
		return record & MINOR_MASK;
	}

}
