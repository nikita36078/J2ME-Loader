/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2015-2016 Nickolay Savchenko
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

package javax.microedition.shell;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;

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
import javax.microedition.lcdui.pointer.VirtualKeyboard;
import javax.microedition.util.ContextHolder;
import javax.microedition.util.param.DataContainer;
import javax.microedition.util.param.SharedPreferencesContainer;

import ua.naiksoftware.j2meloader.R;
import yuku.ambilwarna.AmbilWarnaDialog;

public class ConfigActivity extends AppCompatActivity implements View.OnClickListener {

	protected EditText tfScreenWidth;
	protected EditText tfScreenHeight;
	protected EditText tfScreenBack;
	protected SeekBar sbScaleRatio;
	protected EditText tfScaleRatioValue;
	protected CheckBox cxScaleToFit;
	protected CheckBox cxKeepAspectRatio;
	protected CheckBox cxFilter;
	protected CheckBox cxImmediate;
	protected CheckBox cxClearBuffer;

	protected EditText tfFontSizeSmall;
	protected EditText tfFontSizeMedium;
	protected EditText tfFontSizeLarge;
	protected CheckBox cxFontSizeInSP;
	protected CheckBox cxShowKeyboard;

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

	protected String locale;

	private File keylayoutFile;
	private SharedPreferencesContainer params;
	private String pathToMidletDir;
	public static final String MIDLET_DIR = "/converted/";
	public static final String EMULATOR_DIR = Environment.getExternalStorageDirectory() + "/J2ME-Loader";
	public static final String DATA_DIR = EMULATOR_DIR + "/data/";
	public static final String APP_DIR = EMULATOR_DIR + MIDLET_DIR;
	public static final String TEMP_DEX_DIR = "/tmp_dex";
	public static final String TEMP_DEX_OPT_DIR = "/tmp_dexopt";
	public static final String MIDLET_RES_DIR = "/res";
	public static final String MIDLET_DEX_FILE = "/converted.dex";
	public static final String MIDLET_CONF_FILE = MIDLET_DEX_FILE + ".conf";
	public static final String MIDLET_PATH_KEY = "path";
	public static final String MIDLET_NAME_KEY = "name";
	public static final String SHOW_SETTINGS_KEY = "showSettings";

	/*
	 * <xml locale=en>../../../../res/values/strings.xml</xml> <xml
	 * locale=ru>../../../../res/values-ru/strings.xml</xml>
	 */

	public String getString(int index, String token) {
		return getString(index).replace("%A", token);
	}

	public String getString(int index, String[] tokens) {
		String res = getString(index);

		for (int i = 0; i < tokens.length; i++) {
			res = res.replace("%" + (char) ('A' + i), tokens[i]);
		}

		return res;
	}

