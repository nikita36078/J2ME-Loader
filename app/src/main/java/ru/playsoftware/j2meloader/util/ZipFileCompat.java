/*
 * Copyright 2019 Nikita Shakarun
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

package ru.playsoftware.j2meloader.util;

import android.os.Build;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ZipFileCompat implements Closeable {

	private ZipFile zipFile;
	private Enumeration zipFileEntries;
	private ZipInputStream zis;

	public ZipFileCompat(File file) throws IOException {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				zipFile = new ZipFile(file, StandardCharsets.ISO_8859_1);
			} else {
				zipFile = new ZipFile(file);
			}
			zipFileEntries = zipFile.entries();
		} catch (IOException ioe) {
			FileInputStream fis = new FileInputStream(file);
			zis = new ZipInputStream(fis);
		}
	}

	public ZipEntry getNextEntry() throws IOException {
		if (zipFile != null) {
			return zipFileEntries.hasMoreElements() ? (ZipEntry) zipFileEntries.nextElement() : null;
		} else {
			return zis.getNextEntry();
		}
	}

	public InputStream getInputStream(ZipEntry zipEntry) throws IOException {
		if (zipFile != null) {
			return zipFile.getInputStream(zipEntry);
		} else {
			return zis;
		}
	}

	public ZipEntry getEntry(String name) throws IOException {
		if (zipFile != null) {
			return zipFile.getEntry(name);
		} else {
			ZipEntry zipEntry;
			while ((zipEntry = zis.getNextEntry()) != null && !zipEntry.getName().equals(name));
			return zipEntry;
		}
	}

	@Override
	public void close() throws IOException {
		if (zipFile != null) {
			zipFile.close();
		} else {
			zis.close();
		}
	}
}
