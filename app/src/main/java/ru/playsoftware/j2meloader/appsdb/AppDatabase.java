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

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import ru.playsoftware.j2meloader.applist.AppItem;

@Database(entities = {AppItem.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
	private static AppDatabase instance;

	public abstract AppItemDao appItemDao();

	static AppDatabase getDatabase(Context context) {
		if (instance == null) {
			synchronized (AppDatabase.class) {
				if (instance == null) {
					instance = Room.databaseBuilder(context.getApplicationContext(),
							AppDatabase.class, "apps-database.db").build();
				}
			}
		}
		return instance;
	}
}
