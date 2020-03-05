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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;
import ru.playsoftware.j2meloader.R;

import static ru.playsoftware.j2meloader.config.ConfigActivity.CONFIG_PATH_KEY;

public class LoadProfileAlert extends DialogFragment {

	private ArrayList<Profile> profiles;
	private CheckBox cbConfig;
	private CheckBox cbKeyboard;

	static LoadProfileAlert newInstance(String parent) {
		LoadProfileAlert fragment = new LoadProfileAlert();
		Bundle args = new Bundle();
		args.putString(CONFIG_PATH_KEY, parent);
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
		String configPath = requireArguments().getString(CONFIG_PATH_KEY);
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		@SuppressLint("InflateParams")
		View v = inflater.inflate(R.layout.dialog_load_profile, null);
		ListView listView = v.findViewById(android.R.id.list);
		ArrayAdapter<Profile> adapter = new ArrayAdapter<>(requireActivity(),
				android.R.layout.simple_list_item_single_choice, profiles);
		listView.setOnItemClickListener(this::onItemClick);
		listView.setAdapter(adapter);
		cbConfig = v.findViewById(R.id.cbConfig);
		cbKeyboard = v.findViewById(R.id.cbKeyboard);
		AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
		builder.setTitle(R.string.load_profile)
				.setView(v)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					try {
						final int pos = listView.getCheckedItemPosition();
						final boolean configChecked = cbConfig.isChecked();
						final boolean vkChecked = cbKeyboard.isChecked();
						if (pos == -1) {
							Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
							return;
						}
						ProfilesManager.load((Profile) listView.getItemAtPosition(pos), configPath,
								configChecked, vkChecked);
						((ConfigActivity) requireActivity()).loadParams();
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton(android.R.string.cancel, null);
		final String def = PreferenceManager.getDefaultSharedPreferences(requireContext())
				.getString(Config.DEFAULT_PROFILE_KEY, null);

		if (def != null) {
			for (int i = 0, size = profiles.size(); i < size; i++) {
				Profile profile = profiles.get(i);
				if (profile.getName().equals(def)) {
					listView.setItemChecked(i, true);
					onItemClick(listView, null, i, i);
					break;
				}
			}
		}
		return builder.create();
	}

	private void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
		final Profile profile = profiles.get(pos);
		final boolean hasConfig = profile.hasConfig();
		final boolean hasVk = profile.hasKeyLayout();
		cbConfig.setEnabled(hasConfig && hasVk);
		cbConfig.setChecked(hasConfig);
		cbKeyboard.setEnabled(hasVk && hasConfig);
		cbKeyboard.setChecked(hasVk);
	}

}
