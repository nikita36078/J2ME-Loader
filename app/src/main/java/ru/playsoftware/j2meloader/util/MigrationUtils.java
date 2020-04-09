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
import java.io.IOException;

import ru.playsoftware.j2meloader.config.Config;

public class MigrationUtils {

	private static void moveKeyLayouts() {
		File srcDataDir = new File(Config.DATA_DIR);
		if (!srcDataDir.exists()) {
			return;
		}
		File[] files = srcDataDir.listFiles();
		if (files == null) {
			return;
		}
		for (File srcData : files) {
			File srcKeyLayout = new File(srcData, Config.MIDLET_KEY_LAYOUT_FILE);
			if (!srcKeyLayout.exists()) {
				continue;
			}
			File dstKeyLayout = new File(Config.CONFIGS_DIR,
					srcData.getName() + Config.MIDLET_KEY_LAYOUT_FILE);
			dstKeyLayout.getParentFile().mkdirs();
			try {
				FileUtils.copyFileUsingChannel(srcKeyLayout, dstKeyLayout);
				srcKeyLayout.delete();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void check() {
		moveKeyLayouts();
		moveTemplatesToProfiles();
		moveDefaultToProfiles();
	}

	private static void moveTemplatesToProfiles() {
		File templates = new File(Config.EMULATOR_DIR, "templates");
		if (templates.exists()) {
			File profiles = new File(Config.PROFILES_DIR);
			FileUtils.copyFiles(templates, profiles, null);
		}
	}

	private static void moveDefaultToProfiles() {
		File dir = new File(Config.EMULATOR_DIR, "default");
		final String[] files = dir.list();
		if (files == null || files.length == 0) {
			return;
		}
		File newDir = new File(Config.PROFILES_DIR, "J2ME-Loader");
		FileUtils.copyFiles(dir, newDir, null);
	}
}
