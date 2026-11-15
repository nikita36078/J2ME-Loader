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

import static javax.microedition.lcdui.Canvas.*;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.base.BaseActivity;
import ru.playsoftware.j2meloader.databinding.ActivityKeyCodesBinding;
import ru.playsoftware.j2meloader.databinding.ItemKeyCodeBinding;

@SuppressLint("SetTextI18n")
public class KeyCodesActivity extends BaseActivity {
	private static final String KEY_VALUES = "KEY_VALUES";

	private static final int[] DEFAULT_CODES = {
			KEY_SOFT_LEFT, KEY_SOFT_RIGHT,
			KEY_UP, KEY_DOWN, KEY_LEFT, KEY_RIGHT,
			KEY_FIRE, KEY_SEND, KEY_END,
			KEY_NUM1, KEY_NUM2, KEY_NUM3, KEY_NUM4, KEY_NUM5,
			KEY_NUM6, KEY_NUM7, KEY_NUM8, KEY_NUM9, KEY_NUM0,
			KEY_STAR, KEY_POUND
	};

	private static final int[] LABELS = {
			R.string.virtual_key_left_soft, R.string.virtual_key_right_soft,
			R.string.virtual_key_up, R.string.virtual_key_down,
			R.string.virtual_key_left, R.string.virtual_key_right,
			R.string.virtual_key_f, R.string.virtual_key_d, R.string.virtual_key_c,
			R.string.virtual_key_1, R.string.virtual_key_2,
			R.string.virtual_key_3, R.string.virtual_key_4,
			R.string.virtual_key_5, R.string.virtual_key_6,
			R.string.virtual_key_7, R.string.virtual_key_8,
			R.string.virtual_key_9, R.string.virtual_key_0,
			R.string.virtual_key_star, R.string.virtual_key_pound
	};

	private final EditText[] editTexts = new EditText[DEFAULT_CODES.length];
	private ActivityKeyCodesBinding binding;
	private ProfileModel params;

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
		params = ProfilesManager.loadConfig(new File(path));
		if (params == null) {
			Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		binding = ActivityKeyCodesBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(R.string.key_code_settings);
		}

		LayoutInflater inflater = getLayoutInflater();
		String[] restoredValues = savedInstanceState == null
				? null : savedInstanceState.getStringArray(KEY_VALUES);
		for (int i = 0; i < DEFAULT_CODES.length; i++) {
			ItemKeyCodeBinding item = ItemKeyCodeBinding.inflate(inflater, binding.container, false);
			item.keyName.setText(LABELS[i]);
			EditText editText = item.keyCode;
			String value = restoredValues == null ? Integer.toString(getKeyCode(DEFAULT_CODES[i]))
					: restoredValues[i];
			editText.setText(value);
			editTexts[i] = editText;
			binding.container.addView(item.getRoot());
		}
	}

	private int getKeyCode(int defaultCode) {
		SparseIntArray customKeys = params.keyCodeMap;
		return customKeys == null ? defaultCode : customKeys.get(defaultCode, defaultCode);
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		String[] values = new String[editTexts.length];
		for (int i = 0; i < editTexts.length; i++) {
			EditText editText = editTexts[i];
			values[i] = editText == null ? Integer.toString(DEFAULT_CODES[i])
					: editText.getText().toString();
		}
		outState.putStringArray(KEY_VALUES, values);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.key_codes, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			onBackPressed();
		} else if (itemId == R.id.action_apply_codes) {
			if (save()) {
				finish();
			}
		} else if (itemId == R.id.action_reset_codes) {
			resetToDefaults();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		if (save()) {
			super.onBackPressed();
		}
	}

	private void resetToDefaults() {
		for (int i = 0; i < DEFAULT_CODES.length; i++) {
			editTexts[i].setText(Integer.toString(DEFAULT_CODES[i]));
		}
	}

	private boolean save() {
		int[] values = new int[DEFAULT_CODES.length];
		Set<Integer> changeCodes = new HashSet<>(DEFAULT_CODES.length);
		for (int i = 0; i < DEFAULT_CODES.length; i++) {
			int value;
			String text = editTexts[i].getText().toString().trim();
			if (text.isEmpty()) {
				value = DEFAULT_CODES[i];
			} else {
				try {
					value = Integer.parseInt(text);
				} catch (NumberFormatException e) {
					Toast.makeText(this, R.string.key_code_invalid, Toast.LENGTH_SHORT).show();
					editTexts[i].requestFocus();
					return false;
				}
			}
			if (!changeCodes.add(value)) {
				String message = getString(R.string.key_code_duplicate, value);
				Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
				editTexts[i].requestFocus();
				return false;
			}
			values[i] = value;
		}
		SparseIntArray customKeys = null;
		for (int i = 0; i < DEFAULT_CODES.length; i++) {
			if (values[i] != DEFAULT_CODES[i]) {
				if (customKeys == null) {
					customKeys = new SparseIntArray();
				}
				customKeys.put(DEFAULT_CODES[i], values[i]);
			}
		}
		params.keyCodeMap = customKeys;
		ProfilesManager.saveConfig(params);
		return true;
	}

	@Override
	protected void onDestroy() {
		binding = null;
		super.onDestroy();
	}
}
