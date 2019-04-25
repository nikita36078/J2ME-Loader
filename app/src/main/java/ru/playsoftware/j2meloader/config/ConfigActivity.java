/*
 * Copyright 2018 Nikita Shakarun
 * Copyright 2020 Yury Kharchenko
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.microedition.shell.MicroActivity;
import javax.microedition.util.param.SharedPreferencesContainer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.base.BaseActivity;
import ru.playsoftware.j2meloader.settings.KeyMapperActivity;
import ru.playsoftware.j2meloader.util.FileUtils;
import yuku.ambilwarna.AmbilWarnaDialog;

public class ConfigActivity extends BaseActivity implements View.OnClickListener {

	protected ScrollView rootContainer;
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
	protected CheckBox cxForceFullscreen;
	protected CheckBox cxShowFps;
	protected CheckBox cxLimitFps;
	protected EditText tfFpsLimit;

	protected EditText tfFontSizeSmall;
	protected EditText tfFontSizeMedium;
	protected EditText tfFontSizeLarge;
	protected CheckBox cxFontSizeInSP;
	private Spinner spEncoding;
	protected EditText tfSystemProperties;
	protected CheckBox cxShowKeyboard;

	private View vkContainer;
	protected CheckBox cxVKFeedback;
	protected CheckBox cxVKForceOpacity;
	protected CheckBox cxTouchInput;

	protected Spinner spVKType;
	protected Spinner spLayout;
	protected SeekBar sbVKAlpha;
	protected EditText tfVKHideDelay;
	protected EditText tfVKFore;
	protected EditText tfVKBack;
	protected EditText tfVKSelFore;
	protected EditText tfVKSelBack;
	protected EditText tfVKOutline;

	protected ArrayList<String> screenPresets = new ArrayList<>();

	protected ArrayList<Integer> fontSmall;
	protected ArrayList<Integer> fontMedium;
	protected ArrayList<Integer> fontLarge;
	protected ArrayList<String> fontAdapter;

	private File keylayoutFile;
	private File dataDir;
	private SharedPreferencesContainer params;
	private String appName;
	private FragmentManager fragmentManager;
	private boolean defaultConfig;
	private Display display;
	private File configDir;

	public static final String DEFAULT_CONFIG_KEY = "default";
	public static final String CONFIG_PATH_KEY = "configPath";
	public static final String MIDLET_NAME_KEY = "midletName";
	public static final String SHOW_SETTINGS_KEY = "showSettings";
	private ArrayAdapter<String> encodingAdapter;
	private final ArrayList<String> charsets = new ArrayList<>(Charset.availableCharsets().keySet());

	@SuppressLint({"StringFormatMatches", "StringFormatInvalid"})
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_config);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		Intent intent = getIntent();
		display = getWindowManager().getDefaultDisplay();
		fragmentManager = getSupportFragmentManager();
		defaultConfig = intent.getBooleanExtra(DEFAULT_CONFIG_KEY, false);
		boolean showSettings;
		if (defaultConfig) {
			showSettings = true;
			configDir = new File(Config.DEFAULT_CONFIG_DIR);
		} else {
			showSettings = intent.getBooleanExtra(SHOW_SETTINGS_KEY, false);
			appName = intent.getDataString();
			getSupportActionBar().setTitle(appName);
			dataDir = new File(Config.DATA_DIR, appName);
			dataDir.mkdirs();
			configDir = new File(Config.CONFIGS_DIR, appName);
		}
		configDir.mkdirs();
		loadKeylayout();

		params = new SharedPreferencesContainer(configDir);
		boolean loaded = params.load(defaultConfig);

		rootContainer = findViewById(R.id.configRoot);
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
		cxForceFullscreen = findViewById(R.id.cxForceFullscreen);
		cxShowFps = findViewById(R.id.cxShowFps);
		cxLimitFps = findViewById(R.id.cxLimitFps);
		tfFpsLimit = findViewById(R.id.tfFpsLimit);

		tfFontSizeSmall = findViewById(R.id.tfFontSizeSmall);
		tfFontSizeMedium = findViewById(R.id.tfFontSizeMedium);
		tfFontSizeLarge = findViewById(R.id.tfFontSizeLarge);
		cxFontSizeInSP = findViewById(R.id.cxFontSizeInSP);
		spEncoding = findViewById(R.id.spEncoding);
		tfSystemProperties = findViewById(R.id.tfSystemProperties);

		cxTouchInput = findViewById(R.id.cxTouchInput);
		cxShowKeyboard = findViewById(R.id.cxIsShowKeyboard);
		vkContainer = findViewById(R.id.configVkContainer);
		cxVKFeedback = findViewById(R.id.cxVKFeedback);
		cxVKForceOpacity = findViewById(R.id.cxVKForceOpacity);
		cxTouchInput = findViewById(R.id.cxTouchInput);

		spVKType = findViewById(R.id.spVKType);
		spLayout = findViewById(R.id.spLayout);
		sbVKAlpha = findViewById(R.id.sbVKAlpha);
		tfVKHideDelay = findViewById(R.id.tfVKHideDelay);
		tfVKFore = findViewById(R.id.tfVKFore);
		tfVKBack = findViewById(R.id.tfVKBack);
		tfVKSelFore = findViewById(R.id.tfVKSelFore);
		tfVKSelBack = findViewById(R.id.tfVKSelBack);
		tfVKOutline = findViewById(R.id.tfVKOutline);

		fillScreenSizePresets(display.getWidth(), display.getHeight());

		fontSmall = new ArrayList<>();
		fontMedium = new ArrayList<>();
		fontLarge = new ArrayList<>();
		fontAdapter = new ArrayList<>();

		addFontSizePreset("128 x 128", 9, 13, 15);
		addFontSizePreset("128 x 160", 13, 15, 20);
		addFontSizePreset("176 x 220", 15, 18, 22);
		addFontSizePreset("240 x 320", 18, 22, 26);

		initEncoding();

		findViewById(R.id.cmdScreenSizePresets).setOnClickListener(this::showScreenPresets);
		findViewById(R.id.cmdSwapSizes).setOnClickListener(this);
		findViewById(R.id.cmdAddToPreset).setOnClickListener(v -> addResolutionToPresets());
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
				sbScaleRatio.setProgress(parseInt(s.toString()));
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
		vkContainer.setVisibility(cxShowKeyboard.isChecked() ? View.VISIBLE : View.GONE);
		cxShowKeyboard.setOnCheckedChangeListener((b, checked) -> {
			if (checked) {
				vkContainer.setVisibility(View.VISIBLE);
			} else {
				vkContainer.setVisibility(View.GONE);
			}
			View.OnLayoutChangeListener onLayoutChangeListener = new View.OnLayoutChangeListener() {
				@Override
				public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
					v.scrollTo(0, ConfigActivity.this.findViewById(R.id.tvKeyboardHeader).getBottom());
					v.removeOnLayoutChangeListener(this);
				}
			};
			rootContainer.addOnLayoutChangeListener(onLayoutChangeListener);
		});
		tfScreenBack.addTextChangedListener(new ColorTextWatcher(tfScreenBack));
		tfVKFore.addTextChangedListener(new ColorTextWatcher(tfVKFore));
		tfVKBack.addTextChangedListener(new ColorTextWatcher(tfVKBack));
		tfVKSelFore.addTextChangedListener(new ColorTextWatcher(tfVKSelFore));
		tfVKSelBack.addTextChangedListener(new ColorTextWatcher(tfVKSelBack));
		tfVKOutline.addTextChangedListener(new ColorTextWatcher(tfVKOutline));

		if (loaded && !showSettings) {
			startMIDlet();
		}
	}

	private void initEncoding() {
		encodingAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, charsets);
		encodingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spEncoding.setAdapter(encodingAdapter);
		spEncoding.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				String enc = "microedition.encoding: "
						+ parent.getItemAtPosition(position).toString();
				String[] props = tfSystemProperties.getText().toString().split("\\n");
				int propsLength = props.length;
				if (propsLength == 0) {
					tfSystemProperties.setText(enc);
					return;
				}
				int i = propsLength - 1;
				while (i >= 0) {
					if (props[i].startsWith("microedition.encoding")) {
						props[i] = enc;
						break;
					}
					i--;
				}
				if (i < 0) {
					tfSystemProperties.setText(enc);
					return;
				}
				tfSystemProperties.setText(TextUtils.join("\n", props));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
	}

	private void loadKeylayout() {
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
		keylayoutFile = file;
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
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		fillScreenSizePresets(display.getWidth(), display.getHeight());
	}

	private void fillScreenSizePresets(int w, int h) {
		ArrayList<String> screenPresets = this.screenPresets;
		screenPresets.clear();

		screenPresets.add("128 x 128");
		screenPresets.add("128 x 160");
		screenPresets.add("132 x 176");
		screenPresets.add("176 x 220");
		screenPresets.add("240 x 320");
		screenPresets.add("352 x 416");
		screenPresets.add("640 x 360");
		screenPresets.add("800 x 480");

		if (w > h) {
			screenPresets.add(h * 3 / 4 + " x " + h);
			screenPresets.add(h * 4 / 3 + " x " + h);
		} else {
			screenPresets.add(w + " x " + w * 4 / 3);
			screenPresets.add(w + " x " + w * 3 / 4);
		}

		screenPresets.add(w + " x " + h);
		Set<String> preset = PreferenceManager.getDefaultSharedPreferences(this)
				.getStringSet("ResolutionsPreset", null);
		if (preset != null) {
			screenPresets.addAll(preset);
		}
		Collections.sort(screenPresets, (o1, o2) -> {
			int sep1 = o1.indexOf(" x ");
			int sep2 = o2.indexOf(" x ");
			if (sep1 == -1) {
				if (sep2 != -1) return -1;
				else return 0;
			} else if (sep2 == -1) return 1;
			int r = Integer.decode(o1.substring(0, sep1)).compareTo(Integer.decode(o2.substring(0, sep2)));
			if (r != 0) return r;
			return Integer.decode(o1.substring(sep1 + 3)).compareTo(Integer.decode(o2.substring(sep2 + 3)));
		});
		String prev = null;
		for (Iterator<String> iterator = screenPresets.iterator(); iterator.hasNext(); ) {
			String next = iterator.next();
			if (next.equals(prev)) iterator.remove();
			else prev = next;
		}
	}

	private void addFontSizePreset(String title, int small, int medium, int large) {
		fontSmall.add(small);
		fontMedium.add(medium);
		fontLarge.add(large);
		fontAdapter.add(title);
	}

	private int parseInt(String s) {
		return parseInt(s, 10);
	}

	private int parseInt(String s, int radix) {
		int result;
		try {
			result = Integer.parseInt(s, radix);
		} catch (NumberFormatException e) {
			result = 0;
		}
		return result;
	}

	@SuppressLint("SetTextI18n")
	public void loadParams() {
		params.load(defaultConfig);
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
		cxForceFullscreen.setChecked(params.getBoolean("ForceFullscreen", false));
		cxHwAcceleration.setChecked(params.getBoolean("HwAcceleration", false));
		cxShowFps.setChecked(params.getBoolean("ShowFps", false));
		cxLimitFps.setChecked(params.getBoolean("LimitFps", false));

		tfFontSizeSmall.setText(Integer.toString(params.getInt("FontSizeSmall", 18)));
		tfFontSizeMedium.setText(Integer.toString(params.getInt("FontSizeMedium", 22)));
		tfFontSizeLarge.setText(Integer.toString(params.getInt("FontSizeLarge", 26)));
		cxFontSizeInSP.setChecked(params.getBoolean("FontApplyDimensions", false));
		String systemProperties = params.getString("SystemProperties", "");
		tfSystemProperties.setText(systemProperties);
		String encoding = "iso-8859-1";
		String[] split = systemProperties.split("\\n");
		for (int i = split.length - 1; i >= 0; i--) {
			String s = split[i].toLowerCase();
			if (s.startsWith("microedition.encoding") && s.length() > 22) {
				s = s.substring(22).trim();
				if (!s.isEmpty()) {
					encoding = s;
					break;
				}
			}
		}

		int i = 0;
		while (i < charsets.size()) {
			if (charsets.get(i).toLowerCase().equals(encoding)) {
				break;
			}
			i++;
		}
		if (i < charsets.size())
			spEncoding.setSelection(i);
		cxShowKeyboard.setChecked(params.getBoolean(("ShowKeyboard"), true));
		cxVKFeedback.setChecked(params.getBoolean(("VirtualKeyboardFeedback"), false));
		cxVKForceOpacity.setChecked(params.getBoolean(("VirtualKeyboardForceOpacity"), false));
		cxTouchInput.setChecked(params.getBoolean(("TouchInput"), true));
		tfFpsLimit.setText(Integer.toString(params.getInt("FpsLimit", 0)));

		spVKType.setSelection(params.getInt("VirtualKeyboardType", 0));
		spLayout.setSelection(params.getInt("Layout", 0));
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

	private void saveParams() {
		try {
			params.edit();

			String width = tfScreenWidth.getText().toString();
			String height = tfScreenHeight.getText().toString();
			if (width.isEmpty() || width.equals("-")) width = "-1";
			if (height.isEmpty() || height.equals("-")) height = "-1";
			int w = parseInt(width);
			int h = parseInt(height);
			if (w == 0) w = -1;
			if (h == 0) h = -1;
			params.putInt("ScreenWidth", w);
			params.putInt("ScreenHeight", h);
			params.putInt("ScreenBackgroundColor", parseInt(tfScreenBack.getText().toString(), 16));
			params.putInt("ScreenScaleRatio", sbScaleRatio.getProgress());
			params.putInt("Orientation", spOrientation.getSelectedItemPosition());
			params.putBoolean("ScreenScaleToFit", cxScaleToFit.isChecked());
			params.putBoolean("ScreenKeepAspectRatio", cxKeepAspectRatio.isChecked());
			params.putBoolean("ScreenFilter", cxFilter.isChecked());
			params.putBoolean("ImmediateMode", cxImmediate.isChecked());
			params.putBoolean("HwAcceleration", cxHwAcceleration.isChecked());
			params.putBoolean("ParallelRedrawScreen", cxParallel.isChecked());
			params.putBoolean("ForceFullscreen", cxForceFullscreen.isChecked());
			params.putBoolean("ShowFps", cxShowFps.isChecked());
			params.putBoolean("LimitFps", cxLimitFps.isChecked());
			params.putInt("FpsLimit", parseInt(tfFpsLimit.getText().toString()));

			params.putInt("FontSizeSmall", parseInt(tfFontSizeSmall.getText().toString()));
			params.putInt("FontSizeMedium", parseInt(tfFontSizeMedium.getText().toString()));
			params.putInt("FontSizeLarge", parseInt(tfFontSizeLarge.getText().toString()));
			params.putBoolean("FontApplyDimensions", cxFontSizeInSP.isChecked());
			params.putString("SystemProperties", tfSystemProperties.getText().toString());
			params.putBoolean("ShowKeyboard", cxShowKeyboard.isChecked());
			params.putBoolean("VirtualKeyboardFeedback", cxVKFeedback.isChecked());
			params.putBoolean("VirtualKeyboardForceOpacity", cxVKForceOpacity.isChecked());
			params.putBoolean("TouchInput", cxTouchInput.isChecked());

			params.putInt("VirtualKeyboardType", spVKType.getSelectedItemPosition());
			params.putInt("Layout", spLayout.getSelectedItemPosition());
			params.putInt("VirtualKeyboardAlpha", sbVKAlpha.getProgress());
			params.putInt("VirtualKeyboardDelay", parseInt(tfVKHideDelay.getText().toString()));
			params.putInt("VirtualKeyboardColorBackground",
					parseInt(tfVKBack.getText().toString(), 16));
			params.putInt("VirtualKeyboardColorForeground",
					parseInt(tfVKFore.getText().toString(), 16));
			params.putInt("VirtualKeyboardColorBackgroundSelected",
					parseInt(tfVKSelBack.getText().toString(), 16));
			params.putInt("VirtualKeyboardColorForegroundSelected",
					parseInt(tfVKSelFore.getText().toString(), 16));
			params.putInt("VirtualKeyboardColorOutline",
					parseInt(tfVKOutline.getText().toString(), 16));

			params.apply();
		} catch (Throwable t) {
			t.printStackTrace();
		}
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
				//noinspection ResultOfMethodCallIgnored
				keylayoutFile.delete();
				loadKeylayout();
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
			case R.id.action_map_keys:
				Intent i = new Intent(this, KeyMapperActivity.class);
				i.putExtra(MIDLET_NAME_KEY, appName);
				i.putExtra(DEFAULT_CONFIG_KEY, defaultConfig);
				startActivity(i);
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
				.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
					FileUtils.deleteDirectory(dataDir);
					//noinspection ResultOfMethodCallIgnored
					dataDir.mkdirs();
				})
				.setNegativeButton(android.R.string.cancel, null);
		builder.show();
	}

	private void startMIDlet() {
		Intent i = new Intent(this, MicroActivity.class);
		i.putExtra(MIDLET_NAME_KEY, appName);
		startActivity(i);
		finish();
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.cmdSwapSizes:
				String tmp = tfScreenWidth.getText().toString();
				tfScreenWidth.setText(tfScreenHeight.getText().toString());
				tfScreenHeight.setText(tmp);
				break;
			case R.id.cmdFontSizePresets:
				new AlertDialog.Builder(this)
						.setTitle(getString(R.string.SIZE_PRESETS))
						.setItems(fontAdapter.toArray(new String[0]),
								(dialog, which) -> {
					tfFontSizeSmall.setText(Integer.toString(fontSmall.get(which)));
					tfFontSizeMedium.setText(Integer.toString(fontMedium.get(which)));
					tfFontSizeLarge.setText(Integer.toString(fontLarge.get(which)));
								})
						.show();
				break;
			case R.id.cmdScreenBack:
				showColorPicker(tfScreenBack);
				break;
			case R.id.cmdVKBack:
				showColorPicker(tfVKBack);
				break;
			case R.id.cmdVKFore:
				showColorPicker(tfVKFore);
				break;
			case R.id.cmdVKSelFore:
				showColorPicker(tfVKSelFore);
				break;
			case R.id.cmdVKSelBack:
				showColorPicker(tfVKSelBack);
				break;
			case R.id.cmdVKOutline:
				showColorPicker(tfVKOutline);
				break;
			default:
		}
	}

	private void showScreenPresets(View v) {
		PopupMenu popup = new PopupMenu(this, v);
		Menu menu = popup.getMenu();
		for (String preset : screenPresets) {
			menu.add(preset);
		}
		popup.setOnMenuItemClickListener(item -> {
			String string = item.getTitle().toString();
			int separator = string.indexOf(" x ");
			tfScreenWidth.setText(string.substring(0, separator));
			tfScreenHeight.setText(string.substring(separator + 3));
			return true;
		});
		popup.show();
	}

	private void showColorPicker(EditText et) {
		AmbilWarnaDialog.OnAmbilWarnaListener colorListener = new AmbilWarnaDialog.OnAmbilWarnaListener() {
					@Override
					public void onOk(AmbilWarnaDialog dialog, int color) {
						et.setText(Integer.toHexString(color & 0xFFFFFF).toUpperCase());
					}

					@Override
					public void onCancel(AmbilWarnaDialog dialog) {
					}
		};
		int color = parseInt(et.getText().toString().trim(), 16);
		new AmbilWarnaDialog(this, color | 0xFF000000, colorListener).show();
	}

	private void addResolutionToPresets() {
		String width = tfScreenWidth.getText().toString();
		String height = tfScreenHeight.getText().toString();
		if (width.isEmpty()) width = "-1";
		if (height.isEmpty()) height = "-1";
		int w = parseInt(width);
		int h = parseInt(height);
		if (w <= 0 || h <= 0) {
			Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
			return;
		}
		String preset = width + " x " + height;
		if (screenPresets.contains(preset)) {
			Toast.makeText(this, R.string.not_saved_exists, Toast.LENGTH_SHORT).show();
			return;
		}

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		Set<String> set = preferences.getStringSet("ResolutionsPreset", null);
		if (set == null) {
			set = new HashSet<>(1);
		}
		if (set.add(preset)) {
			preferences.edit().putStringSet("ResolutionsPreset", set).apply();
			screenPresets.add(preset);
			Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, R.string.not_saved_exists, Toast.LENGTH_SHORT).show();
		}
	}

	private static class ColorTextWatcher implements TextWatcher {
		private final EditText editText;
		private final ColorDrawable drawable;

		ColorTextWatcher(EditText editText) {
			this.editText = editText;
			int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32,
					editText.getResources().getDisplayMetrics());
			ColorDrawable colorDrawable = new ColorDrawable();
			colorDrawable.setBounds(0, 0, size, size);
			editText.setCompoundDrawablesRelative(null, null, colorDrawable, null);
			drawable = colorDrawable;
			editText.setFilters(new InputFilter[]{this::filter});
			editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		}

		private CharSequence filter(CharSequence src, int ss, int se, Spanned dst, int ds, int de) {
			StringBuilder sb = new StringBuilder(se - ss);
			for (int i = ss; i < se; i++) {
				char c = src.charAt(i);
				if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F')) {
					sb.append(c);
				} else if (c  >= 'a' && c <= 'f') {
					sb.append((char)(c - 32));
				}
			}
			return sb;
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			if (s.length() > 6) {
				if (start >= 6) editText.getText().delete(6, s.length());
				else {
					int st = start + count;
					int end = st + (before == 0 ? count : before);
					editText.getText().delete(st, Math.min(end, s.length()));
				}
			}
		}

		@Override
		public void afterTextChanged(Editable s) {
			try {
				int color = Integer.parseInt(s.toString(), 16);
				drawable.setColor(color | Color.BLACK);
			} catch (NumberFormatException e) {
				drawable.setColor(Color.BLACK);
			}
		}
	}
}
