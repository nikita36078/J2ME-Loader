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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ru.playsoftware.j2meloader.util.FileUtils;

public class Profile implements Comparable<Profile> {

	private String name;

	public Profile(String name) {
		this.name = name;
	}

	public void create() {
		getDir().mkdirs();
	}

	public boolean renameTo(String newName) {
		File oldDir = getDir();
		File newDir = new File(Config.getProfilesDir(), newName);
		name = newName;
		return oldDir.renameTo(newDir);
	}

	public void delete() {
		FileUtils.deleteDirectory(getDir());
	}

	public String getName() {
		return name;
	}

	public File getDir() {
		return new File(Config.getProfilesDir(), name);
	}

	public File getConfig() {
		return new File(Config.getProfilesDir(), name + Config.MIDLET_CONFIG_FILE);
	}

	public File getKeyLayout() {
		return new File(Config.getProfilesDir(), name + Config.MIDLET_KEY_LAYOUT_FILE);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(@NonNull Profile o) {
		return name.toLowerCase().compareTo(o.name.toLowerCase());
	}

	boolean hasConfig() {
		return getConfig().exists();
	}

	boolean hasKeyLayout() {
		return getKeyLayout().exists();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Profile)) {
			return false;
		}
		return name.equals(((Profile) obj).name);
	}

	public boolean hasOldConfig() {
		return new File(Config.getProfilesDir(), name + "/config.xml").exists();
	}
}
