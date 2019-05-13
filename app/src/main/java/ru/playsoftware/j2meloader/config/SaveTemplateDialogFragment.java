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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import ru.playsoftware.j2meloader.R;

public class SaveTemplateDialogFragment extends DialogFragment {
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String configPath = getArguments().getString(ConfigActivity.CONFIG_PATH_KEY);
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		View v = inflater.inflate(R.layout.dialog_save_template, null);
		EditText etTemplateName = v.findViewById(R.id.etTemplateName);
		CheckBox cbTemplateSettings = v.findViewById(R.id.cbTemplateSettings);
		CheckBox cbTemplateKeyboard = v.findViewById(R.id.cbTemplateKeyboard);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.SAVE_TEMPLATE_CMD)
				.setView(v)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					String templateName = etTemplateName.getText().toString().trim();
					if (templateName.equals("")) {
						Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
					} else {
						try {
							Template template = new Template(templateName);
							TemplatesManager.saveTemplate(template, configPath,
									cbTemplateSettings.isChecked(), cbTemplateKeyboard.isChecked());
						} catch (IOException e) {
							e.printStackTrace();
							Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
						}
					}
				})
				.setNegativeButton(android.R.string.cancel, null);
		return builder.create();
	}
}
