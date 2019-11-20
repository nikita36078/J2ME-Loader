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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import java.io.File;

import javax.microedition.shell.MicroActivity;

public class Config {

	public static final String MIDLET_DIR = "/converted/";
	public static final String EMULATOR_DIR = Environment.getExternalStorageDirectory() + "/J2ME-Loader";
	public static final String SCREENSHOTS_DIR =
			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/J2ME-Loader";
	public static final String DATA_DIR = EMULATOR_DIR + "/data/";
	public static final String DEFAULT_CONFIG_DIR = EMULATOR_DIR + "/default/";
	public static final String CONFIGS_DIR = EMULATOR_DIR + "/configs/";
	public static final String TEMPLATES_DIR = EMULATOR_DIR + "/templates/";
	public static final String APP_DIR = EMULATOR_DIR + MIDLET_DIR;
	public static final String TEMP_DEX_DIR = "/tmp_dex";
	public static final String TEMP_DEX_OPT_DIR = "/tmp_dexopt";
	public static final String MIDLET_RES_DIR = "/res";
	public static final String MIDLET_DEX_FILE = "/converted.dex";
	public static final String MIDLET_RES_FILE = "/res.jar";
	public static final String MIDLET_ICON_FILE = "/icon.png";
	public static final String MIDLET_MANIFEST_FILE = MIDLET_DEX_FILE + ".conf";
	public static final String MIDLET_KEYLAYOUT_FILE = "/VirtualKeyboardLayout";
	public static final String MIDLET_CONFIG_FILE = "/config.xml";

	public static void startApp(Context context, String appName, boolean showSettings) {
		File file = new File(Config.CONFIGS_DIR, appName);
		if (!showSettings && file.exists()) {
			Intent intent = new Intent(context, MicroActivity.class);
			intent.putExtra(ConfigActivity.MIDLET_NAME_KEY, appName);
			context.startActivity(intent);
		} else {
			Intent intent = new Intent(Intent.ACTION_DEFAULT, Uri.parse(appName),
					context, ConfigActivity.class);
			intent.putExtra(ConfigActivity.SHOW_SETTINGS_KEY, true);
			context.startActivity(intent);
		}
	}

}
