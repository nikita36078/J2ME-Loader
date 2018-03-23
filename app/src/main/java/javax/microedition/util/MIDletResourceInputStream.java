/*
 * Copyright 2017 Nikita Shakarun
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

package javax.microedition.util;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MIDletResourceInputStream extends InputStream {
	private FileInputStream fis;

	public MIDletResourceInputStream(@NonNull File file) throws FileNotFoundException {
		fis = new FileInputStream(file);
	}

	@Override
	public int available() throws IOException {
		return fis.available();
	}

	@Override
	public void close() throws IOException {
		fis.close();
	}

	@Override
	public int read() throws IOException {
		return fis.read();
	}

	@Override
	public int read(@NonNull byte[] b) throws IOException {
		return fis.read(b);
	}

	@Override
	public int read(@NonNull byte[] b, int off, int len) throws IOException {
		return fis.read(b, off, len);
	}

	@Override
	public long skip(long n) throws IOException {
		int available = available();
		if (n > available) {
			return fis.skip(available) * 2;
		} else {
			return fis.skip(n);
		}
	}
}