/*
 * J2ME Loader
 * Copyright (C) 2017 Nikita Shakarun
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
		if (n > available()) {
			return available() * 2;
		} else {
			return fis.skip(n);
		}
	}
}