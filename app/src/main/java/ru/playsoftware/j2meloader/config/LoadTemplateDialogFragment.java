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

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

import ru.playsoftware.j2meloader.R;

public class LoadTemplateDialogFragment extends DialogFragment {
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String appName = getArguments().getString(ConfigActivity.MIDLET_NAME_KEY);
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		View v = inflater.inflate(R.layout.dialog_load_template, null);
		Spinner spTemplate = v.findViewById(R.id.spTemplate);
		ArrayList<Template> templates = TemplatesManager.getTemplatesList();
		ArrayAdapter<Template> adapter = new ArrayAdapter<>(getActivity(),
				android.R.layout.simple_spinner_item, templates);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spTemplate.setAdapter(adapter);
		CheckBox cbTemplateSettings = v.findViewById(R.id.cbTemplateSettings);
		CheckBox cbTemplateKeyboard = v.findViewById(R.id.cbTemplateKeyboard);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.LOAD_TEMPLATE_CMD)
				.setView(v)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					try {
						TemplatesManager.loadTemplate((Template) spTemplate.getSelectedItem(), appName,
								cbTemplateSettings.isChecked(), cbTemplateKeyboard.isChecked());
						((ConfigActivity) getActivity()).loadParamsFromFile();
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton(android.R.string.cancel, null);
		return builder.create();
	}
}
