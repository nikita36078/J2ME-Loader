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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.util.ContextHolder;

import ua.naiksoftware.j2meloader.R;

public class MicroActivity extends AppCompatActivity {
	private Displayable current;
	private boolean visible;
	private LinearLayout layout;
	Toolbar toolbar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_micro);
		layout = findViewById(R.id.displayable_container);
		toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		ContextHolder.setCurrentActivity(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		visible = true;
		Display.getDisplay(null).changeActivity(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		visible = false;
	}

	@Override
	protected void onStop() {
		super.onStop();
		Display.getDisplay(null).activityStopped(this);
	}

	@Override
	public void onDestroy() {
		if (current != null) {
			current.setParentActivity(null);
		}
		super.onDestroy();
	}

	private SimpleEvent msgSetCurent = new SimpleEvent() {
		public void process() {
			current.setParentActivity(MicroActivity.this);
			setTitle(current.getTitle());
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
					layoutParams.height = 52;
				}
			} else {
				window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
				actionBar.show();
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
		if (current != null) {
			current.setParentActivity(null);
		}
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
