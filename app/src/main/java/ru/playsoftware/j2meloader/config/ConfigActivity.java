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

package ru.playsoftware.j2meloader.config;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.event.EventQueue;
import javax.microedition.lcdui.pointer.FixedKeyboard;
import javax.microedition.lcdui.pointer.VirtualKeyboard;
import javax.microedition.shell.MicroActivity;
import javax.microedition.util.ContextHolder;
import javax.microedition.util.param.SharedPreferencesContainer;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.base.BaseActivity;
import ru.playsoftware.j2meloader.settings.KeyMapper;
import ru.playsoftware.j2meloader.util.FileUtils;
import yuku.ambilwarna.AmbilWarnaDialog;

public class ConfigActivity extends BaseActivity implements View.OnClickListener {

	protected EditText tfScreenWidth;
	protected EditText tfScreenHeight;
	protected EditText tfScreenBack;
	protected SeekBar sbScaleRatio;
	protected EditText tfScaleRatioValue;
	protected Spinner spOrientation;
	protected CheckBox cxScaleToFit;
	protected CheckBox cxKeepAspectRatio;
	protected CheckBox cxFilter;
	protected CheckBox cxImmediate;
	protected CheckBox cxHwAcceleration;
	protected CheckBox cxParallel;
	protected CheckBox cxShowFps;
	protected CheckBox cxLimitFps;
	protected EditText tfFpsLimit;

	protected EditText tfFontSizeSmall;
	protected EditText tfFontSizeMedium;
	protected EditText tfFontSizeLarge;
	protected CheckBox cxFontSizeInSP;
	protected EditText tfSystemProperties;
	protected CheckBox cxShowKeyboard;
	protected CheckBox cxVKFeedback;
	protected CheckBox cxTouchInput;

	protected Spinner spVKType;
	protected SeekBar sbVKAlpha;
	protected EditText tfVKHideDelay;
	protected EditText tfVKFore;
	protected EditText tfVKBack;
	protected EditText tfVKSelFore;
	protected EditText tfVKSelBack;
	protected EditText tfVKOutline;

	protected ArrayList<Integer> screenWidths;
	protected ArrayList<Integer> screenHeights;
	protected ArrayList<String> screenAdapter;

	protected ArrayList<Integer> fontSmall;
	protected ArrayList<Integer> fontMedium;
	protected ArrayList<Integer> fontLarge;
	protected ArrayList<String> fontAdapter;

	private File keylayoutFile;
	private File dataDir;
	private SharedPreferencesContainer params;
	private String pathToMidletDir;
	private FragmentManager fragmentManager;
	private boolean defaultConfig;

	public static final String DEFAULT_CONFIG_KEY = "default";
	public static final String CONFIG_PATH_KEY = "configPath";
	public static final String MIDLET_PATH_KEY = "midletPath";
	public static final String MIDLET_ORIENTATION_KEY = "orientation";
	public static final String SHOW_SETTINGS_KEY = "showSettings";

