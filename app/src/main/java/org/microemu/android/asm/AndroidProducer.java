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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class AndroidProducer {

	private static HashMap<String, ArrayList<String>> classesHierarchy = new HashMap<>();

	private static HashMap<String, TreeMap<FieldNodeExt, String>> fieldTranslations = new HashMap<>();

	private static HashMap<String, ArrayList<String>> methodTranslations = new HashMap<>();

	private static void analyze(String className, final InputStream classInputStream) throws IOException {
		ClassReader cr = new ClassReader(classInputStream);
		FirstPassVisitor cv = new FirstPassVisitor(classesHierarchy, methodTranslations);
		cr.accept(cv, 0);
	}

	private static byte[] instrument(String name, final InputStream classInputStream, boolean isMidlet) throws IOException {
		ClassReader cr = new ClassReader(classInputStream);
		ClassWriter cw = new ClassWriter(0);
		ClassVisitor cv = new AndroidClassVisitor(cw, isMidlet, classesHierarchy, fieldTranslations, methodTranslations);
		cr.accept(cv, 0);

		return cw.toByteArray();
	}

	public static void processJar(File jarInputFile, File jarOutputFile, boolean isMidlet) throws IOException {
		JarInputStream jis = null;
		JarOutputStream jos = null;
		HashMap<String, byte[]> resources = new HashMap<>();
		try {
			jis = new JarInputStream(new FileInputStream(jarInputFile));
			Manifest manifest = jis.getManifest();
			if (manifest == null) {
				jos = new JarOutputStream(new FileOutputStream(jarOutputFile));
			} else {
				Attributes attributes = manifest.getMainAttributes();
				if (!attributes.containsKey(Attributes.Name.MANIFEST_VERSION)) {
					attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
				}
				jos = new JarOutputStream(new FileOutputStream(jarOutputFile), manifest);
			}

			byte[] buffer = new byte[1024];
			JarEntry jarEntry;
			while ((jarEntry = jis.getNextJarEntry()) != null) {
				if (!jarEntry.isDirectory()) {
					String name = jarEntry.getName();
					int size = 0;
					int read;
					int length = buffer.length;
					while ((read = jis.read(buffer, size, length)) > 0) {
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
					if (name.endsWith(".class")) {
						analyze(name.substring(0, name.length() - ".class".length()), new ByteArrayInputStream(inBuffer));
					}
				}
			}

			for (String name : resources.keySet()) {
				byte[] inBuffer = resources.get(name);
				byte[] outBuffer = inBuffer;
				if (name.endsWith(".class")) {
					outBuffer = instrument(name, new ByteArrayInputStream(inBuffer), isMidlet);
				}
				jos.putNextEntry(new JarEntry(name));
				jos.write(outBuffer);
			}
		} finally {
			if (jis != null) {
				jis.close();
			}
			if (jos != null) {
				jos.close();
			}
		}
	}

	public static void main(String args[]) {
		if (args.length < 2 || args.length > 3) {
			System.out.println("usage: AndroidProducer <infile> <outfile> [midlet]");
		} else {
			boolean midlet = false;
			if (args.length == 3 && args[2].toLowerCase().equals("midlet")) {
				midlet = true;
			}
			try {
				processJar(new File(args[0]), new File(args[1]), midlet);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
