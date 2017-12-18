/*
 * J2ME Loader
 * Copyright (C) 2017 Nikita Shakarun
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package javax.microedition.shell;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.io.File;
import java.util.LinkedHashMap;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.midlet.MIDlet;
import javax.microedition.util.ContextHolder;

import ua.naiksoftware.j2meloader.R;
import ua.naiksoftware.util.FileUtils;
import ua.naiksoftware.util.Log;

public class MicroActivity extends AppCompatActivity {
	private Displayable current;
	private boolean visible;
	private LinearLayout layout;
	private Toolbar toolbar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_micro);
		ContextHolder.setCurrentActivity(this);
		layout = findViewById(R.id.displayable_container);
		toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		String path = getIntent().getStringExtra(ConfigActivity.MIDLET_PATH);
		try {
			loadMIDlet(path).startApp();
		} catch (Exception e) {
			e.printStackTrace();
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.error)
					.setMessage(e.getMessage());
			builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialogInterface) {
					finish();
				}
			});
			builder.show();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		visible = true;
		Display.getDisplay(null).activityResumed();
	}

	@Override
	public void onPause() {
		super.onPause();
		visible = false;
	}

	@Override
	protected void onStop() {
		super.onStop();
		Display.getDisplay(null).activityStopped();
	}

	private MIDlet loadMIDlet(String pathToMidletDir) {
		MIDlet midlet = null;
		LinkedHashMap<String, String> params = FileUtils.loadManifest(new File(
				pathToMidletDir + ConfigActivity.MIDLET_CONF_FILE));
		MIDlet.initProps(params);
		String dex = pathToMidletDir + ConfigActivity.MIDLET_DEX_FILE;
		ClassLoader loader = new MyClassLoader(dex,
				getApplicationInfo().dataDir, null, getClassLoader(), pathToMidletDir + ConfigActivity.MIDLET_RES_DIR);
		try {
			String mainClassParam = params.get("MIDlet-1");
			String mainClass = mainClassParam.substring(
					mainClassParam.lastIndexOf(',') + 1).trim();
			Log.d("inf", "load main: " + mainClass + " from dex:" + dex);
			midlet = (MIDlet) loader.loadClass(mainClass).newInstance();
		} catch (ClassNotFoundException ex) {
			Log.d("err", ex.toString() + "/n" + ex.getMessage());
		} catch (InstantiationException ex) {
			Log.d("err", ex.toString() + "/n" + ex.getMessage());
		} catch (IllegalAccessException ex) {
			Log.d("err", ex.toString() + "/n" + ex.getMessage());
		}
		return midlet;
	}

	private SimpleEvent msgSetCurent = new SimpleEvent() {
		public void process() {
			current.setParentActivity(MicroActivity.this);
			layout.removeAllViews();
			layout.addView(current.getDisplayableView());
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			boolean isActionBarEnabled = sp.getBoolean("pref_actionbar_switch", false);
			Window window = getWindow();
			ActionBar actionBar = getSupportActionBar();
			LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) toolbar.getLayoutParams();
			if (current instanceof Canvas) {
				window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
				if (!isActionBarEnabled) {
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

	public int getToolBarHeight() {
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

	public void showExitConfirmation() {
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setTitle(R.string.CONFIRMATION_REQUIRED)
				.setMessage(R.string.FORCE_CLOSE_CONFIRMATION)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface p1, int p2) {
						Runnable r = new Runnable() {
							public void run() {
								try {
									Display.getDisplay(null).activityDestroyed();
								} catch (Throwable ex) {
									ex.printStackTrace();
								}
								ContextHolder.notifyDestroyed();
							}
						};
						(new Thread(r)).start();
					}
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
			current.menuItemSelected(item);
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (current instanceof Form) {
			((Form) current).contextMenuItemSelected(item);
		}

		return super.onContextItemSelected(item);
	}
}
