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

import android.bluetooth.BluetoothDevice;

import java.io.IOException;

import javax.microedition.io.Connection;

public class RemoteDevice {
	BluetoothDevice dev;

	RemoteDevice(BluetoothDevice dev) {
		this.dev = dev;
	}

	static String javaToAndroidAddress(String addr) {
		StringBuilder sb = new StringBuilder(addr);
		for (int i = 2; i < sb.length(); i += 3)
			sb.insert(i, ':');
		return sb.toString();
	}

	protected RemoteDevice(String address) {
		if (address == null) {
			throw new NullPointerException("address is null");
		}

		dev = DiscoveryAgent.adapter.getRemoteDevice(javaToAndroidAddress(address));
	}

	public String getFriendlyName(boolean alwaysAsk) throws IOException {
		String name = dev.getName();
		if (name == null) {
			name =  "";
		}
		return name;
	}

	public final String getBluetoothAddress() {
		return dev.getAddress().replace(":", "");
	}

	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof RemoteDevice))
			return false;
		return dev.equals(((RemoteDevice) obj).dev);
	}

	public int hashCode() {
		return dev.hashCode();
	}

	public static RemoteDevice getRemoteDevice(Connection conn) throws IOException {
		if (conn == null)
			throw new NullPointerException("conn is null");
		if (!(conn instanceof org.microemu.cldc.btspp.SPPConnectionImpl
				|| conn instanceof org.microemu.cldc.btl2cap.L2CAPConnectionImpl))
			throw new java.lang.IllegalArgumentException("not a RFCOMM connection");

		if (conn instanceof org.microemu.cldc.btspp.SPPConnectionImpl) {
			org.microemu.cldc.btspp.SPPConnectionImpl connection =
					(org.microemu.cldc.btspp.SPPConnectionImpl) conn;
			if (connection.socket == null)
				throw new IOException("socket is null");
			return new RemoteDevice(connection.socket.getRemoteDevice());
		} else {
			org.microemu.cldc.btl2cap.L2CAPConnectionImpl connection =
					(org.microemu.cldc.btl2cap.L2CAPConnectionImpl) conn;
			if (connection.socket == null)
				throw new IOException("socket is null");
			return new RemoteDevice(connection.socket.getRemoteDevice());
		}
	}

	public boolean authenticate() throws IOException {
		return false;
	}

	public boolean authorize(javax.microedition.io.Connection conn) throws IOException {
		return false;
	}

	public boolean encrypt(javax.microedition.io.Connection conn, boolean on) throws IOException {
		return false;
	}

	public boolean isAuthenticated() {
		return false;
	}

	public boolean isAuthorized(javax.microedition.io.Connection conn) throws IOException {
		return false;
	}

	public boolean isEncrypted() {
		return false;
	}

	public boolean isTrustedDevice() {
		return false;
	}
}
