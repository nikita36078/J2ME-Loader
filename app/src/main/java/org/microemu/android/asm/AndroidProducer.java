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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class AndroidProducer {

	private static byte[] instrument(final byte[] classFile) {
		ClassReader cr = new ClassReader(classFile);
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		ClassVisitor cv = new AndroidClassVisitor(cw);
		cr.accept(cv, ClassReader.SKIP_DEBUG);

		return cw.toByteArray();
	}

	public static void processJar(File jarInputFile, File jarOutputFile) throws IOException {
		HashMap<String, byte[]> resources = new HashMap<>();
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jarInputFile));
			 ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jarOutputFile))) {

			byte[] buffer = new byte[1024];
			ZipEntry zipEntry;
			while ((zipEntry = zis.getNextEntry()) != null) {
				if (!zipEntry.isDirectory()) {
					String name = zipEntry.getName();
					int size = 0;
					int read;
					int length = buffer.length;
					while ((read = zis.read(buffer, size, length)) > 0) {
						size += read;

						length = 1024;
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
						outBuffer = instrument(inBuffer);
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
