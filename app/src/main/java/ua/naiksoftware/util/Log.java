/*
 * J2ME Loader
 * Copyright (C) 2015-2016 Nickolay Savchenko
 * Copyright (C) 2017 Nikita Shakarun
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
