/*
 * Copyright 2012 Kulikov Dmitriy
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

package javax.microedition.lcdui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.util.ContextHolder;

public class MicroActivity extends Activity {
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
			Window wnd = getWindow();
			wnd.requestFeature(Window.FEATURE_NO_TITLE);
			wnd.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
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

	public void startActivity(Class cls, boolean isCanvas) {
		Intent intent = new Intent(this, cls);
		intent.putExtra(INTENT_PARAM_IS_CANVAS, isCanvas);
		startActivity(intent);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_MENU:
				return true;
			case KeyEvent.KEYCODE_BACK:
				openOptionsMenu();
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (current != null) {
			current.populateMenu(menu);
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
