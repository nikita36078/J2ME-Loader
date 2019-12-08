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
import java.util.ArrayList;
import java.util.Arrays;

import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.util.FileUtils;

public class File {

	public static final int INSIDE_STORAGE_PATH = 1;
	public static final int OUTSIDE_STORAGE_PATH = 0;
	public static final String STORAGE_DRIVE = "a:";

	private final static String ROOT_SEP_STR = ":/";
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
		FileUtils.copyFileUsingChannel(getFile(source), getFile(dest));
		return 1;
	}

	public static int debugWrite(String fileName, String infoString) throws IOException {
		FileWriter writer = new FileWriter(getFile(fileName), true);
		writer.append(infoString).close();
		return 1;
	}

	public static int delete(String fileName) {
		if (getFile(fileName).delete()) {
			return 1;
		} else {
			return -1;
		}
	}

	public static int exists(String fileName) throws IOException {
		java.io.File file = getFile(fileName);
		if (file.exists()) {
			return 1;
		} else {
			return -1;
		}
	}

	public static boolean getIsHidden(String fileName) throws IOException {
		java.io.File file = getFile(fileName);
		return file.isHidden();
	}

	public static long getLastModified(String fileName) throws IOException {
		java.io.File file = getFile(fileName);
		return file.lastModified();
	}

	public static boolean isDirectory(String fileName) throws IOException {
		java.io.File file = getFile(fileName);
		return file.isDirectory();
	}

	public int length(int fileDescriptor) throws IOException {
		return inputStream.available();
	}

	public static String[] list(String pathName) throws IOException {
		return list(pathName, false);
	}

	public static String[] list(String pathName, boolean includeHidden) throws IOException {
		java.io.File[] files = getFile(pathName).listFiles();
		if (files == null) {
			return new String[0];
		}
		Arrays.sort(files);
		ArrayList<String> list = new ArrayList<>();
		for (java.io.File file : files) {
			if (!includeHidden || !file.isHidden()) {
				list.add(file.getName());
			}
		}
		return list.toArray(new String[0]);
	}

	public int open(String fileName) throws IOException {
		java.io.File file = getFile(fileName);
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
		if (getFile(source).renameTo(getFile(dest))) {
			return 1;
		} else {
			return -1;
		}
	}

	public int seek(int fileDescriptor, int seekpos) throws IOException {
		return (int) inputStream.skip(seekpos);
	}

	public static int spaceAvailable() throws IOException {
		return 1024;
	}

	public int write(int fileDescriptor, byte[] buf, int offset, int numBytes) throws IOException {
		outputStream.write(buf, offset, numBytes);
		return 1;
	}

	private static java.io.File getFile(String fileName) {
		if (!fileName.contains(":/")) {
			return ContextHolder.getFileByName(fileName);
		} else {
			fileName = fileName.replace(OUTSIDE_STORAGE_PATH + ROOT_SEP_STR, "");
			return new java.io.File(System.getProperty("user.home"), fileName);
		}
	}
}