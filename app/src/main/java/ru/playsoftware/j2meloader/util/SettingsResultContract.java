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

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ru.playsoftware.j2meloader.settings.SettingsActivity;

import static ru.playsoftware.j2meloader.util.Constants.RESULT_NEED_RECREATE;

public class SettingsResultContract extends ActivityResultContract<Boolean, Boolean> {
	@NonNull
	@Override
	public Intent createIntent(@NonNull Context context, Boolean requestDir) {
		return new Intent(context, SettingsActivity.class);
	}

	@Override
	public Boolean parseResult(int resultCode, @Nullable Intent intent) {
		return resultCode == RESULT_NEED_RECREATE;
	}
}
