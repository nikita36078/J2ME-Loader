/*
 * Copyright 2019 Nikita Shakarun
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

package ru.playsoftware.j2meloader.util;

import java.io.File;
import java.io.IOException;

import ru.playsoftware.j2meloader.config.Config;

public class LogUtils {

	public static void writeLog() throws IOException {
		File logFile = new File(Config.getEmulatorDir(), "log.txt");
		if (logFile.exists()) {
			logFile.delete();
		}
		Runtime.getRuntime().exec("logcat -t 500 -f " + logFile);
	}

}
