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

import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;

import androidx.preference.PreferenceManager;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.config.ProfileModel;
import ru.playsoftware.j2meloader.config.ProfilesManager;

import static ru.playsoftware.j2meloader.util.Constants.PREF_DEFAULT_PROFILE;

public class MigrationUtils {

	private static final int VERSION_1 = 1;
	private static final int VERSION_2 = 2;
	private static final int VERSION_3 = 3;
	private static final int VERSION = 4;

	private static void moveConfigs(Context context) {
		File srcConfDir = new File(context.getApplicationInfo().dataDir, "/shared_prefs");
		for (File srcConf : srcConfDir.listFiles()) {
			String fileName = srcConf.getName().replace(".xml", "");
			if (fileName.equals("ru.playsoftware.j2meloader_preferences")) {
				continue;
			}
			File dstConf = new File(Config.getConfigsDir(), fileName + Config.MIDLET_CONFIG_FILE);
			dstConf.getParentFile().mkdirs();
			try {
				FileUtils.copyFileUsingChannel(srcConf, dstConf);
				srcConf.delete();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		File srcDataDir = new File(Config.getDataDir());
		if (!srcDataDir.exists()) {
			return;
		}
		for (File srcData : srcDataDir.listFiles()) {
			File srcKeylayout = new File(srcData, Config.MIDLET_KEY_LAYOUT_FILE);
			if (!srcKeylayout.exists()) {
				continue;
			}
			File dstKeylayout = new File(Config.getConfigsDir(),
					srcData.getName() + Config.MIDLET_KEY_LAYOUT_FILE);
			dstKeylayout.getParentFile().mkdirs();
			try {
				FileUtils.copyFileUsingChannel(srcKeylayout, dstKeylayout);
				srcKeylayout.delete();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static boolean moveKeyMappings(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String json = prefs.getString("pref_key_mapping", null);
		prefs.edit().remove("pref_key_mapping").apply();
		if (json != null && json.length() > 20) {
			File newDir = new File(Config.getProfilesDir(), "default");
			ProfileModel params = ProfilesManager.loadConfig(newDir);
			if (params == null) {
				params = new ProfileModel(newDir);
			}
			SparseIntArrayAdapter arrayAdapter = new SparseIntArrayAdapter();
			JsonReader jsonReader = new JsonReader(new StringReader(json));
			try {
				params.keyMappings = arrayAdapter.read(jsonReader);
				ProfilesManager.saveConfig(params);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		}
		return false;
	}

	private static boolean moveDefaultToProfiles() {
		File dir = new File(Config.getEmulatorDir(), "default");
		final String[] files = dir.list();
		if (files == null || files.length == 0) {
			return false;
		}
		File newDir = new File(Config.getProfilesDir(), "default");
		//noinspection ResultOfMethodCallIgnored
		dir.renameTo(newDir);
		final String[] list = newDir.list();
		return list != null && list.length > 0;
	}

	private static void moveDatabase(Context context) {
		File srcDb = new File(context.getApplicationInfo().dataDir, "databases/apps-database.db");
		File dstDb = new File(Config.getEmulatorDir(), "J2ME-apps.db");
		if (srcDb.exists()) {
			try {
				File dstDbShm = new File(dstDb + "-shm");
				File dstDbWal = new File(dstDb + "-wal");
				FileUtils.copyFileUsingChannel(srcDb, dstDb);
				FileUtils.copyFileUsingChannel(new File(srcDb + "-shm"), dstDbShm);
				FileUtils.copyFileUsingChannel(new File(srcDb + "-wal"), dstDbWal);
			} catch (IOException e) {
				e.printStackTrace();
			}
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
		File file = new File(Config.getEmulatorDir(), "DATA_VERSION");
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
			case VERSION_2:
			case VERSION_3:
				File profiles = new File(Config.getProfilesDir());
				if (moveDefaultToProfiles()) {
					PreferenceManager.getDefaultSharedPreferences(context)
							.edit().putString(PREF_DEFAULT_PROFILE, "default")
							.apply();
				}
				if (moveKeyMappings(context)) {
					PreferenceManager.getDefaultSharedPreferences(context)
							.edit().putString(PREF_DEFAULT_PROFILE, "default")
							.apply();
				}
				moveDatabase(context);
		}
		try {
			writeVersion(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
