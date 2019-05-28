/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017-2018 Nikita Shakarun
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

package javax.microedition.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

import javax.microedition.lcdui.pointer.VirtualKeyboard;
import javax.microedition.shell.MyClassLoader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.util.ZipFileCompat;

public class ContextHolder {
	private static final String TAG = ContextHolder.class.getName();

	private static Display display;
	private static VirtualKeyboard vk;
	private static AppCompatActivity currentActivity;

	public static Context getContext() {
		return currentActivity.getApplicationContext();
	}

	public static VirtualKeyboard getVk() {
		return vk;
	}

	public static void setVk(VirtualKeyboard vk) {
		ContextHolder.vk = vk;
	}

	private static Display getDisplay() {
		if (display == null) {
			display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		}
		return display;
	}

	public static int getDisplayWidth() {
		return getDisplay().getWidth();
	}

	public static int getDisplayHeight() {
		return getDisplay().getHeight();
	}

	public static void setCurrentActivity(AppCompatActivity activity) {
		currentActivity = activity;
	}

	public static AppCompatActivity getCurrentActivity() {
		return currentActivity;
	}

	public static InputStream getResourceAsStream(Class resClass, String resName) {
		Log.d(TAG, "CUSTOM GET RES CALLED WITH PATH: " + resName);
		if (resName == null || resName.equals("")) {
			Log.d(TAG, "Can't load res on empty path");
			return null;
		}
		if (resName.charAt(0) == '/') {
			resName = resName.substring(1);
		} else if (resClass != null && resClass.getPackage() != null) {
			String className = resClass.getPackage().getName().replace('.', '/');
			resName = className + "/" + resName;
		}
		// Add support for Siemens file path
		resName = resName.replace('\\', '/');
		// Remove double slashes
		resName = resName.replace("//", "/");
		try {
			return getResource(resName);
		} catch (IOException | NullPointerException e) {
			Log.d(TAG, "Can't load res: " + resName);
			return null;
		}
	}

	private static InputStream getResource(String resName) throws IOException {
		InputStream is;
		byte[] data;
		File midletResFile = new File(Config.APP_DIR,
				MyClassLoader.getName() + Config.MIDLET_RES_FILE);
		if (midletResFile.exists()) {
			ZipFileCompat zipFile = new ZipFileCompat(midletResFile);
			ZipEntry entry = zipFile.getEntry(resName);
			is = zipFile.getInputStream(entry);
			data = new byte[(int) entry.getSize()];
		} else {
			File resFile = new File(MyClassLoader.getResFolder(), resName);
			is = new FileInputStream(resFile);
			data = new byte[(int) resFile.length()];
		}
		DataInputStream dis = new DataInputStream(is);
		dis.readFully(data);
		dis.close();
		return new ByteArrayInputStream(data);
	}

	public static FileOutputStream openFileOutput(String name) throws FileNotFoundException {
		return new FileOutputStream(getFileByName(name));
	}

	public static FileInputStream openFileInput(String name) throws FileNotFoundException {
		return new FileInputStream(getFileByName(name));
	}

	public static boolean deleteFile(String name) {
		return getFileByName(name).delete();
	}

	public static File getFileByName(String name) {
		return new File(Config.DATA_DIR + MyClassLoader.getName(), name);
	}

	public static File getCacheDir() {
		return getContext().getExternalCacheDir();
	}

	public static boolean requestPermission(String permission) {
		if (ContextCompat.checkSelfPermission(currentActivity, permission) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(currentActivity, new String[]{permission}, 0);
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Kill midlet process.
	 */
	public static void notifyDestroyed() {
		currentActivity.finish();
		Process.killProcess(Process.myPid());
	}
}
