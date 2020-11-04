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

package org.microemu.cldc.btspp;

import android.bluetooth.BluetoothSocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.StreamConnection;

public class SPPConnectionImpl implements StreamConnection {
	private BTInputStream btin = null;
	private BTOutputStream btout = null;
	public BluetoothSocket socket;
	private boolean skipAfterWrite;

	// Android closes socket when one of streams is closed, we need to workaround it
	// Also if we're connecting with SPP profile the data we write is returned back to InputStream
	// This is not an expected behavior
	private static class BTInputStream extends InputStream {
		private InputStream is;

		public BTInputStream(InputStream is) {
			this.is = is;
		}

		public int available() throws IOException {
			return is.available();
		}

		public void close() throws IOException {
			// application may call Connection.close(), which closes socket
			// So do nothing
		}

		public void mark(int readlimit) {
			is.mark(readlimit);
		}

		public boolean markSupported() {
			return is.markSupported();
		}

		public int read() throws IOException {
			return is.read();
		}

		public int read(byte[] b) throws IOException {
			return is.read(b);
		}

		public int read(byte[] b, int off, int len) throws IOException {
			return is.read(b, off, len);
		}

		public void reset() throws IOException {
			is.reset();
		}

		public long skip(long n) throws IOException {
			return is.skip(n);
		}
	}

	private static class BTOutputStream extends OutputStream {
		private OutputStream os;
		public InputStream is;

		public BTOutputStream(OutputStream os, InputStream is) {
			this.os = os;
			this.is = is;
		}

		public void close() throws IOException {
			// same as above
			os.flush();
		}

		public void flush() throws IOException {
			os.flush();
		}

		public void write(byte[] b) throws IOException {
			os.write(b);
			if (is != null)
				is.skip(b.length);
		}

		public void write(byte[] b, int off, int len) throws IOException {
			os.write(b, off, len);
			if (is != null)
				is.skip(len);
		}

		public void write(int b) throws IOException {
			os.write(b);
			if (is != null)
				is.skip(1);
		}
	}

	public SPPConnectionImpl(BluetoothSocket socket, boolean skipAfterWrite) throws IOException {
		this.socket = socket;
		this.skipAfterWrite = skipAfterWrite;
	}

	public void close() throws IOException {
		if (socket != null)
			socket.close();
	}

	public InputStream openInputStream() throws IOException {
		if (btin != null)
			return btin;
		if (socket != null)
			return btin = new BTInputStream(socket.getInputStream());
		throw new IOException("socket is null");
	}

	public DataInputStream openDataInputStream() throws IOException {
		if (socket != null)
			return new DataInputStream(openInputStream());
		throw new IOException("socket is null");
	}

	public OutputStream openOutputStream() throws IOException {
		if (btout != null)
			return btout;
		if (socket != null)
			return btout = new BTOutputStream(socket.getOutputStream(), skipAfterWrite ? socket.getInputStream() : null);
		throw new IOException("socket is null");
	}

	public DataOutputStream openDataOutputStream() throws IOException {
		if (socket != null)
			return new DataOutputStream(openOutputStream());
		throw new IOException("socket is null");
	}
}
