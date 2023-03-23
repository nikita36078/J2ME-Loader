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

import static ru.playsoftware.j2meloader.util.Constants.KEY_CONFIG_PATH;
import static ru.playsoftware.j2meloader.util.Constants.PREF_DEFAULT_PROFILE;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.databinding.DialogLoadProfileBinding;

public class LoadProfileDialog extends DialogFragment {
	private ArrayList<Profile> profiles;

	DialogLoadProfileBinding binding;

	static LoadProfileDialog newInstance(String parent) {
		LoadProfileDialog fragment = new LoadProfileDialog();
		Bundle args = new Bundle();
		args.putString(KEY_CONFIG_PATH, parent);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		profiles = ProfilesManager.getProfiles();
		Collections.sort(profiles);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String configPath = requireArguments().getString(KEY_CONFIG_PATH);
		binding = DialogLoadProfileBinding.inflate(LayoutInflater.from(getContext()));
		ArrayAdapter<Profile> adapter = new ArrayAdapter<>(requireActivity(),
				android.R.layout.simple_list_item_single_choice, profiles);
		binding.list.setOnItemClickListener(this::onItemClick);
		binding.list.setAdapter(adapter);
		AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
		builder.setTitle(R.string.load_profile)
				.setView(binding.getRoot())
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					try {
						final int pos = binding.list.getCheckedItemPosition();
						final boolean configChecked = binding.cbConfig.isChecked();
						final boolean vkChecked = binding.cbKeyboard.isChecked();
						if (pos == -1) {
							Toast.makeText(requireActivity(), R.string.error, Toast.LENGTH_SHORT).show();
							return;
						}
						ProfilesManager.load((Profile) binding.list.getItemAtPosition(pos), configPath,
								configChecked, vkChecked);
						((ConfigActivity) requireActivity()).loadParams(true);
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(requireActivity(), R.string.error, Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton(android.R.string.cancel, null);
		final String def = PreferenceManager.getDefaultSharedPreferences(requireContext())
				.getString(PREF_DEFAULT_PROFILE, null);

		if (def != null) {
			for (int i = 0, size = profiles.size(); i < size; i++) {
				Profile profile = profiles.get(i);
				if (profile.getName().equals(def)) {
					binding.list.setItemChecked(i, true);
					onItemClick(binding.list, null, i, i);
					break;
				}
			}
		}
		return builder.create();
	}

	private void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
		final Profile profile = profiles.get(pos);
		final boolean hasConfig = profile.hasConfig() || profile.hasOldConfig();
		final boolean hasVk = profile.hasKeyLayout();
		binding.cbConfig.setEnabled(hasConfig && hasVk);
		binding.cbConfig.setChecked(hasConfig);
		binding.cbKeyboard.setEnabled(hasVk && hasConfig);
		binding.cbKeyboard.setChecked(hasVk);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}
