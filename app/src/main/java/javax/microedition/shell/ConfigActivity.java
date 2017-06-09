/*
 * Copyright 2012 Kulikov Dmitriy, Naik
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
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import filelog.Log;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeMap;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.EventQueue;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.pointer.VirtualKeyboard;
import javax.microedition.midlet.MIDlet;
import javax.microedition.param.DataContainer;
import javax.microedition.param.SharedPreferencesContainer;
import javax.microedition.util.ContextHolder;
import ua.naiksoftware.j2meloader.R;
import ua.naiksoftware.util.FileUtils;
import yuku.ambilwarna.AmbilWarnaDialog;

public class ConfigActivity extends Activity implements
		View.OnKeyListener, View.OnClickListener {

	protected EditText tfScreenWidth;
	protected EditText tfScreenHeight;
	protected EditText tfScreenBack;
	protected CheckBox cxScaleToFit;
	protected CheckBox cxKeepAspectRatio;
	protected CheckBox cxFilter;
	protected CheckBox cxImmediate;

	protected EditText tfFontSizeSmall;
	protected EditText tfFontSizeMedium;
	protected EditText tfFontSizeLarge;
	protected CheckBox cxFontSizeInSP;
    protected CheckBox cxShowKeyboard;

	protected SeekBar sbVKAlpha;
	protected EditText tfVKHideDelay;
	protected EditText tfVKLayoutKeyCode;
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

	private MIDlet midlet;
	public static String pathToMidletDir;
    public static String appName;
	public static final String MIDLET_RES_DIR = "/res/";
	public static final String MIDLET_DEX_FILE = "/converted.dex";
	public static final String MIDLET_CONF_FILE = MIDLET_DEX_FILE + ".conf";

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

	@SuppressLint("StringFormatMatches")
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.config_all);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		MIDlet.setMidletContext(this);
        pathToMidletDir = getIntent().getDataString();
        appName = getIntent().getStringExtra("name");

        DataContainer params = new SharedPreferencesContainer(
				appName, Context.MODE_PRIVATE, this);

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
        System.setProperty("microedition.locale", locale.toLowerCase());
		System.setProperty("user.home", Environment.getExternalStorageDirectory().getAbsolutePath());

		tfScreenWidth = (EditText) findViewById(R.id.tfScreenWidth);
		tfScreenHeight = (EditText) findViewById(R.id.tfScreenHeight);
		tfScreenBack = (EditText) findViewById(R.id.tfScreenBack);
		cxScaleToFit = (CheckBox) findViewById(R.id.cxScaleToFit);
		cxKeepAspectRatio = (CheckBox) findViewById(R.id.cxKeepAspectRatio);
		cxFilter = (CheckBox) findViewById(R.id.cxFilter);
		cxImmediate = (CheckBox) findViewById(R.id.cxImmediate);

		tfFontSizeSmall = (EditText) findViewById(R.id.tfFontSizeSmall);
		tfFontSizeMedium = (EditText) findViewById(R.id.tfFontSizeMedium);
		tfFontSizeLarge = (EditText) findViewById(R.id.tfFontSizeLarge);
		cxFontSizeInSP = (CheckBox) findViewById(R.id.cxFontSizeInSP);
        cxShowKeyboard = (CheckBox) findViewById(R.id.cxIsShowKeyboard);

		sbVKAlpha = (SeekBar) findViewById(R.id.sbVKAlpha);
		tfVKHideDelay = (EditText) findViewById(R.id.tfVKHideDelay);
		tfVKLayoutKeyCode = (EditText) findViewById(R.id.tfVKLayoutKeyCode);
		tfVKFore = (EditText) findViewById(R.id.tfVKFore);
		tfVKBack = (EditText) findViewById(R.id.tfVKBack);
		tfVKSelFore = (EditText) findViewById(R.id.tfVKSelFore);
		tfVKSelBack = (EditText) findViewById(R.id.tfVKSelBack);
		tfVKOutline = (EditText) findViewById(R.id.tfVKOutline);

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

		tfVKLayoutKeyCode.setOnKeyListener(this);

		findViewById(R.id.cmdScreenSizePresets).setOnClickListener(this);
		findViewById(R.id.cmdFontSizePresets).setOnClickListener(this);
		findViewById(R.id.cmdScreenBack).setOnClickListener(this);
		findViewById(R.id.cmdVKBack).setOnClickListener(this);
		findViewById(R.id.cmdVKFore).setOnClickListener(this);
		findViewById(R.id.cmdVKSelBack).setOnClickListener(this);
		findViewById(R.id.cmdVKSelFore).setOnClickListener(this);
		findViewById(R.id.cmdVKOutline).setOnClickListener(this);
		findViewById(R.id.cmdLanguage).setOnClickListener(this);

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

		applyConfiguration(/* new MIDlet() */);// Настройка конфигурации перед
												// запуском конструктора
												// мидлета
	}

	public void onPause() {
		SharedPreferencesContainer params = new SharedPreferencesContainer(
				appName, Context.MODE_PRIVATE, this);
		saveParams(params);
		super.onPause();
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		fillScreenSizePresets(ContextHolder.getDisplayWidth(),
				ContextHolder.getDisplayHeight());
	}

	public void fillScreenSizePresets(int w, int h) {
		screenWidths.clear();
		screenHeights.clear();
		screenAdapter.clear();

		addScreenSizePreset(128, 128);
		addScreenSizePreset(128, 160);
		addScreenSizePreset(132, 176);
		addScreenSizePreset(176, 220);
		addScreenSizePreset(240, 320);

		int w2 = w / 2;
		int h2 = h / 2;

		if (w > h) {
			addScreenSizePreset(h2 * 3 / 4, h2);
			addScreenSizePreset(h2 * 4 / 3, h2);

			addScreenSizePreset(h * 3 / 4, h);
			addScreenSizePreset(h * 4 / 3, h);
		} else {
			addScreenSizePreset(w2, w2 * 4 / 3);
			addScreenSizePreset(w2, w2 * 3 / 4);

			addScreenSizePreset(w, w * 4 / 3);
			addScreenSizePreset(w, w * 3 / 4);
		}

		addScreenSizePreset(w, h);
	}

	public void addScreenSizePreset(int width, int height) {
		screenWidths.add(width);
		screenHeights.add(height);
		screenAdapter.add(Integer.toString(width) + " x "
				+ Integer.toString(height));
	}

	public void addFontSizePreset(String title, int small, int medium, int large) {
		fontSmall.add(small);
		fontMedium.add(medium);
		fontLarge.add(large);
		fontAdapter.add(title);
	}

	public void loadParams(DataContainer params) {
		tfScreenWidth.setText(Integer.toString(params
				.getInt("ScreenWidth", 240)));
		tfScreenHeight.setText(Integer.toString(params.getInt("ScreenHeight",
				320)));
		tfScreenBack
				.setText(Integer.toHexString(
						params.getInt("ScreenBackgroundColor", 0xD0D0D0))
						.toUpperCase());
		cxScaleToFit.setChecked(params.getBoolean("ScreenScaleToFit", true));
		cxKeepAspectRatio.setChecked(params.getBoolean("ScreenKeepAspectRatio",
				true));
		cxFilter.setChecked(params.getBoolean("ScreenFilter", true));
		cxImmediate.setChecked(params.getBoolean("ImmediateMode", false));

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
		tfVKLayoutKeyCode.setText(Integer.toString(params.getInt(
				"VirtualKeyboardLayoutKeyCode", KeyEvent.KEYCODE_MENU)));
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

	public void saveParams(SharedPreferencesContainer editor) {
		try {
			editor.edit();
			editor.putString("Locale", locale);

			editor.putInt("ScreenWidth",
					Integer.parseInt(tfScreenWidth.getText().toString()));
			editor.putInt("ScreenHeight",
					Integer.parseInt(tfScreenHeight.getText().toString()));
			editor.putInt("ScreenBackgroundColor",
					Integer.parseInt(tfScreenBack.getText().toString(), 16));
			editor.putBoolean("ScreenScaleToFit", cxScaleToFit.isChecked());
			editor.putBoolean("ScreenKeepAspectRatio",
					cxKeepAspectRatio.isChecked());
			editor.putBoolean("ScreenFilter", cxFilter.isChecked());
			editor.putBoolean("ImmediateMode", cxImmediate.isChecked());

			editor.putInt("FontSizeSmall",
					Integer.parseInt(tfFontSizeSmall.getText().toString()));
			editor.putInt("FontSizeMedium",
					Integer.parseInt(tfFontSizeMedium.getText().toString()));
			editor.putInt("FontSizeLarge",
					Integer.parseInt(tfFontSizeLarge.getText().toString()));
			editor.putBoolean("FontApplyDimensions", cxFontSizeInSP.isChecked());
            editor.putBoolean("ShowKeyboard", cxShowKeyboard.isChecked());

			editor.putInt("VirtualKeyboardAlpha", sbVKAlpha.getProgress());
			editor.putInt("VirtualKeyboardDelay",
					Integer.parseInt(tfVKHideDelay.getText().toString()));
			editor.putInt("VirtualKeyboardLayoutKeyCode",
					Integer.parseInt(tfVKLayoutKeyCode.getText().toString()));
			editor.putInt("VirtualKeyboardColorBackground",
					Integer.parseInt(tfVKBack.getText().toString(), 16));
			editor.putInt("VirtualKeyboardColorForeground",
					Integer.parseInt(tfVKFore.getText().toString(), 16));
			editor.putInt("VirtualKeyboardColorBackgroundSelected",
					Integer.parseInt(tfVKSelBack.getText().toString(), 16));
			editor.putInt("VirtualKeyboardColorForegroundSelected",
					Integer.parseInt(tfVKSelFore.getText().toString(), 16));
			editor.putInt("VirtualKeyboardColorOutline",
					Integer.parseInt(tfVKOutline.getText().toString(), 16));

			editor.apply();
			editor.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void applyConfiguration(/* MIDlet midlet */) {
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
			boolean screenScaleToFit = cxScaleToFit.isChecked();
			boolean screenKeepAspectRatio = cxKeepAspectRatio.isChecked();
			boolean screenFilter = cxFilter.isChecked();
			boolean immediateMode = cxImmediate.isChecked();

			Font.setSize(Font.SIZE_SMALL, fontSizeSmall);
			Font.setSize(Font.SIZE_MEDIUM, fontSizeMedium);
			Font.setSize(Font.SIZE_LARGE, fontSizeLarge);
			Font.setApplyDimensions(fontApplyDimensions);

			Canvas.setVirtualSize(screenWidth, screenHeight, screenScaleToFit,
					screenKeepAspectRatio);
			Canvas.setFilterBitmap(screenFilter);
			EventQueue.setImmediate(immediateMode);
			Canvas.setBackgroundColor(screenBackgroundColor);

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private void setVirtualKeyboard() {
		int vkAlpha = sbVKAlpha.getProgress();
		int vkDelay = Integer.parseInt(tfVKHideDelay.getText().toString());
		int vkLayoutKeyCode = Integer.parseInt(tfVKLayoutKeyCode.getText()
				.toString());
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
		vk.setLayoutEditKey(vkLayoutKeyCode);

		try {
			DataInputStream dis = new DataInputStream(ContextHolder.openFileInput("VirtualKeyboardLayout"));
			vk.readLayout(dis);
			dis.close();
		} catch (FileNotFoundException fnfe) {
		} catch (IOException ioe) {
			ioe.printStackTrace();
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
					DataOutputStream dos = new DataOutputStream(ContextHolder.openFileOutput(
									"VirtualKeyboardLayout",
									Context.MODE_PRIVATE));
					vk.writeLayout(dos);
					dos.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		};
		vk.setLayoutListener(listener);
		ContextHolder.setVk(vk);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.config, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
			case R.id.action_start:
				try {
					// Теперь применяем конфигурацию к запускаемому мидлету.

					if(cxShowKeyboard.isChecked()) {
						setVirtualKeyboard();
					}
					midlet = loadMIDlet();
					applyConfiguration();
					midlet.start();
					finish();
				} catch (Throwable t) {
					t.printStackTrace();
                    AlertDialog.Builder builder = new AlertDialog.Builder(this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(R.string.error)
                            .setMessage(t.getMessage());
                    builder.show();
				}

				break;
			case R.id.action_reset:
				SharedPreferencesContainer params = new SharedPreferencesContainer(
					appName, Context.MODE_PRIVATE,
					ConfigActivity.this);
				params.edit().clear().commit();
				params.close();
				loadParams(params);
				break;
		    case android.R.id.home:
				if (midlet != null) {
					midlet.notifyDestroyed();
				}
				finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (v == tfVKLayoutKeyCode
				&& (event.getFlags() & KeyEvent.FLAG_SOFT_KEYBOARD) == 0) {
			tfVKLayoutKeyCode.setText(Integer.toString(keyCode));
			return true;
		}

		return false;
	}

	public void setLanguage(int which) {
		locale = getResources().getStringArray(R.array.locales)[which];
		recreate();
	}

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
					setLanguage(which);
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

	private MIDlet loadMIDlet() {
		MIDlet midlet = null;
		TreeMap<String, String> params = FileUtils.loadManifest(new File(
				pathToMidletDir + MIDLET_CONF_FILE));
		MIDlet.initProps(params);
		String dex = pathToMidletDir + ConfigActivity.MIDLET_DEX_FILE;
		ClassLoader loader = new MyClassLoader(dex,
				getApplicationInfo().dataDir, null, getClassLoader(), pathToMidletDir + MIDLET_RES_DIR);
		try {
			String mainClassParam = params.get("MIDlet-1");
			String mainClass = mainClassParam.substring(
					mainClassParam.lastIndexOf(',') + 1).trim();
			Log.d("inf", "load main: " + mainClass + " from dex:" + dex);
			midlet = (MIDlet) loader.loadClass(mainClass).newInstance();// Тут
																		// вызывается
																		// конструктор
																		// по
																		// умолчанию.
		} catch (ClassNotFoundException ex) {
			Log.d("err", ex.toString() + "/n" + ex.getMessage());
		} catch (InstantiationException ex) {
			Log.d("err", ex.toString() + "/n" + ex.getMessage());
		} catch (IllegalAccessException ex) {
			Log.d("err", ex.toString() + "/n" + ex.getMessage());
		}
		return midlet;
	}
}
