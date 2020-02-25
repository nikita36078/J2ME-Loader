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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Process;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import javax.microedition.lcdui.pointer.VirtualKeyboard;
import javax.microedition.shell.AppClassLoader;
import javax.microedition.shell.MicroActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import ru.playsoftware.j2meloader.config.Config;

public class ContextHolder {
	private static Display display;
	private static VirtualKeyboard vk;
	private static AppCompatActivity currentActivity;
	private static ArrayList<ActivityResultListener> resultListeners = new ArrayList<>();

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

	public static void addActivityResultListener(ActivityResultListener listener) {
		if (!resultListeners.contains(listener)) {
			resultListeners.add(listener);
		}
	}

	public static void removeActivityResultListener(ActivityResultListener listener) {
		resultListeners.remove(listener);
	}

	public static void notifyOnActivityResult(int requestCode, int resultCode, Intent data) {
		for (ActivityResultListener listener : resultListeners) {
			listener.onActivityResult(requestCode, resultCode, data);
		}
	}

	public static InputStream getResourceAsStream(Class resClass, String resName) {
		return AppClassLoader.getResourceAsStream(resClass, resName);
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
		return new File(Config.DATA_DIR + AppClassLoader.getName(), name);
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

	public static MicroActivity getActivity() {
		return (MicroActivity) currentActivity;
	}
}
