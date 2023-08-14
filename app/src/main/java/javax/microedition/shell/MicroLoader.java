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

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;

import org.acra.ACRA;
import org.acra.ErrorReporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
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
import javax.microedition.lcdui.keyboard.KeyMapper;
import javax.microedition.lcdui.keyboard.VirtualKeyboard;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.midlet.MIDlet;
import javax.microedition.util.ContextHolder;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ru.playsoftware.j2meloader.BuildConfig;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.config.ProfileModel;
import ru.playsoftware.j2meloader.config.ProfilesManager;
import ru.playsoftware.j2meloader.config.ShaderInfo;
import ru.playsoftware.j2meloader.util.Constants;
import ru.playsoftware.j2meloader.util.FileUtils;
import ru.playsoftware.j2meloader.util.IOUtils;
import ru.woesss.j2me.jar.Descriptor;

public class MicroLoader {
	private static final String TAG = MicroLoader.class.getName();

	private final File appDir;
	private final Context context;
	private final String workDir;
	private final String appDirName;
	private ProfileModel params;

	MicroLoader(Context context, String appPath) {
		this.context = context;
		this.appDir = new File(appPath);
		File converted = appDir.getParentFile();
		if (converted == null)
			throw new NullPointerException("Can't access to parent of " + appPath);
		workDir = converted.getParent();
		appDirName = appDir.getName();
	}

