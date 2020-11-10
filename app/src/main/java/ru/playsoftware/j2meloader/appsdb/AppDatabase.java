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

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import ru.playsoftware.j2meloader.applist.AppItem;
import ru.playsoftware.j2meloader.config.Config;

@Database(entities = {AppItem.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
	private static AppDatabase instance;
	private static volatile int openCount;

	public abstract AppItemDao appItemDao();

	static synchronized AppDatabase getDatabase(Context context) {
		openCount++;
		if (instance == null) {
			instance = Room.databaseBuilder(context.getApplicationContext(),
					AppDatabase.class, Config.getEmulatorDir() + "/J2ME-apps.db")
					.build();
		}
		return instance;
	}

	static synchronized void closeInstance() {
		if (--openCount > 0) return;
		instance.close();
		instance = null;
	}

	public static synchronized void closeQuietly() {
		openCount = 0;
		if (instance != null) {
			instance.close();
			instance = null;
		}
	}
}
