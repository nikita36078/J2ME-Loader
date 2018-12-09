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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import ru.playsoftware.j2meloader.config.Config;

public class MigrationUtils {

	private static int CURRENT_VERSION = 1;

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

	private static int readVersion(File file) throws IOException {
		int version = 0;
		try (FileInputStream in = new FileInputStream(file)) {
			version = in.read();
		}
		return version;
	}

	private static void writeVersion(File file, int version) throws IOException {
		try (FileOutputStream stream = new FileOutputStream(file)) {
			stream.write(version);
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
		if (version < CURRENT_VERSION) {
			try {
				moveConfigs(context);
				writeVersion(file, CURRENT_VERSION);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
