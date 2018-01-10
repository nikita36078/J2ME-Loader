/*
 * Copyright 2015-2016 Nickolay Savchenko
 * Copyright 2017 Nikita Shakarun
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

package ua.naiksoftware.j2meloader;

import java.io.Serializable;

public class AppItem implements Serializable {

	private String imagePath;
	private String title;
	private String author;
	private String version;
	private String path;

	public AppItem(String imagePath_, String title_, String author_, String version_) {
		imagePath = imagePath_;
		title = title_;
		author = author_;
		version = version_;
	}

	public void setPath(String p) {
		path = p;
		setImagePath();
	}

	public String getPath() {
		return path;
	}

	public void setTitle(String title_) {
		title = title_;
	}

	public String getTitle() {
		return title;
	}

	public void setImagePath() {
		String resString = "/res";
		imagePath = imagePath.replace(" ", "");
		if (!imagePath.contains("/")) {
			resString += "/";
		}
		imagePath = path + resString + imagePath;
	}

	public String getImagePath() {
		return imagePath;
	}

	public String getAuthor() {
		return author;
	}

	public String getVersion() {
		return version;
	}

}
