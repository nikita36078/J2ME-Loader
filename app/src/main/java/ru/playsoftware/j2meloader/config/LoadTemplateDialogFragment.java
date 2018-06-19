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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.util.FileUtils;

public class LoadTemplateDialogFragment extends DialogFragment {
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String appName = getArguments().getString(ConfigActivity.MIDLET_NAME_KEY);
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		View v = inflater.inflate(R.layout.dialog_load_template, null);
		Spinner spTemplate = v.findViewById(R.id.spTemplate);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_spinner_item, getTemplatesList());
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spTemplate.setAdapter(adapter);
		CheckBox cbTemplateSettings = v.findViewById(R.id.cbTemplateSettings);
		CheckBox cbTemplateKeyboard = v.findViewById(R.id.cbTemplateKeyboard);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.LOAD_TEMPLATE_CMD)
				.setView(v)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					loadTemplate((String) spTemplate.getSelectedItem(), appName,
							cbTemplateSettings.isChecked(), cbTemplateKeyboard.isChecked());
					((ConfigActivity) getActivity()).loadParamsFromFile();
				})
				.setNegativeButton(android.R.string.cancel, null);
		return builder.create();
	}

	private String[] getTemplatesList() {
		File templatesDir = new File(ConfigActivity.TEMPLATES_DIR);
		File[] templatesList = templatesDir.listFiles();
		if (templatesList == null) {
			return new String[0];
		}
		int size = templatesList.length;
		String[] templates = new String[size];
		for (int i = 0; i < size; i++) {
			templates[i] = templatesList[i].getName();
		}
		return templates;
	}

	private void loadTemplate(String template, String appName, boolean templateSettings, boolean templateKeyboard) {
		if (!templateSettings && !templateKeyboard) {
			return;
		}
		File srcConfig = new File(ConfigActivity.TEMPLATES_DIR, template + ConfigActivity.MIDLET_CONFIG_FILE);
		File srcKeylayout = new File(ConfigActivity.TEMPLATES_DIR, template + ConfigActivity.MIDLET_KEYLAYOUT_FILE);
		File dstConfig = new File(ConfigActivity.CONFIGS_DIR, appName + ConfigActivity.MIDLET_CONFIG_FILE);
		File dstKeylayout = new File(ConfigActivity.CONFIGS_DIR, appName + ConfigActivity.MIDLET_KEYLAYOUT_FILE);
		try {
			if (templateSettings) FileUtils.copyFileUsingChannel(srcConfig, dstConfig);
			if (templateKeyboard) FileUtils.copyFileUsingChannel(srcKeylayout, dstKeylayout);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
		}
	}
}
