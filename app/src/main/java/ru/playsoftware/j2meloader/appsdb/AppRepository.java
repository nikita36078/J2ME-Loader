/*
 * Copyright 2018 Nikita Shakarun
 * Copyright 2020-2023 Yury Kharchenko
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
import static ru.playsoftware.j2meloader.util.Constants.PREF_EMULATOR_DIR;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.schedulers.Schedulers;
import ru.playsoftware.j2meloader.EmulatorApplication;
import ru.playsoftware.j2meloader.applist.AppItem;
import ru.playsoftware.j2meloader.applist.AppListModel;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.util.AppUtils;

public class AppRepository implements SharedPreferences.OnSharedPreferenceChangeListener {
	private final MutableLiveData<List<AppItem>> listLiveData = new MutableLiveData<>();
	private final MutableLiveData<Throwable> errorsLiveData = new MutableLiveData<>();
	private final CompositeDisposable compositeDisposable = new CompositeDisposable();
	private final ErrorObserver errorObserver = new ErrorObserver(errorsLiveData);
	private final MutableSortSQLiteQuery query = new MutableSortSQLiteQuery();

	private AppDatabase db;
	private AppItemDao appItemDao;

	public AppRepository(AppListModel model) {
		if (model.getAppRepository() != null) {
			throw new IllegalStateException("You must get instance from 'AppListModel'");
		}
		Context context = EmulatorApplication.getInstance();
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		preferences.registerOnSharedPreferenceChangeListener(this);
		String emulatorDir = Config.getEmulatorDir();
		File dir = new File(emulatorDir);
		if (dir.isDirectory() && dir.canWrite()) {
			initDb(emulatorDir);
		}
	}

	public void initDb(String path) {
		db = AppDatabase.open(path + Config.APPS_DB_NAME);
		appItemDao = db.appItemDao();
		ConnectableFlowable<List<AppItem>> listConnectableFlowable = getAll()
				.subscribeOn(Schedulers.io())
				.publish();
		compositeDisposable.add(listConnectableFlowable
				.firstElement()
				.subscribe(list -> AppUtils.updateDb(this, new ArrayList<>(list)), errorsLiveData::postValue));
		compositeDisposable.add(listConnectableFlowable.subscribe(listLiveData::postValue, errorsLiveData::postValue));
		compositeDisposable.add(listConnectableFlowable.connect());
	}

	public void observeApps(LifecycleOwner owner, Observer<List<AppItem>> observer) {
		listLiveData.observe(owner, observer);
	}

	private Flowable<List<AppItem>> getAll() {
		return appItemDao.getAll(query);
	}

	private void execute(Completable completable) {
		completable.subscribeOn(Schedulers.io()).subscribe(errorObserver);
	}

	public void insert(AppItem item) {
		execute(appItemDao.insert(item));
	}

	public void insert(List<AppItem> items) {
		execute(appItemDao.insert(items));
	}

	public void update(AppItem item) {
		execute(appItemDao.update(item));
	}

	public void delete(AppItem item) {
		execute(appItemDao.delete(item));
	}

	public void delete(List<AppItem> items) {
		execute(appItemDao.delete(items));
	}

	public void deleteAll() {
		execute(appItemDao.deleteAll());
	}

	public AppItem get(String name, String vendor) {
		return appItemDao.get(name, vendor);
	}

	public AppItem get(int id) {
		return appItemDao.get(id);
	}

	public void close() {
		if (db != null) {
			db.close();
		}
		compositeDisposable.clear();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		if (PREF_APP_SORT.equals(key)) {
			if (query.setSort(sp.getInt(PREF_APP_SORT, 0))) {
				Disposable disposable = appItemDao.getAllSingle(query)
						.subscribeOn(Schedulers.io())
						.subscribe(listLiveData::postValue, errorsLiveData::postValue);
				compositeDisposable.add(disposable);
			}
		} else if (PREF_EMULATOR_DIR.equals(key)) {
			String workDir = sp.getString(key, null);
			if (db != null) {
				if ((workDir + Config.APPS_DB_NAME).equals(db.getOpenHelper().getDatabaseName())) {
					return;
				}
				close();
			}
			initDb(workDir);
		}
	}

	public void observeErrors(LifecycleOwner owner, Observer<Throwable> observer) {
		errorsLiveData.observe(owner, observer);
	}

	public void onWorkDirReady() {
		if (db == null) {
			initDb(Config.getEmulatorDir());
		}
	}

	private static class ErrorObserver implements CompletableObserver {
		private final MutableLiveData<Throwable> callback;

		public ErrorObserver(MutableLiveData<Throwable> callback) {
			this.callback = callback;
		}

		@Override
		public void onSubscribe(@NotNull Disposable d) {
		}

		@Override
		public void onComplete() {
		}

		@Override
		public void onError(@NotNull Throwable e) {
			callback.postValue(e);
		}
	}
}
