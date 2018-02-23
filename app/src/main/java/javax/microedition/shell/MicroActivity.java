/*
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

import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.event.CommandActionEvent;
import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.lcdui.pointer.VirtualKeyboard;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.midlet.MIDlet;
import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.util.FileUtils;

public class MicroActivity extends AppCompatActivity {
	private static final String TAG = MicroActivity.class.getName();
	private Displayable current;
	private boolean visible;
	private boolean loaded;
	private boolean started;
	private LinearLayout layout;
	private Toolbar toolbar;
	private String pathToMidletDir;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_micro);
		ContextHolder.setCurrentActivity(this);
		layout = findViewById(R.id.displayable_container);
		toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		pathToMidletDir = getIntent().getStringExtra(ConfigActivity.MIDLET_PATH_KEY);
		initEmulator();
		try {
			loadMIDlet();
		} catch (Exception e) {
			e.printStackTrace();
			showErrorDialog(e.getMessage());
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		visible = true;
		if (loaded) {
			if (started) {
				Display.getDisplay(null).activityResumed();
			} else {
				started = true;
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		visible = false;
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (loaded) {
			Display.getDisplay(null).activityStopped();
		}
	}

	private void loadMIDlet() throws Exception {
		ArrayList<String> midlets = new ArrayList<>();
		LinkedHashMap<String, String> params = FileUtils.loadManifest(new File(pathToMidletDir + ConfigActivity.MIDLET_CONF_FILE));
		MIDlet.initProps(params);
		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (entry.getKey().matches("MIDlet-[0-9]+")) {
				midlets.add(entry.getValue());
			}
		}
		int size = midlets.size();
		String[] midletsNameArray = new String[size];
		String[] midletsClassArray = new String[size];
		for (int i = 0; i < size; i++) {
			String tmp = midlets.get(i);
			midletsClassArray[i] = tmp.substring(tmp.lastIndexOf(',') + 1).trim();
			midletsNameArray[i] = tmp.substring(0, tmp.indexOf(',')).trim();
		}
		if (size == 0) {
			throw new Exception();
		} else if (size == 1) {
			startMidlet(midletsClassArray[0]);
		} else {
			showMidletDialog(midletsNameArray, midletsClassArray);
		}
	}

	private void showMidletDialog(String[] midletsNameArray, final String[] midletsClassArray) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(R.string.select_dialog_title)
				.setItems(midletsNameArray, (d, n) -> startMidlet(midletsClassArray[n]))
				.setOnCancelListener(dialogInterface -> finish());
		builder.show();
	}

	private void startMidlet(String mainClass) {
		File dexSource = new File(pathToMidletDir, ConfigActivity.MIDLET_DEX_FILE);
		File dexTargetDir = new File(getApplicationInfo().dataDir, ConfigActivity.TEMP_DEX_DIR);
		if (dexTargetDir.exists()) {
			FileUtils.deleteDirectory(dexTargetDir);
		}
		dexTargetDir.mkdir();
		File dexTargetOptDir = new File(getApplicationInfo().dataDir, ConfigActivity.TEMP_DEX_OPT_DIR);
		if (dexTargetOptDir.exists()) {
			FileUtils.deleteDirectory(dexTargetOptDir);
		}
		dexTargetOptDir.mkdir();
		File dexTarget = new File(dexTargetDir, ConfigActivity.MIDLET_DEX_FILE);
		try {
			FileUtils.copyFileUsingChannel(dexSource, dexTarget);
			ClassLoader loader = new MyClassLoader(dexTarget.getAbsolutePath(),
					dexTargetOptDir.getAbsolutePath(), null, getClassLoader(), pathToMidletDir + ConfigActivity.MIDLET_RES_DIR);
			Log.i(TAG, "load main: " + mainClass + " from dex:" + dexTarget.getPath());
			MIDlet midlet = (MIDlet) loader.loadClass(mainClass).newInstance();
			midlet.startApp();
			loaded = true;
		} catch (Throwable t) {
			t.printStackTrace();
			showErrorDialog(t.getMessage());
		}
	}

	private void initEmulator() {
		Display.initDisplay();
		Graphics3D.initGraphics3D();
		File cacheDir = ContextHolder.getCacheDir();
		if (cacheDir.exists()) {
			for (File temp : cacheDir.listFiles()) {
				temp.delete();
			}
		}
	}

	private void showErrorDialog(String message) {
		if (message != null) {
			Log.e(TAG, message);
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.error)
				.setMessage(message);
		builder.setOnCancelListener(dialogInterface -> finish());
		builder.show();
	}

	private SimpleEvent msgSetCurent = new SimpleEvent() {
		public void process() {
			current.setParentActivity(MicroActivity.this);
			layout.removeAllViews();
			layout.addView(current.getDisplayableView());
			invalidateOptionsMenu();
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			boolean actionBarEnabled = sp.getBoolean("pref_actionbar_switch", false);
			Window window = getWindow();
			ActionBar actionBar = getSupportActionBar();
			LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) toolbar.getLayoutParams();
			if (current instanceof Canvas) {
				window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
				if (!actionBarEnabled) {
					actionBar.hide();
				} else {
					actionBar.setTitle(MyClassLoader.getName());
					layoutParams.height = (int) (getToolBarHeight() / 1.5);
				}
			} else {
				window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
				actionBar.show();
				actionBar.setTitle(current.getTitle());
				layoutParams.height = getToolBarHeight();
			}
			toolbar.setLayoutParams(layoutParams);
		}
	};

	private int getToolBarHeight() {
		int[] attrs = new int[]{R.attr.actionBarSize};
		TypedArray ta = obtainStyledAttributes(attrs);
		int toolBarHeight = ta.getDimensionPixelSize(0, -1);
		ta.recycle();
		return toolBarHeight;
	}

	public void setCurrent(Displayable disp) {
		current = disp;
		runOnUiThread(msgSetCurent);
	}

	public Displayable getCurrent() {
		return current;
	}

	public boolean isVisible() {
		return visible;
	}

	private void showExitConfirmation() {
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setTitle(R.string.CONFIRMATION_REQUIRED)
				.setMessage(R.string.FORCE_CLOSE_CONFIRMATION)
				.setPositiveButton(android.R.string.yes, (p1, p2) -> {
					Runnable r = () -> {
						try {
							Display.getDisplay(null).activityDestroyed();
						} catch (Throwable ex) {
							ex.printStackTrace();
						}
						ContextHolder.notifyDestroyed();
					};
					(new Thread(r)).start();
				})
				.setNegativeButton(android.R.string.no, null);
		alertBuilder.create().show();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (current != null) {
			menu.clear();
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.midlet, menu);
			for (Command cmd : current.getCommands()) {
				menu.add(Menu.NONE, cmd.hashCode(), Menu.NONE, cmd.getLabel());
			}
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (current != null) {
			int id = item.getItemId();
			if (item.getGroupId() == R.id.action_group_common_settings) {
				if (id == R.id.action_exit_midlet) {
					showExitConfirmation();
				} else if (current instanceof Canvas && ContextHolder.getVk() != null) {
					VirtualKeyboard vk = ContextHolder.getVk();
					switch (id) {
						case R.id.action_layout_edit_mode:
							vk.switchLayoutEditMode(VirtualKeyboard.LAYOUT_KEYS);
							break;
						case R.id.action_layout_scale_mode:
							vk.switchLayoutEditMode(VirtualKeyboard.LAYOUT_SCALES);
							break;
						case R.id.action_layout_edit_finish:
							vk.switchLayoutEditMode(VirtualKeyboard.LAYOUT_EOF);
							break;
						case R.id.action_layout_switch:
							vk.switchLayout();
							break;
						case R.id.action_hide_buttons:
							showHideButtonDialog();
							break;
					}
				}
				return true;
			}

			CommandListener listener = current.getCommandListener();
			if (listener == null) {
				return false;
			}

			for (Command cmd : current.getCommands()) {
				if (cmd.hashCode() == id) {
					current.postEvent(CommandActionEvent.getInstance(listener, cmd, current));
					return true;
				}
			}
		}

		return super.onOptionsItemSelected(item);
	}

	private void showHideButtonDialog() {
		final VirtualKeyboard vk = ContextHolder.getVk();
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(R.string.hide_buttons)
				.setMultiChoiceItems(vk.getKeyNames(), vk.getKeyVisibility(), (dialogInterface, i, b) -> vk.setKeyVisibility(i, b))
				.setPositiveButton(android.R.string.ok, null);
		builder.show();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (current instanceof Form) {
			((Form) current).contextMenuItemSelected(item);
		}

		return super.onContextItemSelected(item);
	}
}
