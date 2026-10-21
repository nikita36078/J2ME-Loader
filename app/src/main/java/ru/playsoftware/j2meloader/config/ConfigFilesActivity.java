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

package ru.playsoftware.j2meloader.config;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.databinding.ActivityConfigFilesBinding;
import ru.playsoftware.j2meloader.databinding.ItemConfigFileBinding;
import ru.playsoftware.j2meloader.util.FileUtils;
import ru.playsoftware.j2meloader.util.SAFFileResultContract;

public class ConfigFilesActivity extends AppCompatActivity {
	protected enum FileType {
		SHADERS,
		SOUNDBANKS,
	}

	public static final String EXTRA_TYPE = "type";
	private static final String TAG = ConfigFilesActivity.class.getSimpleName();

	private final ActivityResultLauncher<String> addFileLauncher =
			registerForActivityResult(new SAFFileResultContract(), this::onFilesPicked);

	private ActivityConfigFilesBinding binding;
	private File dir;
	private FileAdapter adapter;
	private final CompositeDisposable disposables = new CompositeDisposable();

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FileType type = (FileType) getIntent().getSerializableExtra(EXTRA_TYPE);
		if (type == null) {
			finish();
			return;
		}
		String path;
		if (type == FileType.SHADERS) {
			setTitle(R.string.shaders);
			path = Config.getShadersDir();
		} else {
			setTitle(R.string.soundbanks);
			path = Config.getSoundbanksDir();
		}

		dir = new File(path);
		if (!dir.isDirectory() && !dir.mkdirs()) {
			Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		binding = ActivityConfigFilesBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		adapter = new FileAdapter();
		binding.fileList.setAdapter(adapter);
		binding.fileList.setEmptyView(binding.empty);
		binding.addFile.setOnClickListener(v -> openFilePicker());
		loadFiles();
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
		disposables.clear();
		super.onDestroy();
	}

	private void openFilePicker() {
		try {
			addFileLauncher.launch("");
		} catch (Exception e) {
			Toast.makeText(this, R.string.error_no_picker, Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Can't open file picker", e);
		}
	}

	private void onFilesPicked(@Nullable List<Uri> uris) {
		if (uris == null || uris.isEmpty()) {
			return;
		}
		setResult(RESULT_OK);
		binding.addFile.setEnabled(false);
		disposables.add(Completable.fromCallable(() -> {
					for (Uri uri : uris) {
						String name = FileUtils.getDisplayName(this, uri);
						File dest = getUniqueFile(name);
						FileUtils.copyFileFromUri(this, uri, dest);
					}
					return true;
				})
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(() -> {
					binding.addFile.setEnabled(true);
					setResult(RESULT_OK);
					loadFiles();
				}, throwable -> {
					binding.addFile.setEnabled(true);
					Log.e(TAG, "Failed to add files", throwable);
					Toast.makeText(ConfigFilesActivity.this, R.string.error,
							Toast.LENGTH_SHORT).show();
				}));
	}

	private File getUniqueFile(String name) {
		File file = new File(dir, name);
		if (!file.exists()) {
			return file;
		}
		int dot = name.lastIndexOf('.');
		String base = dot > 0 ? name.substring(0, dot) : name;
		String extension = dot > 0 ? name.substring(dot) : "";
		for (int i = 1; ; i++) {
			file = new File(dir, base + " (" + i + ")" + extension);
			if (!file.exists()) {
				return file;
			}
		}
	}

	private void loadFiles() {
		File[] files = dir.listFiles(File::isFile);
		List<File> list = new ArrayList<>();
		if (files != null) {
			Collections.addAll(list, files);
		}
		Collections.sort(list);
		adapter.setNotifyOnChange(false);
		adapter.clear();
		adapter.addAll(list);
		adapter.notifyDataSetChanged();
		adapter.setNotifyOnChange(true);
	}

	private void confirmDelete(File file) {
		if (file == null) {
			return;
		}
		new AlertDialog.Builder(this)
				.setTitle(R.string.delete_file_title)
				.setMessage(getString(R.string.delete_file_message, file.getName()))
				.setPositiveButton(R.string.action_context_delete, (dialog, which) -> {
					if (file.delete()) {
						setResult(RESULT_OK);
						loadFiles();
					} else {
						Toast.makeText(this, getString(R.string.error_delete_file, file.getName()),
								Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private class FileAdapter extends ArrayAdapter<File> {
		FileAdapter() {
			super(ConfigFilesActivity.this, R.layout.item_config_file, new ArrayList<>());
		}

		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				ItemConfigFileBinding binding = ItemConfigFileBinding.inflate(getLayoutInflater(), parent, false);
				convertView = binding.getRoot();
				holder = new ViewHolder(binding);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			File file = getItem(position);
			holder.name.setText(file != null ? file.getName() : "");
			holder.delete.setOnClickListener(v -> confirmDelete(file));
			return convertView;
		}
	}

	private static class ViewHolder {
		final TextView name;
		final ImageButton delete;

		ViewHolder(ItemConfigFileBinding binding) {
			name = binding.fileName;
			delete = binding.deleteFile;
		}
	}
}
