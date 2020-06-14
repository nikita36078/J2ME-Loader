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

import java.util.HashMap;

class J2MEServiceRecord implements ServiceRecord {
	private RemoteDevice dev;
	private UUID uuid;
	private boolean skipAfterWrite;
	private boolean btl2cap;
	private HashMap<Integer, DataElement> dataElements = new HashMap<>();

	public J2MEServiceRecord(RemoteDevice dev, UUID uuid, boolean skipAfterWrite, boolean btl2cap) {
		this.dev = dev;
		this.uuid = uuid;
		this.skipAfterWrite = skipAfterWrite;
		this.btl2cap = btl2cap;
	}

	public RemoteDevice getHostDevice() {
		return dev;
	}

	public String getConnectionURL(int requiredSecurity, boolean mustBeMaster) {
		StringBuilder sb;
		if (btl2cap)
			sb = new StringBuilder("btl2cap://");
		else
			sb = new StringBuilder("btspp://");
		if (dev != null)
			sb.append(dev.getBluetoothAddress());
		else
			sb.append("localhost");
		sb.append(":");
		sb.append(uuid.toString());

		switch (requiredSecurity) {
			case NOAUTHENTICATE_NOENCRYPT:
				sb.append(";authenticate=false;encrypt=false");
				break;
			case AUTHENTICATE_NOENCRYPT:
				sb.append(";authenticate=true;encrypt=false");
				break;
			case AUTHENTICATE_ENCRYPT:
				sb.append(";authenticate=true;encrypt=true");
				break;
			default:
				throw new IllegalArgumentException();
		}

		if (mustBeMaster)
			sb.append(";master=true");
		else
			sb.append(";master=false");

		if (skipAfterWrite)
			sb.append(";skipAfterWrite=true");

		return sb.toString();
	}

	public boolean setAttributeValue(int attrID, DataElement attrValue) {
		if (attrID == 0)
			throw new IllegalArgumentException("attrID is ServiceRecordHandle (0x0000)");
		if (attrValue == null)
			return false;
		dataElements.put(attrID, attrValue);
		return true;
	}

	public DataElement getAttributeValue(int attrID) {
		return dataElements.get(attrID);
	}

	public int[] getAttributeIDs() {
		int[] arr = new int[dataElements.size()];
		int i = 0;
		for (Integer val : dataElements.keySet()) arr[i++] = val;
		return arr;
	}

	public void setDeviceServiceClasses(int classes) {
	}

	public boolean populateRecord(int[] attrIDs) {
		if (attrIDs == null)
			throw new NullPointerException();
		for (int val : attrIDs) {
			if (dataElements.containsKey(val)) return true;
		}
		return false;
	}
}
