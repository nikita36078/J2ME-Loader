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

package javax.microedition.media;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.media.protocol.SourceStream;

public class InternalSourceStream extends InputStream {

	private static final int BUFFER_SIZE = 1024;

	private SourceStream sourceStream;
	private byte[] buffer;

	public InternalSourceStream(SourceStream sourceStream) {
		this.sourceStream = sourceStream;
		this.buffer = new byte[BUFFER_SIZE];
	}

	@Override
	public int read() throws IOException {
		int read = sourceStream.read(buffer, 0, 1);
		return (read == -1) ? -1 : buffer[0] & 0xff;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return sourceStream.read(b, off, len);
	}

}
