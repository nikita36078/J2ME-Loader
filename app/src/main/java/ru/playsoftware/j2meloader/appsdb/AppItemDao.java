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

import java.util.ArrayList;
import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import io.reactivex.Flowable;
import ru.playsoftware.j2meloader.applist.AppItem;

@Dao
public interface AppItemDao {
	@Query("SELECT * FROM apps ORDER BY title COLLATE LOCALIZED ASC")
	Flowable<List<AppItem>> getAllByName();

	@Query("SELECT * FROM apps ORDER BY id ASC")
	Flowable<List<AppItem>> getAllByDate();

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	void insert(AppItem item);

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	void insertAll(ArrayList<AppItem> items);

	@Delete
	void delete(AppItem item);

	@Query("DELETE FROM apps")
	void deleteAll();
}