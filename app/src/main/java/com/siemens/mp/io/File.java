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
package com.siemens.mp.io;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import javax.microedition.shell.ConfigActivity;
import javax.microedition.shell.MyClassLoader;
import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.util.FileUtils;

public class File {

	private FileInputStream inputStream;
	private FileOutputStream outputStream;

	public int close(int fileDescriptor) throws IOException {
		if (inputStream != null && outputStream != null) {
			inputStream.close();
			outputStream.close();
			return 1;
		} else {
			return -1;
		}
	}

	public static int copy(String source, String dest) throws IOException {
		FileUtils.copyFileUsingChannel(ContextHolder.getFileByName(source),
				ContextHolder.getFileByName(dest));
		return 1;
	}

	public static int debugWrite(String fileName, String infoString) throws IOException {
		FileWriter writer = new FileWriter(ContextHolder.getFileByName(fileName), true);
		writer.append(infoString).close();
		return 1;
	}

	public static int delete(String fileName) {
		if (ContextHolder.getFileByName(fileName).delete()) {
			return 1;
		} else {
			return -1;
		}
	}

	public int open(String fileName) throws IOException {
		java.io.File file = ContextHolder.getFileByName(fileName);
		if (!file.exists()) {
			file.createNewFile();
		}
		inputStream = new FileInputStream(file);
		outputStream = new FileOutputStream(file);
		return 1;
	}

	public int read(int fileDescriptor, byte[] buf, int offset, int numBytes) throws IOException {
		return inputStream.read(buf, offset, numBytes);
	}

	public static int rename(String source, String dest) {
		if (ContextHolder.getFileByName(source).renameTo(ContextHolder.getFileByName(dest))) {
			return 1;
		} else {
			return -1;
		}
	}

	public int write(int fileDescriptor, byte[] buf, int offset, int numBytes) throws IOException {
		outputStream.write(buf, offset, numBytes);
		return 1;
	}
}