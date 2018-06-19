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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.util.FileUtils;

public class SaveTemplateDialogFragment extends DialogFragment {
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String appName = getArguments().getString(ConfigActivity.MIDLET_NAME_KEY);
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		View v = inflater.inflate(R.layout.dialog_save_template, null);
		EditText etTemplateName = v.findViewById(R.id.etTemplateName);
		CheckBox cbTemplateSettings = v.findViewById(R.id.cbTemplateSettings);
		CheckBox cbTemplateKeyboard = v.findViewById(R.id.cbTemplateKeyboard);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.SAVE_TEMPLATE_CMD)
				.setView(v)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					String template = etTemplateName.getText().toString().trim();
					if (template.equals("")) {
						Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
					} else {
						saveTemplate(template, appName,
								cbTemplateSettings.isChecked(), cbTemplateKeyboard.isChecked());
					}
				})
				.setNegativeButton(android.R.string.cancel, null);
		return builder.create();
	}

	private void saveTemplate(String template, String appName, boolean templateSettings, boolean templateKeyboard) {
		if (!templateSettings && !templateKeyboard) {
			return;
		}
		File templateDir = new File(Config.TEMPLATES_DIR, template);
		templateDir.mkdirs();
		File srcConfig = new File(Config.CONFIGS_DIR, appName + Config.MIDLET_CONFIG_FILE);
		File srcKeylayout = new File(Config.CONFIGS_DIR, appName + Config.MIDLET_KEYLAYOUT_FILE);
		File dstConfig = new File(Config.TEMPLATES_DIR, template + Config.MIDLET_CONFIG_FILE);
		File dstKeylayout = new File(Config.TEMPLATES_DIR, template + Config.MIDLET_KEYLAYOUT_FILE);
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
