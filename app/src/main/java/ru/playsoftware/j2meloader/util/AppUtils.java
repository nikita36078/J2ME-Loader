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

package ru.playsoftware.j2meloader.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import ru.playsoftware.j2meloader.applist.AppItem;
import ru.playsoftware.j2meloader.appsdb.AppRepository;
import ru.playsoftware.j2meloader.config.Config;

public class AppUtils {

	private static ArrayList<AppItem> getAppsList() {
		ArrayList<AppItem> apps = new ArrayList<>();
		String[] appFolders = new File(Config.getAppDir()).list();
		if (appFolders != null) {
			for (String appFolder : appFolders) {
				File temp = new File(Config.getAppDir(), appFolder);
				try {
					if (temp.isDirectory() && temp.list().length > 0) {
						AppItem item = getApp(temp.getName());
						apps.add(item);
					} else {
						temp.delete();
					}
				} catch (RuntimeException re) {
					re.printStackTrace();
					FileUtils.deleteDirectory(temp);
				}
			}
		}
		return apps;
	}

	public static AppItem getApp(String path) {
		File appDir = new File(Config.getAppDir(), path);
		LinkedHashMap<String, String> params =
				FileUtils.loadManifest(new File(appDir.getAbsolutePath(), Config.MIDLET_MANIFEST_FILE));
		AppItem item = new AppItem(appDir.getName(), params.get("MIDlet-Name"),
				params.get("MIDlet-Vendor"),
				params.get("MIDlet-Version"));
		item.setImagePathExt(getImagePath(appDir, params));
		return item;
	}

	private static String getImagePath(File appDir, LinkedHashMap<String, String> params) {
		File defaultImage = new File(appDir.getAbsolutePath(), Config.MIDLET_ICON_FILE);
		String imagePath;
		if (defaultImage.exists()) {
			imagePath = defaultImage.getName();
		} else {
			imagePath = Config.MIDLET_RES_DIR + getImagePathFromManifest(params);
		}
		return imagePath;
	}

	public static String getImagePathFromManifest(LinkedHashMap<String, String> params) {
		String tmp;
		String imagePath = "";
		if ((tmp = params.get("MIDlet-Icon")) != null) {
			imagePath = tmp;
		} else if ((tmp = params.get("MIDlet-1")) != null) {
			imagePath = tmp.split(",")[1];
		}
		if (imagePath.length() > 0 && imagePath.charAt(0) != '/') {
			imagePath = "/" + imagePath;
		}
		return imagePath.replace(" ", "");
	}

	public static void deleteApp(AppItem item) {
		File appDir = new File(item.getPathExt());
		FileUtils.deleteDirectory(appDir);
		File appSaveDir = new File(Config.getDataDir(), item.getPath());
		FileUtils.deleteDirectory(appSaveDir);
		File appConfigsDir = new File(Config.getConfigsDir(), item.getPath());
		FileUtils.deleteDirectory(appConfigsDir);
	}

	public static void updateDb(AppRepository appRepository, List<AppItem> items) {
		String[] appFolders = new File(Config.getAppDir()).list();
		int itemsNum = items.size();
		if (appFolders == null || appFolders.length == 0) {
			// If db isn't empty
			if (itemsNum != 0) {
				appRepository.deleteAll();
			}
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
