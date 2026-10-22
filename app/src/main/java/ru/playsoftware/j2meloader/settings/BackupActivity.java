/*
 * Copyright 2026 Nikita Shakarun
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

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.databinding.ActivityBackupBinding;

public class BackupActivity extends AppCompatActivity {
	private static final String TAG = SettingsFragment.class.getSimpleName();

	private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
			new ActivityResultContracts.CreateDocument("application/zip"),
			this::onExportTargetPicked);
	private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
			new ActivityResultContracts.OpenDocument(),
			this::onImportFilePicked);

	private AlertDialog progressDialog;
	private ProgressBar progressBar;

	private final CompositeDisposable disposables = new CompositeDisposable();
	private final PublishSubject<Integer> progressSubject = PublishSubject.create();

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActivityBackupBinding binding = ActivityBackupBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		binding.backupExport.setOnClickListener(v -> openExportPicker());
		binding.backupImport.setOnClickListener(v -> openImportPicker());

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		setTitle(R.string.pref_backup);

		Disposable disposable = progressSubject
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(percent -> {
					if (progressBar != null) {
						progressBar.setProgress(percent);
					}
				});
		disposables.add(disposable);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		dismissProgress();
		disposables.clear();
		super.onDestroy();
	}

	private void openExportPicker() {
		String name = "J2ME-Loader-backup-"
				+ new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()) + ".zip";
		try {
			exportLauncher.launch(name);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, R.string.error_no_picker, Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Can't open file picker", e);
		}
	}

	public void triggerRebirth() {
		PackageManager packageManager = getPackageManager();
		Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
		ComponentName componentName = intent.getComponent();
		Intent mainIntent = Intent.makeRestartActivityTask(componentName);
		// Required for API 34 and later
		// Ref: https://developer.android.com/about/versions/14/behavior-changes-14#safer-intents
		mainIntent.setPackage(getPackageName());
		startActivity(mainIntent);
		Runtime.getRuntime().exit(0);
	}

	private void openImportPicker() {
		try {
			importLauncher.launch(new String[]{
					"application/zip",
					"application/x-zip-compressed",
					"application/octet-stream"
			});
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, R.string.error_no_picker, Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Can't open file picker", e);
		}
	}

	private void onExportTargetPicked(@Nullable Uri uri) {
		if (uri == null) {
			return;
		}
		Context context = getApplicationContext();
		showProgress(R.string.backup_export_title);
		disposables.add(Completable.fromAction(() ->
						BackupManager.exportToZip(context, uri, this::onBackupProgress))
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(() -> {
					dismissProgress();
					Toast.makeText(this, R.string.backup_export_done,
							Toast.LENGTH_SHORT).show();
				}, throwable -> onBackupError(throwable, R.string.backup_export_failed)));
	}

	private void onImportFilePicked(@Nullable Uri uri) {
		if (uri == null) {
			return;
		}
		new AlertDialog.Builder(this)
				.setTitle(R.string.pref_import_data)
				.setMessage(R.string.backup_import_confirm)
				.setPositiveButton(R.string.pref_import_data, (dialog, which) -> startImport(uri))
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void startImport(Uri uri) {
		Context context = getApplicationContext();
		showProgress(R.string.backup_import_title);
		disposables.add(Completable.fromAction(() ->
						BackupManager.importFromZip(context, uri, this::onBackupProgress))
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(() -> {
					dismissProgress();
					Toast.makeText(this, R.string.backup_import_done,
							Toast.LENGTH_SHORT).show();
					triggerRebirth();
				}, throwable -> onBackupError(throwable, R.string.backup_import_failed)));
	}

	private void onBackupProgress(int percent) {
		progressSubject.onNext(percent);
	}

	private void onBackupError(Throwable throwable, int messageRes) {
		Log.e(TAG, "Backup operation failed", throwable);
		dismissProgress();
		new AlertDialog.Builder(this)
				.setTitle(R.string.error)
				.setMessage(messageRes)
				.setPositiveButton(android.R.string.ok, null)
				.show();
	}

	private void showProgress(int titleRes) {
		View view = getLayoutInflater().inflate(R.layout.dialog_progress, null);
		progressBar = view.findViewById(R.id.progress_bar);
		progressBar.setProgress(0);
		progressDialog = new AlertDialog.Builder(this)
				.setTitle(titleRes)
				.setView(view)
				.setCancelable(false)
				.create();
		progressDialog.show();
	}

	private void dismissProgress() {
		if (progressDialog != null) {
			progressDialog.dismiss();
			progressDialog = null;
			progressBar = null;
		}
	}
}