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

package javax.microedition.shell;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.event.EventQueue;
import javax.microedition.lcdui.pointer.FixedKeyboard;
import javax.microedition.lcdui.pointer.VirtualKeyboard;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.midlet.MIDlet;
import javax.microedition.util.ContextHolder;

import io.reactivex.Single;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.config.ProfileModel;
import ru.playsoftware.j2meloader.config.ProfilesManager;
import ru.playsoftware.j2meloader.config.ShaderInfo;
import ru.playsoftware.j2meloader.settings.KeyMapper;
import ru.playsoftware.j2meloader.util.FileUtils;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

public class MicroLoader {
	private static final String TAG = MicroLoader.class.getName();

	private String path;
	private Context context;
	private ProfileModel params;
	private String appPath;

	MicroLoader(Context context, String appPath) {
		this.context = context;
		this.appPath = appPath;
		this.path = Config.getAppDir() + appPath;
	}

	public boolean init() {
		this.params = ProfilesManager.loadConfig(new File(Config.getConfigsDir(), appPath));
		if (params == null) {
			return false;
		}
		Display.initDisplay();
		Graphics3D.initGraphics3D();
		File cacheDir = ContextHolder.getCacheDir();
		// Some phones return null here
		if (cacheDir != null && cacheDir.exists()) {
			for (File temp : cacheDir.listFiles()) {
				temp.delete();
			}
		}
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitNetwork()
				.penaltyLog()
				.build();
		StrictMode.setThreadPolicy(policy);
		return true;
	}

