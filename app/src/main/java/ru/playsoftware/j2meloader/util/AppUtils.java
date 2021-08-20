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

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import ru.playsoftware.j2meloader.applist.AppItem;
import ru.playsoftware.j2meloader.appsdb.AppRepository;
import ru.playsoftware.j2meloader.config.Config;
import ru.woesss.j2me.jar.Descriptor;

public class AppUtils {
	private static final String TAG = AppUtils.class.getSimpleName();

	private static ArrayList<AppItem> getAppsList() {
		ArrayList<AppItem> apps = new ArrayList<>();
		String[] appFolders = new File(Config.getAppDir()).list();
		if (appFolders != null) {
			for (String appFolder : appFolders) {
				File temp = new File(Config.getAppDir(), appFolder);
				if (!temp.isDirectory()) {
					//noinspection ResultOfMethodCallIgnored
					temp.delete();
					continue;
				}
				String[] list = temp.list();
				if (list == null || list.length == 0) {
					//noinspection ResultOfMethodCallIgnored
					temp.delete();
					continue;
				}
				try {
					AppItem item = getApp(temp.getName());
					apps.add(item);
				} catch (Exception e) {
					Log.w(TAG, "getAppsList: ", e);
				}
			}
		}
		return apps;
	}

	private static AppItem getApp(String path) throws IOException {
		File appDir = new File(Config.getAppDir(), path);
		File file = new File(appDir, Config.MIDLET_MANIFEST_FILE);
		Descriptor params = new Descriptor(file, false);
		AppItem item = new AppItem(appDir.getName(), params.getName(),
				params.getVendor(),
				params.getVersion());
		File icon = new File(appDir, Config.MIDLET_ICON_FILE);
		if (icon.exists()) {
			item.setImagePathExt(Config.MIDLET_ICON_FILE);
		} else {
			String iconPath = Config.MIDLET_RES_DIR + '/' + params.getIcon();
			icon = new File(appDir, iconPath);
			if (icon.exists()) {
				item.setImagePathExt(iconPath);
			}
		}
		return item;
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
		File tmp = new File(Config.getAppDir(), ".tmp");
		if (tmp.exists()) {
			// TODO: 30.07.2021 uncompleted installation - may be continue???
			FileUtils.deleteDirectory(tmp);
		}
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
