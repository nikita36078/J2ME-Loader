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
import android.view.ViewConfiguration;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.filepicker.Utils;

import java.io.File;
import java.util.Map;

import ru.playsoftware.j2meloader.applist.AppListModel;
import ru.playsoftware.j2meloader.applist.AppsListFragment;
import ru.playsoftware.j2meloader.base.BaseActivity;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.util.FileUtils;
import ru.playsoftware.j2meloader.util.PickDirResultContract;
import ru.woesss.j2me.installer.InstallerDialog;

import static ru.playsoftware.j2meloader.util.Constants.PREF_EMULATOR_DIR;
import static ru.playsoftware.j2meloader.util.Constants.PREF_STORAGE_WARNING_SHOWN;
import static ru.playsoftware.j2meloader.util.Constants.PREF_TOOLBAR;

public class MainActivity extends BaseActivity {
	private static final String[] STORAGE_PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

	private final ActivityResultLauncher<String[]> permissionsLauncher = registerForActivityResult(
			new ActivityResultContracts.RequestMultiplePermissions(),
			this::onPermissionResult);
	private final ActivityResultLauncher<String> openDirLauncher = registerForActivityResult(
			new PickDirResultContract(),
			this::onPickDirResult);

	private SharedPreferences preferences;
	private AppListModel appListModel;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (FileUtils.isExternalStorageLegacy()) {
			permissionsLauncher.launch(STORAGE_PERMISSIONS);
		}
		appListModel = new ViewModelProvider(this).get(AppListModel.class);
		if (savedInstanceState == null) {
			Intent intent = getIntent();
			Uri uri = null;
			if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
				uri = intent.getData();
			}
			AppsListFragment fragment = AppsListFragment.newInstance(uri);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.container, fragment).commit();
		}
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (!preferences.contains(PREF_TOOLBAR)) {
			boolean enable = !ViewConfiguration.get(this).hasPermanentMenuKey();
			preferences.edit().putBoolean(PREF_TOOLBAR, enable).apply();
		}
		boolean warningShown = preferences.getBoolean(PREF_STORAGE_WARNING_SHOWN, false);
		if (!FileUtils.isExternalStorageLegacy() && !warningShown) {
			showScopedStorageDialog();
			preferences.edit().putBoolean(PREF_STORAGE_WARNING_SHOWN, true).apply();
		}
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}

	private void checkAndCreateDirs() {
		String emulatorDir = Config.getEmulatorDir();
		File dir = new File(emulatorDir);
		if (dir.isDirectory() && dir.canWrite()) {
			FileUtils.initWorkDir(dir);
			appListModel.getAppRepository().onWorkDirReady();
			return;
		}
		if (dir.exists() || dir.getParentFile() == null || !dir.getParentFile().isDirectory()
				|| !dir.getParentFile().canWrite()) {
			alertDirCannotCreate(emulatorDir);
			return;
		}
		alertCreateDir();
	}

	private void alertDirCannotCreate(String emulatorDir) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.error)
				.setCancelable(false)
				.setMessage(getString(R.string.create_apps_dir_failed, emulatorDir))
				.setNegativeButton(R.string.exit, (d, w) -> finish())
				.setPositiveButton(R.string.choose, (d, w) -> openDirLauncher.launch(null))
				.show();
	}

	private void onPermissionResult(Map<String, Boolean> status) {
		if (!status.containsValue(false)) {
			checkAndCreateDirs();
		} else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			new AlertDialog.Builder(this)
					.setTitle(android.R.string.dialog_alert_title)
					.setCancelable(false)
					.setMessage(R.string.permission_request_failed)
					.setNegativeButton(R.string.retry, (d, w) ->
							permissionsLauncher.launch(STORAGE_PERMISSIONS))
					.setPositiveButton(R.string.exit, (d, w) -> finish())
					.show();
		} else {
			Toast.makeText(this, R.string.permission_request_failed, Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	private void showScopedStorageDialog() {
		String message = getString(R.string.scoped_storage_warning) + Config.getEmulatorDir();
		new AlertDialog.Builder(this)
				.setTitle(R.string.warning)
				.setCancelable(false)
				.setMessage(message)
				.setPositiveButton(android.R.string.ok, null)
				.show();
	}

	private void onPickDirResult(Uri uri) {
		if (uri == null) {
			checkAndCreateDirs();
			return;
		}
		File file = Utils.getFileForUri(uri);
		applyWorkDir(file);
	}

	private void alertCreateDir() {
		String emulatorDir = Config.getEmulatorDir();
		String lblChange = getString(R.string.change);
		String msg = getString(R.string.alert_msg_workdir_not_exists, emulatorDir, lblChange);
		new AlertDialog.Builder(this)
				.setTitle(android.R.string.dialog_alert_title)
				.setCancelable(false)
				.setMessage(msg)
				.setPositiveButton(R.string.create, (d, w) -> applyWorkDir(new File(emulatorDir)))
				.setNeutralButton(lblChange, (d, w) -> openDirLauncher.launch(emulatorDir))
				.setNegativeButton(R.string.exit, (d, w) -> finish())
				.show();
	}

	private void applyWorkDir(File file) {
		String path = file.getAbsolutePath();
		if (!FileUtils.initWorkDir(file)) {
			alertDirCannotCreate(path);
			return;
		}
		preferences.edit().putString(PREF_EMULATOR_DIR, path).apply();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Uri uri = intent.getData();
		if (uri != null) {
			InstallerDialog.newInstance(uri).show(getSupportFragmentManager(), "installer");
		}
	}
}
