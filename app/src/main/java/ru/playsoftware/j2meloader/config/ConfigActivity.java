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

import static ru.playsoftware.j2meloader.util.Constants.ACTION_EDIT;
import static ru.playsoftware.j2meloader.util.Constants.ACTION_EDIT_PROFILE;
import static ru.playsoftware.j2meloader.util.Constants.KEY_MIDLET_NAME;
import static ru.playsoftware.j2meloader.util.Constants.PREF_DEFAULT_PROFILE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.microedition.shell.MicroActivity;
import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.databinding.ActivityConfigBinding;
import ru.playsoftware.j2meloader.settings.KeyMapperActivity;
import ru.playsoftware.j2meloader.util.FileUtils;
import yuku.ambilwarna.AmbilWarnaDialog;

public class ConfigActivity extends AppCompatActivity implements View.OnClickListener, ShaderTuneDialog.Callback {
	private static final String TAG = ConfigActivity.class.getSimpleName();

	protected ArrayList<String> screenPresets = new ArrayList<>();
	protected Set<String> customPresets = new HashSet<>();
	private AlertDialog presetsDialog;
	private ResolutionAdapter presetsAdapter;

	protected ArrayList<int[]> fontPresetValues = new ArrayList<>();
	protected ArrayList<String> fontPresetTitles = new ArrayList<>();

	private File keylayoutFile;
	private File dataDir;
	private ProfileModel params;
	private FragmentManager fragmentManager;
	private boolean isProfile;
	private Display display;
	private File configDir;
	private String defProfile;
	private ArrayAdapter<ShaderInfo> spShaderAdapter;
	private String workDir;
	private boolean needShow;
	private ArrayAdapter<String> spSoundBankAdapter;

	private ActivityConfigBinding binding;

