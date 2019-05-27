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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.util.LinkedHashMap;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.ViewHandler;
import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.lcdui.overlay.OverlayView;
import javax.microedition.lcdui.pointer.FixedKeyboard;
import javax.microedition.lcdui.pointer.VirtualKeyboard;
import javax.microedition.midlet.MIDlet;
import javax.microedition.util.ContextHolder;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.config.ConfigActivity;
import ru.playsoftware.j2meloader.util.LogUtils;

public class MicroActivity extends AppCompatActivity {
	private static final int ORIENTATION_DEFAULT = 0;
	private static final int ORIENTATION_AUTO = 1;
	private static final int ORIENTATION_PORTRAIT = 2;
	private static final int ORIENTATION_LANDSCAPE = 3;

	private Displayable current;
	private boolean visible;
	private boolean loaded;
	private boolean started;
	private boolean actionBarEnabled;
	private LinearLayout layout;
	private Toolbar toolbar;
	private MicroLoader microLoader;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		setTheme(sp.getString("pref_theme", "light"));
		super.onCreate(savedInstanceState);
		ContextHolder.setCurrentActivity(this);
		setContentView(R.layout.activity_micro);
		OverlayView overlayView = findViewById(R.id.vOverlay);
		VirtualKeyboard vk = ContextHolder.getVk();
		if (vk != null) {
			vk.setView(overlayView);
			overlayView.addLayer(vk);
		}
		layout = findViewById(R.id.displayable_container);
		toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		actionBarEnabled = sp.getBoolean("pref_actionbar_switch", false);
		boolean wakelockEnabled = sp.getBoolean("pref_wakelock_switch", false);
		if (wakelockEnabled) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		Intent intent = getIntent();
		if (ContextHolder.getVk() instanceof FixedKeyboard) {
			setOrientation(ORIENTATION_PORTRAIT);
		} else {
			int orientation = intent.getIntExtra(ConfigActivity.MIDLET_ORIENTATION_KEY, ORIENTATION_DEFAULT);
			setOrientation(orientation);
		}
		String pathToMidletDir = intent.getStringExtra(ConfigActivity.MIDLET_PATH_KEY);
		microLoader = new MicroLoader(this, pathToMidletDir);
		microLoader.init();
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

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && current instanceof Canvas) {
			hideSystemUI();
		}
	}

	private void setOrientation(int orientation) {
		switch (orientation) {
			case ORIENTATION_AUTO:
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
				break;
			case ORIENTATION_PORTRAIT:
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
				break;
			case ORIENTATION_LANDSCAPE:
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
				break;
			case ORIENTATION_DEFAULT:
			default:
				break;
		}
	}

	private void setTheme(String theme) {
		if (theme.equals("dark")) {
			setTheme(R.style.AppTheme_NoActionBar);
		} else {
			setTheme(R.style.AppTheme_Light_NoActionBar);
		}
	}

	private void loadMIDlet() throws Exception {
		LinkedHashMap<String, String> midlets = microLoader.loadMIDletList();
		int size = midlets.size();
		String[] midletsNameArray = midlets.values().toArray(new String[0]);
		String[] midletsClassArray = midlets.keySet().toArray(new String[0]);
		if (size == 0) {
			throw new Exception("No MIDlets found");
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
		try {
			MIDlet midlet = microLoader.loadMIDlet(mainClass);
			// Start midlet in Thread
			Runnable r = () -> {
				try {
					midlet.startApp();
					loaded = true;
				} catch (Throwable t) {
					t.printStackTrace();
					ContextHolder.notifyDestroyed();
				}
			};
			(new Thread(r, "MIDletLoader")).start();
		} catch (Throwable t) {
			t.printStackTrace();
			showErrorDialog(t.getMessage());
		}
	}

	private void showErrorDialog(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.error)
				.setMessage(message);
		builder.setOnCancelListener(dialogInterface -> ContextHolder.notifyDestroyed());
		builder.show();
	}

	private SimpleEvent msgSetCurrent = new SimpleEvent() {
		@Override
		public void process() {
			current.setParentActivity(MicroActivity.this);
			current.clearDisplayableView();
			layout.removeAllViews();
			layout.addView(current.getDisplayableView());
			invalidateOptionsMenu();
			ActionBar actionBar = getSupportActionBar();
			LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) toolbar.getLayoutParams();
			if (current instanceof Canvas) {
				hideSystemUI();
				if (!actionBarEnabled) {
					actionBar.hide();
				} else {
					actionBar.setTitle(MyClassLoader.getName());
					layoutParams.height = (int) (getToolBarHeight() / 1.5);
				}
			} else {
				showSystemUI();
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

	private void hideSystemUI() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			getWindow().getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
							| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_FULLSCREEN
							| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		} else {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

	private void showSystemUI() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

	public void setCurrent(Displayable disp) {
		current = disp;
		ViewHandler.postEvent(msgSetCurrent);
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
					(new Thread(r, "MIDletDestroyThread")).start();
				})
				.setNegativeButton(android.R.string.no, null);
		alertBuilder.create().show();
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_MENU && event.getAction() == KeyEvent.ACTION_DOWN) {
			onKeyDown(event.getKeyCode(), event);
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public void openOptionsMenu() {
		if (!actionBarEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && current instanceof Canvas) {
			showSystemUI();
		}
		super.openOptionsMenu();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
			case KeyEvent.KEYCODE_MENU:
				openOptionsMenu();
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (current != null) {
			menu.clear();
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.midlet_displayable, menu);
			if (current instanceof Canvas) {
				SubMenu group = menu.getItem(0).getSubMenu();
				inflater.inflate(R.menu.midlet_canvas_no_keys, group);
				VirtualKeyboard vk = ContextHolder.getVk();
				if (vk instanceof FixedKeyboard) {
					inflater.inflate(R.menu.midlet_canvas_fixed, group);
				} else if (vk != null) {
					inflater.inflate(R.menu.midlet_canvas, group);
				}
			}
			for (Command cmd : current.getCommands()) {
				menu.add(Menu.NONE, cmd.hashCode(), Menu.NONE, cmd.getAndroidLabel());
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
				} else if (id == R.id.action_take_screenshot) {
					takeScreenshot();
				} else if (id == R.id.action_save_log) {
					saveLog();
				} else if (ContextHolder.getVk() != null) {
					// Handled only when virtual keyboard is enabled
					handleVkOptions(id);
				}
				return true;
			}
			return current.menuItemSelected(id);
		}

		return super.onOptionsItemSelected(item);
	}

	private void handleVkOptions(int id) {
		VirtualKeyboard vk = ContextHolder.getVk();
		switch (id) {
			case R.id.action_layout_edit_mode:
				vk.setLayoutEditMode(VirtualKeyboard.LAYOUT_KEYS);
				Toast.makeText(this, R.string.layout_edit_mode,
						Toast.LENGTH_SHORT).show();
				break;
			case R.id.action_layout_scale_mode:
				vk.setLayoutEditMode(VirtualKeyboard.LAYOUT_SCALES);
				Toast.makeText(this, R.string.layout_scale_mode,
						Toast.LENGTH_SHORT).show();
				break;
			case R.id.action_layout_edit_finish:
				vk.setLayoutEditMode(VirtualKeyboard.LAYOUT_EOF);
				Toast.makeText(this, R.string.layout_edit_finished,
						Toast.LENGTH_SHORT).show();
				break;
			case R.id.action_layout_switch:
				vk.switchLayout();
				break;
			case R.id.action_hide_buttons:
				showHideButtonDialog();
				break;
		}
	}

	@SuppressLint("CheckResult")
	private void takeScreenshot() {
		microLoader.takeScreenshot((Canvas) current)
				.subscribeOn(Schedulers.computation())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribeWith(new SingleObserver<String>() {
					@Override
					public void onSubscribe(Disposable d) {
					}

					@Override
					public void onSuccess(String s) {
						Toast.makeText(MicroActivity.this, getString(R.string.screenshot_saved)
								+ " " + s, Toast.LENGTH_LONG).show();
					}

					@Override
					public void onError(Throwable e) {
						e.printStackTrace();
						Toast.makeText(MicroActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
					}
				});
	}

	private void saveLog() {
		try {
			LogUtils.writeLog();
			Toast.makeText(this, R.string.log_saved, Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
		}
	}

	private void showHideButtonDialog() {
		final VirtualKeyboard vk = ContextHolder.getVk();
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(R.string.hide_buttons)
				.setMultiChoiceItems(vk.getKeyNames(), vk.getKeyVisibility(),
						(dialogInterface, i, b) -> vk.setKeyVisibility(i, b))
				.setPositiveButton(android.R.string.ok, null);
		builder.show();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (current instanceof Form) {
			((Form) current).contextMenuItemSelected(item);
		} else if (current instanceof List) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			((List) current).contextMenuItemSelected(item, info.position);
		}

		return super.onContextItemSelected(item);
	}
}
