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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import ru.playsoftware.j2meloader.applist.AppItem;
import ru.playsoftware.j2meloader.appsdb.AppRepository;
import ru.playsoftware.j2meloader.config.Config;
import ru.woesss.j2me.jar.Descriptor;

public class AppUtils {
	private static final String TAG = AppUtils.class.getSimpleName();

	private static ArrayList<AppItem> getAppsList(@NonNull List<String> appFolders) {
		ArrayList<AppItem> apps = new ArrayList<>();
		File appsDir = new File(Config.getAppDir());
		for (String appFolderName : appFolders) {
			File appFolder = new File(appsDir, appFolderName);
			if (!appFolder.isDirectory()) {
				if (!appFolder.delete()) {
					Log.e(TAG, "getAppsList() failed delete file: " + appFolder);
				}
				continue;
			}
			File compressed = new File(appFolder, Config.MIDLET_DEX_ARCH);
			if (!compressed.isFile()) {
				File dex = new File(appFolder, Config.MIDLET_DEX_FILE);
				if (!dex.isFile()) {
					FileUtils.deleteDirectory(appFolder);
					continue;
				}
			}
			try {
				AppItem item = getApp(appFolder);
				apps.add(item);
			} catch (Exception e) {
				Log.w(TAG, "getAppsList: ", e);
				FileUtils.deleteDirectory(appFolder);
			}
		}
		return apps;
	}

	private static AppItem getApp(File appDir) throws IOException {
		File mf = new File(appDir, Config.MIDLET_MANIFEST_FILE);
		Descriptor params = new Descriptor(mf, false);
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
			// TODO: 30.07.2021 incomplete installation - maybe can continue?
			FileUtils.deleteDirectory(tmp);
		}
		String[] appFolders = new File(Config.getAppDir()).list();
		if (appFolders == null || appFolders.length == 0) {
			// If db isn't empty
			if (items.size() != 0) {
				appRepository.deleteAll();
			}
			return;
		}
		List<String> appFoldersList = new ArrayList<>(Arrays.asList(appFolders));
		// Delete invalid app items from db
		ListIterator<AppItem> iterator = items.listIterator(items.size());
		while (iterator.hasPrevious()) {
			AppItem item = iterator.previous();
			if (appFoldersList.remove(item.getPath())) {
				iterator.remove();
			}
		}
		if (items.size() > 0) {
			appRepository.delete(items);
		}
		if (appFoldersList.size() > 0) {
			appRepository.insert(getAppsList(appFoldersList));
		}
	}

	public static Bitmap getIconBitmap(AppItem appItem) {
		String file = appItem.getImagePathExt();
		if (file == null) {
			return null;
		}
		return BitmapFactory.decodeFile(file);
	}
}
