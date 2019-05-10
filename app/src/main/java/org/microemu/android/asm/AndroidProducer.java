/**
 * MicroEmulator
 * Copyright (C) 2008 Bartek Teodorczyk <barteo@barteo.net>
 * Copyright (C) 2017-2018 Nikita Shakarun
 * <p>
 * It is licensed under the following two licenses as alternatives:
 * 1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 * 2. Apache License (the "AL") Version 2.0
 * <p>
 * You may not use this file except in compliance with at least one of
 * the above two licenses.
 * <p>
 * You may obtain a copy of the LGPL at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 * <p>
 * You may obtain a copy of the AL at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the LGPL or the AL for the specific language governing permissions and
 * limitations.
 *
 * @version $Id$
 */

package org.microemu.android.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ru.playsoftware.j2meloader.util.ZipFileCompat;

public class AndroidProducer {

	private static final int BUFFER_SIZE = 2048;

	private static byte[] instrument(final byte[] classFile, String classFileName, String encoding)
			throws IllegalArgumentException {
		ClassReader cr = new ClassReader(classFile);
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		ClassVisitor cv = new AndroidClassVisitor(cw, encoding);
		if (!cr.getClassName().equals(classFileName)) {
			throw new IllegalArgumentException("Class name does not match path");
		}
		cr.accept(cv, ClassReader.SKIP_DEBUG);

		return cw.toByteArray();
	}

	public static void processJar(File jarInputFile, File jarOutputFile, String encoding) throws IOException {
		HashMap<String, byte[]> resources = new HashMap<>();
		ZipEntry zipEntry;
		InputStream zis;
		try (ZipFileCompat zip = new ZipFileCompat(jarInputFile);
			 ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jarOutputFile))) {
			byte[] buffer = new byte[BUFFER_SIZE];
			while ((zipEntry = zip.getNextEntry()) != null) {
				if (!zipEntry.isDirectory()) {
					zis = zip.getInputStream(zipEntry);
					String name = zipEntry.getName();
					int size = 0;
					int read;
					int length = buffer.length;
					while ((read = zis.read(buffer, size, length)) > 0) {
						size += read;

						length = BUFFER_SIZE;
						if (size + length > buffer.length) {
							byte[] newInputBuffer = new byte[size + length];
							System.arraycopy(buffer, 0, newInputBuffer, 0, buffer.length);
							buffer = newInputBuffer;
						}
					}
					byte[] inBuffer = new byte[size];
					System.arraycopy(buffer, 0, inBuffer, 0, size);
					resources.put(name, inBuffer);
				}
			}

			for (String name : resources.keySet()) {
				byte[] inBuffer = resources.get(name);
				byte[] outBuffer = inBuffer;
				try {
					if (name.endsWith(".class")) {
						outBuffer = instrument(inBuffer,
								name.replace(".class", ""), encoding);
					}
					zos.putNextEntry(new ZipEntry(name));
					zos.write(outBuffer);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
