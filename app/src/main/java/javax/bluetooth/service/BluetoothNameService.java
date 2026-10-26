/*
 * Copyright 2026 Nikita Shakarun
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

package javax.bluetooth.service;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import org.microemu.microedition.io.ServiceRecordHolder;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.bluetooth.ServiceRecord;

public final class BluetoothNameService extends Thread {
	private static final String TAG = BluetoothNameService.class.getName();
	private final BluetoothServerSocket serverSocket;
	private final String name;
	private final boolean l2cap;
	private final ServiceRecordHolder recordHolder;

	public BluetoothNameService(BluetoothServerSocket serverSocket, String name, boolean l2cap,
								ServiceRecordHolder recordHolder) {
		this.serverSocket = serverSocket;
		this.name = name;
		this.l2cap = l2cap;
		this.recordHolder = recordHolder;
		setDaemon(true);
	}

	@Override
	public void run() {
		try {
			byte[] dstByte = new byte[256];
			byte[] srcByte = name.getBytes();
			System.arraycopy(srcByte, 0, dstByte, 0, Math.min(srcByte.length, dstByte.length));
			BluetoothSocket connectSocket = serverSocket.accept();
			OutputStream os = connectSocket.getOutputStream();
			DataOutputStream socketStream = new DataOutputStream(os);

			socketStream.write(l2cap ? 1 : 0);
			socketStream.write(dstByte);

			ServiceRecord record = recordHolder.getServiceRecord();
			if (record != null) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ServiceRecordCodec.write(record, baos);
				byte[] serviceByte = baos.toByteArray();
				socketStream.writeInt(serviceByte.length);
				socketStream.write(serviceByte);
			} else {
				Log.w(TAG, "No local service record to send");
				socketStream.writeInt(0);
			}
			socketStream.flush();

			serverSocket.close();
		} catch (IOException e) {
			Log.w(TAG, "openConnection: ", e);
		}
	}
}
