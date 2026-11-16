/*
 *  Copyright 2026 Nikita Shakarun
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.playsoftware.j2meloader.config;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.util.Arrays;

import javax.microedition.lcdui.keyboard.VirtualKeyboard;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.base.BaseActivity;

public class KeyboardLayoutActivity extends BaseActivity {
	private VirtualKeyboard vk;
	private KeyboardLayoutView view;
	private boolean modified;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		String path = intent.getDataString();
		if (path == null) {
			Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		File dir = new File(path);
		ProfileModel params = ProfilesManager.loadConfig(dir);
		if (params == null) {
			params = new ProfileModel(dir);
		}
		params.vkHideDelay = 0;

		vk = new VirtualKeyboard(params);
		view = new KeyboardLayoutView(this, vk, params);
		view.setOnModifiedListener(() -> modified = true);
		setContentView(view);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(R.string.key_layout_title);
		}

		vk.setLayoutEditMode(VirtualKeyboard.LAYOUT_KEYS);
		view.refresh();
		Toast.makeText(this, R.string.layout_edit_mode, Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.keyboard_layout, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			onBackPressed();
		} else if (itemId == R.id.action_layout_edit_mode) {
			startEditing(VirtualKeyboard.LAYOUT_KEYS, R.string.layout_edit_mode);
		} else if (itemId == R.id.action_layout_scale_mode) {
			startEditing(VirtualKeyboard.LAYOUT_SCALES, R.string.layout_scale_mode);
		} else if (itemId == R.id.action_hide_buttons) {
			showHideButtonsDialog();
		} else if (itemId == R.id.action_layout_switch) {
			showLayoutDialog();
		} else if (itemId == R.id.action_layout_save) {
			save();
			finish();
		}
		return super.onOptionsItemSelected(item);
	}

	private void startEditing(int mode, int message) {
		modified = true;
		vk.setLayoutEditMode(mode);
		view.refresh();
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	private void showHideButtonsDialog() {
		boolean[] states = vk.getKeysVisibility();
		boolean[] changed = states.clone();
		new AlertDialog.Builder(this)
				.setTitle(R.string.hide_buttons)
				.setMultiChoiceItems(vk.getKeyNames(), changed, (dialog, which, isChecked) -> {})
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					if (!Arrays.equals(states, changed)) {
						vk.setKeysVisibility(changed);
						modified = true;
						save();
						view.invalidate();
					}
				}).show();
	}

	private void showLayoutDialog() {
		int current = vk.getLayout();
		new AlertDialog.Builder(this)
				.setTitle(R.string.layout_switch)
				.setSingleChoiceItems(R.array.PREF_VK_TYPE_ENTRIES, current, null)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
					if (position < 0 || position == vk.getLayout()) {
						return;
					}
					save();
					vk.setLayout(position);
					modified = false;
					view.refresh();
				}).show();
	}

	private void save() {
		if (modified) {
			modified = false;
			vk.onLayoutChanged(VirtualKeyboard.TYPE_CUSTOM);
			view.refresh();
		}
	}

	@Override
	public void onBackPressed() {
		save();
		super.onBackPressed();
	}

	@Override
	protected void onPause() {
		save();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if (vk != null) {
			vk.cancel();
		}
		super.onDestroy();
	}
}
