/*
 * Copyright 2020 Nikita Shakarun
 * Copyright 2026 Yury Kharchenko
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import javax.bluetooth.L2CAPConnection;

public class L2CAPConnectionImpl implements L2CAPConnection {
	// Android exposes RFCOMM, which is a byte stream. Add a frame length so
	// one send() maps to one receive(), as J2ME L2CAP applications expect.
	private static final int MAX_FRAME_SIZE = 1024 * 1024;

	public BluetoothSocket socket;
	private final DataInputStream is;
	private final DataOutputStream os;

	public L2CAPConnectionImpl(BluetoothSocket socket) throws IOException {
		this.socket = socket;
		this.os = new DataOutputStream(socket.getOutputStream());
		this.is = new DataInputStream(socket.getInputStream());
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
		if (data == null) {
			throw new NullPointerException();
		}
		if (data.length > MAX_FRAME_SIZE) {
			throw new IOException("L2CAP frame is too large: " + data.length);
		}
		synchronized (os) {
			os.writeInt(data.length);
			os.write(data);
			os.flush();
		}
	}

	@Override
	public int receive(byte[] inBuf) throws IOException {
		if (inBuf == null) {
			throw new NullPointerException();
		}
		int length = is.readInt();
		if (length < 0 || length > MAX_FRAME_SIZE) {
			throw new IOException("Invalid L2CAP frame length: " + length);
		}
		int read = Math.min(length, inBuf.length);
		if (read > 0) {
			is.readFully(inBuf, 0, read);
		}
		int remaining = length - read;
		while (remaining > 0) {
			long skipped = is.skip(remaining);
			if (skipped > 0) {
				remaining -= (int) skipped;
			} else {
				byte[] discard = new byte[Math.min(remaining, 256)];
				int count = is.read(discard);
				if (count < 0) {
					throw new EOFException();
				}
				remaining -= count;
			}
		}
		return read;
	}

	@Override
	public boolean ready() throws IOException {
		return is.available() > 0;
	}

	@Override
	public void close() throws IOException {
		if (socket != null) {
			socket.close();
		}
	}
}
