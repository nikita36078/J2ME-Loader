/*
 * Copyright 2015-2016 Nickolay Savchenko
 * Copyright 2017-2020 Nikita Shakarun
 * Copyright 2020-2023 Yury Kharchenko
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
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.List;

import kotlin.io.FilesKt;
import ru.playsoftware.j2meloader.config.Config;

public class FileUtils {
	private static final String TAG = FileUtils.class.getName();
	private static final String TEMP_JAR_NAME = "tmp.jar";
	private static final String TEMP_JAD_NAME = "tmp.jad";
	private static final String TEMP_KJX_NAME = "tmp.kjx";
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
		try (FileInputStream fis = new FileInputStream(source);
			 FileChannel sourceChannel = fis.getChannel();
			 FileOutputStream fos = new FileOutputStream(dest);
			 FileChannel destChannel = fos.getChannel()) {
			destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
		}
	}

	public static boolean deleteDirectory(File dir) {
		if (dir.isDirectory()) {
			File[] listFiles = dir.listFiles();
			if (listFiles != null) {
				for (File file : listFiles) {
					deleteDirectory(file);
				}
			}
		}
		if (!dir.delete() && dir.exists()) {
			Log.w(TAG, "Can't delete file: " + dir);
			return false;
		}
		return true;
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
			} else if (buf[0] == 'K' && buf[1] == 'J' && buf[2] == 'X') {
				file = new File(tmpDir, TEMP_KJX_NAME);
			} else {
				file = new File(tmpDir, TEMP_JAD_NAME);
			}
			//noinspection IOStreamConstructor
			try (OutputStream out = new FileOutputStream(file)) {
				out.write(buf, 0, len);
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
			}
		}
		return file;
	}

	public static void copyFileFromUri(Context context, Uri uri, File dest) throws IOException {
		try (InputStream in = context.getContentResolver().openInputStream(uri);
			 OutputStream out = new FileOutputStream(dest)) {
			if (in == null) {
				throw new IOException("Can't read data from uri: " + uri);
			}
			byte[] buffer = new byte[BUFFER_SIZE];
			int len;
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}
		} catch (IOException | RuntimeException e) {
			//noinspection ResultOfMethodCallIgnored
			dest.delete();
			throw e;
		}
	}

	@NonNull
	public static String getDisplayName(@NonNull Context context, Uri uri) {
		if ("file".equals(uri.getScheme())) {
			String path = uri.getPath();
			if (path != null) {
				return new File(path).getName();
			}
		}
		try (Cursor cursor = context.getContentResolver().query(uri,
				new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
				if (column != -1) {
					String name = cursor.getString(column);
					if (name != null) {
						return name;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		String name = uri.getLastPathSegment();
		return name == null ? "" : name;
	}

	public static byte[] getBytes(File file) throws IOException {
		return FilesKt.readBytes(file);
	}

	public static void clearDirectory(File dir) {
		final File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.isDirectory()) {
				deleteDirectory(file);
			} else {
				//noinspection ResultOfMethodCallIgnored
				file.delete();
			}
		}
	}

	public static String getText(String path) {
		try {
			//noinspection CharsetObjectCanBeUsed
			return FilesKt.readText(new File(path), Charset.forName("UTF-8"));
		} catch (Exception e) {
			Log.e(TAG, "getText: " + path, e);
		}
		return "";
	}

	public static boolean initWorkDir(File dir) {
		if ((dir.isDirectory() || dir.mkdirs()) && dir.canWrite()) {
			//noinspection ResultOfMethodCallIgnored
			new File(dir, Config.SHADERS_DIR).mkdir();
			//noinspection ResultOfMethodCallIgnored
			new File(dir, Config.SOUNDBANKS_DIR).mkdir();
			try {
				File nomedia = new File(dir, ".nomedia");
				//noinspection ResultOfMethodCallIgnored
				nomedia.createNewFile();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		}
		return false;
	}

	public static boolean isExternalStorageLegacy() {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Environment.isExternalStorageLegacy();
	}

	public static ActivityResultContract<String, List<Uri>> getFilePicker() {
		if (isExternalStorageLegacy()) {
			return new PickFileResultContract();
		} else {
			return new SAFFileResultContract();
		}
	}
}
