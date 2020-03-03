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

package ru.playsoftware.j2meloader.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.NonNull;
import ru.playsoftware.j2meloader.util.FileUtils;

class ProfilesManager {

	static ArrayList<Profile> getProfiles() {
		File root = new File(Config.PROFILES_DIR);
		return getList(root);
	}

	static ArrayList<Profile> getConfigs() {
		File root = new File(Config.CONFIGS_DIR);
		return getList(root);
	}

	@NonNull
	private static ArrayList<Profile> getList(File root) {
		File[] dirs = root.listFiles();
		if (dirs == null) {
			return new ArrayList<>();
		}
		int size = dirs.length;
		Profile[] profiles = new Profile[size];
		for (int i = 0; i < size; i++) {
			profiles[i] = new Profile(dirs[i].getName());
		}
		return new ArrayList<>(Arrays.asList(profiles));
	}

	static void load(Profile from, String toPath, boolean config, boolean keyboard)
			throws IOException {
		if (!config && !keyboard) {
			return;
		}
		File dstConfig = new File(toPath, Config.MIDLET_CONFIG_FILE);
		File dstKeyLayout = new File(toPath, Config.MIDLET_KEY_LAYOUT_FILE);
		try {
			if (config) FileUtils.copyFileUsingChannel(from.getConfig(), dstConfig);
			if (keyboard) FileUtils.copyFileUsingChannel(from.getKeyLayout(), dstKeyLayout);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	static void save(Profile profile, String fromPath, boolean config, boolean keyboard)
			throws IOException {
		if (!config && !keyboard) {
			return;
		}
		profile.create();
		File srcConfig = new File(fromPath, Config.MIDLET_CONFIG_FILE);
		File srcKeyLayout = new File(fromPath, Config.MIDLET_KEY_LAYOUT_FILE);
		try {
			if (config) FileUtils.copyFileUsingChannel(srcConfig, profile.getConfig());
			if (keyboard) FileUtils.copyFileUsingChannel(srcKeyLayout, profile.getKeyLayout());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
