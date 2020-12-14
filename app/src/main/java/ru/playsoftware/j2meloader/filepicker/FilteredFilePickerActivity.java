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

package ru.playsoftware.j2meloader.filepicker;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.nononsenseapps.filepicker.AbstractFilePickerActivity;
import com.nononsenseapps.filepicker.AbstractFilePickerFragment;

import java.io.File;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import ru.playsoftware.j2meloader.R;

import static ru.playsoftware.j2meloader.util.Constants.PREF_THEME;

public class FilteredFilePickerActivity extends AbstractFilePickerActivity<File> {

	private FilteredFilePickerFragment currentFragment;

	@Override
	protected AbstractFilePickerFragment<File> getFragment(@Nullable String startPath, int mode, boolean allowMultiple,
														   boolean allowCreateDir, boolean allowExistingFile, boolean singleClick) {
		currentFragment = new FilteredFilePickerFragment();
		currentFragment.setArgs(startPath, mode, allowMultiple, allowCreateDir, allowExistingFile, singleClick);
		return currentFragment;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String theme = preferences.getString(PREF_THEME, "light");
		if ("dark".equals(theme)) {
			setTheme(R.style.FilePickerTheme);
		} else {
			setTheme(R.style.FilePickerTheme_Light);
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onBackPressed() {
		if (currentFragment == null || currentFragment.isBackTop()) {
			super.onBackPressed();
		} else {
			currentFragment.goBack();
		}
	}
}
