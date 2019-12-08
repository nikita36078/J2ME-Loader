/*
 * Copyright 2018 Nikita Shakarun
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

package org.microemu.cldc.resource;

import org.microemu.microedition.io.ConnectionImplementation;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.InputConnection;
import javax.microedition.util.ContextHolder;

public class Connection implements InputConnection, ConnectionImplementation {

	private final static String PROTOCOL = "resource://";

	private final static String PROTOCOL2 = "resource:";

	private String path;

	private InputStream stream;

	@Override
	public javax.microedition.io.Connection openConnection(String name, int mode, boolean timeouts) throws IOException {
		path = name.replace(PROTOCOL, "").replace(PROTOCOL2, "");
		return this;
	}

	@Override
	public InputStream openInputStream() throws IOException {
		if (stream != null) {
			throw new IOException("InputStream already opened");
		}
		stream = ContextHolder.getResourceAsStream(null, path);
		if (stream == null) {
			throw new IOException("File not found");
		}
		return new ResourceInputStream(stream);
	}

	@Override
	public DataInputStream openDataInputStream() throws IOException {
		return new DataInputStream(openInputStream());
	}

	@Override
	public void close() throws IOException {
		stream.close();
		stream = null;
	}

	class ResourceInputStream extends InputStream {

		private InputStream inputStream;

		ResourceInputStream(InputStream inputStream) {
			this.inputStream = inputStream;
		}

		@Override
		public int read() throws IOException {
			return inputStream.read();
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int result = inputStream.read(b, off, len);
			if (result == -1) result = 0;
			return result;
		}
	}
}