	@SuppressLint({"StringFormatMatches", "StringFormatInvalid"})
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.config_all);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		ContextHolder.setCurrentActivity(this);
		pathToMidletDir = getIntent().getDataString();
		String appName = getIntent().getStringExtra(MIDLET_NAME_KEY);
		appName = appName.replace(":", "").replace("/", "");
		keylayoutFile = new File(DATA_DIR + appName, "VirtualKeyboardLayout");

		params = new SharedPreferencesContainer(appName, Context.MODE_PRIVATE, this);

		locale = params.getString("Locale", Locale.getDefault().getCountry());
		System.setProperty("microedition.sensor.version", "1");
		System.setProperty("microedition.platform", "Nokia 6233");
		System.setProperty("microedition.configuration", "CDLC1.1");
		System.setProperty("microedition.profiles", "MIDP2.0");
		System.setProperty("microedition.m3g.version", "1.1");
		System.setProperty("microedition.media.version", "1.0");
		System.setProperty("supports.mixing", "true");
		System.setProperty("supports.audio.capture", "false");
		System.setProperty("supports.video.capture", "false");
		System.setProperty("supports.recording", "false");
		System.setProperty("microedition.pim.version", "1.0");
		System.setProperty("microedition.io.file.FileConnection.version", "1.0");
		System.setProperty("microedition.locale", locale.toLowerCase());
		System.setProperty("microedition.encoding", "ISO-8859-1");
		System.setProperty("user.home", Environment.getExternalStorageDirectory().getAbsolutePath());

		tfScreenWidth = findViewById(R.id.tfScreenWidth);
		tfScreenHeight = findViewById(R.id.tfScreenHeight);
		tfScreenBack = findViewById(R.id.tfScreenBack);
		cxScaleToFit = findViewById(R.id.cxScaleToFit);
		sbScaleRatio = findViewById(R.id.sbScaleRatio);
		tfScaleRatioValue = findViewById(R.id.tfScaleRatioValue);
		cxKeepAspectRatio = findViewById(R.id.cxKeepAspectRatio);
		cxFilter = findViewById(R.id.cxFilter);
		cxImmediate = findViewById(R.id.cxImmediate);
		cxClearBuffer = findViewById(R.id.cxClearBuffer);

		tfFontSizeSmall = findViewById(R.id.tfFontSizeSmall);
		tfFontSizeMedium = findViewById(R.id.tfFontSizeMedium);
		tfFontSizeLarge = findViewById(R.id.tfFontSizeLarge);
		cxFontSizeInSP = findViewById(R.id.cxFontSizeInSP);
		cxShowKeyboard = findViewById(R.id.cxIsShowKeyboard);

		sbVKAlpha = findViewById(R.id.sbVKAlpha);
		tfVKHideDelay = findViewById(R.id.tfVKHideDelay);
		tfVKFore = findViewById(R.id.tfVKFore);
		tfVKBack = findViewById(R.id.tfVKBack);
		tfVKSelFore = findViewById(R.id.tfVKSelFore);
		tfVKSelBack = findViewById(R.id.tfVKSelBack);
		tfVKOutline = findViewById(R.id.tfVKOutline);

		screenWidths = new ArrayList();
		screenHeights = new ArrayList();
		screenAdapter = new ArrayList();

		fillScreenSizePresets(ContextHolder.getDisplayWidth(),
				ContextHolder.getDisplayHeight());

		fontSmall = new ArrayList();
		fontMedium = new ArrayList();
		fontLarge = new ArrayList();
		fontAdapter = new ArrayList();

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
		findViewById(R.id.cmdLanguage).setOnClickListener(this);
		sbScaleRatio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
				tfScaleRatioValue.setText(String.valueOf(progress));
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
				Integer enteredProgress = Integer.valueOf(s.toString());
				sbScaleRatio.setProgress(enteredProgress);
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		loadParams(params);

		String[] locales = getResources().getStringArray(R.array.locales);
		int index = -1;

		for (int i = 0; i < locales.length; i++) {
			if (locales[i].equalsIgnoreCase(locale)) {
				index = i;
				break;
			}
		}

		String language;

		if (index >= 0) {
			language = getResources().getStringArray(R.array.languages)[index];
		} else {
			language = locale;
		}

		((Button) findViewById(R.id.cmdLanguage)).setText(getString(
				R.string.PREF_LANGUAGE, language));

		applyConfiguration();
		File appSettings = new File(getFilesDir().getParent() + File.separator + "shared_prefs", appName + ".xml");
		if (appSettings.exists() && !getIntent().getBooleanExtra(SHOW_SETTINGS_KEY, false)) {
			startMIDlet();
		}
	}

	@Override
	public void onPause() {
		saveParams();
		super.onPause();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		fillScreenSizePresets(ContextHolder.getDisplayWidth(),
				ContextHolder.getDisplayHeight());
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
		screenAdapter.add(Integer.toString(width) + " x "
				+ Integer.toString(height));
	}

	private void addFontSizePreset(String title, int small, int medium, int large) {
		fontSmall.add(small);
		fontMedium.add(medium);
		fontLarge.add(large);
		fontAdapter.add(title);
	}

	private void loadParams(DataContainer params) {
		tfScreenWidth.setText(Integer.toString(params
				.getInt("ScreenWidth", 240)));
		tfScreenHeight.setText(Integer.toString(params.getInt("ScreenHeight",
				320)));
		tfScreenBack
				.setText(Integer.toHexString(
						params.getInt("ScreenBackgroundColor", 0xD0D0D0))
						.toUpperCase());
		sbScaleRatio.setProgress(params.getInt("ScreenScaleRatio", 100));
		tfScaleRatioValue.setText(String.valueOf(sbScaleRatio.getProgress()));
		cxScaleToFit.setChecked(params.getBoolean("ScreenScaleToFit", true));
		cxKeepAspectRatio.setChecked(params.getBoolean("ScreenKeepAspectRatio",
				true));
		cxFilter.setChecked(params.getBoolean("ScreenFilter", true));
		cxImmediate.setChecked(params.getBoolean("ImmediateMode", false));
		cxClearBuffer.setChecked(params.getBoolean("ClearBuffer", false));

		tfFontSizeSmall.setText(Integer.toString(params.getInt("FontSizeSmall",
				18)));
		tfFontSizeMedium.setText(Integer.toString(params.getInt(
				"FontSizeMedium", 22)));
		tfFontSizeLarge.setText(Integer.toString(params.getInt("FontSizeLarge",
				26)));
		cxFontSizeInSP.setChecked(params.getBoolean("FontApplyDimensions",
				false));
		cxShowKeyboard.setChecked(params.getBoolean(("ShowKeyboard"), true));

		sbVKAlpha.setProgress(params.getInt("VirtualKeyboardAlpha", 64));
		tfVKHideDelay.setText(Integer.toString(params.getInt(
				"VirtualKeyboardDelay", -1)));
		tfVKBack.setText(Integer.toHexString(
				params.getInt("VirtualKeyboardColorBackground", 0xD0D0D0))
				.toUpperCase());
		tfVKFore.setText(Integer.toHexString(
				params.getInt("VirtualKeyboardColorForeground", 0x000080))
				.toUpperCase());
		tfVKSelBack.setText(Integer.toHexString(
				params.getInt("VirtualKeyboardColorBackgroundSelected",
						0x000080)).toUpperCase());
		tfVKSelFore.setText(Integer.toHexString(
				params.getInt("VirtualKeyboardColorForegroundSelected",
						0xFFFFFF)).toUpperCase());
		tfVKOutline.setText(Integer.toHexString(
				params.getInt("VirtualKeyboardColorOutline", 0xFFFFFF))
				.toUpperCase());
	}

	private void saveParams() {
		try {
			params.edit();
			params.putString("Locale", locale);

			params.putInt("ScreenWidth",
					Integer.parseInt(tfScreenWidth.getText().toString()));
			params.putInt("ScreenHeight",
					Integer.parseInt(tfScreenHeight.getText().toString()));
			params.putInt("ScreenBackgroundColor",
					Integer.parseInt(tfScreenBack.getText().toString(), 16));
			params.putInt("ScreenScaleRatio", sbScaleRatio.getProgress());
			params.putBoolean("ScreenScaleToFit", cxScaleToFit.isChecked());
			params.putBoolean("ScreenKeepAspectRatio",
					cxKeepAspectRatio.isChecked());
			params.putBoolean("ScreenFilter", cxFilter.isChecked());
			params.putBoolean("ImmediateMode", cxImmediate.isChecked());
			params.putBoolean("ClearBuffer", cxClearBuffer.isChecked());

			params.putInt("FontSizeSmall",
					Integer.parseInt(tfFontSizeSmall.getText().toString()));
			params.putInt("FontSizeMedium",
					Integer.parseInt(tfFontSizeMedium.getText().toString()));
			params.putInt("FontSizeLarge",
					Integer.parseInt(tfFontSizeLarge.getText().toString()));
			params.putBoolean("FontApplyDimensions", cxFontSizeInSP.isChecked());
			params.putBoolean("ShowKeyboard", cxShowKeyboard.isChecked());

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
			params.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private void applyConfiguration() {
		try {
			int fontSizeSmall = Integer.parseInt(tfFontSizeSmall.getText()
					.toString());
			int fontSizeMedium = Integer.parseInt(tfFontSizeMedium.getText()
					.toString());
			int fontSizeLarge = Integer.parseInt(tfFontSizeLarge.getText()
					.toString());
			boolean fontApplyDimensions = cxFontSizeInSP.isChecked();

			int screenWidth = Integer.parseInt(tfScreenWidth.getText()
					.toString());
			int screenHeight = Integer.parseInt(tfScreenHeight.getText()
					.toString());
			int screenBackgroundColor = Integer.parseInt(tfScreenBack.getText()
					.toString(), 16);
			int screenScaleRatio = sbScaleRatio.getProgress();
			boolean screenScaleToFit = cxScaleToFit.isChecked();
			boolean screenKeepAspectRatio = cxKeepAspectRatio.isChecked();
			boolean screenFilter = cxFilter.isChecked();
			boolean immediateMode = cxImmediate.isChecked();
			boolean clearBuffer = cxClearBuffer.isChecked();

			Font.setSize(Font.SIZE_SMALL, fontSizeSmall);
			Font.setSize(Font.SIZE_MEDIUM, fontSizeMedium);
			Font.setSize(Font.SIZE_LARGE, fontSizeLarge);
			Font.setApplyDimensions(fontApplyDimensions);

			Canvas.setVirtualSize(screenWidth, screenHeight, screenScaleToFit,
					screenKeepAspectRatio, screenScaleRatio);
			Canvas.setFilterBitmap(screenFilter);
			EventQueue.setImmediate(immediateMode);
			Canvas.setBackgroundColor(screenBackgroundColor);
			Canvas.setClearBuffer(clearBuffer);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private void setVirtualKeyboard() {
		int vkAlpha = sbVKAlpha.getProgress();
		int vkDelay = Integer.parseInt(tfVKHideDelay.getText().toString());
		int vkColorBackground = Integer.parseInt(tfVKBack.getText().toString(),
				16);
		int vkColorForeground = Integer.parseInt(tfVKFore.getText().toString(),
				16);
		int vkColorBackgroundSelected = Integer.parseInt(tfVKSelBack.getText()
				.toString(), 16);
		int vkColorForegroundSelected = Integer.parseInt(tfVKSelFore.getText()
				.toString(), 16);
		int vkColorOutline = Integer.parseInt(tfVKOutline.getText().toString(),
				16);

		VirtualKeyboard vk = new VirtualKeyboard();

		vk.setOverlayAlpha(vkAlpha);
		vk.setHideDelay(vkDelay);

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

		VirtualKeyboard.LayoutListener listener = new VirtualKeyboard.LayoutListener() {
			public void layoutChanged(VirtualKeyboard vk) {
				try {
					FileOutputStream fos = new FileOutputStream(keylayoutFile);
					DataOutputStream dos = new DataOutputStream(fos);
					vk.writeLayout(dos);
					fos.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		};
		vk.setLayoutListener(listener);
		ContextHolder.setVk(vk);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.config, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_start:
				startMIDlet();
				break;
			case R.id.action_reset:
				params.edit().clear().commit();
				params.close();
				loadParams(params);
				break;
			case R.id.action_reset_layout:
				keylayoutFile.delete();
				break;
			case android.R.id.home:
				finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void startMIDlet() {
		// Теперь применяем конфигурацию к запускаемому мидлету.
		if (cxShowKeyboard.isChecked()) {
			setVirtualKeyboard();
		} else {
			ContextHolder.setVk(null);
		}
		applyConfiguration();
		Intent i = new Intent(this, MicroActivity.class);
		i.putExtra(MIDLET_PATH_KEY, pathToMidletDir);
		startActivity(i);
		finish();
	}

	@Override
	public void onClick(View v) {
		String[] presets = null;
		DialogInterface.OnClickListener presetListener = null;

		int color = 0;
		AmbilWarnaDialog.OnAmbilWarnaListener colorListener = null;

		int id = v.getId();

		if (id == R.id.cmdScreenSizePresets) {
			presets = screenAdapter.toArray(new String[0]);

			presetListener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					tfScreenWidth.setText(Integer.toString(screenWidths
							.get(which)));
					tfScreenHeight.setText(Integer.toString(screenHeights
							.get(which)));
				}
			};
		} else if (id == R.id.cmdSwapSizes) {
			String tmp = tfScreenWidth.getText().toString();
			tfScreenWidth.setText(tfScreenHeight.getText().toString());
			tfScreenHeight.setText(tmp);
		} else if (id == R.id.cmdFontSizePresets) {
			presets = fontAdapter.toArray(new String[0]);

			presetListener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					tfFontSizeSmall.setText(Integer.toString(fontSmall
							.get(which)));
					tfFontSizeMedium.setText(Integer.toString(fontMedium
							.get(which)));
					tfFontSizeLarge.setText(Integer.toString(fontLarge
							.get(which)));
				}
			};
		} else if (id == R.id.cmdLanguage) {
			presets = getResources().getStringArray(R.array.languages);

			presetListener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					locale = getResources().getStringArray(R.array.locales)[which];
				}
			};
		} else if (id == R.id.cmdScreenBack) {
			color = Integer.parseInt(tfScreenBack.getText().toString(), 16);

			colorListener = new AmbilWarnaDialog.OnAmbilWarnaListener() {
				public void onOk(AmbilWarnaDialog dialog, int color) {
					tfScreenBack.setText(Integer.toHexString(color & 0xFFFFFF)
							.toUpperCase());
				}

				public void onCancel(AmbilWarnaDialog dialog) {
				}
			};
		} else if (id == R.id.cmdVKBack) {
			color = Integer.parseInt(tfVKBack.getText().toString(), 16);

			colorListener = new AmbilWarnaDialog.OnAmbilWarnaListener() {
				public void onOk(AmbilWarnaDialog dialog, int color) {
					tfVKBack.setText(Integer.toHexString(color & 0xFFFFFF)
							.toUpperCase());
				}

				public void onCancel(AmbilWarnaDialog dialog) {
				}
			};
		} else if (id == R.id.cmdVKFore) {
			color = Integer.parseInt(tfVKFore.getText().toString(), 16);

			colorListener = new AmbilWarnaDialog.OnAmbilWarnaListener() {
				public void onOk(AmbilWarnaDialog dialog, int color) {
					tfVKFore.setText(Integer.toHexString(color & 0xFFFFFF)
							.toUpperCase());
				}

				public void onCancel(AmbilWarnaDialog dialog) {
				}
			};
		} else if (id == R.id.cmdVKSelFore) {
			color = Integer.parseInt(tfVKSelFore.getText().toString(), 16);

			colorListener = new AmbilWarnaDialog.OnAmbilWarnaListener() {
				public void onOk(AmbilWarnaDialog dialog, int color) {
					tfVKSelFore.setText(Integer.toHexString(color & 0xFFFFFF)
							.toUpperCase());
				}

				public void onCancel(AmbilWarnaDialog dialog) {
				}
			};
		} else if (id == R.id.cmdVKSelBack) {
			color = Integer.parseInt(tfVKSelBack.getText().toString(), 16);

			colorListener = new AmbilWarnaDialog.OnAmbilWarnaListener() {
				public void onOk(AmbilWarnaDialog dialog, int color) {
					tfVKSelBack.setText(Integer.toHexString(color & 0xFFFFFF)
							.toUpperCase());
				}

				public void onCancel(AmbilWarnaDialog dialog) {
				}
			};
		} else if (id == R.id.cmdVKOutline) {
			color = Integer.parseInt(tfVKOutline.getText().toString(), 16);

			colorListener = new AmbilWarnaDialog.OnAmbilWarnaListener() {
				public void onOk(AmbilWarnaDialog dialog, int color) {
					tfVKOutline.setText(Integer.toHexString(color & 0xFFFFFF)
							.toUpperCase());
				}

				public void onCancel(AmbilWarnaDialog dialog) {
				}
			};
		} else {
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
