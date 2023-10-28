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

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import ru.playsoftware.j2meloader.applist.AppItem;

@Dao
public interface AppItemDao {

	@RawQuery(observedEntities = AppItem.class)
	Flowable<List<AppItem>> getAll(SupportSQLiteQuery query);

	@RawQuery(observedEntities = AppItem.class)
	Single<List<AppItem>> getAllSingle(SupportSQLiteQuery query);

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	Completable insert(AppItem item);

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	Completable insert(List<AppItem> items);

	@Update
	Completable update(AppItem item);

	@Delete
	Completable delete(AppItem item);

	@Delete
	Completable delete(List<AppItem> items);

	@Query("DELETE FROM apps")
	Completable deleteAll();

	@Query("SELECT * FROM apps WHERE title = :name AND author = :vendor")
	AppItem get(String name, String vendor);

	@Query("SELECT * FROM apps WHERE id = :id")
	AppItem get(int id);
}