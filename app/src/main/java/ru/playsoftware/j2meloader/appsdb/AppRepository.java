/*
 * Copyright 2018 Nikita Shakarun
 * Copyright 2021 Yury Kharchenko
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

import android.annotation.SuppressLint;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import androidx.sqlite.db.SupportSQLiteProgram;
import androidx.sqlite.db.SupportSQLiteQuery;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.applist.AppItem;

public class AppRepository {
	private final AppItemDao appItemDao;
	private final String[] orderTerms;
	private boolean isClose;
	private int sortVariant;
	private final AppDatabase db;

	public AppRepository(Context context) {
		db = AppDatabase.getDatabase(context);
		appItemDao = db.appItemDao();
		orderTerms = context.getResources().getStringArray(R.array.pref_app_sort_values);
	}

	public AppRepository(Context context, int sort) {
		this(context);
		sortVariant = sort;
	}

	public Flowable<List<AppItem>> getAll() {
		return appItemDao.getAll(new MutableSortSQLiteQuery());
	}

	public void insert(AppItem item) {
		Completable.fromAction(() -> appItemDao.insert(item))
				.subscribeOn(Schedulers.io())
				.subscribe();
	}

	public void insertAll(ArrayList<AppItem> items) {
		Completable.fromAction(() -> appItemDao.insertAll(items))
				.subscribeOn(Schedulers.io())
				.subscribe();
	}

	public void update(AppItem item) {
		Completable.fromAction(() -> appItemDao.update(item))
				.subscribeOn(Schedulers.io())
				.subscribe();
	}

	public void delete(AppItem item) {
		Completable.fromAction(() -> appItemDao.delete(item))
				.subscribeOn(Schedulers.io())
				.subscribe();
	}

	public void deleteAll() {
		Completable.fromAction(appItemDao::deleteAll)
				.subscribeOn(Schedulers.io())
				.subscribe();
	}

	public AppItem get(String name, String vendor) {
		return appItemDao.get(name, vendor);
	}

	@Override
	protected void finalize() {
		if (!isClose)
			AppDatabase.closeInstance();
	}

	public void close() {
		AppDatabase.closeInstance();
		isClose = true;
	}

	public int getSort() {
		return sortVariant;
	}

	@SuppressLint({"RestrictedApi", "VisibleForTests"})
	public int setSort(int variant) {
		if (this.sortVariant == variant) {
			variant |= 0x80000000;
		}
		this.sortVariant = variant;
		db.getInvalidationTracker().notifyObserversByTableNames("apps");
		return variant;
	}

	private class MutableSortSQLiteQuery implements SupportSQLiteQuery {
		private static final String SELECT = "SELECT * FROM apps ORDER BY ";

		@Override
		public String getSql() {
			String order = sortVariant >= 0 ? " ASC" : " DESC";
			return SELECT + String.format(orderTerms[sortVariant & 0x7FFFFFFF], order);
		}

		@Override
		public void bindTo(SupportSQLiteProgram statement) {
		}

		@Override
		public int getArgCount() {
			return 0;
		}
	}
}
