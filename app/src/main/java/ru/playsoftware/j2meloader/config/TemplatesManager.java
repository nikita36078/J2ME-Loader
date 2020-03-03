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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import ru.playsoftware.j2meloader.util.FileUtils;

class TemplatesManager {

	static ArrayList<Template> getTemplatesList() {
		File templatesDir = new File(Config.TEMPLATES_DIR);
		File[] templatesList = templatesDir.listFiles();
		if (templatesList == null) {
			return new ArrayList<>();
		}
		int size = templatesList.length;
		ArrayList<Template> templates = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			templates.add(new Template(templatesList[i].getName()));
		}
		return templates;
	}

	static void loadTemplate(Template template, String path,
							 boolean templateSettings, boolean templateKeyboard) throws IOException {
		if (!templateSettings && !templateKeyboard) {
			return;
		}
		File dstConfig = new File(path, Config.MIDLET_CONFIG_FILE);
		File dstKeyLayout = new File(path, Config.MIDLET_KEYLAYOUT_FILE);
		try {
			if (templateSettings) FileUtils.copyFileUsingChannel(template.getConfig(), dstConfig);
			if (templateKeyboard) FileUtils.copyFileUsingChannel(template.getKeyLayout(), dstKeyLayout);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	static void saveTemplate(Template template, String path, boolean config, boolean keyboard)
			throws IOException {
		if (!config && !keyboard) {
			return;
		}
		template.create();
		File srcConfig = new File(path, Config.MIDLET_CONFIG_FILE);
		File srcKeyLayout = new File(path, Config.MIDLET_KEYLAYOUT_FILE);
		try {
			if (config) FileUtils.copyFileUsingChannel(srcConfig, template.getConfig());
			if (keyboard) FileUtils.copyFileUsingChannel(srcKeyLayout, template.getKeyLayout());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
