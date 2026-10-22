/*
 * Copyright 2018-2019 Nikita Shakarun
 * Copyright 2020-2022 Yury Kharchenko
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import com.google.gson.GsonBuilder;

import java.io.File;
import java.util.ArrayList;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.keyboard.KeyMapper;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.base.BaseActivity;
import ru.playsoftware.j2meloader.config.ProfileModel;
import ru.playsoftware.j2meloader.config.ProfilesManager;
import ru.playsoftware.j2meloader.databinding.ActivityKeymapperBinding;
import ru.playsoftware.j2meloader.util.SparseIntArrayAdapter;

public class KeyMapperActivity extends BaseActivity implements View.OnClickListener {
	private static final String KEY_SAVE = "KEY_MAP_SAVE";
	private final SparseIntArray defaultKeyMap = KeyMapper.getDefaultKeyMap();
	private final SparseIntArray idToCanvasKey = new SparseIntArray();
	private final Rect popupRect = new Rect();
	private SparseIntArray androidToMIDP;
	private ProfileModel params;
	private int canvasKey;

	ActivityKeymapperBinding binding;
	ArrayList<ButtonMapping> virtualKeyboardMappingsList;
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		String path = intent.getDataString();
		if (path == null) {
			Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		binding = ActivityKeymapperBinding.inflate(getLayoutInflater());
		View view = binding.getRoot();
		setContentView(view);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(R.string.pref_map_keys);
		}
		params = ProfilesManager.loadConfig(new File(path));

		virtualKeyboardMappingsList = new ArrayList<>();
		addVirtualKeyboardMapping(binding.virtualKeyLeftSoft, Canvas.KEY_SOFT_LEFT);
		addVirtualKeyboardMapping(binding.virtualKeyRightSoft, Canvas.KEY_SOFT_RIGHT);
		addVirtualKeyboardMapping(binding.virtualKeyD, Canvas.KEY_SEND);
		addVirtualKeyboardMapping(binding.virtualKeyC, Canvas.KEY_END);
		addVirtualKeyboardMapping(binding.virtualKeyLeft, Canvas.KEY_LEFT);
		addVirtualKeyboardMapping(binding.virtualKeyRight, Canvas.KEY_RIGHT);
		addVirtualKeyboardMapping(binding.virtualKeyUp, Canvas.KEY_UP);
		addVirtualKeyboardMapping(binding.virtualKeyDown, Canvas.KEY_DOWN);
		addVirtualKeyboardMapping(binding.virtualKeyF, Canvas.KEY_FIRE);
		addVirtualKeyboardMapping(binding.virtualKey1, Canvas.KEY_NUM1);
		addVirtualKeyboardMapping(binding.virtualKey2, Canvas.KEY_NUM2);
		addVirtualKeyboardMapping(binding.virtualKey3, Canvas.KEY_NUM3);
		addVirtualKeyboardMapping(binding.virtualKey4, Canvas.KEY_NUM4);
		addVirtualKeyboardMapping(binding.virtualKey5, Canvas.KEY_NUM5);
		addVirtualKeyboardMapping(binding.virtualKey6, Canvas.KEY_NUM6);
		addVirtualKeyboardMapping(binding.virtualKey7, Canvas.KEY_NUM7);
		addVirtualKeyboardMapping(binding.virtualKey8, Canvas.KEY_NUM8);
		addVirtualKeyboardMapping(binding.virtualKey9, Canvas.KEY_NUM9);
		addVirtualKeyboardMapping(binding.virtualKey0,Canvas.KEY_NUM0);
		addVirtualKeyboardMapping(binding.virtualKeyStar, Canvas.KEY_STAR);
		addVirtualKeyboardMapping(binding.virtualKeyPound, Canvas.KEY_POUND);
		addVirtualKeyboardMapping(binding.virtualKeyA, KeyMapper.SE_KEY_SPECIAL_GAMING_A);
		addVirtualKeyboardMapping(binding.virtualKeyB, KeyMapper.SE_KEY_SPECIAL_GAMING_B);
		addVirtualKeyboardMapping(binding.virtualKeyMenu, KeyMapper.KEY_OPTIONS_MENU);

		for (ButtonMapping mapping : virtualKeyboardMappingsList) {
			setupButton(mapping.button, mapping.keyId);
		}

		if (savedInstanceState == null) {
			SparseIntArray keyMap = params.keyMappings;
			androidToMIDP = keyMap == null ? defaultKeyMap.clone() : keyMap.clone();
		} else {
			String save = savedInstanceState.getString(KEY_SAVE);
			if (save == null) {
				androidToMIDP = defaultKeyMap.clone();
			} else if (save.isEmpty()) {
				SparseIntArray keyMap = params.keyMappings;
				androidToMIDP = keyMap == null ? defaultKeyMap.clone() : keyMap.clone();
			} else {
				androidToMIDP = new GsonBuilder()
						.registerTypeAdapter(SparseIntArray.class, new SparseIntArrayAdapter())
						.create()
						.fromJson(save, SparseIntArray.class);
			}
		}
	}

	static class ButtonMapping {
		Button button;
		Integer keyId;

		public ButtonMapping(Button button, Integer keyId) {
			this.button = button;this.keyId = keyId;
		}
	}

	void addVirtualKeyboardMapping(Button button, Integer keyId) {
		virtualKeyboardMappingsList.add(new ButtonMapping(button, keyId));
	}

	private void setupButton(Button button, int index) {
		idToCanvasKey.put(button.getId(), index);
		button.setOnClickListener(this);
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		if (!equalMaps(androidToMIDP, defaultKeyMap)) {
			if (!equalMaps(params.keyMappings, androidToMIDP)) {
				String currMap = new GsonBuilder()
						.registerTypeAdapter(SparseIntArray.class, new SparseIntArrayAdapter())
						.create()
						.toJson(androidToMIDP);
				outState.putString(KEY_SAVE, currMap);
			} else {
				outState.putString(KEY_SAVE, "");
			}
		}

		super.onSaveInstanceState(outState);
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
		binding.keyMapperPopupMsg.setText(getString(R.string.mapping_dialog_message, keyName));
		binding.keyMapperLayer.setVisibility(View.VISIBLE);
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
		if (androidToMIDP.indexOfValue(KeyMapper.KEY_OPTIONS_MENU) < 0) {
			alertMenuKey();
			return;
		}
		save();
		super.onBackPressed();
	}

	private void save() {
		SparseIntArray newMap = androidToMIDP;
		SparseIntArray oldMap = params.keyMappings;
		if (equalMaps(newMap, defaultKeyMap)) {
			newMap = null;
		}
		if (!equalMaps(oldMap, newMap)) {
			params.keyMappings = newMap;
			ProfilesManager.saveConfig(params);
		}
	}

	private void alertMenuKey() {
		new AlertDialog.Builder(this)
				.setMessage(R.string.alert_map_menu)
				.setTitle(R.string.warning)
				.setNegativeButton(R.string.save, (d, w) -> {
					save();
					super.onBackPressed();
				})
				.setPositiveButton(R.string.CANCEL_CMD, null)
				.show();
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
		if (binding.keyMapperLayer.getVisibility() == View.VISIBLE
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
					binding.keyMapperLayer.setVisibility(View.GONE);
					return true;
			}
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (binding.keyMapperLayer.getVisibility() == View.VISIBLE && event.getAction() == MotionEvent.ACTION_DOWN) {
			binding.keyMapperPopup.getGlobalVisibleRect(popupRect);
			if (!popupRect.contains(((int) event.getX()), ((int) event.getY()))) {
				binding.keyMapperLayer.setVisibility(View.GONE);
			}
			return true;
		}
		return super.dispatchTouchEvent(event);
	}

	@Override
	protected void onDestroy() {
		binding = null;
		super.onDestroy();
	}
}
