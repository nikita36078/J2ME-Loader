/*
 * Copyright 2015-2016 Nickolay Savchenko
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

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.nononsenseapps.filepicker.Utils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

public class FileUtils {

	private static final String TAG = FileUtils.class.getName();
	private static final String TEMP_JAR_NAME = "tmp.jar";
	private static final String TEMP_JAD_NAME = "tmp.jad";
	private static final int BUFFER_SIZE = 1024;
	public static final String ILLEGAL_FILENAME_CHARS = "[/\\\\:*?\"<>|]";

	public static void copyFiles(File src, File dst, FilenameFilter filter) {
		if (!dst.exists() && !dst.mkdirs()) {
			Log.e(TAG, "copyFiles() failed create dir: " + dst);
			return;
		}
		File[] list = src.listFiles(filter);
		if (list == null) {
			return;
		}
		for (File file : list) {
			File to = new File(dst, file.getName());
			if (file.isDirectory()) {
				copyFiles(src, to, filter);
			} else {
				try {
					copyFileUsingChannel(file, to);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void copyFileUsingChannel(File source, File dest) throws IOException {
		try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
			 FileChannel destChannel = new FileOutputStream(dest).getChannel()) {
			destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
		}
	}

	public static void deleteDirectory(File dir) {
		if (dir.isDirectory()) {
			File[] listFiles = dir.listFiles();
			if (listFiles != null && listFiles.length != 0) {
				for (File file : listFiles) {
					deleteDirectory(file);
				}
			}
		}
		if (!dir.delete() && dir.exists()) {
			Log.w(TAG, "Can't delete file: " + dir);
		}
	}

	public static File getFileForUri(Context context, Uri uri) throws IOException {
		if ("file".equals(uri.getScheme())) {
			String path = uri.getPath();
			if (path != null) {
				File file = new File(path);
				if (file.exists()) {
					return file;
				}
			}
		}
		if ((context.getPackageName() + ".provider").equals(uri.getAuthority())) {
			try {
				File file = Utils.getFileForUri(uri);
				if (file.isFile()) {
					return file;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		File tmpDir = new File(context.getCacheDir(), "installer");
		if (!tmpDir.exists() && !tmpDir.mkdirs()) {
			throw new IOException("Can't create directory: " + tmpDir);
		}
		File file;
		try (InputStream in = context.getContentResolver().openInputStream(uri)) {
			byte[] buf = new byte[BUFFER_SIZE];
			int len;
			if (in == null || (len = in.read(buf)) == -1)
				throw new IOException("Can't read data from uri: " + uri);
			if (buf[0] == 0x50 && buf[1] == 0x4B) {
				file = new File(tmpDir, TEMP_JAR_NAME);
			} else {
				file = new File(tmpDir, TEMP_JAD_NAME);
			}
			try (OutputStream out = new FileOutputStream(file)) {
				out.write(buf, 0, len);
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
			}
		}
		return file;
	}

	public static byte[] getBytes(File file) throws IOException {
		try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
			byte[] b = new byte[(int) file.length()];
			dis.readFully(b);
			return b;
		}
	}

	public static void clearDirectory(File dir) {
		if (!dir.isDirectory()) return;
		final File[] files = dir.listFiles();
		if (files == null) return;
		for (File file : files) {
			if (file.isDirectory()) {
				deleteDirectory(dir);
			} else {
				//noinspection ResultOfMethodCallIgnored
				file.delete();
			}
		}
	}

	static void moveFiles(File src, File dst) {
		if (src.renameTo(dst)) return;
		File[] files = src.listFiles();
		if (files == null) {
			return;
		}
		if (!dst.mkdirs()) {
			Log.e(TAG, "moveFiles() can't create directory: " + dst);
		}
		for (File file : files) {
			File to = new File(dst, file.getName());
			if (file.isDirectory()) {
				moveFiles(file, to);
			} else if (!file.renameTo(to)) {
				try {
					copyFileUsingChannel(file, to);
					if (!file.delete()) {
						Log.e(TAG, "moveFiles() can't delete: " + file);
					}
				} catch (IOException e) {
					Log.e(TAG, "moveFiles() can't move [" + file + "] to [" + to + "]", e);
				}
			}
		}
		if (!src.delete()) {
			Log.e(TAG, "moveFiles() can't delete: " + src);
		}
	}

	public static String getText(String path) {
		try (DataInputStream dis = new DataInputStream(new FileInputStream(path))) {
			byte[] buf = new byte[dis.available()];
			dis.readFully(buf);
			return new String(buf);
		} catch (IOException e) {
			Log.e(TAG, "getText: " + path, e);
		}
		return "";
	}
}
