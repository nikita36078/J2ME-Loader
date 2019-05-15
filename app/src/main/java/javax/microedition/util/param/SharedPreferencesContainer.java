/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017 Nikita Shakarun
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

package javax.microedition.util.param;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.util.XmlUtils;

public class SharedPreferencesContainer implements DataContainer, DataEditor {
	private HashMap<String, Object> configMap;
	private File configFile;

	public SharedPreferencesContainer(File configDir) {
		configMap = new HashMap<>();
		configFile = new File(configDir, Config.MIDLET_CONFIG_FILE);
	}

	@Override
	public boolean load(boolean defaultConfig) {
		boolean loaded;
		File defaultConfigFile;
		if (!defaultConfig && !configFile.exists()) {
			defaultConfigFile = new File(Config.DEFAULT_CONFIG_DIR, Config.MIDLET_CONFIG_FILE);
			loaded = false;
		} else {
			defaultConfigFile = configFile;
			loaded = true;
		}
		try (FileInputStream fis = new FileInputStream(defaultConfigFile)) {
			configMap = XmlUtils.readMapXml(fis);
		} catch (IOException e) {
			e.printStackTrace();
			loaded = false;
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			loaded = false;
		}
		return loaded;
	}

	@Override
	public DataEditor edit() {
		return this;
	}

	@Override
	public boolean contains(String key) {
		return configMap.containsKey(key);
	}

	@Override
	public boolean getBoolean(String key, boolean defValue) {
		Boolean v = (Boolean) configMap.get(key);
		return v != null ? v : defValue;
	}

	@Override
	public float getFloat(String key, float defValue) {
		Float v = (Float) configMap.get(key);
		return v != null ? v : defValue;
	}

	@Override
	public int getInt(String key, int defValue) {
		Integer v = (Integer) configMap.get(key);
		return v != null ? v : defValue;
	}

	@Override
	public long getLong(String key, long defValue) {
		Long v = (Long) configMap.get(key);
		return v != null ? v : defValue;
	}

	@Override
	public String getString(String key, String defValue) {
		String v = (String) configMap.get(key);
		return v != null ? v : defValue;
	}

	@Override
	public DataEditor clear() {
		configMap.clear();
		return this;
	}

	@Override
	public DataEditor remove(String key) {
		configMap.remove(key);
		return this;
	}

	@Override
	public DataEditor putBoolean(String key, boolean value) {
		configMap.put(key, value);
		return this;
	}

	@Override
	public DataEditor putFloat(String key, float value) {
		configMap.put(key, value);
		return this;
	}

	@Override
	public DataEditor putInt(String key, int value) {
		configMap.put(key, value);
		return this;
	}

	@Override
	public DataEditor putLong(String key, long value) {
		configMap.put(key, value);
		return this;
	}

	@Override
	public DataEditor putString(String key, String value) {
		configMap.put(key, value);
		return this;
	}

	@Override
	public void apply() {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(configFile);
			XmlUtils.writeMapXml(configMap, fos);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
	}
}
