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
import java.util.LinkedHashMap;

import ru.playsoftware.j2meloader.applist.AppItem;
import ru.playsoftware.j2meloader.config.Config;

public class AppUtils {

	public static ArrayList<AppItem> getAppsList() {
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
				}
			}
		}
		return apps;
	}

	public static AppItem getApp(String path) {
		File file = new File(Config.APP_DIR, path);
		LinkedHashMap<String, String> params =
				FileUtils.loadManifest(new File(file.getAbsolutePath(), Config.MIDLET_MANIFEST_FILE));
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

	public static void renameApp(String oldName, String newName) {
		File appSaveDirOld = new File(Config.DATA_DIR, oldName);
		File appSaveDirNew = new File(Config.DATA_DIR, newName);
		appSaveDirOld.renameTo(appSaveDirNew);
		File appConfigsDirOld = new File(Config.CONFIGS_DIR, oldName);
		File appConfigsDirNew = new File(Config.CONFIGS_DIR, newName);
		appConfigsDirOld.renameTo(appConfigsDirNew);
	}

	public static void deleteApp(AppItem item) {
		File appDir = new File(item.getPathExt());
		FileUtils.deleteDirectory(appDir);
		File appSaveDir = new File(Config.DATA_DIR, item.getTitle());
		FileUtils.deleteDirectory(appSaveDir);
		File appConfigsDir = new File(Config.CONFIGS_DIR, item.getTitle());
		FileUtils.deleteDirectory(appConfigsDir);
	}
}
