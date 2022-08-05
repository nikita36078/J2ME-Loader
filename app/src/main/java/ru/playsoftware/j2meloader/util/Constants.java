/*
 *  Copyright 2020 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.playsoftware.j2meloader.util;

public class Constants {
	public static final String ACTION_EDIT = "config.edit";
	public static final String ACTION_EDIT_PROFILE = "config.edit.profile";

	public static final String KEY_APP_URI = "appUri";
	public static final String KEY_APPCENTER_ATTACHMENT = "key_appcenter_attachment";
	public static final String KEY_CONFIG_PATH = "configPath";
	public static final String KEY_MIDLET_NAME = "midletName";
	public static final String KEY_START_ARGUMENTS = "startArguments";

	public static final String PREF_APP_SORT = "pref_app_sort";
	public static final String PREF_DEFAULT_PROFILE = "default_profile";
	public static final String PREF_EMULATOR_DIR = "emulator_dir";
	public static final String PREF_KEEP_SCREEN = "pref_wakelock_switch";
	public static final String PREF_LAST_PATH = "pref_last_path";
	public static final String PREF_STATUSBAR = "pref_statusbar_switch";
	public static final String PREF_THEME = "pref_theme";
	public static final String PREF_TOOLBAR = "pref_actionbar_switch";
	public static final String PREF_VIBRATION = "pref_vibration_switch";
	public static final String PREF_SCREENSHOT_SWITCH = "pref_screenshot_switch";
	public static final String PREF_STORAGE_WARNING_SHOWN = "pref_storage_warning_shown";

	public static final int RESULT_NEED_RECREATE = 1;

	private Constants(){}
}
