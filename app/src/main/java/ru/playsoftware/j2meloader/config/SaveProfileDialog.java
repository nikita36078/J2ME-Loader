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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.databinding.DialogSaveProfileBinding;

import static ru.playsoftware.j2meloader.util.Constants.KEY_CONFIG_PATH;
import static ru.playsoftware.j2meloader.util.Constants.PREF_DEFAULT_PROFILE;

public class SaveProfileDialog extends DialogFragment {

	private String configPath;

	DialogSaveProfileBinding binding;

	@NonNull
	public static SaveProfileDialog getInstance(String parent) {
		SaveProfileDialog saveProfileAlert = new SaveProfileDialog();
		Bundle bundleSave = new Bundle();
		bundleSave.putString(KEY_CONFIG_PATH, parent);
		saveProfileAlert.setArguments(bundleSave);
		return saveProfileAlert;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		configPath = requireArguments().getString(KEY_CONFIG_PATH);
		binding = DialogSaveProfileBinding.inflate(LayoutInflater.from(getContext()));
		AlertDialog dialog = new AlertDialog.Builder(requireActivity())
				.setTitle(R.string.save_profile).setView(binding.getRoot()).create();
		binding.negativeButton.setOnClickListener(v1 -> dismiss());
		binding.positiveButton.setOnClickListener(v1 -> {
			String name = binding.editText.getText().toString().trim().replaceAll("[/\\\\:*?\"<>|]", "");
			if (name.isEmpty()) {
				binding.editText.requestFocus();
				Toast.makeText(requireActivity(), R.string.error_name, Toast.LENGTH_SHORT).show();
				return;
			}

			final File config = new File(Config.getProfilesDir(), name + Config.MIDLET_CONFIG_FILE);
			if (config.exists()) {
				alertRewriteExists(name);
				return;
			}
			save(name);
		});
		return dialog;
	}

	private void alertRewriteExists(String name) {
		new AlertDialog.Builder(requireContext())
				.setMessage(getString(R.string.alert_rewrite_profile, name))
				.setPositiveButton(android.R.string.ok, (dialog, which) -> save(name))
				.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
					binding.editText.setText(name);
					binding.editText.requestFocus();
					binding.editText.setSelection(0, binding.editText.getText().length());
				})
				.show();
	}

	@SuppressLint("StringFormatInvalid")
	private void save(String name) {
		try {
			Profile profile = new Profile(name);
			ProfilesManager.save(profile, this.configPath, binding.cbConfig.isChecked(), binding.cbKeyboard.isChecked());
			if (binding.cbDefault.isChecked()) {
				PreferenceManager.getDefaultSharedPreferences(requireContext())
						.edit().putString(PREF_DEFAULT_PROFILE, name).apply();
			}
			Toast.makeText(requireContext(), getString(R.string.saved, name), Toast.LENGTH_SHORT).show();
			dismiss();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(requireActivity(), R.string.error, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}
