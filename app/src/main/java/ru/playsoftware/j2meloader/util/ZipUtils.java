/*
 * Copyright 2017-2018 Nikita Shakarun
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

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ZipUtils {

	private static final int BUFFER_SIZE = 8096;

	public static void unzipEntry(File srcZip, String name, File dst) throws IOException {
		ZipFile zip = new ZipFile(srcZip);
		FileHeader entry = zip.getFileHeader(name);
		if (entry == null) {
			throw new IOException("Entry '" + name + "' not found in zip: " + srcZip);
		}
		try (BufferedInputStream bis = new BufferedInputStream(zip.getInputStream(entry));
			 BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dst), BUFFER_SIZE)) {
			byte[] data = new byte[BUFFER_SIZE];
			int read;
			while ((read = bis.read(data)) != -1) {
				bos.write(data, 0, read);
			}
		}
	}
}
