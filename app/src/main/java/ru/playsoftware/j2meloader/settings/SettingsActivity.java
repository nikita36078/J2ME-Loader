/*
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

package ru.playsoftware.j2meloader.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;

import java.util.Map;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.preference.PreferenceManager;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.base.BaseActivity;
import ru.playsoftware.j2meloader.config.Config;

public class SettingsActivity extends BaseActivity {
	private Map<String, ?> oldParams;
	private SharedPreferences preferences;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.action_settings);
		setResult(RESULT_OK);
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		oldParams = preferences.getAll();
	}

	@Override
	public void finish() {
		String defPath = Environment.getExternalStorageDirectory() + "/" + getString(R.string.app_name);
		//noinspection ConstantConditions
		if (preferences.getString("pref_theme", "light").equals(oldParams.get("pref_theme"))
		&& preferences.getString("pref_app_sort", "name").equals(oldParams.get("pref_app_sort"))
		&& preferences.getString(Config.PREF_EMULATOR_DIR, defPath).equals(oldParams.get(Config.PREF_EMULATOR_DIR))) {
			setResult(RESULT_OK);
		} else {
			setResult(1);
		}
		super.finish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}
