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
import java.util.Arrays;

import ru.playsoftware.j2meloader.util.FileUtils;

public class TemplatesManager {

	public static ArrayList<Template> getTemplatesList() {
		File templatesDir = new File(Config.TEMPLATES_DIR);
		File[] templatesList = templatesDir.listFiles();
		if (templatesList == null) {
			return new ArrayList<>();
		}
		int size = templatesList.length;
		Template[] templates = new Template[size];
		for (int i = 0; i < size; i++) {
			templates[i] = new Template(templatesList[i].getName());
		}
		return new ArrayList<>(Arrays.asList(templates));
	}

	public static void loadTemplate(Template template, String path,
									boolean templateSettings, boolean templateKeyboard) throws IOException {
		if (!templateSettings && !templateKeyboard) {
			return;
		}
		File dstConfig = new File(path, Config.MIDLET_CONFIG_FILE);
		File dstKeylayout = new File(path, Config.MIDLET_KEYLAYOUT_FILE);
		try {
			if (templateSettings) FileUtils.copyFileUsingChannel(template.getConfig(), dstConfig);
			if (templateKeyboard) FileUtils.copyFileUsingChannel(template.getKeylayout(), dstKeylayout);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void saveTemplate(Template template, String path,
									boolean templateSettings, boolean templateKeyboard) throws IOException {
		if (!templateSettings && !templateKeyboard) {
			return;
		}
		template.create();
		File srcConfig = new File(path, Config.MIDLET_CONFIG_FILE);
		File srcKeylayout = new File(path, Config.MIDLET_KEYLAYOUT_FILE);
		try {
			if (templateSettings) FileUtils.copyFileUsingChannel(srcConfig, template.getConfig());
			if (templateKeyboard) FileUtils.copyFileUsingChannel(srcKeylayout, template.getKeylayout());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
