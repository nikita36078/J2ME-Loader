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

package ru.playsoftware.j2meloader.filepicker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nononsenseapps.filepicker.AbstractFilePickerActivity;
import com.nononsenseapps.filepicker.AbstractFilePickerFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilteredFilePickerActivity extends AbstractFilePickerActivity<File> {

	@Override
	protected AbstractFilePickerFragment<File> getFragment(@Nullable String startPath, int mode, boolean allowMultiple,
														   boolean allowCreateDir, boolean allowExistingFile, boolean singleClick) {
		FilteredFilePickerFragment fragment = new FilteredFilePickerFragment();
		fragment.setArgs(startPath, mode, allowMultiple, allowCreateDir, allowExistingFile, singleClick);
		return fragment;
	}

	@Override
	public void onFilesPicked(@NonNull List<Uri> files) {
		// Don't put uris into ClipData: it causes FileUriExposedException
		Intent i = new Intent();
		i.putExtra(EXTRA_ALLOW_MULTIPLE, true);
		ArrayList<String> paths = new ArrayList<>();
		for (Uri file : files) {
			paths.add(file.toString());
		}
		i.putStringArrayListExtra(EXTRA_PATHS, paths);
		setResult(Activity.RESULT_OK, i);
		finish();
	}
}