	LinkedHashMap<String, String> loadMIDletList() throws IOException {
		LinkedHashMap<String, String> midlets = new LinkedHashMap<>();
		LinkedHashMap<String, String> params =
				FileUtils.loadManifest(new File(path, Config.MIDLET_MANIFEST_FILE));
		MIDlet.initProps(params);
		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (entry.getKey().matches("MIDlet-[0-9]+")) {
				String tmp = entry.getValue();
				String clazz = tmp.substring(tmp.lastIndexOf(',') + 1).trim();
				String title = tmp.substring(0, tmp.indexOf(',')).trim();
				midlets.put(clazz, title);
			}
		}
		return midlets;
	}

	MIDlet loadMIDlet(String mainClass) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, NoSuchMethodException, InvocationTargetException, IOException {
		File dexSource = new File(path, Config.MIDLET_DEX_FILE);
		File codeCacheDir = SDK_INT >= LOLLIPOP ? context.getCodeCacheDir() : context.getCacheDir();
		File dexOptDir = new File(codeCacheDir, Config.DEX_OPT_CACHE_DIR);
		if (dexOptDir.exists()) {
			FileUtils.clearDirectory(dexOptDir);
		} else if (!dexOptDir.mkdir()) {
			throw new IOException("Cant't create directory: [" + dexOptDir + ']');
		}
		File resDir = new File(path, Config.MIDLET_RES_DIR);
		ClassLoader loader = new AppClassLoader(dexSource.getAbsolutePath(),
				dexOptDir.getAbsolutePath(), context.getClassLoader(), resDir);
		Log.i(TAG, "loadMIDletList main: " + mainClass + " from dex:" + dexSource.getPath());
		Log.i(TAG, "MIDlet-Name: " + AppClassLoader.getName());
		//noinspection unchecked
		Class<MIDlet> clazz = (Class<MIDlet>) loader.loadClass(mainClass);
		Constructor<MIDlet> init = clazz.getDeclaredConstructor();
		init.setAccessible(true);
		return init.newInstance();
	}

	private void setProperties() {
		final Locale defaultLocale = Locale.getDefault();
		final String country = defaultLocale.getCountry();
		System.setProperty("microedition.locale", defaultLocale.getLanguage()
				+ (country.length() == 2 ? "-" + country : ""));
		final String primaryStoragePath = Environment.getExternalStorageDirectory().getPath();
		String uri = "file:///c:" + Config.getDataDir().substring(primaryStoragePath.length()) + appPath;
		System.setProperty("fileconn.dir.cache", uri + "/cache");
		System.setProperty("fileconn.dir.private", uri + "/private");
		System.setProperty("user.home", primaryStoragePath);
	}

	public int getOrientation() {
		return params.orientation;
	}

	void applyConfiguration() {
		try {
			// Apply configuration to the launching MIDlet
			if (params.showKeyboard) {
				setVirtualKeyboard();
			} else {
				ContextHolder.setVk(null);
			}
			setProperties();

			Font.setSize(Font.SIZE_SMALL, params.fontSizeSmall);
			Font.setSize(Font.SIZE_MEDIUM, params.fontSizeSmall);
			Font.setSize(Font.SIZE_LARGE, params.fontSizeLarge);
			Font.setApplyDimensions(params.fontApplyDimensions);

			final String[] propLines = params.systemProperties.split("\n");
			for (String line : propLines) {
				String[] prop = line.split(":[ ]*", 2);
				if (prop.length == 2) {
					System.setProperty(prop[0], prop[1]);
				}
			}
			try {
				Charset.forName(System.getProperty("microedition.encoding"));
			} catch (Exception e) {
				System.setProperty("microedition.encoding", "ISO-8859-1");
			}

			Displayable.setVirtualSize(params.screenWidth, params.screenHeight);
			Canvas.setScale(params.screenScaleToFit, params.screenKeepAspectRatio, params.screenScaleRatio);
			Canvas.setFilterBitmap(params.screenFilter);
			EventQueue.setImmediate(params.immediateMode);
			Canvas.setGraphicsMode(params.getGraphicsMode(), params.parallelRedrawScreen);
			Canvas.setBackgroundColor(params.screenBackgroundColor);
			Canvas.setKeyMapping(params.keyCodesLayout, KeyMapper.getArrayPref(params));
			Canvas.setHasTouchInput(params.touchInput);
			Canvas.setForceFullscreen(params.forceFullscreen);
			Canvas.setShowFps(params.showFps);
			Canvas.setLimitFps(params.fpsLimit);
			ShaderInfo shader = params.shader;
			if (shader == null) shader = new ShaderInfo();
			Canvas.setShaderFilter(shader);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setVirtualKeyboard() {
		int vkType = params.vkType;
		VirtualKeyboard vk;
		if (vkType == VirtualKeyboard.CUSTOMIZABLE_TYPE) {
			vk = new VirtualKeyboard();
		} else if (vkType == VirtualKeyboard.PHONE_DIGITS_TYPE) {
			vk = new FixedKeyboard(0);
		} else {
			vk = new FixedKeyboard(1);
		}
		vk.setHideDelay(params.vkHideDelay);
		vk.setHasHapticFeedback(params.vkFeedback);
		vk.setButtonShape(params.vkButtonShape);
		vk.setForceOpacity(params.vkForceOpacity);

		File keylayoutFile = new File(Config.getConfigsDir(), appPath + Config.MIDLET_KEY_LAYOUT_FILE);
		if (keylayoutFile.exists()) {
			try {
				FileInputStream fis = new FileInputStream(keylayoutFile);
				DataInputStream dis = new DataInputStream(fis);
				vk.readLayout(dis);
				fis.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

		int vkAlpha = params.vkAlpha << 24;
		vk.setColor(VirtualKeyboard.BACKGROUND, vkAlpha | params.vkBgColor);
		vk.setColor(VirtualKeyboard.FOREGROUND, vkAlpha | params.vkFgColor);
		vk.setColor(VirtualKeyboard.BACKGROUND_SELECTED, vkAlpha | params.vkBgColorSelected);
		vk.setColor(VirtualKeyboard.FOREGROUND_SELECTED, vkAlpha | params.vkFgColorSelected);
		vk.setColor(VirtualKeyboard.OUTLINE, vkAlpha | params.vkOutlineColor);

		VirtualKeyboard.LayoutListener listener = vk1 -> {
			try {
				FileOutputStream fos = new FileOutputStream(keylayoutFile);
				DataOutputStream dos = new DataOutputStream(fos);
				vk1.writeLayout(dos);
				fos.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		};
		vk.setLayoutListener(listener);
		ContextHolder.setVk(vk);
	}

	@SuppressLint("SimpleDateFormat")
	Single<String> takeScreenshot(Canvas canvas) {
		return Single.create(emitter -> {
			Bitmap bitmap = canvas.getScreenShot();
			Calendar calendar = Calendar.getInstance();
			Date now = calendar.getTime();
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
			String fileName = "Screenshot_" + simpleDateFormat.format(now) + ".png";
			File screenshotDir = new File(Config.SCREENSHOTS_DIR);
			File screenshotFile = new File(screenshotDir, fileName);
			if (!screenshotDir.exists()) {
				screenshotDir.mkdirs();
			}
			FileOutputStream out = new FileOutputStream(screenshotFile);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
			emitter.onSuccess(screenshotFile.getAbsolutePath());
		});
	}
}
