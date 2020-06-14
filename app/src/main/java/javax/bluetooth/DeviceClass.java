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

import android.bluetooth.BluetoothClass;

public class DeviceClass {
	private static final int SERVICE_MASK = 0xffe000;
	private static final int MAJOR_MASK = 0x001f00;
	private static final int MINOR_MASK = 0x0000fc;

	private int record;

	public DeviceClass(int record) {
		if ((record & 0xff000000) != 0)
			throw new IllegalArgumentException();
		this.record = record;
	}

	DeviceClass() {
		this(BluetoothClass.Device.PHONE_CELLULAR | BluetoothClass.Service.TELEPHONY);
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
