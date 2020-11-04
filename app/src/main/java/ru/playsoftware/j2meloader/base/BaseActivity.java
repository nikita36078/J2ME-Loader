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

package ru.playsoftware.j2meloader.base;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import ru.playsoftware.j2meloader.R;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String theme = preferences.getString("pref_theme", "light");
		if (theme.equals("dark")) {
			setTheme(R.style.AppTheme);
		} else {
			setTheme(R.style.AppTheme_Light);
		}
		if (getSupportActionBar() != null) {
			getSupportActionBar().setElevation(getResources().getDisplayMetrics().density * 2);
		}
		super.onCreate(savedInstanceState);
	}
}
