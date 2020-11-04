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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipUtils {

	public static void unzip(File zipFile, File extractFolder) throws IOException {
		ZipFile zip = new ZipFile(zipFile);
		extractFolder.mkdir();
		Enumeration zipFileEntries = zip.entries();
		while (zipFileEntries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
			String currentEntry = entry.getName();
			File destFile = new File(extractFolder, currentEntry);
			File destinationParent = destFile.getParentFile();
			destinationParent.mkdirs();
			if (!entry.isDirectory() && !destFile.exists() && !entry.getName().endsWith(".class")) {
				BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
				// write the current file to disk
				FileOutputStream fos = new FileOutputStream(destFile);
				BufferedOutputStream dest = new BufferedOutputStream(fos);
				IOUtils.copy(is, dest);
				dest.flush();
				dest.close();
				is.close();
			}
		}
	}
}