	@SuppressLint({"StringFormatMatches", "StringFormatInvalid"})
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_config);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		ContextHolder.setCurrentActivity(this);
		Intent intent = getIntent();
		fragmentManager = getSupportFragmentManager();
		defaultConfig = intent.getBooleanExtra(DEFAULT_CONFIG_KEY, false);
		File configDir;
		boolean showSettings;
		if (defaultConfig) {
			showSettings = true;
			configDir = new File(Config.DEFAULT_CONFIG_DIR);
		} else {
			showSettings = intent.getBooleanExtra(SHOW_SETTINGS_KEY, false);
			pathToMidletDir = intent.getDataString();
			String appName = pathToMidletDir.substring(pathToMidletDir.lastIndexOf('/') + 1);
			getSupportActionBar().setTitle(appName);
			dataDir = new File(Config.DATA_DIR, appName);
			dataDir.mkdirs();
			configDir = new File(Config.CONFIGS_DIR, appName);
		}
		configDir.mkdirs();
		keylayoutFile = loadKeylayout(configDir);

		params = new SharedPreferencesContainer(configDir);
		boolean loaded = params.load(defaultConfig);

		setProperties();

		tfScreenWidth = findViewById(R.id.tfScreenWidth);
		tfScreenHeight = findViewById(R.id.tfScreenHeight);
		tfScreenBack = findViewById(R.id.tfScreenBack);
		cxScaleToFit = findViewById(R.id.cxScaleToFit);
		sbScaleRatio = findViewById(R.id.sbScaleRatio);
		tfScaleRatioValue = findViewById(R.id.tfScaleRatioValue);
		spOrientation = findViewById(R.id.spOrientation);
		cxKeepAspectRatio = findViewById(R.id.cxKeepAspectRatio);
		cxFilter = findViewById(R.id.cxFilter);
		cxImmediate = findViewById(R.id.cxImmediate);
		cxHwAcceleration = findViewById(R.id.cxHwAcceleration);
		cxParallel = findViewById(R.id.cxParallel);
		cxShowFps = findViewById(R.id.cxShowFps);
		cxLimitFps = findViewById(R.id.cxLimitFps);
		tfFpsLimit = findViewById(R.id.tfFpsLimit);

		tfFontSizeSmall = findViewById(R.id.tfFontSizeSmall);
		tfFontSizeMedium = findViewById(R.id.tfFontSizeMedium);
		tfFontSizeLarge = findViewById(R.id.tfFontSizeLarge);
		cxFontSizeInSP = findViewById(R.id.cxFontSizeInSP);
		tfSystemProperties = findViewById(R.id.tfSystemProperties);
		cxShowKeyboard = findViewById(R.id.cxIsShowKeyboard);
		cxVKFeedback = findViewById(R.id.cxVKFeedback);
		cxTouchInput = findViewById(R.id.cxTouchInput);

		spVKType = findViewById(R.id.spVKType);
		sbVKAlpha = findViewById(R.id.sbVKAlpha);
		tfVKHideDelay = findViewById(R.id.tfVKHideDelay);
		tfVKFore = findViewById(R.id.tfVKFore);
		tfVKBack = findViewById(R.id.tfVKBack);
		tfVKSelFore = findViewById(R.id.tfVKSelFore);
		tfVKSelBack = findViewById(R.id.tfVKSelBack);
		tfVKOutline = findViewById(R.id.tfVKOutline);

		screenWidths = new ArrayList<>();
		screenHeights = new ArrayList<>();
		screenAdapter = new ArrayList<>();

		fillScreenSizePresets(ContextHolder.getDisplayWidth(), ContextHolder.getDisplayHeight());

		fontSmall = new ArrayList<>();
		fontMedium = new ArrayList<>();
		fontLarge = new ArrayList<>();
		fontAdapter = new ArrayList<>();

		addFontSizePreset("128 x 128", 9, 13, 15);
		addFontSizePreset("128 x 160", 13, 15, 20);
		addFontSizePreset("176 x 220", 15, 18, 22);
		addFontSizePreset("240 x 320", 18, 22, 26);

		findViewById(R.id.cmdScreenSizePresets).setOnClickListener(this);
		findViewById(R.id.cmdSwapSizes).setOnClickListener(this);
		findViewById(R.id.cmdFontSizePresets).setOnClickListener(this);
		findViewById(R.id.cmdScreenBack).setOnClickListener(this);
		findViewById(R.id.cmdVKBack).setOnClickListener(this);
		findViewById(R.id.cmdVKFore).setOnClickListener(this);
		findViewById(R.id.cmdVKSelBack).setOnClickListener(this);
		findViewById(R.id.cmdVKSelFore).setOnClickListener(this);
		findViewById(R.id.cmdVKOutline).setOnClickListener(this);
		sbScaleRatio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) tfScaleRatioValue.setText(String.valueOf(progress));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		tfScaleRatioValue.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				sbScaleRatio.setProgress(Integer.parseInt(s.toString()));
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			cxHwAcceleration.setOnCheckedChangeListener((buttonView, isChecked) -> {
				if (isChecked) {
					cxParallel.setChecked(false);
				}
			});
			cxParallel.setOnCheckedChangeListener((buttonView, isChecked) -> {
				if (isChecked) {
					cxHwAcceleration.setChecked(false);
				}
			});
		}

		loadParams();
		applyConfiguration();

		cxVKFeedback.setEnabled(cxShowKeyboard.isChecked());
		cxShowKeyboard.setOnClickListener(v -> {
			if (!((CheckBox) v).isChecked()) {
				cxVKFeedback.setEnabled(false);
			} else {
				cxVKFeedback.setEnabled(true);
			}
		});

		if (loaded && !showSettings) {
			startMIDlet();
		}
	}

	private File loadKeylayout(File configDir) {
		File file = new File(configDir, Config.MIDLET_KEYLAYOUT_FILE);
		if (!defaultConfig && !file.exists()) {
			File defaultKeylayoutFile = new File(Config.DEFAULT_CONFIG_DIR, Config.MIDLET_KEYLAYOUT_FILE);
			if (defaultKeylayoutFile.exists()) {
				try {
					FileUtils.copyFileUsingChannel(defaultKeylayoutFile, file);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return file;
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
		System.setProperty("user.home", Environment.getExternalStorageDirectory().getPath());
		System.setProperty("com.siemens.IMEI", "000000000000000");
		System.setProperty("com.siemens.mp.systemfolder.ringingtone", "fs/MyStuff/Ringtones");
		System.setProperty("com.siemens.mp.systemfolder.pictures", "fs/MyStuff/Pictures");
		System.setProperty("device.imei", "000000000000000");
	}

	@Override
	public void onPause() {
		saveParams();
		super.onPause();
	}

	@Override
	protected void onResume() {
		loadParams();
		super.onResume();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		fillScreenSizePresets(ContextHolder.getDisplayWidth(), ContextHolder.getDisplayHeight());
	}

	private void fillScreenSizePresets(int w, int h) {
		screenWidths.clear();
		screenHeights.clear();
		screenAdapter.clear();

		addScreenSizePreset(128, 128);
		addScreenSizePreset(128, 160);
		addScreenSizePreset(132, 176);
		addScreenSizePreset(176, 220);
		addScreenSizePreset(240, 320);
		addScreenSizePreset(352, 416);
		addScreenSizePreset(640, 360);
		addScreenSizePreset(800, 480);

		if (w > h) {
			addScreenSizePreset(h * 3 / 4, h);
			addScreenSizePreset(h * 4 / 3, h);
		} else {
			addScreenSizePreset(w, w * 4 / 3);
			addScreenSizePreset(w, w * 3 / 4);
		}

		addScreenSizePreset(w, h);
	}

	private void addScreenSizePreset(int width, int height) {
		screenWidths.add(width);
		screenHeights.add(height);
		screenAdapter.add(Integer.toString(width) + " x " + Integer.toString(height));
	}

	private void addFontSizePreset(String title, int small, int medium, int large) {
		fontSmall.add(small);
		fontMedium.add(medium);
		fontLarge.add(large);
		fontAdapter.add(title);
	}

	@SuppressLint("SetTextI18n")
	private void loadParams() {
		tfScreenWidth.setText(Integer.toString(params.getInt("ScreenWidth", 240)));
		tfScreenHeight.setText(Integer.toString(params.getInt("ScreenHeight", 320)));
		tfScreenBack.setText(Integer.toHexString(params.
				getInt("ScreenBackgroundColor", 0xD0D0D0)).toUpperCase());
		sbScaleRatio.setProgress(params.getInt("ScreenScaleRatio", 100));
		tfScaleRatioValue.setText(String.valueOf(sbScaleRatio.getProgress()));
		spOrientation.setSelection(params.getInt("Orientation", 0));
		cxScaleToFit.setChecked(params.getBoolean("ScreenScaleToFit", true));
		cxKeepAspectRatio.setChecked(params.getBoolean("ScreenKeepAspectRatio", true));
		cxFilter.setChecked(params.getBoolean("ScreenFilter", false));
		cxImmediate.setChecked(params.getBoolean("ImmediateMode", false));
		cxParallel.setChecked(params.getBoolean("ParallelRedrawScreen", false));
		cxHwAcceleration.setChecked(params.getBoolean("HwAcceleration", false));
		cxShowFps.setChecked(params.getBoolean("ShowFps", false));
		cxLimitFps.setChecked(params.getBoolean("LimitFps", false));

		tfFontSizeSmall.setText(Integer.toString(params.getInt("FontSizeSmall", 18)));
		tfFontSizeMedium.setText(Integer.toString(params.getInt("FontSizeMedium", 22)));
		tfFontSizeLarge.setText(Integer.toString(params.getInt("FontSizeLarge", 26)));
		cxFontSizeInSP.setChecked(params.getBoolean("FontApplyDimensions", false));
		tfSystemProperties.setText(params.getString("SystemProperties", ""));
		cxShowKeyboard.setChecked(params.getBoolean(("ShowKeyboard"), true));
		cxVKFeedback.setChecked(params.getBoolean(("VirtualKeyboardFeedback"), false));
		cxTouchInput.setChecked(params.getBoolean(("TouchInput"), true));
		tfFpsLimit.setText(Integer.toString(params.getInt("FpsLimit", 0)));

		spVKType.setSelection(params.getInt("VirtualKeyboardType", 0));
		sbVKAlpha.setProgress(params.getInt("VirtualKeyboardAlpha", 64));
		tfVKHideDelay.setText(Integer.toString(params.getInt("VirtualKeyboardDelay", -1)));
		tfVKBack.setText(Integer.toHexString(
				params.getInt("VirtualKeyboardColorBackground", 0xD0D0D0)).toUpperCase());
		tfVKFore.setText(Integer.toHexString(
				params.getInt("VirtualKeyboardColorForeground", 0x000080)).toUpperCase());
		tfVKSelBack.setText(Integer.toHexString(
				params.getInt("VirtualKeyboardColorBackgroundSelected", 0x000080)).toUpperCase());
		tfVKSelFore.setText(Integer.toHexString(
				params.getInt("VirtualKeyboardColorForegroundSelected", 0xFFFFFF)).toUpperCase());
		tfVKOutline.setText(Integer.toHexString(
				params.getInt("VirtualKeyboardColorOutline", 0xFFFFFF)).toUpperCase());
	}

	public void loadParamsFromFile() {
		params.load(defaultConfig);
		loadParams();
	}

	private void saveParams() {
		try {
			params.edit();

			params.putInt("ScreenWidth", Integer.parseInt(tfScreenWidth.getText().toString()));
			params.putInt("ScreenHeight", Integer.parseInt(tfScreenHeight.getText().toString()));
			params.putInt("ScreenBackgroundColor", Integer.parseInt(tfScreenBack.getText().toString(), 16));
			params.putInt("ScreenScaleRatio", sbScaleRatio.getProgress());
			params.putInt("Orientation", spOrientation.getSelectedItemPosition());
			params.putBoolean("ScreenScaleToFit", cxScaleToFit.isChecked());
			params.putBoolean("ScreenKeepAspectRatio", cxKeepAspectRatio.isChecked());
			params.putBoolean("ScreenFilter", cxFilter.isChecked());
			params.putBoolean("ImmediateMode", cxImmediate.isChecked());
			params.putBoolean("HwAcceleration", cxHwAcceleration.isChecked());
			params.putBoolean("ParallelRedrawScreen", cxParallel.isChecked());
			params.putBoolean("ShowFps", cxShowFps.isChecked());
			params.putBoolean("LimitFps", cxLimitFps.isChecked());
			params.putInt("FpsLimit", Integer.parseInt(tfFpsLimit.getText().toString()));

			params.putInt("FontSizeSmall",
					Integer.parseInt(tfFontSizeSmall.getText().toString()));
			params.putInt("FontSizeMedium",
					Integer.parseInt(tfFontSizeMedium.getText().toString()));
			params.putInt("FontSizeLarge",
					Integer.parseInt(tfFontSizeLarge.getText().toString()));
			params.putBoolean("FontApplyDimensions", cxFontSizeInSP.isChecked());
			params.putString("SystemProperties", tfSystemProperties.getText().toString());
			params.putBoolean("ShowKeyboard", cxShowKeyboard.isChecked());
			params.putBoolean("VirtualKeyboardFeedback", cxVKFeedback.isChecked());
			params.putBoolean("TouchInput", cxTouchInput.isChecked());

			params.putInt("VirtualKeyboardType", spVKType.getSelectedItemPosition());
			params.putInt("VirtualKeyboardAlpha", sbVKAlpha.getProgress());
			params.putInt("VirtualKeyboardDelay",
					Integer.parseInt(tfVKHideDelay.getText().toString()));
			params.putInt("VirtualKeyboardColorBackground",
					Integer.parseInt(tfVKBack.getText().toString(), 16));
			params.putInt("VirtualKeyboardColorForeground",
					Integer.parseInt(tfVKFore.getText().toString(), 16));
			params.putInt("VirtualKeyboardColorBackgroundSelected",
					Integer.parseInt(tfVKSelBack.getText().toString(), 16));
			params.putInt("VirtualKeyboardColorForegroundSelected",
					Integer.parseInt(tfVKSelFore.getText().toString(), 16));
			params.putInt("VirtualKeyboardColorOutline",
					Integer.parseInt(tfVKOutline.getText().toString(), 16));

			params.apply();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private void applyConfiguration() {
		try {
			int fontSizeSmall = Integer.parseInt(tfFontSizeSmall.getText().toString());
			int fontSizeMedium = Integer.parseInt(tfFontSizeMedium.getText().toString());
			int fontSizeLarge = Integer.parseInt(tfFontSizeLarge.getText().toString());
			boolean fontApplyDimensions = cxFontSizeInSP.isChecked();

			int screenWidth = Integer.parseInt(tfScreenWidth.getText().toString());
			int screenHeight = Integer.parseInt(tfScreenHeight.getText().toString());
			int screenBackgroundColor = Integer.parseInt(tfScreenBack.getText().toString(), 16);
			int screenScaleRatio = sbScaleRatio.getProgress();
			boolean screenScaleToFit = cxScaleToFit.isChecked();
			boolean screenKeepAspectRatio = cxKeepAspectRatio.isChecked();
			boolean screenFilter = cxFilter.isChecked();
			boolean immediateMode = cxImmediate.isChecked();
			boolean touchInput = cxTouchInput.isChecked();
			boolean hwAcceleration = cxHwAcceleration.isChecked();
			boolean parallel = cxParallel.isChecked();
			boolean showFps = cxShowFps.isChecked();
			boolean limitFps = cxLimitFps.isChecked();
			int fpsLimit = Integer.parseInt(tfFpsLimit.getText().toString());

			Font.setSize(Font.SIZE_SMALL, fontSizeSmall);
			Font.setSize(Font.SIZE_MEDIUM, fontSizeMedium);
			Font.setSize(Font.SIZE_LARGE, fontSizeLarge);
			Font.setApplyDimensions(fontApplyDimensions);

			final String[] propLines = tfSystemProperties.getText().toString().split("\n");
			for (String line : propLines) {
				String[] prop = line.split(":", 2);
				if (prop.length == 2) {
					System.setProperty(prop[0], prop[1]);
				}
			}

			SparseIntArray intArray = KeyMapper.getArrayPref(this);
			Canvas.setVirtualSize(screenWidth, screenHeight, screenScaleToFit,
					screenKeepAspectRatio, screenScaleRatio);
			Canvas.setFilterBitmap(screenFilter);
			EventQueue.setImmediate(immediateMode);
			Canvas.setHardwareAcceleration(hwAcceleration, parallel);
			Canvas.setBackgroundColor(screenBackgroundColor);
			Canvas.setKeyMapping(intArray);
			Canvas.setHasTouchInput(touchInput);
			Canvas.setShowFps(showFps);
			Canvas.setLimitFps(limitFps, fpsLimit);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setVirtualKeyboard() {
		int vkType = spVKType.getSelectedItemPosition();
		int vkAlpha = sbVKAlpha.getProgress();
		int vkDelay = Integer.parseInt(tfVKHideDelay.getText().toString());
		int vkColorBackground = Integer.parseInt(tfVKBack.getText().toString(), 16);
		int vkColorForeground = Integer.parseInt(tfVKFore.getText().toString(), 16);
		int vkColorBackgroundSelected = Integer.parseInt(tfVKSelBack.getText().toString(), 16);
		int vkColorForegroundSelected = Integer.parseInt(tfVKSelFore.getText().toString(), 16);
		int vkColorOutline = Integer.parseInt(tfVKOutline.getText().toString(), 16);
		boolean vkFeedback = cxVKFeedback.isChecked();

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

		String shapeStr = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
				.getString("pref_button_shape", "round");
		int shape;
		if (shapeStr.equals("square")) {
			shape = VirtualKeyboard.SQUARE_SHAPE;
		} else {
			shape = VirtualKeyboard.ROUND_SHAPE;
		}
		vk.setButtonShape(shape);

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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.config, menu);
		if (defaultConfig) {
			menu.findItem(R.id.action_start).setVisible(false);
			menu.findItem(R.id.action_clear_data).setVisible(false);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_start:
				startMIDlet();
				break;
			case R.id.action_clear_data:
				showClearDataDialog();
				break;
			case R.id.action_reset_settings:
				params.edit().clear().apply();
				loadParams();
				break;
			case R.id.action_reset_layout:
				keylayoutFile.delete();
				break;
			case R.id.action_load_template:
				LoadTemplateDialogFragment loadTemplateFragment = new LoadTemplateDialogFragment();
				Bundle bundleLoad = new Bundle();
				bundleLoad.putString(CONFIG_PATH_KEY, keylayoutFile.getParent());
				loadTemplateFragment.setArguments(bundleLoad);
				loadTemplateFragment.show(fragmentManager, "load_template");
				break;
			case R.id.action_save_template:
				saveParams();
				SaveTemplateDialogFragment saveTemplateFragment = new SaveTemplateDialogFragment();
				Bundle bundleSave = new Bundle();
				bundleSave.putString(CONFIG_PATH_KEY, keylayoutFile.getParent());
				saveTemplateFragment.setArguments(bundleSave);
				saveTemplateFragment.show(fragmentManager, "save_template");
				break;
			case android.R.id.home:
				finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void showClearDataDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(android.R.string.dialog_alert_title)
				.setMessage(R.string.message_clear_data)
				.setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
					FileUtils.deleteDirectory(dataDir);
				})
				.setNegativeButton(android.R.string.no, null);
		builder.show();
	}

	private void startMIDlet() {
		// Apply configuration to the launching MIDlet
		if (cxShowKeyboard.isChecked()) {
			setVirtualKeyboard();
		} else {
			ContextHolder.setVk(null);
		}
		applyConfiguration();
		Intent i = new Intent(this, MicroActivity.class);
		i.putExtra(MIDLET_PATH_KEY, pathToMidletDir);
		i.putExtra(MIDLET_ORIENTATION_KEY, spOrientation.getSelectedItemPosition());
		startActivity(i);
		finish();
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onClick(View v) {
		String[] presets = null;
		DialogInterface.OnClickListener presetListener = null;

		int color = 0;
		AmbilWarnaDialog.OnAmbilWarnaListener colorListener = null;

		switch (v.getId()) {
			case R.id.cmdScreenSizePresets:
				presets = screenAdapter.toArray(new String[0]);

				presetListener = (dialog, which) -> {
					tfScreenWidth.setText(Integer.toString(screenWidths.get(which)));
					tfScreenHeight.setText(Integer.toString(screenHeights.get(which)));
				};
				break;
			case R.id.cmdSwapSizes:
				String tmp = tfScreenWidth.getText().toString();
				tfScreenWidth.setText(tfScreenHeight.getText().toString());
				tfScreenHeight.setText(tmp);
				break;
			case R.id.cmdFontSizePresets:
				presets = fontAdapter.toArray(new String[0]);

				presetListener = (dialog, which) -> {
					tfFontSizeSmall.setText(Integer.toString(fontSmall.get(which)));
					tfFontSizeMedium.setText(Integer.toString(fontMedium.get(which)));
					tfFontSizeLarge.setText(Integer.toString(fontLarge.get(which)));
				};
				break;
			case R.id.cmdScreenBack:
				color = Integer.parseInt(tfScreenBack.getText().toString(), 16);

				colorListener = new AmbilWarnaDialog.OnAmbilWarnaListener() {
					@Override
					public void onOk(AmbilWarnaDialog dialog, int color) {
						tfScreenBack.setText(Integer.toHexString(color & 0xFFFFFF).toUpperCase());
					}

					@Override
					public void onCancel(AmbilWarnaDialog dialog) {
					}
				};
				break;
			case R.id.cmdVKBack:
				color = Integer.parseInt(tfVKBack.getText().toString(), 16);

				colorListener = new AmbilWarnaDialog.OnAmbilWarnaListener() {
					@Override
					public void onOk(AmbilWarnaDialog dialog, int color) {
						tfVKBack.setText(Integer.toHexString(color & 0xFFFFFF).toUpperCase());
					}

					@Override
					public void onCancel(AmbilWarnaDialog dialog) {
					}
				};
				break;
			case R.id.cmdVKFore:
				color = Integer.parseInt(tfVKFore.getText().toString(), 16);

				colorListener = new AmbilWarnaDialog.OnAmbilWarnaListener() {
					@Override
					public void onOk(AmbilWarnaDialog dialog, int color) {
						tfVKFore.setText(Integer.toHexString(color & 0xFFFFFF).toUpperCase());
					}

					@Override
					public void onCancel(AmbilWarnaDialog dialog) {
					}
				};
				break;
			case R.id.cmdVKSelFore:
				color = Integer.parseInt(tfVKSelFore.getText().toString(), 16);

				colorListener = new AmbilWarnaDialog.OnAmbilWarnaListener() {
					@Override
					public void onOk(AmbilWarnaDialog dialog, int color) {
						tfVKSelFore.setText(Integer.toHexString(color & 0xFFFFFF).toUpperCase());
					}

					@Override
					public void onCancel(AmbilWarnaDialog dialog) {
					}
				};
				break;
			case R.id.cmdVKSelBack:
				color = Integer.parseInt(tfVKSelBack.getText().toString(), 16);

				colorListener = new AmbilWarnaDialog.OnAmbilWarnaListener() {
					@Override
					public void onOk(AmbilWarnaDialog dialog, int color) {
						tfVKSelBack.setText(Integer.toHexString(color & 0xFFFFFF).toUpperCase());
					}

					@Override
					public void onCancel(AmbilWarnaDialog dialog) {
					}
				};
				break;
			case R.id.cmdVKOutline:
				color = Integer.parseInt(tfVKOutline.getText().toString(), 16);

				colorListener = new AmbilWarnaDialog.OnAmbilWarnaListener() {
					@Override
					public void onOk(AmbilWarnaDialog dialog, int color) {
						tfVKOutline.setText(Integer.toHexString(color & 0xFFFFFF).toUpperCase());
					}

					@Override
					public void onCancel(AmbilWarnaDialog dialog) {
					}
				};
				break;
			default:
				return;
		}

		if (presetListener != null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.SIZE_PRESETS));
			builder.setItems(presets, presetListener);

			AlertDialog alert = builder.create();
			alert.show();
		} else if (colorListener != null) {
			AmbilWarnaDialog dialog = new AmbilWarnaDialog(this,
					color | 0xFF000000, colorListener);
			dialog.show();
		}
	}
}
