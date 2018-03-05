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

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesContainer implements DataContainer, DataEditor {
	protected SharedPreferences prefs;
	protected SharedPreferences.Editor editor;

	public SharedPreferencesContainer(String name, int mode, Context context) {
		prefs = context.getSharedPreferences(name, mode);
	}

	@Override
	public DataEditor edit() {
		if (editor == null) {
			editor = prefs.edit();
		}

		return this;
	}

	@Override
	public boolean contains(String key) {
		return prefs.contains(key);
	}

	@Override
	public boolean getBoolean(String key, boolean defValue) {
		return prefs.getBoolean(key, defValue);
	}

	@Override
	public float getFloat(String key, float defValue) {
		return prefs.getFloat(key, defValue);
	}

	@Override
	public int getInt(String key, int defValue) {
		return prefs.getInt(key, defValue);
	}

	@Override
	public long getLong(String key, long defValue) {
		return prefs.getLong(key, defValue);
	}

	@Override
	public String getString(String key, String defValue) {
		return prefs.getString(key, defValue);
	}

	@Override
	public boolean getBoolean(String key) {
		return prefs.getBoolean(key, false);
	}

	@Override
	public float getFloat(String key) {
		return prefs.getFloat(key, 0);
	}

	@Override
	public int getInt(String key) {
		return prefs.getInt(key, 0);
	}

	@Override
	public long getLong(String key) {
		return prefs.getLong(key, 0);
	}

	@Override
	public String getString(String key) {
		return prefs.getString(key, "");
	}

	@Override
	public DataEditor clear() {
		editor.clear();
		return this;
	}

	@Override
	public DataEditor remove(String key) {
		editor.remove(key);
		return this;
	}

	@Override
	public DataEditor putBoolean(String key, boolean value) {
		editor.putBoolean(key, value);
		return this;
	}

	@Override
	public DataEditor putFloat(String key, float value) {
		editor.putFloat(key, value);
		return this;
	}

	@Override
	public DataEditor putInt(String key, int value) {
		editor.putInt(key, value);
		return this;
	}

	@Override
	public DataEditor putLong(String key, long value) {
		editor.putLong(key, value);
		return this;
	}

	@Override
	public DataEditor putString(String key, String value) {
		editor.putString(key, value);
		return this;
	}

	@Override
	public void apply() {
		editor.apply();
	}

	@Override
	public boolean commit() {
		return editor.commit();
	}

	@Override
	public void close() {
		editor = null;
	}
}
