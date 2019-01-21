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
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
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

import javax.microedition.lcdui.pointer.VirtualKeyboard;
import javax.microedition.shell.MyClassLoader;

import ru.playsoftware.j2meloader.config.Config;

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
		if (resName.charAt(0) != '/' && resClass != null && resClass.getPackage() != null) {
			String className = resClass.getPackage().getName().replace('.', '/');
			resName = className + "/" + resName;
		}
		// Add support for Siemens file path
		resName = resName.replace('\\', '/');
		File resFile = new File(MyClassLoader.getResFolder(), resName);
		byte[] data = new byte[(int) resFile.length()];
		try (DataInputStream dis = new DataInputStream(new FileInputStream(resFile))) {
			dis.readFully(data);
			return new ByteArrayInputStream(data);
		} catch (IOException e) {
			Log.d(TAG, "Can't load res " + resName + " on path: " + MyClassLoader.getResFolder().getPath() + resName);
			return null;
		}
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

	/**
	 * Kill midlet process.
	 */
	public static void notifyDestroyed() {
		currentActivity.finish();
		Process.killProcess(Process.myPid());
	}
}
