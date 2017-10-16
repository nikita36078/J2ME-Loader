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
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.midlet.MIDlet;
import javax.microedition.util.ContextHolder;

import ua.naiksoftware.j2meloader.R;

public class MicroActivity extends AppCompatActivity {
	public final static String INTENT_PARAM_IS_CANVAS = "isCanvas";
	private Displayable current;
	private boolean visible;
	private boolean isCanvas;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ContextHolder.addActivityToPool(this);
		isCanvas = getIntent().getBooleanExtra(INTENT_PARAM_IS_CANVAS, false);
		if (isCanvas) {
			setTheme(R.style.AppTheme_Fullscreen);
		} else {
			setTheme(R.style.AppTheme);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		ContextHolder.setCurrentActivity(this);
		visible = true;
		Display.getDisplay(null).changeActivity(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		ContextHolder.setCurrentActivity(null);
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
		ContextHolder.compactActivityPool(this);
		super.onDestroy();
	}

	public boolean isCanvas() {
		return isCanvas;
	}

	private SimpleEvent msgSetCurent = new SimpleEvent() {
		public void process() {
			current.setParentActivity(MicroActivity.this);
			setTitle(current.getTitle());
			setContentView(current.getDisplayableView());
		}
	};

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
									MIDlet.callDestroyApp(true);
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

	public void startActivity(Class cls, boolean isCanvas) {
		Intent intent = new Intent(this, cls);
		intent.putExtra(INTENT_PARAM_IS_CANVAS, isCanvas);
		startActivity(intent);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (current != null) {
			menu.clear();
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.midlet, menu);
			for (Command cmd : current.getCommands()) {
				menu.add(Menu.NONE, cmd.hashCode(), cmd.getPriority(), cmd.getLabel());
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
