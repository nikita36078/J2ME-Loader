/*
 * Copyright 2020 Nikita Shakarun
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

package org.microemu.cldc.btl2cap;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.bluetooth.L2CAPConnection;

public class L2CAPConnectionImpl implements L2CAPConnection {
	public BluetoothSocket socket;
	private OutputStream os;
	private InputStream is;

	public L2CAPConnectionImpl(BluetoothSocket socket) throws IOException {
		this.socket = socket;
		this.os = socket.getOutputStream();
		this.is = socket.getInputStream();
	}

	@Override
	public int getTransmitMTU() throws IOException {
		return L2CAPConnection.DEFAULT_MTU;
	}

	@Override
	public int getReceiveMTU() throws IOException {
		return L2CAPConnection.DEFAULT_MTU;
	}

	@Override
	public void send(byte[] data) throws IOException {
		os.write(data);
	}

	@Override
	public int receive(byte[] inBuf) throws IOException {
		return is.read(inBuf);
	}

	@Override
	public boolean ready() throws IOException {
		return is.available() > 0;
	}

	@Override
	public void close() throws IOException {
		if (socket != null)
			socket.close();
	}
}
