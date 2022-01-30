/*
 *  Copyright 2021 Yury Kharchenko
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

package ru.playsoftware.j2meloader.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nononsenseapps.filepicker.FilePickerActivity;

import ru.playsoftware.j2meloader.filepicker.FilteredFilePickerActivity;

public class PickFileResultContract extends ActivityResultContract<String, Uri> {
	@NonNull
	@Override
	public Intent createIntent(@NonNull Context context, String input) {
		Intent i = new Intent(context, FilteredFilePickerActivity.class);
		i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
		i.putExtra(FilePickerActivity.EXTRA_SINGLE_CLICK, true);
		i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
		i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
		i.putExtra(FilePickerActivity.EXTRA_START_PATH, input);
		return i;
	}

	@Override
	public Uri parseResult(int resultCode, @Nullable Intent intent) {
		if (resultCode == Activity.RESULT_OK && intent != null) {
			return intent.getData();
		}
		return null;
	}
}
