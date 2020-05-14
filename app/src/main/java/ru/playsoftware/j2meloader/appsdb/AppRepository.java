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

package ru.playsoftware.j2meloader.appsdb;

import android.app.Application;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import ru.playsoftware.j2meloader.applist.AppItem;

public class AppRepository {

	private AppItemDao appItemDao;
	private boolean appDateSort;

	public AppRepository(Application application, boolean dateSort) {
		AppDatabase db = AppDatabase.getDatabase(application);
		appDateSort = dateSort;
		appItemDao = db.appItemDao();
	}

	public Flowable<List<AppItem>> getAll() {
		if (appDateSort) {
			return appItemDao.getAllByDate();
		} else {
			return appItemDao.getAllByName();
		}
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

	public void delete(AppItem item) {
		Completable.fromAction(() -> appItemDao.delete(item))
				.subscribeOn(Schedulers.io())
				.subscribe();
	}

	public void deleteAll() {
		Completable.fromAction(() -> appItemDao.deleteAll())
				.subscribeOn(Schedulers.io())
				.subscribe();
	}

}
