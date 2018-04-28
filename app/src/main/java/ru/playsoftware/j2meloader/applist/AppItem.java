/*
 * Copyright 2015-2016 Nickolay Savchenko
 * Copyright 2017-2018 Nikita Shakarun
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

package ru.playsoftware.j2meloader.applist;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.content.Context;

import javax.microedition.shell.ConfigActivity;

import ru.playsoftware.j2meloader.R;

@Entity(tableName = "apps", indices = {@Index(value = {"path"}, unique = true)})
public class AppItem {
	@PrimaryKey(autoGenerate = true)
	private int id;
	private String imagePath;
	private String title;
	private String author;
	private String version;
	@ColumnInfo(name = "path")
	private String path;

	public AppItem(String path, String title, String author, String version) {
		this.path = path;
		this.title = title;
		this.author = author;
		this.version = version;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getPath() {
		return path;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	public String getAuthor() {
		return author;
	}

	public String getVersion() {
		return version;
	}

	public String getPathExt() {
		return ConfigActivity.APP_DIR + path;
	}

	public void setImagePathExt(String imagePath) {
		if (imagePath.charAt(0) != '/') {
			imagePath = "/" + imagePath;
		}
		this.imagePath = path + ConfigActivity.MIDLET_RES_DIR + imagePath.replace(" ", "");
	}

	public String getImagePathExt() {
		return ConfigActivity.APP_DIR + imagePath;
	}

	public String getAuthorExt(Context context) {
		return context.getString(R.string.author) + author;
	}

	public String getVersionExt(Context context) {
		return context.getString(R.string.version) + version;
	}

}
