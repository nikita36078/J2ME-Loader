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

package ru.playsoftware.j2meloader.applist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import ru.playsoftware.j2meloader.appsdb.AppRepository;

public class AppListModel extends AndroidViewModel {
	private final AppRepository appRepository;

	public AppListModel(@NonNull Application application) {
		super(application);
		appRepository = new AppRepository(this);
	}

	public AppRepository getAppRepository() {
		return appRepository;
	}

	@Override
	protected void onCleared() {
		appRepository.close();
	}
}
