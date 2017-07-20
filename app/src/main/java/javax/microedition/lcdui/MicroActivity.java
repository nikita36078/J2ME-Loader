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
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.util.ContextHolder;

public class MicroActivity extends Activity {
	private Displayable current;
	private boolean visible;

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

		if (disp != null) {
			runOnUiThread(msgSetCurent);
		} else {
			ContextHolder.notifyPaused();
		}
	}

	public Displayable getCurrent() {
		return current;
	}

	public void setFullScreenMode() {
		Window wnd = getWindow();
		wnd.requestFeature(Window.FEATURE_NO_TITLE);
		wnd.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	public boolean isVisible() {
		return visible;
	}

	public void startActivity(Class cls) {
		Intent intent = new Intent(this, cls);
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(intent);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ContextHolder.addActivityToPool(this);
	}

	public void onResume() {
		super.onResume();

		ContextHolder.setCurrentActivity(this);
		visible = true;
	}

	public void onPause() {
		ContextHolder.setCurrentActivity(null);
		visible = false;

		super.onPause();
	}

	public void onDestroy() {
		if (current != null) {
			current.setParentActivity(null);
		}

		ContextHolder.compactActivityPool(this);
		super.onDestroy();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		ContextHolder.notifyOnActivityResult(requestCode, resultCode, data);
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		if (current != null) {
			current.populateMenu(menu);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		if (current != null) {
			current.menuItemSelected(item);
		}

		return super.onOptionsItemSelected(item);
	}

	public boolean onContextItemSelected(MenuItem item) {
		if (current instanceof Form) {
			((Form) current).contextMenuItemSelected(item);
		}

		return super.onContextItemSelected(item);
	}
}
