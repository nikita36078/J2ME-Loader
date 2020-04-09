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
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

import javax.microedition.lcdui.Canvas;
import javax.microedition.util.param.SharedPreferencesContainer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.base.BaseActivity;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.config.ConfigActivity;

public class KeyMapperActivity extends BaseActivity implements View.OnClickListener {
	private static SparseIntArray idToCanvasKey = new SparseIntArray();
	private static SparseIntArray androidToMIDP;
	private SharedPreferencesContainer params;

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
		String dirName = intent.getDataString();
		if (dirName == null) {
			Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		File configDir;
		boolean isProfile = ConfigActivity.ACTION_EDIT_PROFILE.equals(intent.getAction());
		if (isProfile) {
			configDir = new File(Config.getProfilesDir(), dirName);
		} else {
			configDir = new File(Config.getConfigsDir(), dirName);
		}
		params = new SharedPreferencesContainer(configDir);
		params.load();

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
		androidToMIDP = KeyMapper.getArrayPref(params);
	}

	private void setupButton(int resId, int index) {
		idToCanvasKey.put(resId, index);
		Button button = findViewById(resId);
		button.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		int canvasKey = idToCanvasKey.get(v.getId());
		if (canvasKey != 0) {
			showMappingDialog(canvasKey);
		}
	}

	private void showMappingDialog(int canvasKey) {
		int id = androidToMIDP.indexOfValue(canvasKey);
		String keyName = "";
		if (id >= 0) {
			keyName = KeyEvent.keyCodeToString(androidToMIDP.keyAt(id));
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(R.string.mapping_dialog_title)
				.setMessage(getString(R.string.mapping_dialog_message, keyName))
				.setOnKeyListener((dialog, keyCode, event) -> {
					if (keyCode == KeyEvent.KEYCODE_BACK) {
						dialog.dismiss();
						return false;
					} else {
						deleteDuplicates(canvasKey);
						androidToMIDP.put(keyCode, canvasKey);
						KeyMapper.saveArrayPref(params, androidToMIDP);
						dialog.dismiss();
						return true;
					}
				});
		builder.show();
	}

	private void deleteDuplicates(int value) {
		for (int i = 0; i < androidToMIDP.size(); i++) {
			if (androidToMIDP.indexOfValue(value) == i) {
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
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				break;
			case R.id.action_reset_mapping:
				androidToMIDP.clear();
				KeyMapper.initArray(androidToMIDP);
				KeyMapper.saveArrayPref(params, androidToMIDP);
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}
