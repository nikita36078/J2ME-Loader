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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import com.nononsenseapps.filepicker.Utils;

import java.io.File;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.config.ProfilesActivity;
import ru.playsoftware.j2meloader.util.FileUtils;
import ru.playsoftware.j2meloader.util.PickDirResultContract;

import static ru.playsoftware.j2meloader.util.Constants.PREF_EMULATOR_DIR;
import static ru.playsoftware.j2meloader.util.Constants.PREF_STR;

public class SettingsFragment extends PreferenceFragmentCompat {
	private Preference prefFolder;
	private final ActivityResultLauncher<String> openDirLauncher = registerForActivityResult(
			new PickDirResultContract(),
			this::onPickDirResult);

	private final ActivityResultLauncher<String> changeWorkDirLauncher = registerForActivityResult(
			FileUtils.getDirPicker(),
			this::onChangeWorkDirResult);

	private void onChangeWorkDirResult(Uri uri) {
		if (uri == null) {
			return;
		}
		String filePath = FileUtils.fileUriToStr(uri);
		requireActivity().getSharedPreferences(PREF_STR, Context.MODE_WORLD_WRITEABLE).edit().putString(PREF_EMULATOR_DIR, filePath).apply();
		prefFolder = findPreference(PREF_EMULATOR_DIR);
		prefFolder.setSummary(filePath);

	}


	@Override
	public void onCreatePreferences(Bundle bundle, String s) {
		addPreferencesFromResource(R.xml.preferences);
		findPreference("pref_default_settings").setIntent(new Intent(requireActivity(), ProfilesActivity.class));
		prefFolder = findPreference(PREF_EMULATOR_DIR);
		prefFolder.setSummary(Config.getEmulatorDir());
		if (FileUtils.isExternalStorageLegacy()) {
			prefFolder.setOnPreferenceClickListener(preference -> {
				openDirLauncher.launch(null);
				return true;
			});
		}
		findPreference(PREF_EMULATOR_DIR).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				File dir = Environment.getExternalStorageDirectory();
				if (dir.canRead()) {
					try {
						changeWorkDirLauncher.launch(dir.getAbsolutePath());
					} catch (ActivityNotFoundException e) {
						Toast.makeText(getContext(), R.string.error_no_picker, Toast.LENGTH_SHORT).show();
						e.printStackTrace();
					}
				}
				return false;
			}
		});

	}

	private void onPickDirResult(Uri uri) {
		if (uri == null) {
			return;
		}
		File file = Utils.getFileForUri(uri);
		String path = file.getAbsolutePath();
		if (!FileUtils.initWorkDir(file)) {
			new AlertDialog.Builder(requireActivity())
					.setTitle(R.string.error)
					.setCancelable(false)
					.setMessage(getString(R.string.create_apps_dir_failed, path))
					.setNegativeButton(android.R.string.cancel, null)
					.setPositiveButton(R.string.choose, (d, w) -> openDirLauncher.launch(null))
					.show();
			return;
		}
		requireActivity().getSharedPreferences(PREF_STR, Context.MODE_WORLD_WRITEABLE).edit()
				.putString(PREF_EMULATOR_DIR, path)
				.apply();
		prefFolder.setSummary(path);
	}
}
