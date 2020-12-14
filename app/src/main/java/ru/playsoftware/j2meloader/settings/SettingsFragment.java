/*
 * Copyright 2017-2018 Nikita Shakarun
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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;

import java.io.File;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.config.ProfilesActivity;
import ru.playsoftware.j2meloader.filepicker.FilteredFilePickerActivity;

import static ru.playsoftware.j2meloader.util.Constants.*;

public class SettingsFragment extends PreferenceFragmentCompat {
	private Preference prefFolder;

	@Override
	public void onCreatePreferences(Bundle bundle, String s) {
		addPreferencesFromResource(R.xml.preferences);
		findPreference("pref_default_settings").setOnPreferenceClickListener(preference -> {
			Intent intent = new Intent(getActivity(), ProfilesActivity.class);
			startActivity(intent);
			return true;
		});
		prefFolder = findPreference(PREF_EMULATOR_DIR);
		prefFolder.setSummary(Config.getEmulatorDir());
		prefFolder.setOnPreferenceClickListener(this::pickFolder);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == REQUEST_FILE && resultCode == Activity.RESULT_OK && data != null) {
			List<Uri> files = Utils.getSelectedFilesFromResult(data);
			File file = Utils.getFileForUri(files.get(0));
			applyChangeFolder(file);
		}
	}

	private void applyChangeFolder(File file) {
		if (!file.canWrite()) {
			Toast.makeText(getContext(), R.string.error, Toast.LENGTH_SHORT).show();
			return;
		}
		String path = file.getAbsolutePath();
		getPreferenceManager().getSharedPreferences().edit()
				.putString(PREF_EMULATOR_DIR, path)
				.apply();
		prefFolder.setSummary(path);
	}

	private boolean pickFolder(Preference preference) {
		Intent i = new Intent(getActivity(), FilteredFilePickerActivity.class);
		i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
		i.putExtra(FilePickerActivity.EXTRA_SINGLE_CLICK, false);
		i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
		i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
		i.putExtra(FilePickerActivity.EXTRA_START_PATH, Config.getEmulatorDir());
		startActivityForResult(i, REQUEST_FILE);
		return true;
	}
}