	@SuppressLint({"StringFormatMatches", "StringFormatInvalid"})
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		String action = intent.getAction();
		isProfile = ACTION_EDIT_PROFILE.equals(action);
		needShow = isProfile || ACTION_EDIT.equals(action);
		String path = intent.getDataString();
		if (path == null) {
			needShow = false;
			finish();
			return;
		}
		if (isProfile) {
			setResult(RESULT_OK, new Intent().setData(intent.getData()));
			configDir = new File(Config.getProfilesDir(), path);
			workDir = Config.getEmulatorDir();
			setTitle(path);
		} else {
			setTitle(intent.getStringExtra(KEY_MIDLET_NAME));
			File appDir = new File(path);
			File convertedDir = appDir.getParentFile();
			if (!appDir.isDirectory() || convertedDir == null
					|| (workDir = convertedDir.getParent()) == null) {
				needShow = false;
				String storageName = "";
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					StorageManager sm = (StorageManager) getSystemService(STORAGE_SERVICE);
					if (sm != null) {
						StorageVolume storageVolume = sm.getStorageVolume(appDir);
						if (storageVolume != null) {
							String desc = storageVolume.getDescription(this);
							if (desc != null) {
								storageName = "\"" + desc + "\" ";
							}
						}
					}
				}
				new AlertDialog.Builder(this)
						.setTitle(R.string.error)
						.setMessage(getString(R.string.err_missing_app, storageName))
						.setPositiveButton(R.string.exit, (d, w) -> finish())
						.setCancelable(false)
						.show();
				return;
			}
			dataDir = new File(workDir + Config.MIDLET_DATA_DIR + appDir.getName());
			dataDir.mkdirs();
			configDir = new File(workDir + Config.MIDLET_CONFIGS_DIR + appDir.getName());
		}
		configDir.mkdirs();

		defProfile = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
				.getString(PREF_DEFAULT_PROFILE, null);
		loadConfig();
		if (!params.isNew && !needShow) {
			startMIDlet();
			return;
		}
		loadKeyLayout();
		binding = ActivityConfigBinding.inflate(getLayoutInflater());
		View view = binding.getRoot();
		setContentView(view);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		display = getWindowManager().getDefaultDisplay();
		fragmentManager = getSupportFragmentManager();

		fillScreenSizePresets(display.getWidth(), display.getHeight());

		addFontSizePreset("128 x 128", 9, 13, 15);
		addFontSizePreset("128 x 160", 13, 15, 20);
		addFontSizePreset("176 x 220", 15, 18, 22);
		addFontSizePreset("240 x 320", 18, 22, 26);
		addFontSizePreset("360 x 640", 22, 26, 30);

		binding.keepAspectRatioToggle.setOnCheckedChangeListener(this::onLockAspectChanged);
		binding.showScreenSizePresets.setOnClickListener(this::showScreenPresets);
		binding.swapScreenSides.setOnClickListener(this);
		binding.addScreenSizeToPresets.setOnClickListener(v -> addResolutionToPresets());
		binding.showFontSizePresets.setOnClickListener(this);
		binding.selectScreenBackgroundColor.setOnClickListener(this);
		binding.showKeyMappings.setOnClickListener(this);
		binding.updateKeyboardNotPressedButtonBackgroundColor.setOnClickListener(this);
		binding.updateKeyboardNotPressedButtonLabelColor.setOnClickListener(this);
		binding.updateKeyboardPressedButtonBackgroundColor.setOnClickListener(this);
		binding.updateKeyboardPressedButtonLabelColor.setOnClickListener(this);
		binding.updateKeyboardOutlineColor.setOnClickListener(this);
		binding.updateEncoding.setOnClickListener(this::showCharsetPicker);
		binding.tuneSelectedShader.setOnClickListener(this::showShaderSettings);
		binding.scaleRatio.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				int length = s.length();
				if (length > 4) {
					if (start >= 4) {
						binding.scaleRatio.getText().delete(4, length);
					} else {
						int st = start + count;
						int end = st + (before == 0 ? count : before);
						binding.scaleRatio.getText().delete(st, Math.min(end, length));
					}
				}
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (s.length() == 0) return;
				try {
					int progress = Integer.parseInt(s.toString());
					if (progress > 1000) {
						s.replace(0, s.length(), "1000");
					}
				} catch (NumberFormatException e) {
					s.clear();
				}
			}
		});
		binding.graphicalModeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				switch (position) {
					case 0:
					case 3:
						binding.parallelScreenRedrawingToggle.setVisibility(View.VISIBLE);
						binding.shaderRoot.setVisibility(View.GONE);
						break;
					case 1:
						binding.parallelScreenRedrawingToggle.setVisibility(View.GONE);
						initShaderSpinner();
						break;
					case 2:
						binding.parallelScreenRedrawingToggle.setVisibility(View.GONE);
						binding.shaderRoot.setVisibility(View.GONE);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		binding.showVirtualKeyboardToggle.setOnClickListener((b) -> {
			View.OnLayoutChangeListener onLayoutChangeListener = new View.OnLayoutChangeListener() {
				@Override
				public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
					View focus = binding.wholeConfigRoot.findFocus();
					if (focus != null) focus.clearFocus();
					v.scrollTo(0, binding.inputConfigRoot.getTop());
					v.removeOnLayoutChangeListener(this);
				}
			};
			binding.wholeConfigRoot.addOnLayoutChangeListener(onLayoutChangeListener);
			binding.virtualKeyboardConfigGroup.setVisibility(binding.showVirtualKeyboardToggle.isChecked() ? View.VISIBLE : View.GONE);
		});
		binding.screenBackgroundHexColor.addTextChangedListener(
				new ColorTextWatcher(binding.screenBackgroundHexColor));
		binding.keyboardNotPressedButtonLabelColorHex.addTextChangedListener(
				new ColorTextWatcher(binding.keyboardNotPressedButtonLabelColorHex));
		binding.keyboardNotPressedButtonBackgroundColorHex.addTextChangedListener(
				new ColorTextWatcher(binding.keyboardNotPressedButtonBackgroundColorHex));
		binding.keyboardPressedButtonLabelColorHex.addTextChangedListener(
				new ColorTextWatcher(binding.keyboardPressedButtonLabelColorHex));
		binding.keyboardPressedButtonBackgroundColorHex.addTextChangedListener(
				new ColorTextWatcher(binding.keyboardPressedButtonBackgroundColorHex));
		binding.keyboardOutlineColorHex.addTextChangedListener(
				new ColorTextWatcher(binding.keyboardOutlineColorHex));
        initSoundBankSpinner();
    }

	private void initSoundBankSpinner() {
		File dir = new File(workDir + Config.SOUNDBANKS_DIR);
		if (!dir.exists()) {
			//noinspection ResultOfMethodCallIgnored
			dir.mkdirs();
		}
		spSoundBankAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
		spSoundBankAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		binding.soundbankSelector.setAdapter(spSoundBankAdapter);
		spSoundBankAdapter.add(getString(R.string.default_label, "Android"));
		String[] files = dir.list((d, n) -> new File(d, n).isFile());
		if (files != null) {
			Arrays.sort(files, (o1, o2) -> {
				int res = o1.toLowerCase().compareTo(o2.toLowerCase());
				if (res != 0) {
					return res;
				}
				return o1.compareTo(o2);
			});
			spSoundBankAdapter.addAll(files);
		}
		spSoundBankAdapter.notifyDataSetChanged();
		int position = spSoundBankAdapter.getPosition(params.soundBank);
		binding.soundbankSelector.setSelection(Math.max(position, 0));
	}

	private void onLockAspectChanged(CompoundButton cb, boolean isChecked) {
		if (isChecked) {
			float w;
			try {
				w = Integer.parseInt(binding.screenWidth.getText().toString());
			} catch (Exception ignored) {
				w = 0;
			}
			if (w <= 0) {
				cb.setChecked(false);
				return;
			}
			float h;
			try {
				h = Integer.parseInt(binding.screenHeight.getText().toString());
			} catch (Exception ignored) {
				h = 0;
			}
			if (h <= 0) {
				cb.setChecked(false);
				return;
			}
			float finalW = w;
			float finalH = h;
			binding.screenWidth.setOnFocusChangeListener(new ResolutionAutoFill(
					binding.screenWidth, binding.screenHeight, finalH / finalW));
			binding.screenHeight.setOnFocusChangeListener(new ResolutionAutoFill(
					binding.screenHeight, binding.screenWidth, finalW / finalH));

		} else {
			View.OnFocusChangeListener listener = binding.screenWidth.getOnFocusChangeListener();
			if (listener != null) {
				listener.onFocusChange(binding.screenWidth, false);
				binding.screenWidth.setOnFocusChangeListener(null);
			}
			listener = binding.screenHeight.getOnFocusChangeListener();
			if (listener != null) {
				listener.onFocusChange(binding.screenHeight, false);
				binding.screenHeight.setOnFocusChangeListener(null);
			}
		}
	}

	void loadConfig() {
		params = ProfilesManager.loadConfig(configDir);
		if (params == null && defProfile != null) {
			FileUtils.copyFiles(new File(Config.getProfilesDir(), defProfile), configDir, null);
			params = ProfilesManager.loadConfig(configDir);
		}
		if (params == null) {
			params = new ProfileModel(configDir);
		}
	}

	private void showShaderSettings(View v) {
		ShaderInfo shader = (ShaderInfo) binding.shaderSelector.getSelectedItem();
		params.shader = shader;
		ShaderTuneDialog.newInstance(shader).show(getSupportFragmentManager(), "ShaderTuning");
	}

	private void initShaderSpinner() {
		if (spShaderAdapter != null) {
			binding.shaderRoot.setVisibility(View.VISIBLE);
			return;
		}
		File dir = new File(workDir + Config.SHADERS_DIR);
		if (!dir.exists()) {
			//noinspection ResultOfMethodCallIgnored
			dir.mkdirs();
		}
		ArrayList<ShaderInfo> list = new ArrayList<>();
		spShaderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
		spShaderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		binding.shaderSelector.setAdapter(spShaderAdapter);
		File[] files = dir.listFiles((f) -> f.isFile() && f.getName().toLowerCase().endsWith(".ini"));
		if (files != null) {
			for (File file : files) {
				String text = FileUtils.getText(file.getAbsolutePath());
				String[] split = text.split("[\\n\\r]+");
				ShaderInfo info = null;
				for (String line : split) {
					if (line.startsWith("[")) {
						if (info != null && info.fragment != null && info.vertex != null) {
							list.add(info);
						}
						info = new ShaderInfo(line.replaceAll("[\\[\\]]", ""), "unknown");
					} else if (info != null) {
						try {
							info.set(line);
						} catch (Exception e) {
							Log.e(TAG, "initShaderSpinner: ", e);
						}
					}
				}
				if (info != null && info.fragment != null && info.vertex != null) {
					list.add(info);
				}
			}
			Collections.sort(list);
		}
		list.add(0, new ShaderInfo(getString(R.string.identity_filter), "woesss"));
		spShaderAdapter.notifyDataSetChanged();
		ShaderInfo selected = params.shader;
		if (selected != null) {
			int position = list.indexOf(selected);
			if (position > 0) {
				list.get(position).values = selected.values;
				binding.shaderSelector.setSelection(position);
			}
		}
		binding.shaderRoot.setVisibility(View.VISIBLE);
		binding.shaderSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				ShaderInfo item = (ShaderInfo) parent.getItemAtPosition(position);
				ShaderInfo.Setting[] settings = item.settings;
				float[] values = item.values;
				if (values == null) {
					for (int i = 0; i < 4; i++) {
						if (settings[i] != null) {
							if (values == null) {
								values = new float[4];
							}
							values[i] = settings[i].def;
						}
					}
				}
				if (values == null) {
					binding.tuneSelectedShader.setVisibility(View.GONE);
				} else {
					item.values = values;
					binding.tuneSelectedShader.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	private void showCharsetPicker(View v) {
		String[] charsets = Charset.availableCharsets().keySet().toArray(new String[0]);
		new AlertDialog.Builder(this).setItems(charsets, (d, w) -> {
			String text = binding.systemProperties.getText().toString();
			String key = "microedition.encoding:";
			int idx = text.lastIndexOf(key);
			if (idx != -1) {
				int nl = text.indexOf('\n', idx);
				text = text.substring(0, idx + key.length()) + " " + charsets[w] + (nl == -1 ? "\n" : text.substring(nl));
                binding.systemProperties.setText(text);
				return;
			}

			if (!text.endsWith("\n")) {
                binding.systemProperties.append("\n");
			}
            binding.systemProperties.append(key);
            binding.systemProperties.append(" ");
            binding.systemProperties.append(charsets[w]);
            binding.systemProperties.append("\n");
		}).setTitle(R.string.pref_encoding_title).show();
	}

	private void loadKeyLayout() {
		File file = new File(configDir, Config.MIDLET_KEY_LAYOUT_FILE);
		keylayoutFile = file;
		if (isProfile || file.exists()) {
			return;
		}
		if (defProfile == null) {
			return;
		}
		File defaultKeyLayoutFile = new File(Config.getProfilesDir() + defProfile, Config.MIDLET_KEY_LAYOUT_FILE);
		if (!defaultKeyLayoutFile.exists()) {
			return;
		}
		try {
			FileUtils.copyFileUsingChannel(defaultKeyLayoutFile, file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onPause() {
		if (needShow && configDir != null) {
			saveParams();
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (needShow) {
			loadParams(true);
		}
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		fillScreenSizePresets(display.getWidth(), display.getHeight());
	}

	private void fillScreenSizePresets(int w, int h) {
		ArrayList<String> screenPresets = this.screenPresets;
		screenPresets.clear();
		customPresets.clear();

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
			customPresets.addAll(preset);
		}
		sortPresets();
		String prev = null;
		for (Iterator<String> iterator = screenPresets.iterator(); iterator.hasNext(); ) {
			String next = iterator.next();
			if (next.equals(prev)) iterator.remove();
			else prev = next;
		}
	}

	private void sortPresets() {
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
	}

	private void addFontSizePreset(String title, int small, int medium, int large) {
		fontPresetValues.add(new int[]{small, medium, large});
		fontPresetTitles.add(title);
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
	public void loadParams(boolean reloadFromFile) {
		if (reloadFromFile) {
			loadConfig();
		}
		int screenWidth = params.screenWidth;
		if (screenWidth != 0) {
			binding.screenWidth.setText(Integer.toString(screenWidth));
		}
		int screenHeight = params.screenHeight;
		if (screenHeight != 0) {
			binding.screenHeight.setText(Integer.toString(screenHeight));
		}
		binding.screenBackgroundHexColor.setText(String.format("%06X", params.screenBackgroundColor));
		binding.scaleRatio.setText(Integer.toString(params.screenScaleRatio));
		binding.screenOrientationSelector.setSelection(params.orientation);
		binding.scaleTypeSelector.setSelection(params.screenScaleType);
		binding.screenGravitySelector.setSelection(params.screenGravity);
		binding.filteringToggle.setChecked(params.screenFilter);
		binding.immediateProcessingToggle.setChecked(params.immediateMode);
		binding.parallelScreenRedrawingToggle.setChecked(params.parallelRedrawScreen);
		binding.forceFullscreenToggle.setChecked(params.forceFullscreen);
		binding.graphicalModeSelector.setSelection(params.graphicsMode);
		binding.showFpsToggle.setChecked(params.showFps);
		binding.shaderSelector.setSelection(0);
		if (spShaderAdapter != null) {
			ShaderInfo shader = params.shader;
			int position = shader == null ? -1 : spShaderAdapter.getPosition(shader);
			if (position > 0) {
				spShaderAdapter.getItem(position).values = shader.values;
				binding.shaderSelector.setSelection(position);
			}
		}

		binding.fontSizeSmall.setText(Integer.toString(params.fontSizeSmall));
		binding.fontSizeMedium.setText(Integer.toString(params.fontSizeMedium));
		binding.fontSizeLarge.setText(Integer.toString(params.fontSizeLarge));
		binding.showFontSizesInScaledPixelsToggle.setChecked(params.fontApplyDimensions);
		binding.enableAntiAliasingToggle.setChecked(params.fontAA);
		boolean showVk = params.showKeyboard;
		binding.showVirtualKeyboardToggle.setChecked(showVk);
		binding.virtualKeyboardConfigGroup.setVisibility(showVk ? View.VISIBLE : View.GONE);
		binding.enableHapticFeedbackToggle.setChecked(params.vkFeedback);
		binding.forceOpacityForOffscreenKeysToggle.setChecked(params.vkForceOpacity);
		binding.enableTouchInputToggle.setChecked(params.touchInput);
		int fpsLimit = params.fpsLimit;
		binding.fpsLimit.setText(fpsLimit > 0 ? Integer.toString(fpsLimit) : "");

		binding.buttonsLayoutSelector.setSelection(params.keyCodesLayout);
		binding.buttonShapeSelector.setSelection(params.vkButtonShape);
		binding.changeOpacitySeekbar.setProgress(params.vkAlpha);
		int vkHideDelay = params.vkHideDelay;
		binding.virtualKeyboardHideDelay.setText(vkHideDelay > 0 ? Integer.toString(vkHideDelay) : "");

		binding.keyboardNotPressedButtonBackgroundColorHex.setText(String.format("%06X", params.vkBgColor));
		binding.keyboardNotPressedButtonLabelColorHex.setText(String.format("%06X", params.vkFgColor));
		binding.keyboardPressedButtonBackgroundColorHex.setText(String.format("%06X", params.vkBgColorSelected));
		binding.keyboardPressedButtonLabelColorHex.setText(String.format("%06X", params.vkFgColorSelected));
		binding.keyboardOutlineColorHex.setText(String.format("%06X", params.vkOutlineColor));

		String systemProperties = params.systemProperties;
		if (systemProperties == null) {
			systemProperties = ContextHolder.getAssetAsString("defaults/system.props");
		}
		binding.systemProperties.setText(getSystemProperties(systemProperties));
	}

	private void saveParams() {
		try {
			int width = parseInt(binding.screenWidth.getText().toString());
			params.screenWidth = width;
			int height = parseInt(binding.screenHeight.getText().toString());
			params.screenHeight = height;
			try {
				params.screenBackgroundColor = Integer.parseInt(
						binding.screenBackgroundHexColor.getText().toString(), 16);
			} catch (NumberFormatException ignored) {
			}
			try {
				params.screenScaleRatio = Integer.parseInt(binding.scaleRatio.getText().toString());
			} catch (NumberFormatException e) {
				params.screenScaleRatio = 100;
			}
			params.orientation = binding.screenOrientationSelector.getSelectedItemPosition();
			params.screenGravity = binding.screenGravitySelector.getSelectedItemPosition();
			params.screenScaleType = binding.scaleTypeSelector.getSelectedItemPosition();
			params.screenFilter = binding.filteringToggle.isChecked();
			params.immediateMode = binding.immediateProcessingToggle.isChecked();
			int mode = binding.graphicalModeSelector.getSelectedItemPosition();
			params.graphicsMode = mode;
			if (mode == 1) {
				if (binding.shaderSelector.getSelectedItemPosition() == 0)
					params.shader = null;
				else
					params.shader = (ShaderInfo) binding.shaderSelector.getSelectedItem();
			}
			params.parallelRedrawScreen = binding.parallelScreenRedrawingToggle.isChecked();
			params.forceFullscreen = binding.forceFullscreenToggle.isChecked();
			params.showFps = binding.showFpsToggle.isChecked();
			params.fpsLimit = parseInt(binding.fpsLimit.getText().toString());

			try {
				params.fontSizeSmall = Integer.parseInt(binding.fontSizeSmall.getText().toString());
			} catch (NumberFormatException e) {
				params.fontSizeSmall = 0;
			}
			try {
				params.fontSizeMedium = Integer.parseInt(binding.fontSizeMedium.getText().toString());
			} catch (NumberFormatException e) {
				params.fontSizeMedium = 0;
			}
			try {
				params.fontSizeLarge = Integer.parseInt(binding.fontSizeLarge.getText().toString());
			} catch (NumberFormatException e) {
				params.fontSizeLarge = 0;
			}
			params.fontApplyDimensions = binding.showFontSizesInScaledPixelsToggle.isChecked();
			params.fontAA = binding.enableAntiAliasingToggle.isChecked();
			params.showKeyboard = binding.showVirtualKeyboardToggle.isChecked();
			params.vkFeedback = binding.enableHapticFeedbackToggle.isChecked();
			params.vkForceOpacity = binding.forceOpacityForOffscreenKeysToggle.isChecked();
			params.touchInput = binding.enableTouchInputToggle.isChecked();

			params.keyCodesLayout = binding.buttonsLayoutSelector.getSelectedItemPosition();
			params.vkButtonShape = binding.buttonShapeSelector.getSelectedItemPosition();
			params.vkAlpha = binding.changeOpacitySeekbar.getProgress();
			params.vkHideDelay = parseInt(binding.virtualKeyboardHideDelay.getText().toString());
			try {
				params.vkBgColor = Integer.parseInt(
						binding.keyboardNotPressedButtonBackgroundColorHex.getText().toString(), 16);
			} catch (Exception ignored) {
			}
			try {
				params.vkFgColor = Integer.parseInt(
						binding.keyboardNotPressedButtonLabelColorHex.getText().toString(), 16);
			} catch (Exception ignored) {
			}
			try {
				params.vkBgColorSelected = Integer.parseInt(
						binding.keyboardPressedButtonBackgroundColorHex.getText().toString(), 16);
			} catch (Exception ignored) {
			}
			try {
				params.vkFgColorSelected = Integer.parseInt(
						binding.keyboardPressedButtonLabelColorHex.getText().toString(), 16);
			} catch (Exception ignored) {
			}
			try {
				params.vkOutlineColor = Integer.parseInt(
						binding.keyboardOutlineColorHex.getText().toString(), 16);
			} catch (Exception ignored) {
			}
			params.soundBank = binding.soundbankSelector.getSelectedItemPosition() > 0 ? (String)
					binding.soundbankSelector.getSelectedItem() : null;
			params.systemProperties = getSystemProperties(binding.systemProperties.getText().toString());

			ProfilesManager.saveConfig(params);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@NonNull
	private String getSystemProperties(String text) {
		String[] lines = text.split("[\\r\\n]+");
		ArrayList<String> list = new ArrayList<>();
		Set<String> keys = new HashSet<>();
		for (int i = lines.length - 1; i >= 0; i--) {
			String line = lines[i];
			int colon = line.indexOf(':');
			if (colon != -1 && keys.add(line.substring(0, colon).trim())) {
				list.add(line);
			}
		}
		Collections.sort(list);
		StringBuilder sb = new StringBuilder();
		for (String string : list) {
			sb.append(string);
			sb.append("\n");
		}
		return sb.toString();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.config, menu);
		if (isProfile) {
			menu.findItem(R.id.action_start).setVisible(false);
			menu.findItem(R.id.action_clear_data).setVisible(false);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.action_start) {
			startMIDlet();
		} else if (itemId == R.id.action_clear_data) {
			showClearDataDialog();
		} else if (itemId == R.id.action_reset_settings) {
			params = new ProfileModel(configDir);
			loadParams(false);
		} else if (itemId == R.id.action_reset_layout) {//noinspection ResultOfMethodCallIgnored
			keylayoutFile.delete();
			loadKeyLayout();
		} else if (itemId == R.id.action_load_profile) {
			LoadProfileDialog.newInstance(keylayoutFile.getParent())
					.show(fragmentManager, "load_profile");
		} else if (itemId == R.id.action_save_profile) {
			saveParams();
			SaveProfileDialog.getInstance(keylayoutFile.getParent())
					.show(fragmentManager, "save_profile");
		} else if (itemId == android.R.id.home) {
			finish();
		}
		return super.onOptionsItemSelected(item);
	}

	private void showClearDataDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(android.R.string.dialog_alert_title)
				.setMessage(R.string.message_clear_data)
				.setPositiveButton(android.R.string.ok, (d, w) -> FileUtils.clearDirectory(dataDir))
				.setNegativeButton(android.R.string.cancel, null);
		builder.show();
	}

	private void startMIDlet() {
		Intent i = new Intent(this, MicroActivity.class);
		i.setData(getIntent().getData());
		i.putExtra(KEY_MIDLET_NAME, getIntent().getStringExtra(KEY_MIDLET_NAME));
		startActivity(i);
		finish();
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.swap_screen_sides) {
			String tmp = binding.screenWidth.getText().toString();
			binding.screenWidth.setText(binding.screenHeight.getText().toString());
			binding.screenHeight.setText(tmp);
		} else if (id == R.id.show_font_size_presets) {
			new AlertDialog.Builder(this)
					.setTitle(getString(R.string.SIZE_PRESETS))
					.setItems(fontPresetTitles.toArray(new String[0]),
							(dialog, which) -> {
								int[] values = fontPresetValues.get(which);
								binding.fontSizeSmall.setText(Integer.toString(values[0]));
								binding.fontSizeMedium.setText(Integer.toString(values[1]));
								binding.fontSizeLarge.setText(Integer.toString(values[2]));
							})
					.show();
		} else if (id == R.id.select_screen_background_color) {
			showColorPicker(binding.screenBackgroundHexColor);
		} else if (id == R.id.update_keyboard_not_pressed_button_background_color) {
			showColorPicker(binding.keyboardNotPressedButtonBackgroundColorHex);
		} else if (id == R.id.update_keyboard_not_pressed_button_label_color) {
			showColorPicker(binding.keyboardNotPressedButtonLabelColorHex);
		} else if (id == R.id.update_keyboard_pressed_button_label_color) {
			showColorPicker(binding.keyboardPressedButtonLabelColorHex);
		} else if (id == R.id.update_keyboard_pressed_button_background_color) {
			showColorPicker(binding.keyboardPressedButtonBackgroundColorHex);
		} else if (id == R.id.update_keyboard_outline_color) {
			showColorPicker(binding.keyboardOutlineColorHex);
		} else if (id == R.id.show_key_mappings) {
			Intent i = new Intent(getIntent().getAction(), Uri.parse(configDir.getPath()),
					this, KeyMapperActivity.class);
			startActivity(i);
		}
	}

	private void showScreenPresets(View v) {
		presetsAdapter = new ResolutionAdapter();
		presetsDialog = new AlertDialog.Builder(this)
				.setTitle(R.string.SIZE_PRESETS)
				.setAdapter(presetsAdapter, (d, which) -> {
					String string = screenPresets.get(which);
					int separator = string.indexOf(" x ");
					binding.screenWidth.setText(string.substring(0, separator));
					binding.screenHeight.setText(string.substring(separator + 3));
				})
				.setNegativeButton(android.R.string.cancel, null)
				.create();
		presetsDialog.setOnShowListener(d -> {
			ListView listView = ((AlertDialog) d).getListView();
			listView.setOnItemLongClickListener((parent, view, position, id) -> {
				String item = screenPresets.get(position);
				if (!customPresets.contains(item)) {
					Toast.makeText(this, R.string.preset_cannot_delete, Toast.LENGTH_SHORT).show();
				} else {
					showPencilMenu(view, item);
				}
				return true;
			});
		});
		presetsDialog.show();
	}

	private void showPencilMenu(View anchor, String preset) {
		PopupMenu popup = new PopupMenu(this, anchor);
		Menu menu = popup.getMenu();
		SpannableString editLabel = new SpannableString(getString(R.string.edit));
		editLabel.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.btn_bg_normal)), 0, editLabel.length(), 0);
		menu.add(0, 0, 0, editLabel);
		SpannableString deleteLabel = new SpannableString(getString(R.string.action_context_delete));
		deleteLabel.setSpan(new ForegroundColorSpan(Color.RED), 0, deleteLabel.length(), 0);
		menu.add(0, 1, 1, deleteLabel);
		popup.setOnMenuItemClickListener(menuItem -> {
			if (menuItem.getItemId() == 0) {
				showEditResolutionDialog(preset);
			} else {
				if (presetsDialog != null) presetsDialog.dismiss();
				new AlertDialog.Builder(this)
						.setTitle(R.string.delete_resolution_title)
						.setMessage(getString(R.string.delete_resolution_message, preset))
						.setPositiveButton(R.string.action_context_delete, (d, w) -> deleteCustomPreset(preset))
						.setNegativeButton(android.R.string.cancel, null)
						.show();
			}
			return true;
		});
		popup.show();
	}

	private void showEditResolutionDialog(String preset) {
		int sep = preset.indexOf(" x ");
		String currentWidth = preset.substring(0, sep);
		String currentHeight = preset.substring(sep + 3);
		int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
				getResources().getDisplayMetrics());
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setPadding(padding * 3, padding, padding * 3, padding);
		layout.setGravity(Gravity.CENTER_VERTICAL);
		EditText etWidth = new EditText(this);
		etWidth.setInputType(InputType.TYPE_CLASS_NUMBER);
		etWidth.setText(currentWidth);
		etWidth.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
		TextView xLabel = new TextView(this);
		xLabel.setText(" x ");
		EditText etHeight = new EditText(this);
		etHeight.setInputType(InputType.TYPE_CLASS_NUMBER);
		etHeight.setText(currentHeight);
		etHeight.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
		layout.addView(etWidth);
		layout.addView(xLabel);
		layout.addView(etHeight);
		new AlertDialog.Builder(this)
				.setTitle(R.string.edit_resolution_title)
				.setView(layout)
				.setPositiveButton(R.string.save, (d, w) -> {
					int newW = parseInt(etWidth.getText().toString().trim());
					int newH = parseInt(etHeight.getText().toString().trim());
					if (newW <= 0 || newH <= 0) {
						Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
						return;
					}
					String newPreset = newW + " x " + newH;
					if (!newPreset.equals(preset)) {
						updateCustomPreset(preset, newPreset);
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void updateCustomPreset(String oldPreset, String newPreset) {
		if (screenPresets.contains(newPreset)) {
			Toast.makeText(this, R.string.not_saved_exists, Toast.LENGTH_SHORT).show();
			return;
		}
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		Set<String> saved = preferences.getStringSet("ResolutionsPreset", null);
		Set<String> updated = saved != null ? new HashSet<>(saved) : new HashSet<>();
		updated.remove(oldPreset);
		updated.add(newPreset);
		preferences.edit().putStringSet("ResolutionsPreset", updated).apply();
		customPresets.remove(oldPreset);
		screenPresets.remove(oldPreset);
		customPresets.add(newPreset);
		screenPresets.add(newPreset);
		sortPresets();
		if (presetsAdapter != null) presetsAdapter.notifyDataSetChanged();
	}

	private void deleteCustomPreset(String preset) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		Set<String> saved = preferences.getStringSet("ResolutionsPreset", null);
		if (saved != null) {
			Set<String> updated = new HashSet<>(saved);
			updated.remove(preset);
			preferences.edit().putStringSet("ResolutionsPreset", updated).apply();
		}
		customPresets.remove(preset);
		screenPresets.remove(preset);
		if (presetsAdapter != null) presetsAdapter.notifyDataSetChanged();
	}

	private void showColorPicker(EditText et) {
		AmbilWarnaDialog.OnAmbilWarnaListener colorListener = new AmbilWarnaDialog.OnAmbilWarnaListener() {
			@Override
			public void onOk(AmbilWarnaDialog dialog, int color) {
				et.setText(String.format("%06X", color & 0xFFFFFF));
				ColorDrawable drawable = (ColorDrawable) TextViewCompat.getCompoundDrawablesRelative(et)[2];
				drawable.setColor(color);
			}

			@Override
			public void onCancel(AmbilWarnaDialog dialog) {
			}
		};
		int color = parseInt(et.getText().toString().trim(), 16);
		new AmbilWarnaDialog(this, color | 0xFF000000, colorListener).show();
	}

	private void addResolutionToPresets() {
		String width = binding.screenWidth.getText().toString();
		String height = binding.screenHeight.getText().toString();
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
		Set<String> saved = preferences.getStringSet("ResolutionsPreset", null);
		Set<String> set = saved != null ? new HashSet<>(saved) : new HashSet<>(1);
		if (set.add(preset)) {
			preferences.edit().putStringSet("ResolutionsPreset", set).apply();
			screenPresets.add(preset);
			customPresets.add(preset);
			sortPresets();
			if (presetsAdapter != null) presetsAdapter.notifyDataSetChanged();
			Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, R.string.not_saved_exists, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onTuneComplete(float[] values) {
		params.shader.values = values;
	}

	private class ResolutionAdapter extends ArrayAdapter<String> {
		ResolutionAdapter() {
			super(ConfigActivity.this, R.layout.item_resolution_preset, screenPresets);
		}

		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.item_resolution_preset, parent, false);
			}
			String item = getItem(position);
			((TextView) convertView.findViewById(R.id.resolution_text)).setText(item);
			View icon = convertView.findViewById(R.id.custom_icon);
			if (customPresets.contains(item)) {
				icon.setVisibility(View.VISIBLE);
				icon.setOnClickListener(clickedView -> showPencilMenu(clickedView, item));
			} else {
				icon.setVisibility(View.GONE);
				icon.setOnClickListener(null);
			}
			return convertView;
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
			TextViewCompat.setCompoundDrawablesRelative(editText,null, null, colorDrawable, null);
			drawable = colorDrawable;
			editText.setFilters(new InputFilter[]{this::filter});
		}

		private CharSequence filter(CharSequence src, int ss, int se, Spanned dst, int ds, int de) {
			StringBuilder sb = new StringBuilder(se - ss);
			for (int i = ss; i < se; i++) {
				char c = src.charAt(i);
				if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F')) {
					sb.append(c);
				} else if (c >= 'a' && c <= 'f') {
					sb.append((char) (c - 32));
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
			if (s.length() == 0) return;
			try {
				int color = Integer.parseInt(s.toString(), 16);
				drawable.setColor(color | Color.BLACK);
			} catch (NumberFormatException e) {
				drawable.setColor(Color.BLACK);
				s.clear();
			}
		}
	}

	private static class ResolutionAutoFill implements TextWatcher, View.OnFocusChangeListener {
		private final EditText src;
		private final EditText dst;
		private final float aspect;

		public ResolutionAutoFill(EditText src, EditText dst, float aspect) {
			this.src = src;
			this.dst = dst;
			this.aspect = aspect;
			if (src.hasFocus())
				src.addTextChangedListener(this);
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {

		}

		@Override
		public void afterTextChanged(Editable s) {
			try {
				int size = Integer.parseInt(src.getText().toString());
				if (size <= 0) return;
				int value = Math.round(size * aspect);
				dst.setText(String.valueOf(value));
			} catch (NumberFormatException ignored) { }
		}

		public void onFocusChange(View v, boolean hasFocus) {
			if (hasFocus) {
				src.addTextChangedListener(this);
			} else {
				src.removeTextChangedListener(this);
			}
		}
	}

	@Override
	protected void onDestroy() {
		binding = null;
		super.onDestroy();
	}
}