	public boolean init() {
		File config = new File(workDir + Config.MIDLET_CONFIGS_DIR + appDirName);
		this.params = ProfilesManager.loadConfig(config);
		if (params == null) {
			return false;
		}
		Display.initDisplay();
		Graphics3D.initGraphics3D();
		File cacheDir = ContextHolder.getCacheDir();
		// Some phones return null here
		if (cacheDir != null && cacheDir.exists()) {
			FileUtils.clearDirectory(cacheDir);
		}
		File internalDriveDir = new File(Config.getFsInternalDir());
		if (!internalDriveDir.exists()) {
			internalDriveDir.mkdirs();
		}
		File externalDriveDir = new File(Config.getFsExternalDir());
		if (!externalDriveDir.exists()) {
			externalDriveDir.mkdirs();
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
		String jarHash = null;
		Descriptor descriptor;
		if (BuildConfig.FULL_EMULATOR) {
			descriptor = new Descriptor(new File(appDir, Config.MIDLET_MANIFEST_FILE), false);
			try {
				byte[] bytes = FileUtils.getBytes(new File(appDir, Config.MIDLET_RES_FILE));
				byte[] sum = MessageDigest.getInstance("md5").digest(bytes);
				jarHash = String.format("%032x", new BigInteger(1, sum));
			} catch (Throwable ignored) {
			}
		} else {
			try (InputStream stream = getClass().getResourceAsStream("/MIDLET-META-INF/MANIFEST.MF")) {
				if (stream == null) {
					throw new RuntimeException("App manifest not found! It MUST be on project path:" +
							" 'app/midlet/resources/MIDLET-META-INF/MANIFEST.MF'");
				}
				String text = new String(IOUtils.toByteArray(stream));
				descriptor = new Descriptor(text, false);
			}
		}
		Map<String, String> attr = descriptor.getAttrs();
		ErrorReporter errorReporter = ACRA.getErrorReporter();
		String report = errorReporter.getCustomData(Constants.KEY_APPCENTER_ATTACHMENT);
		StringBuilder sb = new StringBuilder();
		if (report != null) {
			sb.append(report).append("\n");
		}
		sb.append(Descriptor.MIDLET_NAME).append(": ").append(descriptor.getName()).append("\n");
		sb.append(Descriptor.MIDLET_VENDOR).append(": ").append(descriptor.getVendor()).append("\n");
		sb.append(Descriptor.MIDLET_VERSION).append(": ").append(descriptor.getVersion()).append("\n");
		if (jarHash != null) {
			sb.append("JAR_HASH_MD5").append(": ").append(jarHash);
		}
		errorReporter.putCustomData(Constants.KEY_APPCENTER_ATTACHMENT, sb.toString());
		MIDlet.initProps(attr);
		for (int i = 1; ; i++) {
			String v = attr.get("MIDlet-" + i);
			if (v == null) {
				break;
			}
			String clazz = v.substring(v.lastIndexOf(',') + 1).trim();
			String title = v.substring(0, v.indexOf(',')).trim();
			midlets.put(clazz, title);
		}
		return midlets;
	}

	MIDlet loadMIDlet(String mainClass) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, NoSuchMethodException, InvocationTargetException, IOException {
		if (BuildConfig.FULL_EMULATOR) {
			File dexSource = new File(appDir, Config.MIDLET_DEX_FILE);
			File codeCacheDir = SDK_INT >= LOLLIPOP ? context.getCodeCacheDir() : context.getCacheDir();
			File dexOptDir = new File(codeCacheDir, Config.DEX_OPT_CACHE_DIR);
			if (dexOptDir.exists()) {
				FileUtils.clearDirectory(dexOptDir);
			} else if (!dexOptDir.mkdir()) {
				throw new IOException("Can't create directory: [" + dexOptDir + ']');
			}
			ClassLoader loader = new AppClassLoader(dexSource.getAbsolutePath(),
					dexOptDir.getAbsolutePath(), context.getClassLoader(), appDir);
			Log.i(TAG, "loadMIDletList main: " + mainClass + " from dex:" + dexSource.getPath());
			//noinspection unchecked
			Class<MIDlet> clazz = (Class<MIDlet>) loader.loadClass(mainClass);
			Constructor<MIDlet> init = clazz.getDeclaredConstructor();
			init.setAccessible(true);
			return init.newInstance();
		} else {
			AppClassLoader.setDataDir(appDir);
			//noinspection unchecked
			Class<MIDlet> clazz = (Class<MIDlet>) Class.forName(mainClass);
			Constructor<MIDlet> init = clazz.getDeclaredConstructor();
			init.setAccessible(true);
			return init.newInstance();
		}
	}

	private void setProperties() {
		final Locale defaultLocale = Locale.getDefault();
		final String country = defaultLocale.getCountry();
		System.setProperty("microedition.locale", defaultLocale.getLanguage()
				+ (country.length() == 2 ? "-" + country : ""));
		// FIXME: 21.10.2020 Config.getDataDir() may be in different storage
		final String primaryStoragePath = Environment.getExternalStorageDirectory().getPath();
		String dataUri = "file:///c:" + Config.getDataDir().substring(primaryStoragePath.length()) + appDirName;
		String musicUri = "file:///c:" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
				.getPath().substring(primaryStoragePath.length());
		System.setProperty("fileconn.dir.cache", dataUri + "/cache");
		System.setProperty("fileconn.dir.private", dataUri + "/private");
		System.setProperty("fileconn.dir.music", musicUri);
		System.setProperty("user.home", Config.getFsInternalDir());
	}

	public int getOrientation() {
		return params.orientation;
	}

	void setLimitFps(int fps) {
		if (fps == -1) Canvas.setLimitFps(params.fpsLimit);
		else Canvas.setLimitFps(fps);
	}

	void applyConfiguration() {
		try {
			// Apply configuration to the launching MIDlet
			if (params.showKeyboard) {
				ContextHolder.setVk(new VirtualKeyboard(params));
			} else {
				ContextHolder.setVk(null);
			}
			setProperties();

			final String[] propLines = params.systemProperties.split("\n");
			for (String line : propLines) {
				String[] prop = line.split(":[ ]*", 2);
				if (prop.length == 2) {
					System.setProperty(prop[0], prop[1]);
					MidletSystem.setProperty(prop[0], prop[1]);
				}
			}
			try {
				Charset.forName(System.getProperty("microedition.encoding"));
			} catch (Exception e) {
				System.setProperty("microedition.encoding", "ISO-8859-1");
				MidletSystem.setProperty("microedition.encoding", "ISO-8859-1");
			}

			int screenWidth = params.screenWidth;
			int screenHeight = params.screenHeight;
			Displayable.setVirtualSize(screenWidth, screenHeight);
			Canvas.setBackgroundColor(params.screenBackgroundColor);
			Canvas.setScale(params.screenGravity, params.screenScaleType, params.screenScaleRatio);
			Canvas.setFilterBitmap(params.screenFilter);
			EventQueue.setImmediate(params.immediateMode);
			Canvas.setGraphicsMode(params.graphicsMode, params.parallelRedrawScreen);
			ShaderInfo shader = params.shader;
			if (shader != null) {
				shader.dir = workDir + Config.SHADERS_DIR;
			}
			Canvas.setShaderFilter(shader);
			Canvas.setForceFullscreen(params.forceFullscreen);
			Canvas.setShowFps(params.showFps);
			Canvas.setLimitFps(params.fpsLimit);

			Font.applySettings(params);

			KeyMapper.setKeyMapping(params);
			Canvas.setHasTouchInput(params.touchInput);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void takeScreenshot(Canvas canvas, SingleObserver<String> observer) {
		canvas.getScreenShot()
				.subscribeOn(Schedulers.computation())
				.observeOn(Schedulers.io())
				.map(bitmap -> {
					Calendar calendar = Calendar.getInstance();
					Date now = calendar.getTime();
					//noinspection SpellCheckingInspection
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
					String fileName = "Screenshot_" + simpleDateFormat.format(now) + ".png";
					File screenshotDir = new File(Config.SCREENSHOTS_DIR);
					File screenshotFile = new File(screenshotDir, fileName);
					if (!screenshotDir.exists() && !screenshotDir.mkdirs()) {
						throw new IOException("Can't create directory: " + screenshotDir);
					}
					FileOutputStream out = new FileOutputStream(screenshotFile);
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
					return screenshotFile.getAbsolutePath();
				})
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(observer);
	}

	public int getMenuKeyCode() {
		SparseIntArray mappings = params.keyMappings;
		if (mappings == null) {
			return KeyEvent.KEYCODE_BACK;
		}
		int i = mappings.indexOfValue(KeyMapper.KEY_OPTIONS_MENU);
		if (i < 0) {
			return KeyEvent.KEYCODE_BACK;
		}
		return mappings.keyAt(i);
	}
}
