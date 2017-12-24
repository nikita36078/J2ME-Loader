/*
 * Copyright (C) 2015-2016 Nickolay Savchenko
 * Copyright (C) 2017 Nikita Shakarun
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

package ua.naiksoftware.util;

public class Log {

	private static final String token = " : ";
	private static final long MAX_LEN = 300 * 1024;//50 Kb

	public static void d(String tag, String message) {
		System.out.println(message);
		/*try {
            boolean noClear;
            File file = new File(Environment.getExternalStorageDirectory(), "log_j2meloader.txt");
            if (file.length() > MAX_LEN) {
                noClear = false;
            } else {
                noClear = true;
            }
            FileWriter fw = new FileWriter(file, noClear);
            String msg = "\n" + new Date().toLocaleString() + token + tag + token + message;
            fw.write(msg);
            fw.flush();
            fw.close();
        } catch (IOException e) {
            android.util.Log.e("L", "err in logging", e);
        }*/
	}
}
