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
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.shell.ConfigActivity;

import ru.playsoftware.j2meloader.MainActivity;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.applist.AppItem;

public class FileUtils {

	private static String TAG = FileUtils.class.getName();
	private static final int BUFFER_SIZE = 1024;

	public static void copyFiles(String src, String dest, FilenameFilter filter) {
		File fsrc = new File(src);
		File fdest = new File(dest);
		fdest.mkdirs();
		String to;
		File[] list = fsrc.listFiles(filter);
		for (File entry : list) {
			to = entry.getPath().replace(src, dest);
			if (entry.isDirectory()) {
				copyFiles(entry.getPath(), to, filter);
			} else {
				try {
					copyFileUsingChannel(entry, new File(to));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void copyFileUsingChannel(File source, File dest) throws IOException {
		try (FileChannel sourceChannel = new FileInputStream(source).getChannel(); FileChannel destChannel = new FileOutputStream(dest).getChannel()) {
			destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
		}
	}

	public static void deleteDirectory(File dir) {
		if (dir.isDirectory()) {
			File[] listFiles = dir.listFiles();
			if (listFiles.length != 0) {
				for (File file : listFiles) {
					deleteDirectory(file);
				}
			}
		}
		dir.delete();
	}

	public static LinkedHashMap<String, String> loadManifest(File mf) {
		LinkedHashMap<String, String> params = new LinkedHashMap<>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(mf)));
			String line;
			int index;
			while ((line = br.readLine()) != null) {
				index = line.indexOf(':');
				if (index > 0) {
					params.put(line.substring(0, index).trim(), line.substring(index + 1).trim());
				}
				if (line.length() > 0 && Character.isWhitespace(line.charAt(0))) {
					Iterator<Map.Entry<String, String>> iter = params.entrySet().iterator();
					Map.Entry<String, String> entry = null;
					while (iter.hasNext()) {
						entry = iter.next();
					}
					params.put(entry.getKey(), entry.getValue() + line.substring(1));
				}
			}
			br.close();
		} catch (Throwable t) {
			Log.e(TAG, "getAppProperty() will not be available due to " + t.toString());
		}
		return params;
	}

	public static String getJarPath(Context context, Uri uri) throws FileNotFoundException {
		InputStream in = context.getContentResolver().openInputStream(uri);
		OutputStream out = null;
		File folder = new File(context.getApplicationInfo().dataDir, JarConverter.TEMP_URI_FOLDER_NAME);
		folder.mkdir();
		File file = new File(folder, JarConverter.TEMP_JAR_NAME);
		try {
			out = new FileOutputStream(file);
			byte[] buf = new byte[BUFFER_SIZE];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.close();
				}
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return file.getPath();
	}

	public static ArrayList<AppItem> getAppsList(Context context) {
		ArrayList<AppItem> apps = new ArrayList<>();
		String[] appFolders = new File(ConfigActivity.APP_DIR).list();
		if (appFolders != null) {
			for (String appFolder : appFolders) {
				File temp = new File(ConfigActivity.APP_DIR, appFolder);
				try {
					if (temp.isDirectory() && temp.list().length > 0) {
						AppItem item = getApp(temp);
						apps.add(item);
					} else {
						temp.delete();
					}
				} catch (RuntimeException re) {
					re.printStackTrace();
					FileUtils.deleteDirectory(temp);
					Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show();
				}
			}
		}
		return apps;
	}

	public static AppItem getApp(File file) {
		LinkedHashMap<String, String> params = FileUtils
				.loadManifest(new File(file.getAbsolutePath(), ConfigActivity.MIDLET_MANIFEST_FILE));
		String imagePath = params.get("MIDlet-Icon");
		if (imagePath == null) {
			imagePath = params.get("MIDlet-1").split(",")[1];
		}
		AppItem item = new AppItem(file.getName(), params.get("MIDlet-Name"),
				params.get("MIDlet-Vendor"),
				params.get("MIDlet-Version"));
		item.setImagePathExt(imagePath);
		return item;
	}

	public static boolean checkDb(Context context, List<AppItem> items) {
		String[] appFolders = new File(ConfigActivity.APP_DIR).list();
		int itemsNum = items.size();
		// If db is empty
		if (itemsNum == 0) {
			if (appFolders == null || appFolders.length == 0) {
				return true;
			} else {
				return false;
			}
		} else if (appFolders == null || appFolders.length == 0) {
			// Else if app folder is empty
			((MainActivity) context).deleteAllApps();
			return true;
		}
		List<String> appFoldersList = Arrays.asList(appFolders);
		boolean result = true;
		// Delete invalid app items from db
		Iterator<AppItem> iterator = items.iterator();
		while (iterator.hasNext()) {
			AppItem item = iterator.next();
			if (!appFoldersList.contains(item.getPath())) {
				result = false;
				((MainActivity) context).deleteApp(item);
				iterator.remove();
			}
		}
		if (appFolders.length != items.size()) {
			result = false;
		}
		return result;
	}
}
