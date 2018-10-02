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

import ru.playsoftware.j2meloader.applist.AppItem;
import ru.playsoftware.j2meloader.appsdb.AppRepository;
import ru.playsoftware.j2meloader.config.Config;

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

	public static String getAppPath(Context context, Uri uri) throws FileNotFoundException {
		InputStream in = context.getContentResolver().openInputStream(uri);
		OutputStream out = null;
		File folder = new File(context.getApplicationInfo().dataDir, JarConverter.TEMP_URI_FOLDER_NAME);
		folder.mkdir();
		String uriPath = uri.getPath();
		String extension = uriPath.substring(uriPath.lastIndexOf('.'));
		File file;
		if (extension.equalsIgnoreCase(".jar")) {
			file = new File(folder, JarConverter.TEMP_JAR_NAME);
		}
		else {
			file = new File(folder, JarConverter.TEMP_JAD_NAME);
		}
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

	private static ArrayList<AppItem> getAppsList() {
		ArrayList<AppItem> apps = new ArrayList<>();
		String[] appFolders = new File(Config.APP_DIR).list();
		if (appFolders != null) {
			for (String appFolder : appFolders) {
				File temp = new File(Config.APP_DIR, appFolder);
				try {
					if (temp.isDirectory() && temp.list().length > 0) {
						AppItem item = getApp(temp.getName());
						apps.add(item);
					} else {
						temp.delete();
					}
				} catch (RuntimeException re) {
					re.printStackTrace();
					deleteDirectory(temp);
				}
			}
		}
		return apps;
	}

	public static AppItem getApp(String path) {
		File file = new File(Config.APP_DIR, path);
		LinkedHashMap<String, String> params =
				loadManifest(new File(file.getAbsolutePath(), Config.MIDLET_MANIFEST_FILE));
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

	public static void deleteApp(AppItem item) {
		File appDir = new File(item.getPathExt());
		deleteDirectory(appDir);
		File appSaveDir = new File(Config.DATA_DIR, item.getTitle());
		deleteDirectory(appSaveDir);
		File appConfigsDir = new File(Config.CONFIGS_DIR, item.getTitle());
		deleteDirectory(appConfigsDir);
	}

	public static void updateDb(AppRepository appRepository, List<AppItem> items) {
		String[] appFolders = new File(Config.APP_DIR).list();
		int itemsNum = items.size();
		if (appFolders == null || appFolders.length == 0) {
			// If db isn't empty
			if (itemsNum != 0) {
				appRepository.deleteAll();
			}
			appRepository.insertAll(getAppsList());
			return;
		}
		List<String> appFoldersList = Arrays.asList(appFolders);
		boolean result = true;
		// Delete invalid app items from db
		Iterator<AppItem> iterator = items.iterator();
		while (iterator.hasNext()) {
			AppItem item = iterator.next();
			if (!appFoldersList.contains(item.getPath())) {
				result = false;
				appRepository.delete(item);
				iterator.remove();
			}
		}
		if (appFolders.length != items.size()) {
			result = false;
		}
		if (!result) {
			appRepository.insertAll(getAppsList());
		}
	}
}
