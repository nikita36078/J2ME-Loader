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
import android.util.SparseIntArray;

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
import javax.microedition.util.param.SharedPreferencesContainer;

import androidx.preference.PreferenceManager;
import io.reactivex.Single;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.settings.KeyMapper;
import ru.playsoftware.j2meloader.util.FileUtils;

public class MicroLoader {
	private static final String TAG = MicroLoader.class.getName();

	private String path;
	private String appName;
	private Context context;
	private SharedPreferencesContainer params;

	public MicroLoader(Context context, String appName) {
		this.context = context;
		this.appName = appName;
		this.path = Config.APP_DIR + appName;
		this.params = new SharedPreferencesContainer(appName);
	}

	public void init() {
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
		params.load(false);
	}

	public LinkedHashMap<String, String> loadMIDletList() {
		LinkedHashMap<String, String> midlets = new LinkedHashMap<>();
		LinkedHashMap<String, String> params =
				FileUtils.loadManifest(new File(path, Config.MIDLET_MANIFEST_FILE));
		MIDlet.initProps(params);
		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (entry.getKey().matches("MIDlet-[0-9]+")) {
				String tmp = entry.getValue();
				String key = tmp.substring(tmp.lastIndexOf(',') + 1).trim();
				String value = tmp.substring(0, tmp.indexOf(',')).trim();
				midlets.put(key, value);
			}
		}
		return midlets;
	}

	MIDlet loadMIDlet(String mainClass)
			throws IOException, ClassNotFoundException, InstantiationException,
			IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		File dexSource = new File(path, Config.MIDLET_DEX_FILE);
		File dexTargetDir = new File(context.getApplicationInfo().dataDir, Config.TEMP_DEX_DIR);
		if (dexTargetDir.exists()) {
			FileUtils.deleteDirectory(dexTargetDir);
		}
		dexTargetDir.mkdir();
		File dexTargetOptDir = new File(context.getApplicationInfo().dataDir, Config.TEMP_DEX_OPT_DIR);
		if (dexTargetOptDir.exists()) {
			FileUtils.deleteDirectory(dexTargetOptDir);
		}
		dexTargetOptDir.mkdir();
		File dexTarget = new File(dexTargetDir, Config.MIDLET_DEX_FILE);
		FileUtils.copyFileUsingChannel(dexSource, dexTarget);
		File resDir = new File(path, Config.MIDLET_RES_DIR);
		ClassLoader loader = new AppClassLoader(dexTarget.getAbsolutePath(),
				dexTargetOptDir.getAbsolutePath(), context.getClassLoader(), resDir);
		Log.i(TAG, "loadMIDletList main: " + mainClass + " from dex:" + dexTarget.getPath());
		Log.i(TAG, "MIDlet-Name: " + AppClassLoader.getName());
		//noinspection unchecked
		Class<MIDlet> clazz = (Class<MIDlet>) loader.loadClass(mainClass);
		Constructor<MIDlet> init = clazz.getDeclaredConstructor();
		init.setAccessible(true);
		MIDlet midlet = init.newInstance();
		return midlet;
	}

	private void setProperties() {
		System.setProperty("microedition.sensor.version", "1");
		System.setProperty("microedition.platform", "Nokia 6233");
		System.setProperty("microedition.configuration", "CDLC-1.1");
		System.setProperty("microedition.profiles", "MIDP-2.0");
		System.setProperty("microedition.m3g.version", "1.1");
		System.setProperty("microedition.media.version", "1.0");
		System.setProperty("supports.mixing", "true");
		System.setProperty("supports.audio.capture", "true");
		System.setProperty("supports.video.capture", "false");
		System.setProperty("supports.recording", "false");
		System.setProperty("microedition.pim.version", "1.0");
		System.setProperty("microedition.io.file.FileConnection.version", "1.0");
		final Locale defaultLocale = Locale.getDefault();
		final String country = defaultLocale.getCountry();
		System.setProperty("microedition.locale", defaultLocale.getLanguage()
				+ (country.length() == 2 ? "-" + country : ""));
		System.setProperty("microedition.encoding", "ISO-8859-1");
		final String externalStoragePath = Environment.getExternalStorageDirectory().getPath();
		System.setProperty("user.home", externalStoragePath);
		System.setProperty("com.siemens.IMEI", "000000000000000");
		System.setProperty("com.siemens.mp.systemfolder.ringingtone", "fs/MyStuff/Ringtones");
		System.setProperty("com.siemens.mp.systemfolder.pictures", "fs/MyStuff/Pictures");
		System.setProperty("com.siemens.OSVersion", "11");
		System.setProperty("device.imei", "000000000000000");
		System.setProperty("com.nokia.mid.imei", "000000000000000");
		System.setProperty("fileconn.dir.cache", "file:///c:"
				+ Config.DATA_DIR.replace(externalStoragePath, "") + appName);
		System.setProperty("fileconn.dir.private", "file:///c:"
				+ Config.DATA_DIR.replace(externalStoragePath, "") + appName);
		System.setProperty("com.nokia.mid.impl.isa.visual_radio_operator_id", "0");
		System.setProperty("com.nokia.mid.impl.isa.visual_radio_channel_freq", "0");
	}

	public int getOrientation() {
		return params.getInt("Orientation", 0);
	}

	public void applyConfiguration() {
		try {
			boolean cxShowKeyboard = params.getBoolean(("ShowKeyboard"), true);

			// Apply configuration to the launching MIDlet
			if (cxShowKeyboard) {
				setVirtualKeyboard();
			} else {
				ContextHolder.setVk(null);
			}
			setProperties();

			int fontSizeSmall = params.getInt("FontSizeSmall", 18);
			int fontSizeMedium = params.getInt("FontSizeSmall", 18);
			int fontSizeLarge = params.getInt("FontSizeLarge", 26);
			boolean fontApplyDimensions = params.getBoolean("FontApplyDimensions", false);

			int screenWidth = params.getInt("ScreenWidth", 240);
			int screenHeight = params.getInt("ScreenHeight", 320);
			int screenBackgroundColor = params.getInt("ScreenBackgroundColor", 0xD0D0D0);
			int screenScaleRatio = params.getInt("ScreenScaleRatio", 100);
			boolean screenScaleToFit = params.getBoolean("ScreenScaleToFit", true);
			boolean screenKeepAspectRatio = params.getBoolean("ScreenKeepAspectRatio", true);
			boolean screenFilter = params.getBoolean("ScreenFilter", false);
			boolean immediateMode = params.getBoolean("ImmediateMode", false);
			boolean touchInput = params.getBoolean(("TouchInput"), true);
			boolean hwAcceleration = params.getBoolean("HwAcceleration", false);
			boolean parallel = params.getBoolean("ParallelRedrawScreen", false);
			boolean forceFullScreen = params.getBoolean("ForceFullscreen", false);
			boolean showFps = params.getBoolean("ShowFps", false);
			boolean limitFps = params.getBoolean("LimitFps", false);
			int fpsLimit = params.getInt("FpsLimit", 0);
			int layout = params.getInt("Layout", 0);

			Font.setSize(Font.SIZE_SMALL, fontSizeSmall);
			Font.setSize(Font.SIZE_MEDIUM, fontSizeMedium);
			Font.setSize(Font.SIZE_LARGE, fontSizeLarge);
			Font.setApplyDimensions(fontApplyDimensions);

			final String[] propLines = params.getString("SystemProperties", "").split("\n");
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

			SparseIntArray intArray = KeyMapper.getArrayPref(params);
			Displayable.setVirtualSize(screenWidth, screenHeight);
			Canvas.setScale(screenScaleToFit, screenKeepAspectRatio, screenScaleRatio);
			Canvas.setFilterBitmap(screenFilter);
			EventQueue.setImmediate(immediateMode);
			Canvas.setHardwareAcceleration(hwAcceleration, parallel);
			Canvas.setBackgroundColor(screenBackgroundColor);
			Canvas.setKeyMapping(layout, intArray);
			Canvas.setHasTouchInput(touchInput);
			Canvas.setForceFullscreen(forceFullScreen);
			Canvas.setShowFps(showFps);
			Canvas.setLimitFps(limitFps, fpsLimit);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setVirtualKeyboard() {
		int vkType = params.getInt("VirtualKeyboardType", 0);
		int vkAlpha = params.getInt("VirtualKeyboardAlpha", 64);
		int vkDelay = params.getInt("VirtualKeyboardDelay", -1);
		int vkColorBackground = params.getInt("VirtualKeyboardColorBackground", 0xD0D0D0);
		int vkColorForeground = params.getInt("VirtualKeyboardColorForeground", 0x000080);
		int vkColorBackgroundSelected = params.getInt("VirtualKeyboardColorBackgroundSelected", 0x000080);
		int vkColorForegroundSelected = params.getInt("VirtualKeyboardColorForegroundSelected", 0xFFFFFF);
		int vkColorOutline = params.getInt("VirtualKeyboardColorOutline", 0xFFFFFF);
		boolean vkFeedback = params.getBoolean(("VirtualKeyboardFeedback"), false);
		boolean vkForceOpacity = params.getBoolean(("VirtualKeyboardForceOpacity"), false);

		VirtualKeyboard vk;
		if (vkType == VirtualKeyboard.CUSTOMIZABLE_TYPE) {
			vk = new VirtualKeyboard();
		} else if (vkType == VirtualKeyboard.PHONE_DIGITS_TYPE) {
			vk = new FixedKeyboard(0);
		} else {
			vk = new FixedKeyboard(1);
		}
		vk.setOverlayAlpha(vkAlpha);
		vk.setHideDelay(vkDelay);
		vk.setHasHapticFeedback(vkFeedback);
		vk.setForceOpacity(vkForceOpacity);

		String shapeStr = PreferenceManager.getDefaultSharedPreferences(context)
				.getString("pref_button_shape", "round");
		int shape;
		if (shapeStr.equals("square")) {
			shape = VirtualKeyboard.SQUARE_SHAPE;
		} else {
			shape = VirtualKeyboard.ROUND_SHAPE;
		}
		vk.setButtonShape(shape);

		File keylayoutFile = new File(Config.CONFIGS_DIR, appName + Config.MIDLET_KEYLAYOUT_FILE);
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

		vk.setColor(VirtualKeyboard.BACKGROUND, vkColorBackground);
		vk.setColor(VirtualKeyboard.FOREGROUND, vkColorForeground);
		vk.setColor(VirtualKeyboard.BACKGROUND_SELECTED,
				vkColorBackgroundSelected);
		vk.setColor(VirtualKeyboard.FOREGROUND_SELECTED,
				vkColorForegroundSelected);
		vk.setColor(VirtualKeyboard.OUTLINE, vkColorOutline);

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
	public Single<String> takeScreenshot(Canvas canvas) {
		return Single.create(emitter -> {
			Bitmap bitmap = canvas.getOffscreenCopy().getBitmap();
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
