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
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import ru.playsoftware.j2meloader.R;

public class LoadTemplateDialogFragment extends DialogFragment {
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String configPath = requireArguments().getString(ConfigActivity.CONFIG_PATH_KEY);
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		@SuppressLint("InflateParams")
		View v = inflater.inflate(R.layout.dialog_load_template, null);
		ListView lvTemplate = v.findViewById(R.id.lvTemplate);
		ArrayList<Template> templates = TemplatesManager.getTemplatesList();
		ArrayAdapter<Template> adapter = new ArrayAdapter<>(requireActivity(),
				android.R.layout.simple_list_item_single_choice, templates);
		lvTemplate.setAdapter(adapter);
		CheckBox cbTemplateSettings = v.findViewById(R.id.cbTemplateSettings);
		CheckBox cbTemplateKeyboard = v.findViewById(R.id.cbTemplateKeyboard);
		AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
		builder.setTitle(R.string.LOAD_TEMPLATE_CMD)
				.setView(v)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					try {
						final int pos = lvTemplate.getCheckedItemPosition();
						final boolean configChecked = cbTemplateSettings.isChecked();
						final boolean vkChecked = cbTemplateKeyboard.isChecked();
						if (pos == -1 || !(configChecked || vkChecked)) {
							Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
							return;
						}
						TemplatesManager.loadTemplate((Template) lvTemplate.getItemAtPosition(pos), configPath,
								configChecked, vkChecked);
						((ConfigActivity) requireActivity()).loadParams();
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton(android.R.string.cancel, null);
		return builder.create();
	}
}
