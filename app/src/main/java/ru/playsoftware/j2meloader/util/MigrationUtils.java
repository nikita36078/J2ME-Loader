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

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.microedition.util.param.SharedPreferencesContainer;

import androidx.preference.PreferenceManager;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.settings.KeyMapper;

public class MigrationUtils {

	private static final int VERSION_1 = 1;
	private static final int VERSION_2 = 2;
	private static final int VERSION = 3;

	private static void moveConfigs(Context context) {
		File srcConfDir = new File(context.getApplicationInfo().dataDir, "/shared_prefs");
		for (File srcConf : srcConfDir.listFiles()) {
			String fileName = srcConf.getName().replace(".xml", "");
			if (fileName.equals("ru.playsoftware.j2meloader_preferences")) {
				continue;
			}
			File dstConf = new File(Config.CONFIGS_DIR, fileName + Config.MIDLET_CONFIG_FILE);
			dstConf.getParentFile().mkdirs();
			try {
				FileUtils.copyFileUsingChannel(srcConf, dstConf);
				srcConf.delete();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		File srcDataDir = new File(Config.DATA_DIR);
		if (!srcDataDir.exists()) {
			return;
		}
		for (File srcData : srcDataDir.listFiles()) {
			File srcKeylayout = new File(srcData, Config.MIDLET_KEYLAYOUT_FILE);
			if (!srcKeylayout.exists()) {
				continue;
			}
			File dstKeylayout = new File(Config.CONFIGS_DIR,
					srcData.getName() + Config.MIDLET_KEYLAYOUT_FILE);
			dstKeylayout.getParentFile().mkdirs();
			try {
				FileUtils.copyFileUsingChannel(srcKeylayout, dstKeylayout);
				srcKeylayout.delete();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void moveKeyMappings(Context context) {
		File defaultConfigDir = new File(Config.EMULATOR_DIR + "/default");
		SharedPreferencesContainer container = new SharedPreferencesContainer(defaultConfigDir);
		container.load();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String json = prefs.getString("pref_key_mapping", null);
		prefs.edit().remove("pref_key_mapping").apply();
		if (json != null && json.length() > 20) {
			container.edit().putString(KeyMapper.PREF_KEY, json).apply();
		}
	}

	private static int readVersion(File file) throws IOException {
		int version;
		try (FileInputStream in = new FileInputStream(file)) {
			version = in.read();
		}
		return version;
	}

	private static void writeVersion(File file) throws IOException {
		try (FileOutputStream stream = new FileOutputStream(file)) {
			stream.write(VERSION);
		}
	}

	public static void check(Context context) {
		File file = new File(Config.EMULATOR_DIR, "DATA_VERSION");
		int version = 0;
		try {
			version = readVersion(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		switch (version) {
			case 0:
				moveConfigs(context);
			case VERSION_1:
				moveKeyMappings(context);
			case VERSION_2:
				if (moveDefaultToTemplate()) {
					PreferenceManager.getDefaultSharedPreferences(context)
							.edit().putString(Config.DEFAULT_TEMPLATE_KEY, "default_migrated")
							.apply();
				}
		}
		try {
			writeVersion(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean moveDefaultToTemplate() {
		File dir = new File(Config.EMULATOR_DIR + "/default");
		final String[] files = dir.list();
		if (files == null || files.length == 0) {
			return false;
		}
		File newDir = new File(Config.TEMPLATES_DIR + "/default_migrated");
		//noinspection ResultOfMethodCallIgnored
		dir.renameTo(newDir);
		final String[] list = newDir.list();
		return list != null && list.length > 0;
	}
}
