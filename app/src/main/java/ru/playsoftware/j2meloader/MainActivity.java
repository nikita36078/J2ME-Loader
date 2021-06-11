/*
 * Copyright 2015-2016 Nickolay Savchenko
 * Copyright 2017-2018 Nikita Shakarun
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

package ru.playsoftware.j2meloader;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.filepicker.Utils;

import java.io.File;
import java.util.Map;

import ru.playsoftware.j2meloader.applist.AppsListFragment;
import ru.playsoftware.j2meloader.base.BaseActivity;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.util.MigrationUtils;
import ru.playsoftware.j2meloader.util.PickDirResultContract;
import ru.playsoftware.j2meloader.util.SettingsResultContract;

import static ru.playsoftware.j2meloader.util.Constants.PREF_EMULATOR_DIR;
import static ru.playsoftware.j2meloader.util.Constants.PREF_TOOLBAR;

public class MainActivity extends BaseActivity {
	private static final String[] STORAGE_PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

	private SharedPreferences sp;
	private String emulatorDir;
	private final ActivityResultLauncher<String[]> permissionsLauncher = registerForActivityResult(
			new ActivityResultContracts.RequestMultiplePermissions(),
			this::onPermissionResult);
	private final ActivityResultLauncher<String> openDirLauncher = registerForActivityResult(
			new PickDirResultContract(),
			this::onPickDirResult);
	private final ActivityResultLauncher<Boolean> settingsLauncher = registerForActivityResult(
			new SettingsResultContract(),
			this::onSettingsResult);
	private boolean isIntentUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Intent intent = getIntent();
		Uri uri = intent.getData();
		if (!isTaskRoot() && uri == null) {
			finish();
			return;
		}
		isIntentUri = (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0 && uri != null;
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		permissionsLauncher.launch(STORAGE_PERMISSIONS);
	}

	private void setupActivity() {
		if (!initFolders()) {
			String msg = getString(R.string.create_apps_dir_failed, emulatorDir);
			new AlertDialog.Builder(this)
					.setTitle(R.string.error)
					.setCancelable(false)
					.setMessage(msg)
					.setNegativeButton(R.string.close, (d, w) -> finish())
					.setPositiveButton(R.string.action_settings, (d, w) -> openDirLauncher.launch(null))
					.show();
			return;
		}
		checkActionBar();
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		MigrationUtils.check(this);
		Uri data = isIntentUri ? getIntent().getData() : null;
		AppsListFragment fragment = AppsListFragment.newInstance(data);
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.container, fragment).commitNowAllowingStateLoss();
	}

	private boolean initFolders() {
		emulatorDir = Config.getEmulatorDir();
		File appsDir = new File(emulatorDir);
		File nomedia = new File(appsDir, ".nomedia");
		if (appsDir.isDirectory() || appsDir.mkdirs()) {
			//noinspection ResultOfMethodCallIgnored
			new File(Config.getShadersDir()).mkdir();
			try {
				//noinspection ResultOfMethodCallIgnored
				nomedia.createNewFile();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		}
		return false;
	}

	private void checkActionBar() {
		if (sp.getAll().get(PREF_TOOLBAR) == null) {
			boolean enable = !ViewConfiguration.get(this).hasPermanentMenuKey();
			sp.edit().putBoolean(PREF_TOOLBAR, enable).apply();
		}
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == R.id.action_settings) {
			settingsLauncher.launch(false);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void onPermissionResult(Map<String, Boolean> status) {
		if (!status.containsValue(false)) {
			setupActivity();
		} else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			showRequestPermissionRationale();
		} else {
			Toast.makeText(this, R.string.permission_request_failed, Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	private void showRequestPermissionRationale() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.error)
				.setCancelable(false)
				.setMessage(R.string.permission_request_failed)
				.setNegativeButton(R.string.retry, (d, w) ->
						permissionsLauncher.launch(STORAGE_PERMISSIONS))
				.setPositiveButton(R.string.exit, (d, w) -> finish())
				.show();
	}

	private void onSettingsResult(Boolean needRecreate) {
		if (needRecreate) {
			ActivityCompat.recreate(this);
		}
	}

	private void applyChangeFolder(File file) {
		String path = file.getAbsolutePath();
		if (path.equals(sp.getString(PREF_EMULATOR_DIR, null))) {
			return;
		}
		sp.edit().putString(PREF_EMULATOR_DIR, path).apply();
		ActivityCompat.recreate(this);
	}

	private void onPickDirResult(Uri uri) {
		if (uri == null) {
			return;
		}
		File file = Utils.getFileForUri(uri);
		applyChangeFolder(file);
	}

}
