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

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

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

import ru.playsoftware.j2meloader.util.IOUtils;

public class AndroidProducer {

	private static byte[] instrument(final byte[] classFile, String classFileName)
			throws IllegalArgumentException {
		ClassReader cr = new ClassReader(classFile);
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		ClassVisitor cv = new AndroidClassVisitor(cw);
		if (!cr.getClassName().equals(classFileName)) {
			throw new IllegalArgumentException("Class name does not match path");
		}
		cr.accept(cv, ClassReader.SKIP_DEBUG);

		return cw.toByteArray();
	}

	public static void processJar(File jarInputFile, File jarOutputFile) throws IOException {
		HashMap<String, byte[]> resources = new HashMap<>();
		ZipEntry zipEntry;
		InputStream zis;
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jarOutputFile))) {
			ZipFile zip = new ZipFile(jarInputFile);
			for (FileHeader header : zip.getFileHeaders()) {
				// Some zip entries have zero length names
				if (header.getFileNameLength() > 0 && !header.isDirectory()) {
					zis = zip.getInputStream(header);
					String name = header.getFileName();
					byte[] inBuffer = IOUtils.toByteArray(zis);
					resources.put(name, inBuffer);
					zis.close();
				}
			}

			for (String name : resources.keySet()) {
				byte[] inBuffer = resources.get(name);
				byte[] outBuffer = inBuffer;
				try {
					if (name.endsWith(".class")) {
						outBuffer = instrument(inBuffer, name.replace(".class", ""));
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
