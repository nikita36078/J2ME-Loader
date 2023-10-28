/*
 * Copyright 2023 Yury Kharchenko
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

package ru.playsoftware.j2meloader.appsdb;

import static ru.playsoftware.j2meloader.util.Constants.PREF_APP_SORT;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.sqlite.db.SupportSQLiteProgram;
import androidx.sqlite.db.SupportSQLiteQuery;

import ru.playsoftware.j2meloader.EmulatorApplication;
import ru.playsoftware.j2meloader.R;

class MutableSortSQLiteQuery implements SupportSQLiteQuery {
	private static final String SELECT = "SELECT * FROM apps ORDER BY ";
	private final String[] orderTerms;
	private int sortVariant;

	MutableSortSQLiteQuery() {
		Context context = EmulatorApplication.getInstance();
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		try {
			sortVariant = preferences.getInt(PREF_APP_SORT, 0);
		} catch (Exception e) {
			sortVariant = preferences.getString(PREF_APP_SORT, "name").equals("name") ? 0 : 1;
			preferences.edit().putInt(PREF_APP_SORT, sortVariant).apply();
		}
		orderTerms = context.getResources().getStringArray(R.array.pref_app_sort_values);
	}

	@NonNull
	@Override
	public String getSql() {
		int sortVariant = this.sortVariant;
		String order = sortVariant >= 0 ? " ASC" : " DESC";
		return SELECT + String.format(orderTerms[sortVariant & 0x7FFFFFFF], order);
	}

	@Override
	public void bindTo(@NonNull SupportSQLiteProgram statement) {
	}

	@Override
	public int getArgCount() {
		return 0;
	}

	boolean setSort(int variant) {
		if (variant == sortVariant) {
			return false;
		}
		sortVariant = variant;
		return true;
	}
}
