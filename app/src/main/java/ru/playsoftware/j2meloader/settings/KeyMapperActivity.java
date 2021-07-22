/*
 * Copyright 2018 Nikita Shakarun
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

package ru.playsoftware.j2meloader.settings;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.keyboard.KeyMapper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.base.BaseActivity;
import ru.playsoftware.j2meloader.config.ProfileModel;
import ru.playsoftware.j2meloader.config.ProfilesManager;

public class KeyMapperActivity extends BaseActivity implements View.OnClickListener {
	private final SparseIntArray defaultKeyMap = KeyMapper.getDefaultKeyMap();
	private final SparseIntArray idToCanvasKey = new SparseIntArray();
	private final Rect popupRect = new Rect();
	private SparseIntArray androidToMIDP;
	private ProfileModel params;
	private View popupLayout;
	private TextView popupMsg;
	private View keyMapperLayer;
	private int canvasKey;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_keymapper);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(R.string.pref_map_keys);
		}
		Intent intent = getIntent();
		String path = intent.getDataString();
		if (path == null) {
			Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		params = ProfilesManager.loadConfig(new File(path));

		keyMapperLayer = findViewById(R.id.keyMapperLayer);
		popupLayout = findViewById(R.id.keyMapperPopup);
		popupMsg = findViewById(R.id.keyMapperPopupMsg);

		setupButton(R.id.virtual_key_left_soft, Canvas.KEY_SOFT_LEFT);
		setupButton(R.id.virtual_key_right_soft, Canvas.KEY_SOFT_RIGHT);
		setupButton(R.id.virtual_key_d, Canvas.KEY_SEND);
		setupButton(R.id.virtual_key_c, Canvas.KEY_END);
		setupButton(R.id.virtual_key_left, Canvas.KEY_LEFT);
		setupButton(R.id.virtual_key_right, Canvas.KEY_RIGHT);
		setupButton(R.id.virtual_key_up, Canvas.KEY_UP);
		setupButton(R.id.virtual_key_down, Canvas.KEY_DOWN);
		setupButton(R.id.virtual_key_f, Canvas.KEY_FIRE);
		setupButton(R.id.virtual_key_1, Canvas.KEY_NUM1);
		setupButton(R.id.virtual_key_2, Canvas.KEY_NUM2);
		setupButton(R.id.virtual_key_3, Canvas.KEY_NUM3);
		setupButton(R.id.virtual_key_4, Canvas.KEY_NUM4);
		setupButton(R.id.virtual_key_5, Canvas.KEY_NUM5);
		setupButton(R.id.virtual_key_6, Canvas.KEY_NUM6);
		setupButton(R.id.virtual_key_7, Canvas.KEY_NUM7);
		setupButton(R.id.virtual_key_8, Canvas.KEY_NUM8);
		setupButton(R.id.virtual_key_9, Canvas.KEY_NUM9);
		setupButton(R.id.virtual_key_0, Canvas.KEY_NUM0);
		setupButton(R.id.virtual_key_star, Canvas.KEY_STAR);
		setupButton(R.id.virtual_key_pound, Canvas.KEY_POUND);
		setupButton(R.id.virtual_key_a, KeyMapper.SE_KEY_SPECIAL_GAMING_A);
		setupButton(R.id.virtual_key_b, KeyMapper.SE_KEY_SPECIAL_GAMING_B);
		setupButton(R.id.virtual_key_menu, KeyMapper.KEY_OPTIONS_MENU);
		SparseIntArray keyMap = params.keyMappings;
		androidToMIDP = keyMap == null ? defaultKeyMap.clone() : keyMap.clone();
	}

	private void setupButton(int resId, int index) {
		idToCanvasKey.put(resId, index);
		Button button = findViewById(resId);
		button.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		int canvasKey = idToCanvasKey.get(v.getId());
		showMappingDialog(canvasKey);
	}

	private void showMappingDialog(int canvasKey) {
		this.canvasKey = canvasKey;
		SparseIntArray androidToMIDP = this.androidToMIDP;
		int idx = androidToMIDP.indexOfValue(canvasKey);
		String keyName;
		if (idx < 0) {
			keyName = getString(R.string.mapping_dialog_key_not_specified);
		} else {
			keyName = KeyEvent.keyCodeToString(androidToMIDP.keyAt(idx));
		}
		popupMsg.setText(getString(R.string.mapping_dialog_message, keyName));
		keyMapperLayer.setVisibility(View.VISIBLE);
	}

	private void deleteDuplicates(int value) {
		SparseIntArray androidToMIDP = this.androidToMIDP;
		for (int i = androidToMIDP.size() - 1; i >= 0; i--) {
			if (androidToMIDP.valueAt(i) == value) {
				androidToMIDP.removeAt(i);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.keymapper, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			onBackPressed();
		} else if (itemId == R.id.action_reset_mapping) {
			androidToMIDP = defaultKeyMap.clone();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		SparseIntArray newMap = androidToMIDP;
		if (newMap.indexOfValue(KeyMapper.KEY_OPTIONS_MENU) < 0) {
			Toast.makeText(this, R.string.alert_map_menu, Toast.LENGTH_SHORT).show();
			return;
		}
		SparseIntArray oldMap = params.keyMappings;
		if (equalMaps(newMap, defaultKeyMap)) {
			newMap = null;
		}
		if (!equalMaps(oldMap, newMap)) {
			params.keyMappings = newMap;
			ProfilesManager.saveConfig(params);
		}
		super.onBackPressed();
	}

	private boolean equalMaps(SparseIntArray map1, SparseIntArray map2) {
		if (map1 == map2) {
			return true;
		}
		if (map1 == null || map2 == null || map1.size() != map2.size()) {
			return false;
		}
		for (int i = 0, size = map1.size(); i < size; i++) {
			if (map2.keyAt(i) != map1.keyAt(i) ||
					map2.valueAt(i) != map1.valueAt(i)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (keyMapperLayer.getVisibility() == View.VISIBLE
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			int keyCode = event.getKeyCode();
			switch (keyCode) {
				case KeyEvent.KEYCODE_HOME:
				case KeyEvent.KEYCODE_VOLUME_UP:
				case KeyEvent.KEYCODE_VOLUME_DOWN:
					break;
				default:
					deleteDuplicates(canvasKey);
					androidToMIDP.put(keyCode, canvasKey);
					keyMapperLayer.setVisibility(View.GONE);
					return true;
			}
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (keyMapperLayer.getVisibility() == View.VISIBLE && event.getAction() == MotionEvent.ACTION_DOWN) {
			popupLayout.getGlobalVisibleRect(popupRect);
			if (!popupRect.contains(((int) event.getX()), ((int) event.getY()))) {
				keyMapperLayer.setVisibility(View.GONE);
			}
			return true;
		}
		return super.dispatchTouchEvent(event);
	}
}
